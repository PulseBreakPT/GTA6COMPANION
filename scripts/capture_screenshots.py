#!/usr/bin/env python3
"""Capture real app screens from an attached emulator using observed accessibility bounds."""
import os,re,subprocess,time,xml.etree.ElementTree as ET
from pathlib import Path
adb=os.environ.get('ADB','adb');out=Path('store/screenshots');out.mkdir(parents=True,exist_ok=True)
def cmd(*args): return subprocess.check_output([adb,*args])
def tap(text, attempts=12):
 for _ in range(attempts):
  cmd('shell','uiautomator','dump','/sdcard/atlas-window.xml')
  root=ET.fromstring(cmd('shell','cat','/sdcard/atlas-window.xml'))
  for node in root.iter('node'):
   if node.attrib.get('text')==text or node.attrib.get('content-desc')==text:
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.attrib['bounds']))
    cmd('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(1);return True
  time.sleep(.5)
 return False
def capture(name):
 time.sleep(1)
 (out/(name+'.png')).write_bytes(cmd('exec-out','screencap','-p'))
cmd('shell','am','start','-n','pt.leonida.atlas/.MainActivity');time.sleep(3)
tap('Skip',2)
assert tap('Settings');assert tap('Night');assert tap('Home');capture('01-home-dark')
assert tap('Atlas');capture('02-editorial-atlas')
assert tap('Discover');capture('03-search')
assert tap('News');capture('04-news')
assert tap('My vault');capture('05-personal-vault')
assert tap('Settings');assert tap('Day');capture('06-settings-light')
assert tap('Home');capture('07-home-light')
print('Captured seven actual emulator screenshots in',out)
