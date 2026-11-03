#!/usr/bin/env python3
"""Native wood stool: a shaped seat, four legs, and an all-round foot brace."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,data):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def box(a,b,texture):
 return {'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['north','south','east','west','up','down']}}
elements=[box([2,10,3],[14,12,13],'seat'),box([3,10,2],[13,12,14],'seat'),box([3,9,3],[13,10,13],'trim')]
for x in [3,11]:
 for z in [3,11]:elements.append(box([x,0,z],[x+2,10,z+2],'legs'))
for a,b in [([4,3,4],[12,4,5]),([4,3,11],[12,4,12]),([4,3,4],[5,4,12]),([11,3,4],[12,4,12])]:elements.append(box(a,b,'legs'))
write('assets/vinum/models/block/barstool.json',{'parent':'minecraft:block/block','textures':{'seat':'minecraft:block/spruce_planks','trim':'minecraft:block/dark_oak_planks','legs':'minecraft:block/stripped_oak_log','particle':'minecraft:block/spruce_planks'},'elements':elements})
write('assets/vinum/models/item/barstool.json',{'parent':'vinum:block/barstool'})
write('assets/vinum/blockstates/barstool.json',{'variants':{'':{'model':'vinum:block/barstool'}}})
write('data/vinum/recipe/crafting/barstool.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':['SSS','T T','T T'],'key':{'S':{'tag':'minecraft:wooden_slabs'},'T':{'item':'minecraft:stick'}},'result':{'id':'vinum:barstool'}})
write('data/vinum/loot_table/blocks/barstool.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'vinum:barstool'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write('data/vinum/advancement/recipes/crafting/barstool.json',{'parent':'minecraft:recipes/root','criteria':{'has_slabs':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'#minecraft:wooden_slabs'}]}}},'requirements':[['has_slabs']],'rewards':{'recipes':['vinum:crafting/barstool']}})
