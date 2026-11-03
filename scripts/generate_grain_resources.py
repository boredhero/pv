#!/usr/bin/env python3
"""Static rye/corn models and datapack resources; crop textures are authored separately."""
import json
from pathlib import Path
R=Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,indent=2)+'\n')
def stage(age):return 0 if age<2 else 1 if age<4 else 2 if age<7 else 3
for crop in ('rye','corn'):
 variants={}
 for age in range(8):
  halves=['lower','upper'] if crop=='corn' else ['']
  for half in halves:
   name=f'crop_{crop}_{stage(age)}'+('_'+half if half else '')
   texture=f'{crop}_stage_{stage(age)}'+('_'+half if half else '')
   write(f'assets/vinum/models/block/{name}.json',{'parent':'minecraft:block/cross','render_type':'minecraft:cutout','textures':{'cross':f'vinum:block/{texture}'}})
   variants[f'age={age}'+(f',half={half}' if half else '')]={'model':f'vinum:block/{name}'}
 write(f'assets/vinum/blockstates/crop_{crop}.json',{'variants':variants})
 lower={'condition':'minecraft:block_state_property','block':f'vinum:crop_{crop}','properties':{'half':'lower'}} if crop=='corn' else None
 mature={'condition':'minecraft:block_state_property','block':f'vinum:crop_{crop}','properties':{'age':'7'}}
 pools=[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'vinum:{crop}_seeds'}], 'conditions':([lower] if lower else [])},
 {'rolls':1,'conditions':([lower] if lower else [])+[mature],'entries':[{'type':'minecraft:item','name':f'vinum:{crop}','functions':[{'function':'minecraft:set_count','count':2 if crop=='corn' else 1}]}]},
 {'rolls':1,'conditions':([lower] if lower else [])+[mature],'entries':[{'type':'minecraft:item','name':f'vinum:{crop}_seeds','functions':[{'function':'minecraft:apply_bonus','enchantment':'minecraft:fortune','formula':'minecraft:binomial_with_bonus_count','parameters':{'extra':3,'probability':.5714286}}]}]}]
 write(f'data/vinum/loot_table/blocks/crop_{crop}.json',{'type':'minecraft:block','pools':pools,'random_sequence':f'vinum:blocks/crop_{crop}'})
 for item in (crop,crop+'_seeds'):
  write(f'assets/vinum/models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'vinum:item/{item}'}})
 write(f'data/vinum/recipe/{crop}_seeds.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':f'vinum:{crop}'}],'result':{'id':f'vinum:{crop}_seeds','count':2}})
 write(f'data/vinum/advancement/recipes/{crop}_seeds.json',{'parent':'minecraft:recipes/root','criteria':{'has_crop':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':f'vinum:{crop}'}]}},'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':f'vinum:{crop}_seeds'}}},'requirements':[['has_crop','has_the_recipe']],'rewards':{'recipes':[f'vinum:{crop}_seeds']}})
 write(f'data/c/tags/item/crops/{crop}.json',{'replace':False,'values':[f'vinum:{crop}']})
 write(f'data/c/tags/item/seeds/{crop}.json',{'replace':False,'values':[f'vinum:{crop}_seeds']})
write('data/vinum/loot_modifiers/grass_grain_seeds.json',{'type':'vinum:grass_grain_seeds','conditions':[{'condition':'neoforge:loot_table_id','loot_table_id':'minecraft:blocks/short_grass'}]})
# Merge additive tags/index rather than replacing another feature's entries.
for path,values in {
 'data/neoforge/loot_modifiers/global_loot_modifiers.json':['vinum:grass_grain_seeds'],
 'data/minecraft/tags/block/crops.json':['vinum:crop_rye','vinum:crop_corn'],
 'data/minecraft/tags/item/chicken_food.json':['vinum:rye_seeds','vinum:corn_seeds'],
 'data/c/tags/item/crops.json':['#c:crops/rye','#c:crops/corn'],
 'data/c/tags/item/seeds.json':['#c:seeds/rye','#c:seeds/corn'],
}.items():
 p=R/path;data=json.loads(p.read_text()) if p.exists() else {'replace':False}
 key='entries' if 'global_loot_modifiers' in path else 'values';data[key]=list(dict.fromkeys(data.get(key,[])+values));write(path,data)
