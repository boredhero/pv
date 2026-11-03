#!/usr/bin/env python3
"""Generate standalone/end/middle counters with continuous bodies, trim and footrails."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def box(a,b,texture):
 return {'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['north','south','east','west','up','down']}}
variants={}
for left in [False,True]:
 for right in [False,True]:
  lo=0 if left else 1
  hi=16 if right else 15
  name='bar'+('_left' if left else '')+('_right' if right else '')
  elements=[box([lo,0,2],[hi,14,15],'body'),box([0,14,0],[16,15,16],'trim'),box([0,15,0],[16,16,16],'top'),
   box([lo,1,1.5],[hi,2,2],'trim'),box([lo,12,1.5],[hi,13,2],'trim')]
  for x in ([1] if not left else [])+[7.5]+([14] if not right else []):
   elements.append(box([x,2,1.5],[x+.5,12,2],'trim'))
  elements.extend([box([lo,2,.5],[hi,2.6,1.1],'rail'),box([2,1.5,1],[2.5,3,2],'rail'),box([13.5,1.5,1],[14,3,2],'rail')])
  write(Path(f'assets/vinum/models/block/{name}.json'),{'parent':'minecraft:block/block','textures':{'body':'minecraft:block/oak_planks','trim':'minecraft:block/dark_oak_planks','top':'minecraft:block/spruce_planks','rail':'minecraft:block/stripped_oak_log','particle':'minecraft:block/oak_planks'},'elements':elements})
  for face,rotation in [('north',0),('east',90),('south',180),('west',270)]:
   variants[f'facing={face},left={str(left).lower()},right={str(right).lower()}']={'model':'vinum:block/'+name,'y':rotation,'uvlock':True}
write(Path('assets/vinum/models/item/bar.json'),{'parent':'vinum:block/bar'})
write(Path('assets/vinum/blockstates/bar.json'),{'variants':variants})
