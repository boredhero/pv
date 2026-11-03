#!/usr/bin/env python3
"""Native paneled counter model; the block entity draws its stored upright bottles."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def box(a,b,texture):
 return {'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['north','south','east','west','up','down']}}
elements=[box([1,0,2],[15,14,15],'body'),box([0,14,0],[16,15,16],'trim'),box([0,15,0],[16,16,16],'top'),
 box([1,1,1.5],[15,2,2],'trim'),box([1,12,1.5],[15,13,2],'trim')]
for x in [1,7.5,14]:elements.append(box([x,2,1.5],[x+.5,12,2],'trim'))
# Front footrest and two understated brackets.
elements.extend([box([1,2,.5],[15,2.6,1.1],'rail'),box([2,1.5,1],[2.5,3,2],'rail'),box([13.5,1.5,1],[14,3,2],'rail')])
write(Path('assets/vinum/models/block/bar.json'),{'parent':'minecraft:block/block','textures':{'body':'minecraft:block/oak_planks','trim':'minecraft:block/dark_oak_planks','top':'minecraft:block/spruce_planks','rail':'minecraft:block/stripped_oak_log','particle':'minecraft:block/oak_planks'},'elements':elements})
write(Path('assets/vinum/models/item/bar.json'),{'parent':'vinum:block/bar'})
write(Path('assets/vinum/blockstates/bar.json'),{'variants':{f'facing={face}':dict({'model':'vinum:block/bar'},**({'y':rotation} if rotation else {})) for face,rotation in [('north',0),('east',90),('south',180),('west',270)]}})
