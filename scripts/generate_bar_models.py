#!/usr/bin/env python3
"""Generate counter footprints and decorate only their exposed perimeter edges."""
import json
from itertools import product
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def box(a,b,texture,turn=0):
 for _ in range(turn):
  a,b=[16-b[2],a[1],a[0]],[16-a[2],b[1],b[0]]
 return {'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['north','south','east','west','up','down']}}
textures={'body':'minecraft:block/oak_planks','trim':'minecraft:block/dark_oak_planks','top':'minecraft:block/spruce_planks','rail':'minecraft:block/stripped_oak_log','particle':'minecraft:block/oak_planks'}
variants={}
for left,right,front,back in product([False,True],repeat=4):
 flags=dict(left=left,right=right,front=front,back=back)
 name='bar'+''.join('_'+key for key,val in flags.items() if val)
 elements=[box([0 if left else 2,0,0 if front else 2],[16 if right else 14,14,16 if back else 14],'body'),
  box([0,14,0],[16,15,16],'trim'),box([0,15,0],[16,16,16],'top')]
 connected=[front,right,back,left]
 for side in range(4):
  if connected[side]:continue
  start,end=connected[(side+3)%4],connected[(side+1)%4]
  lo,hi=(0 if start else 1.5),(16 if end else 14.5)
  elements.extend([box([lo,1,1.5],[hi,2,2],'trim',side),box([lo,12,1.5],[hi,13,2],'trim',side)])
  for x in ([] if start else [1.5])+[7.5]+([] if end else [14]):
   elements.append(box([x,2,1.5],[x+.5,12,2],'trim',side))
  elements.extend([box([0 if start else .5,2,.5],[16 if end else 15.5,2.6,1.1],'rail',side),
   box([2,1.5,1],[2.5,3,2],'rail',side),box([13.5,1.5,1],[14,3,2],'rail',side)])
 write(Path(f'assets/vinum/models/block/{name}.json'),{'parent':'minecraft:block/block','textures':textures,'elements':elements})
 for face,rotation in [('north',0),('east',90),('south',180),('west',270)]:
  key=f'facing={face},'+','.join(f'{k}={str(v).lower()}' for k,v in flags.items())
  variants[key]={'model':'vinum:block/'+name,'y':rotation,'uvlock':True}
write(Path('assets/vinum/models/item/bar.json'),{'parent':'vinum:block/bar'})
write(Path('assets/vinum/blockstates/bar.json'),{'variants':variants})
