#!/usr/bin/env python3
"""Generate vanilla-behavior cinnamon building resources from the installed 1.21.1 client assets.
Raster artwork is supplied separately; this script only writes native models/data.
"""
import argparse
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
parser = argparse.ArgumentParser()
parser.add_argument('--minecraft-jar', type=Path)
args = parser.parse_args()
if args.minecraft_jar is None:
    for candidate in (Path.home()/'.gradle/caches/VanillaExtract/1.21.1').glob('client-*.jar'):
        with zipfile.ZipFile(candidate) as probe:
            if 'assets/minecraft/blockstates/oak_planks.json' in probe.namelist():
                args.minecraft_jar = candidate
                break
if args.minecraft_jar is None:
    parser.error('Pass --minecraft-jar pointing to a Minecraft 1.21.1 client jar with assets.')
FAMILY = ('planks', 'stairs', 'slab', 'fence', 'fence_gate', 'door', 'trapdoor', 'button', 'pressure_plate')

def write(path, value):
    p = RES / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, indent=2) + '\n')

def convert(value):
    if isinstance(value, dict): return {key: convert(child) for key, child in value.items()}
    if isinstance(value, list): return [convert(child) for child in value]
    if isinstance(value, str):
        if value.startswith('minecraft:block/oak_'): return value.replace('minecraft:block/oak_', 'vinum:block/cinnamon_', 1)
        if value.startswith('minecraft:oak_'): return value.replace('minecraft:oak_', 'vinum:cinnamon_', 1)
    return value

def box(lo, hi, tex):
    return {'from': lo, 'to': hi, 'faces': {side: {'texture': tex} for side in ('down','up','north','south','west','east')}}

def tag(namespace, kind, name, values):
    path = f'data/{namespace}/tags/{kind}/{name}.json'
    old = json.loads((RES/path).read_text()) if (RES/path).exists() else {'replace': False, 'values': []}
    old['values'] = list(dict.fromkeys(old['values'] + values))
    write(path, old)

with zipfile.ZipFile(args.minecraft_jar) as archive:
    def read(path): return json.loads(archive.read(path))
    for suffix in FAMILY:
        name = 'cinnamon_' + suffix
        state = convert(read(f'assets/minecraft/blockstates/oak_{suffix}.json'))
        write(f'assets/vinum/blockstates/{name}.json', state)
        models = set()
        def collect(node):
            if isinstance(node,dict):
                if 'model' in node: models.add(node['model'].split('/')[-1])
                for child in node.values(): collect(child)
            elif isinstance(node,list):
                for child in node: collect(child)
        collect(state)
        item_model = convert(read(f'assets/minecraft/models/item/oak_{suffix}.json'))
        if suffix != 'door' and item_model.get('parent','').startswith('vinum:'):
            models.add(item_model['parent'].split('/')[-1])
        for model_name in sorted(models):
            model = convert(read('assets/minecraft/models/block/' + model_name.replace('cinnamon_', 'oak_', 1) + '.json'))
            model['textures'] = {key: 'vinum:block/cinnamon_planks' for key in model.get('textures',{})}
            if suffix == 'door':
                # Vanilla door geometry/orientation with timber panels and a visible metal latch.
                template = read('assets/minecraft/models/block/' + model['parent'].split('/')[-1] + '.json')
                model['elements'] = template['elements']
                model['textures']['hardware'] = 'minecraft:block/gray_concrete'
                if '_bottom_' in model_name:
                    panel = model['elements'][0]
                    lo, hi = panel['from'], panel['to']
                    thin = 0 if hi[0]-lo[0] == 3 else 2
                    if thin == 0:
                        x = hi[0] if lo[0] == 0 else lo[0]-1
                        model['elements'].append(box([x, 11, 12], [x+1, 13, 14], '#hardware'))
                    else:
                        z = hi[2] if lo[2] == 0 else lo[2]-1
                        model['elements'].append(box([12, 11, z], [14, 13, z+1], '#hardware'))
            write(f'assets/vinum/models/block/{model_name}.json', model)
        if suffix == 'door':
            item_model = {'parent': 'minecraft:block/block', 'textures': {'wood':'vinum:block/cinnamon_planks','hardware':'minecraft:block/gray_concrete','particle':'vinum:block/cinnamon_planks'},
                'elements':[box([0,0,7],[16,32,10],'#wood'),box([12,11,10],[14,13,11],'#hardware')],
                'display':{'gui':{'rotation':[20,35,0],'translation':[0,-3,0],'scale':[.35,.35,.35]}}}
        write(f'assets/vinum/models/item/{name}.json', item_model)
        loot = convert(read(f'data/minecraft/loot_table/blocks/oak_{suffix}.json'))
        write(f'data/vinum/loot_table/blocks/{name}.json', loot)
        recipe = convert(read(f'data/minecraft/recipe/oak_{suffix}.json'))
        write(f'data/vinum/recipe/crafting/{name}.json', recipe)
        ingredient = recipe.get('ingredients', list(recipe.get('key',{}).values()))[0]
        item = ingredient.get('item', '#' + ingredient.get('tag',''))
        ident = 'vinum:crafting/' + name
        write(f'data/vinum/advancement/recipes/crafting/{name}.json', {
          'parent':'minecraft:recipes/root', 'criteria':{
            'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':item}]}},
            'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':ident}}},
          'requirements':[['has_material','has_recipe']], 'rewards':{'recipes':[ident]}})

for kind in ('block','item'):
    tag('vinum',kind,'cinnamon_logs',['vinum:cinnamon_log'])
    tag('minecraft',kind,'planks',['vinum:cinnamon_planks'])
    for suffix, names in {
        'stairs':('wooden_stairs','stairs'), 'slab':('wooden_slabs','slabs'),
        'fence':('wooden_fences','fences'), 'fence_gate':('fence_gates',),
        'door':('wooden_doors','doors'), 'trapdoor':('wooden_trapdoors','trapdoors'),
        'button':('wooden_buttons','buttons'), 'pressure_plate':('wooden_pressure_plates','pressure_plates')}.items():
        for name in names: tag('minecraft',kind,name,['vinum:cinnamon_'+suffix])
tag('minecraft','block','mineable/axe',['vinum:cinnamon_'+part for part in FAMILY])
tag('minecraft','block','overworld_natural_logs',['vinum:cinnamon_log'])
tag('minecraft','item','logs_that_burn',['vinum:cinnamon_log'])

# Log/wood families include their axis variants and normal construction conversions.
with zipfile.ZipFile(args.minecraft_jar) as archive:
    def read(path): return json.loads(archive.read(path))
    def cinnamon(value):
        if isinstance(value, dict): return {k:cinnamon(v) for k,v in value.items()}
        if isinstance(value, list): return [cinnamon(v) for v in value]
        if isinstance(value, str):
            return value.replace('minecraft:block/stripped_oak_', 'vinum:block/stripped_cinnamon_').replace('minecraft:stripped_oak_', 'vinum:stripped_cinnamon_').replace('minecraft:block/oak_', 'vinum:block/cinnamon_').replace('minecraft:oak_', 'vinum:cinnamon_')
        return value
    for original in ('oak_log','oak_wood','stripped_oak_log','stripped_oak_wood','oak_leaves','oak_sapling'):
        name=original.replace('oak','cinnamon')
        state=cinnamon(read(f'assets/minecraft/blockstates/{original}.json'))
        write(f'assets/vinum/blockstates/{name}.json',state)
        models=set();collect(state)
        for model_name in models:
            model=cinnamon(read('assets/minecraft/models/block/'+model_name.replace('cinnamon','oak')+'.json'))
            if 'leaves' in name: model['render_type']='minecraft:cutout_mipped'
            if 'sapling' in name: model['render_type']='minecraft:cutout'
            write(f'assets/vinum/models/block/{model_name}.json',model)
        item=cinnamon(read(f'assets/minecraft/models/item/{original}.json'))
        write(f'assets/vinum/models/item/{name}.json',item)
        if original=='oak_leaves':
            # Cinnamon leaves provide saplings/sticks, never apples.
            loot=cinnamon(read('data/minecraft/loot_table/blocks/birch_leaves.json'))
            def replace_birch(v):
                if isinstance(v,dict):return {k:replace_birch(x) for k,x in v.items()}
                if isinstance(v,list):return [replace_birch(x) for x in v]
                return v.replace('minecraft:birch_', 'vinum:cinnamon_') if isinstance(v,str) else v
            loot=replace_birch(loot)
        else: loot=cinnamon(read(f'data/minecraft/loot_table/blocks/{original}.json'))
        write(f'data/vinum/loot_table/blocks/{name}.json',loot)
    for original in ('oak_wood','stripped_oak_wood'):
        name=original.replace('oak','cinnamon');recipe=cinnamon(read(f'data/minecraft/recipe/{original}.json'))
        write(f'data/vinum/recipe/crafting/{name}.json',recipe)
        material='vinum:'+('stripped_cinnamon_log' if original.startswith('stripped') else 'cinnamon_log')
        recipeid='vinum:crafting/'+name
        write(f'data/vinum/advancement/recipes/crafting/{name}.json',{'parent':'minecraft:recipes/root','criteria':{'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':material}]}},'has_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':recipeid}}},'requirements':[['has_material','has_recipe']],'rewards':{'recipes':[recipeid]}})
logs=['vinum:'+name for name in ('cinnamon_log','cinnamon_wood','stripped_cinnamon_log','stripped_cinnamon_wood')]
for kind in ('block','item'):
    tag('vinum',kind,'cinnamon_logs',logs)
    tag('minecraft',kind,'logs_that_burn',logs)
    tag('minecraft',kind,'leaves',['vinum:cinnamon_leaves'])
    tag('minecraft',kind,'saplings',['vinum:cinnamon_sapling'])
tag('minecraft','block','mineable/axe',logs)
tag('minecraft','block','mineable/hoe',['vinum:cinnamon_leaves'])
write('data/vinum/worldgen/configured_feature/cinnamon_tree.json',{'type':'vinum:cinnamon_tree','config':{'min_height':8,'max_height':12}})
for biome,chance in [('jungle',8),('sparse_jungle',12)]:
    feature='cinnamon_'+biome
    write(f'data/vinum/worldgen/placed_feature/{feature}.json',{'feature':'vinum:cinnamon_tree','placement':[{'type':'minecraft:rarity_filter','chance':chance},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'OCEAN_FLOOR_WG'},{'type':'minecraft:biome'}]})
    write(f'data/vinum/neoforge/biome_modifier/add_{feature}.json',{'type':'neoforge:add_features','biomes':'minecraft:'+biome,'features':'vinum:'+feature,'step':'vegetal_decoration'})
print('Generated cinnamon tree/wood models, loot, recipes, tags, and jungle worldgen.')
