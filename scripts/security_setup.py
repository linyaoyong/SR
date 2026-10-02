#!/usr/bin/env python3
"""Install pinned audit tools in this checkout. Requires Python 3.13, no API keys."""
import hashlib
import os
from pathlib import Path
import platform
import subprocess
import tarfile
import urllib.request
import venv

ROOT = Path(__file__).resolve().parents[1] / '.security-tools'


def download(url, filename, digest):
    path = ROOT / filename
    with urllib.request.urlopen(url, timeout=120) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != digest:
        raise RuntimeError('Download checksum mismatch: ' + filename)
    path.write_bytes(data)
    return path


def main():
    os.umask(0o077)
    ROOT.mkdir(exist_ok=True)
    target = ROOT / 'venv'
    if not target.exists():
        venv.create(target, with_pip=True)
    python = str(target / 'bin/python')
    subprocess.run([python, '-m', 'pip', 'install', '--disable-pip-version-check', 'semgrep==1.179.0'], check=True)
    wheel = download('https://github.com/Unclecheng-li/DeepSec/releases/download/v0.2.0/deepsec-0.2.0-py3-none-any.whl',
                     'deepsec-0.2.0-py3-none-any.whl', '541a9eab8f22fefa8981ea1ac47217257e0262cc147d394fb13befd3a2551e40')
    subprocess.run([python, '-m', 'pip', 'install', '--disable-pip-version-check', str(wheel)], check=True)
    variants = {
        ('Darwin', 'arm64'): ('darwin_arm64', 'darwin_arm64',
                             '98c460dcd37de25819babd757d04542045b6243113e209edcd4d89fedb0256b4',
                             'b40ab0ae55c505963e365f271a8d3846efbc170aa17f2607f13df610a9aeb6a5'),
        ('Linux', 'x86_64'): ('linux_amd64', 'linux_x64',
                             'ca69b3d3cd08f889a49dc0a383122f71cc528b83803671df5fd874d97485b108',
                             '551f6fc83ea457d62a0d98237cbad105af8d557003051f41f3e7ca7b3f2470eb'),
    }
    osv, leaks, osv_hash, leaks_hash = variants[(platform.system(), platform.machine())]
    binaries = ROOT / 'bin'
    binaries.mkdir(exist_ok=True)
    path = download(f'https://github.com/google/osv-scanner/releases/download/v2.6.0/osv-scanner_{osv}',
                    'osv-download', osv_hash)
    (binaries / 'osv-scanner').write_bytes(path.read_bytes())
    path = download(f'https://github.com/gitleaks/gitleaks/releases/download/v8.30.1/gitleaks_8.30.1_{leaks}.tar.gz',
                    'gitleaks-download.tar.gz', leaks_hash)
    # Extract only the regular executable; never trust archive paths/symlinks.
    with tarfile.open(path) as archive:
        member = archive.getmember('gitleaks')
        if not member.isfile():
            raise RuntimeError('Unexpected gitleaks archive member')
        (binaries / 'gitleaks').write_bytes(archive.extractfile(member).read())
    for name in ('osv-scanner', 'gitleaks'):
        (binaries / name).chmod(0o700)
    print('Pinned security tools installed in .security-tools/')


if __name__ == '__main__':
    main()
