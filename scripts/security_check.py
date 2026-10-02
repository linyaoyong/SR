#!/usr/bin/env python3
"""SR security checks. Raw output stays private; never print scanner evidence."""
import argparse
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import tomllib

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.security-tools'


def read_json(path):
    return json.loads(path.read_text(encoding='utf-8'))


def safe_source_path(root, relative):
    if relative.is_absolute() or '..' in relative.parts:
        raise ValueError('outside repository')
    current = root
    for part in relative.parts:
        current /= part
        if current.is_symlink():
            raise ValueError('symlink needs manual review')
    current.resolve().relative_to(root.resolve())
    return current


def classify_osv(data):
    """OSV groups consolidate aliases and provide the scanner's CVSS score."""
    blockers, warnings = [], []
    for result in data['results']:
        for package in result.get('packages', []):
            identity = package['package']
            if package.get('vulnerabilities') and not package.get('groups'):
                raise ValueError('Advisories without classification groups')
            severity = {v['id']: v.get('database_specific', {}).get('severity', '').upper()
                        for v in package.get('vulnerabilities', [])}
            for group in package.get('groups', []):
                score = group.get('max_severity')
                # Unknown severity requires triage; a known lower score is a warning.
                score = float(score) if score not in (None, '') else None
                row = {'package': identity['name'], 'version': identity['version'],
                       'ids': group['ids'], 'score': score}
                high = any(severity.get(i) in ('HIGH', 'CRITICAL') for i in group['ids'])
                (blockers if high or score is None or score >= 7 else warnings).append(row)
    return blockers, warnings


def adjudicate_deepsec(data):
    # Exact constant values only. Do not suppress a rule or whole source file.
    policy = tomllib.loads((ROOT / '.security-policy.toml').read_text())
    exceptions = {(x['file'], x['rule'], x['line']): x['reason'] for x in policy['deepsec']}
    findings, dismissed = [], []
    for finding in data['findings']:
        path = Path(finding['target'])
        # Scanned export retains repository-relative layout.
        parts = path.parts
        relative = next(('/'.join(parts[i:]) for i, v in enumerate(parts)
                         if v in ('backend', 'frontend', 'admin-frontend', 'scripts')), str(path))
        rule = finding['detection_rule']
        severity = finding['severity'].lower()
        if severity not in ('critical', 'high', 'medium', 'low', 'info'):
            raise ValueError('Unknown scanner severity')
        row = {'file': relative, 'line': finding.get('line'), 'rule': rule, 'severity': severity}
        line = finding.get('line')
        source = path.read_text(encoding='utf-8').splitlines()
        reason = exceptions.get((relative, rule, source[line - 1].strip())) if isinstance(line, int) and 0 < line <= len(source) else None
        if reason:
            row['reason'] = reason
            dismissed.append(row)
        else:
            findings.append(row)
    return findings, dismissed


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--history-only', action='store_true', help='redacted history scan only')
    args = parser.parse_args()
    os.umask(0o077)
    run_id = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    out = ROOT / '.security-reports' / run_id
    out.mkdir(parents=True)
    rows = []
    # Give scanners/builds only operational variables, never ambient API tokens or DB credentials.
    allowed = ('PATH', 'JAVA_HOME', 'TMPDIR', 'TEMP', 'TMP', 'HOME', 'SYSTEMROOT',
               'HTTPS_PROXY', 'HTTP_PROXY', 'ALL_PROXY', 'NO_PROXY',
               'https_proxy', 'http_proxy', 'all_proxy', 'no_proxy', 'CI')
    env = {key: os.environ[key] for key in allowed if key in os.environ}
    env.update(DEEPSEC_CONFIG_DIR=str(out / 'deepsec-config'),
               npm_config_cache=str(TOOLS / 'npm-cache'),
               SEMGREP_LOG_FILE=str(out / 'semgrep.log'),
               SEMGREP_SETTINGS_FILE=str(out / 'semgrep-settings.yml'),
               SEMGREP_SEND_METRICS='off',
               SPRING_CLOUD_NACOS_DISCOVERY_ENABLED='false',
               SPRING_CLOUD_NACOS_CONFIG_ENABLED='false',
               SPRING_MAIN_LAZY_INITIALIZATION='true',
               SPRING_CONFIG_IMPORT='optional:classpath:/audit-test.yml',
               SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:23306/audit_unused',
               SPRING_DATASOURCE_PASSWORD='audit-unused-dummy',
               SPRING_DATA_REDIS_PORT='26379', SPRING_RABBITMQ_PORT='25672',
               SR_JWT_SECRET='audit-only-jwt-secret-at-least-32-bytes-long',
               SR_INTERNAL_TOKEN='audit-only-internal-transport-token')

    def binary(name):
        explicit = os.environ.get('SECURITY_' + name.upper().replace('-', '_'))
        local = TOOLS / ('venv/bin/' if name in ('semgrep', 'deepsec') else 'bin/') / name
        return explicit or (str(local) if local.is_file() else name)

    def run(name, command, cwd=ROOT, acceptable=(0,)):
        try:
            with (out / (name + '.log')).open('wb') as log, (out / (name + '.stderr.log')).open('wb') as errors:
                result = subprocess.run(command, cwd=cwd, env=env, stdout=log,
                                        stderr=errors, timeout=1800)
            if result.returncode not in acceptable:
                rows.append({'check': name, 'status': 'ERROR', 'exit': result.returncode})
                print(name + ': ERROR (see private log)', flush=True)
                return None
            return result.returncode
        except (OSError, subprocess.TimeoutExpired):
            rows.append({'check': name, 'status': 'ERROR'})
            print(name + ': ERROR (tool missing or timed out)', flush=True)
            return None

    def report(name, blockers=(), warnings=(), dismissed=()):
        status = 'FINDINGS' if blockers else ('WARNING' if warnings else 'PASS')
        rows.append({'check': name, 'status': status, 'blocking': list(blockers),
                     'warnings': list(warnings), 'dismissed': list(dismissed)})
        print(f'{name}: {status}; blocking={len(blockers)}, warnings={len(warnings)}', flush=True)

    def inspect(name, callback):
        try:
            callback()
            return True
        except (ValueError, KeyError, TypeError, OSError):
            rows.append({'check': name, 'status': 'ERROR', 'reason': 'invalid or missing report'})
            print(name + ': ERROR (invalid or missing report)', flush=True)
            return False

    def history():
        name = 'secrets-history'
        if run('git-history-check', ['git', 'rev-parse', '--verify', 'HEAD']) is None:
            return
        if run('git-history-depth', ['git', 'rev-parse', '--is-shallow-repository']) is None:
            return
        if (out / 'git-history-depth.log').read_text().strip() != 'false':
            rows.append({'check': name, 'status': 'ERROR', 'reason': 'shallow history'})
            print(name + ': ERROR (fetch full history before scanning)', flush=True)
            return
        target = out / (name + '.json')
        if run(name, [binary('gitleaks'), 'git', str(ROOT), '--log-opts=--all',
                      '--config', str(ROOT / '.gitleaks.toml'), '--redact=100', '--no-banner',
                      '--report-format=json', '--report-path', str(target)], acceptable=(0, 1)) is not None:
            inspect(name, lambda: report(name, [{k: x.get(k) for k in ('RuleID', 'File', 'StartLine', 'Commit')}
                                               for x in read_json(target)]))

    if args.history_only:
        history()
    else:
        # Export only existing tracked + nonignored untracked files, never node_modules/logs/uploads.
        # A tracked file remains scanned even when its path matches .gitignore.
        with tempfile.TemporaryDirectory(prefix='sr-security-') as temporary:
            export = Path(temporary)
            names = subprocess.check_output(['git', 'ls-files', '-z', '-c', '-o', '--exclude-standard'], cwd=ROOT).split(b'\0')
            for raw in names:
                if not raw:
                    continue
                relative = Path(os.fsdecode(raw))
                try:
                    source = safe_source_path(ROOT, relative)
                except ValueError:
                    # Do not silently scan arbitrary files outside the checkout.
                    rows.append({'check': 'source-export', 'status': 'ERROR', 'file': str(relative), 'reason': 'symlink needs manual review'})
                    continue
                if source.is_file():
                    target = export / relative
                    target.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copyfile(source, target)
            target = out / 'semgrep.json'
            if run('semgrep', [binary('semgrep'), 'scan', '--config=p/java', '--config=p/typescript',
                               '--config=p/owasp-top-ten', '--metrics=off', '--disable-version-check',
                               '--json', '--output', str(target), str(export)], acceptable=(0, 1)) is not None:
                def semgrep():
                    data = read_json(target)
                    scanned = data.get('paths', {}).get('scanned', [])
                    if not scanned or not any(x.endswith('.java') for x in scanned) or not any(x.endswith(('.ts', '.tsx')) for x in scanned):
                        raise ValueError('Missing Java/TypeScript scan coverage')
                    findings = [{'file': str(Path(x['path']).relative_to(export)), 'line': x['start']['line'],
                                 'rule': x['check_id'], 'severity': x['extra']['severity']}
                                for x in data['results']]
                    if any(x['severity'] not in ('ERROR', 'WARNING', 'INFO') for x in findings):
                        raise ValueError('Unknown scanner severity')
                    report('semgrep', [x for x in findings if x['severity'] == 'ERROR'] +
                           [{'reason': 'scanner error', 'code': x.get('code')} for x in data.get('errors', []) if x.get('level') != 'warn'],
                           [x for x in findings if x['severity'] != 'ERROR'] +
                           [{'reason': 'partial parsing; review unparsed source manually', 'file': str(Path(x['path']).relative_to(export))}
                            for x in data.get('errors', []) if x.get('level') == 'warn' and x.get('path')])
                inspect('semgrep', semgrep)
            for layer in ('l1', 'l2'):
                name = 'deepsec-' + layer
                target = out / (name + '.json')
                if run(name, [binary('deepsec'), 'shield', 'scan', str(export), '--layer', layer,
                              '--no-remote-l3', '--format=json', '--output', str(target)], acceptable=(0, 2)) is not None:
                    def deepsec():
                        data = read_json(target)
                        # Shield 0.2.0 CLI emits only {findings}; its library metadata is
                        # not present in CLI JSON. Check nonempty relevant input ourselves.
                        if not any(export.rglob('*.java')) or not any(export.rglob('*.tsx')):
                            raise ValueError('missing relevant source input')
                        if 'filesScanned' in data and data['filesScanned'] == 0:
                            raise ValueError('empty scan')
                        findings, dismissed = adjudicate_deepsec(data)
                        report(name, [x for x in findings if x['severity'] in ('high', 'critical')],
                               [x for x in findings if x['severity'] not in ('high', 'critical')], dismissed)
                    inspect(name, deepsec)
            for folder in ('frontend', 'admin-frontend'):
                name = 'npm-audit-' + folder
                target = out / (name + '.log')
                if run(name, ['npm', 'audit', '--package-lock-only', '--ignore-scripts', '--audit-level=high', '--json'],
                       ROOT / folder, acceptable=(0, 1)) is not None:
                    def npm_audit():
                        data = read_json(target)
                        if 'error' in data or 'vulnerabilities' not in data:
                            raise ValueError('audit failed')
                        findings = [{'package': k, 'severity': v['severity']} for k, v in data['vulnerabilities'].items()]
                        if any(x['severity'] not in ('critical', 'high', 'moderate', 'low', 'info') for x in findings):
                            raise ValueError('Unknown scanner severity')
                        report(name, [x for x in findings if x['severity'] in ('high', 'critical')],
                               [x for x in findings if x['severity'] not in ('high', 'critical')])
                    inspect(name, npm_audit)
            maven = ['mvn', '-B']
            if os.environ.get('SECURITY_MAVEN_SETTINGS'):
                maven += ['-s', os.environ['SECURITY_MAVEN_SETTINGS']]
            if os.environ.get('SECURITY_MAVEN_REPO'):
                maven += ['-Dmaven.repo.local=' + os.environ['SECURITY_MAVEN_REPO']]
            if run('maven-sbom', maven + ['org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom',
                                       '-DoutputFormat=json', '-DincludeTestScope=true', '-DoutputDirectory=' + str(out / 'sbom')], ROOT / 'backend') is not None:
                target = out / 'maven-osv.json'
                bom = out / 'sbom/bom.json'
                def validate_bom():
                    if not read_json(bom).get('components'):
                        raise ValueError('Empty aggregate dependency graph')
                if inspect('maven-sbom', validate_bom) and run('maven-osv', [binary('osv-scanner'), 'scan', 'source', '--lockfile', str(bom),
                                    '--format=json', '--output-file', str(target)], acceptable=(0, 1)) is not None:
                    inspect('maven-osv', lambda: report('maven-osv', *classify_osv(read_json(target))))
            else:
                rows.append({'check': 'maven-osv', 'status': 'SKIPPED', 'reason': 'SBOM generation failed'})
            target = out / 'secrets-current.json'
            if run('secrets-current', [binary('gitleaks'), 'dir', str(export), '--config', str(ROOT / '.gitleaks.toml'),
                                       '--redact=100', '--no-banner', '--report-format=json', '--report-path', str(target)], acceptable=(0, 1)) is not None:
                inspect('secrets-current', lambda: report('secrets-current',
                        [{'RuleID': x.get('RuleID'), 'File': str(Path(x['File']).relative_to(export))
                          if Path(x['File']).is_absolute() else x['File'], 'StartLine': x.get('StartLine')}
                         for x in read_json(target)]))
            history()
            if run('tests-security-runner', [sys.executable, str(ROOT / 'scripts/security_check_test.py')]) is not None:
                report('tests-security-runner')
            if run('tests-backend', maven + ['-DargLine=-Djava.awt.headless=true', 'test'], ROOT / 'backend') is not None:
                report('tests-backend')
            for folder in ('frontend', 'admin-frontend'):
                name = 'install-' + folder
                if run(name, ['npm', 'ci', '--ignore-scripts', '--no-audit', '--no-fund'], ROOT / folder) is not None:
                    for operation, command in [('test', ['npm', 'test']), ('build', ['npm', 'run', 'build'])]:
                        name = operation + '-' + folder
                        if run(name, command, ROOT / folder) is not None:
                            report(name)
                else:
                    rows.append({'check': 'tests-' + folder, 'status': 'SKIPPED', 'reason': 'install failed'})
            rows.append({'check': 'backend-e2e', 'status': 'SKIPPED', 'reason': 'requires disposable infrastructure and synthetic accounts; separate release acceptance'})
            rows.append({'check': 'codex-semantic-review', 'status': 'SKIPPED', 'reason': 'human/agent review required after automated checks; see SECURITY.md'})
    summary = out / 'summary.json'
    summary.write_text(json.dumps({'checks': rows}, ensure_ascii=False, indent=2) + '\n')
    print('Sanitized summary: ' + str(summary))
    # Backend E2E and semantic review are explicit manual release gates, not local scan errors.
    return 1 if any(x['status'] in ('FINDINGS', 'ERROR') or
                    (x['status'] == 'SKIPPED' and x['check'] not in ('backend-e2e', 'codex-semantic-review'))
                    for x in rows) else 0


if __name__ == '__main__':
    sys.exit(main())
