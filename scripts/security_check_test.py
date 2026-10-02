"""Regression checks for audit decisions and secret-safe output."""
import contextlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('security_check', Path(__file__).with_name('security_check.py'))
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class AuditDecisionsTest(unittest.TestCase):
    def test_export_rejects_symlink_parent_and_path_escape(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / 'repo'
            outside = Path(tmp) / 'outside'
            root.mkdir()
            outside.mkdir()
            (outside / 'credential.txt').write_text('do not copy')
            (root / 'nested').symlink_to(outside, target_is_directory=True)
            with self.assertRaises(ValueError):
                module.safe_source_path(root, Path('nested/credential.txt'))
            with self.assertRaises(ValueError):
                module.safe_source_path(root, Path('../outside/credential.txt'))

    def test_shallow_history_is_an_error(self):
        from types import SimpleNamespace
        def execute(command, **kwargs):
            if '--is-shallow-repository' in command:
                kwargs['stdout'].write(b'true\n')
            return SimpleNamespace(returncode=0)
        with tempfile.TemporaryDirectory() as tmp, patch.object(module, 'ROOT', Path(tmp)), \
                patch('sys.argv', ['security_check.py', '--history-only']), \
                patch.object(module.subprocess, 'run', side_effect=execute):
            with contextlib.redirect_stdout(io.StringIO()):
                result = module.main()
            self.assertEqual(1, result)
            rows = json.loads(next(Path(tmp).rglob('summary.json')).read_text())['checks']
            self.assertTrue(any(x.get('reason') == 'shallow history' for x in rows))

    def test_unknown_severity_and_high_database_rating_are_blocking(self):
        data = {'results': [{'packages': [{'package': {'name': 'example', 'version': '1'},
                'vulnerabilities': [{'id': 'GHSA-example', 'database_specific': {'severity': 'HIGH'}}],
                'groups': [{'ids': ['GHSA-example'], 'max_severity': '0.0'},
                           {'ids': ['unknown']}, {'ids': ['moderate'], 'max_severity': '5.1'}]}]}]}
        blocked, warnings = module.classify_osv(data)
        self.assertEqual(2, len(blocked))
        self.assertEqual(1, len(warnings))

    def test_missing_osv_structure_is_an_error(self):
        with self.assertRaises(KeyError):
            module.classify_osv({'error': 'network failure'})

    def test_only_exact_nonsecret_constant_is_dismissed(self):
        with tempfile.TemporaryDirectory() as tmp:
            file = Path(tmp) / 'backend/common/src/main/java/com/share/rental/common/redis/RedisKey.java'
            file.parent.mkdir(parents=True)
            file.write_text('public static final String REFRESH_TOKEN = "sr:auth:refresh:";\n')
            finding = {'target': str(file), 'line': 1, 'detection_rule': 'hardcoded_secret_assignment', 'severity': 'critical'}
            active, dismissed = module.adjudicate_deepsec({'findings': [finding]})
            self.assertEqual((0, 1), (len(active), len(dismissed)))
            file.write_text('public static final String OTHER_TOKEN = "real-secret-placeholder";\n')
            active, dismissed = module.adjudicate_deepsec({'findings': [finding]})
            self.assertEqual((1, 0), (len(active), len(dismissed)))
            finding['severity'] = 'unexpected'
            with self.assertRaises(ValueError):
                module.adjudicate_deepsec({'findings': [finding]})

    def test_tool_failure_returns_nonzero_without_echoing_output(self):
        from types import SimpleNamespace
        with tempfile.TemporaryDirectory() as tmp, patch.object(module, 'ROOT', Path(tmp)), \
                patch('sys.argv', ['security_check.py', '--history-only']), \
                patch.object(module.subprocess, 'run', return_value=SimpleNamespace(returncode=2)):
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                result = module.main()
            self.assertEqual(1, result)
            self.assertIn('ERROR', output.getvalue())
            summaries = list(Path(tmp).rglob('summary.json'))
            self.assertEqual('ERROR', json.loads(summaries[0].read_text())['checks'][0]['status'])


if __name__ == '__main__':
    unittest.main()
