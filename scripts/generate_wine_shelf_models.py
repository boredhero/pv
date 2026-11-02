#!/usr/bin/env python3
"""Native five-opening X wine rack, with optional outside trim for connected cellar walls."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,obj):
 p=ROOT/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n')
def box(lo,hi):
 return {'from':lo,'to':hi,'faces':{f:{'texture':'#wood'} for f in ('north','south','east','west','up','down')}}
def model(name,elements):
 write(Path('assets/vinum/models/block/wine_shelf')/(name+'.json'),{'parent':'minecraft:block/block','textures':{'wood':'minecraft:block/dark_oak_planks','particle':'minecraft:block/dark_oak_planks'},'elements':elements})
base=[box([0,0,15],[16,16,16])]
for x,y,angle in ((3.4,3.4,-45),(12.6,3.4,45),(3.4,12.6,45),(12.6,12.6,-45)):
 e=box([x-.45,y-3.4,1.5],[x+.45,y+3.4,15]);e['rotation']={'origin':[x,y,8],'axis':'z','angle':angle,'rescale':False};base.append(e)
base += [box([5.65,5.65,1.5],[6.25,10.35,15]),box([9.75,5.65,1.5],[10.35,10.35,15]),box([6.25,5.65,1.5],[9.75,6.25,15]),box([6.25,9.75,1.5],[9.75,10.35,15])]
edges={'left':box([0,0,0],[1.1,16,16]),'right':box([14.9,0,0],[16,16,16]),'down':box([0,0,0],[16,1.1,16]),'up':box([0,14.9,0],[16,16,16])}
model('base',base)
for key,value in edges.items():model(key,[value])
model('item',base+list(edges.values()))
parts=[]
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
 parts.append({'when':{'facing':facing},'apply':{'model':'vinum:block/wine_shelf/base','y':angle}})
 for key in edges:parts.append({'when':{'facing':facing,key:'false'},'apply':{'model':'vinum:block/wine_shelf/'+key,'y':angle}})
write(Path('assets/vinum/blockstates/wine_shelf.json'),{'multipart':parts})
write(Path('assets/vinum/models/item/wine_shelf.json'),{'parent':'vinum:block/wine_shelf/item','display':{'gui':{'rotation':[30,225,0],'scale':[.65,.65,.65]}}})
write(Path('data/vinum/loot_table/blocks/wine_shelf.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'vinum:wine_shelf'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/vinum/recipe/crafting/wine_shelf.json'),{'type':'minecraft:crafting_shaped','category':'building','pattern':['PPP','S S','PPP'],'key':{'P':{'tag':'minecraft:planks'},'S':{'item':'minecraft:stick'}},'result':{'id':'vinum:wine_shelf'}})
write(Path('data/vinum/advancement/recipes/crafting/wine_shelf.json'),{'parent':'minecraft:recipes/root','criteria':{'has_planks':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'#minecraft:planks'}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'vinum:crafting/wine_shelf'}}},'requirements':[['has_planks','has_recipe']],'rewards':{'recipes':['vinum:crafting/wine_shelf']}})
for tag in ['mineable/axe']:
 p=ROOT/f'data/minecraft/tags/block/{tag}.json';obj=json.loads(p.read_text());
 if 'vinum:wine_shelf' not in obj['values']:obj['values'].append('vinum:wine_shelf')
 p.write_text(json.dumps(obj,indent=2)+'\n')
p=ROOT/'assets/vinum/lang/en_us.json';obj=json.loads(p.read_text());obj['block.vinum.wine_shelf']='Wine Shelf';p.write_text(json.dumps(obj,indent=2)+'\n')
print('Generated connected five-bottle wine shelf models, recipe, loot, and tags.')
