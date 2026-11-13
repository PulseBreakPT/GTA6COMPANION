#!/usr/bin/env python3
"""Sign an editorial pack using an owner-held RSA key; requires OpenSSL, no Python dependencies."""
import argparse,base64,json,subprocess,tempfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('content');p.add_argument('--private-key',required=True);p.add_argument('--output',required=True);a=p.parse_args()
raw=Path(a.content).read_bytes();json.loads(raw)
with tempfile.TemporaryDirectory() as tmp:
 sig=Path(tmp)/'signature';subprocess.run(['openssl','dgst','-sha256','-sign',a.private_key,'-out',str(sig),a.content],check=True)
 Path(a.output).write_text(json.dumps({'payload':base64.b64encode(raw).decode(),'signature':base64.b64encode(sig.read_bytes()).decode()}))
