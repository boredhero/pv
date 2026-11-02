#!/usr/bin/env python3
"""Generate vanilla-behavior cork oak building resources from the installed 1.21.1 client assets.
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
        if value.startswith('minecraft:block/oak_'): return value.replace('minecraft:block/oak_', 'vinum:block/cork_oak_', 1)
        if value.startswith('minecraft:oak_'): return value.replace('minecraft:oak_', 'vinum:cork_oak_', 1)
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
        name = 'cork_oak_' + suffix
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
            model = convert(read('assets/minecraft/models/block/' + model_name.replace('cork_oak_', 'oak_', 1) + '.json'))
            model['textures'] = {key: 'vinum:block/cork_oak_planks' for key in model.get('textures',{})}
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
            item_model = {'parent': 'minecraft:block/block', 'textures': {'wood':'vinum:block/cork_oak_planks','hardware':'minecraft:block/gray_concrete','particle':'vinum:block/cork_oak_planks'},
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
    tag('vinum',kind,'cork_oak_logs',['vinum:cork_oak_log'])
    tag('minecraft',kind,'planks',['vinum:cork_oak_planks'])
    for suffix, names in {
        'stairs':('wooden_stairs','stairs'), 'slab':('wooden_slabs','slabs'),
        'fence':('wooden_fences','fences'), 'fence_gate':('fence_gates',),
        'door':('wooden_doors','doors'), 'trapdoor':('wooden_trapdoors','trapdoors'),
        'button':('wooden_buttons','buttons'), 'pressure_plate':('wooden_pressure_plates','pressure_plates')}.items():
        for name in names: tag('minecraft',kind,name,['vinum:cork_oak_'+suffix])
tag('minecraft','block','mineable/axe',['vinum:cork_oak_'+part for part in FAMILY])
tag('minecraft','block','overworld_natural_logs',['vinum:cork_oak_log'])
tag('minecraft','item','logs_that_burn',['vinum:cork_oak_log'])
lang_path = RES/'assets/vinum/lang/en_us.json'
lang = json.loads(lang_path.read_text())
for suffix in FAMILY: lang['block.vinum.cork_oak_'+suffix] = 'Cork Oak ' + suffix.replace('_',' ').title()
lang_path.write_text(json.dumps(lang,indent=2,ensure_ascii=True)+'\n')
write('assets/vinum/models/item/cork_oak_log.json',{'parent':'vinum:block/cork_oak_log','overrides':[{'predicate':{'vinum:cork_harvested':1},'model':'vinum:block/cork_oak_log_harvested'}]})
print('Generated cork oak building family, recipes, tags and harvested-log item model.')
