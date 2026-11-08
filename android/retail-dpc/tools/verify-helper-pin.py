#!/usr/bin/env python3
from pathlib import Path
from hashlib import sha256
import re, sys
root=Path(__file__).resolve().parents[1]
props=dict(line.strip().split('=',1) for line in (root/'helper-apk.defaults.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
print('Pinned Helper URL:', props.get('helper.url'))
print('Pinned Helper SHA-256:', props.get('helper.sha256'))
if not re.fullmatch('[a-f0-9]{64}', props.get('helper.sha256','')): sys.exit('BAD SHA')
if len(sys.argv)>1:
    actual=sha256(Path(sys.argv[1]).read_bytes()).hexdigest()
    if actual!=props['helper.sha256']: sys.exit('BAD APK MATCH: '+actual)
    print('APK SHA-256: MATCH')
