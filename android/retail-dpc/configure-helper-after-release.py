#!/usr/bin/env python3
"""Update pinned Helper 0.2.9 source after verifying exact APK checksum."""
from pathlib import Path
import argparse,re
p=argparse.ArgumentParser()
p.add_argument('--sha256',required=True)
p.add_argument('--source',choices=['raw','release'],default='raw')
a=p.parse_args()
if not re.fullmatch(r'[0-9a-fA-F]{64}',a.sha256):p.error('SHA-256 must be 64 hexadecimal chars')
raw='https://raw.githubusercontent.com/pitsmich-netizen/retailmode/main/provisioning/RetailHelper-029.apk'
release='https://github.com/pitsmich-netizen/retailmode/releases/download/helper-029-test/RetailHelper.apk'
root=Path(__file__).resolve().parent
url=raw if a.source=='raw' else release
(root/'helper-apk.defaults.properties').write_text('helper.url='+url+'\nhelper.sha256='+a.sha256.lower()+'\n',encoding='utf-8')
print('Updated source. Rebuild and verify DPC 0.10.1 before creating a new QR.')
