#!/usr/bin/env python3
"""Keep the portable carboy's fill/airlock variants consistent with its placed body."""
import copy
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/vinum'
FACES = ('down', 'up', 'north', 'south', 'west', 'east')
def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')
def box(a, b, texture, tint=None):
    faces = {face: {'texture': texture, 'uv': [0, 0, 16, 16]} for face in FACES}
    if tint is not None:
        for face in faces.values(): face['tintindex'] = tint
    return {'from': a, 'to': b, 'faces': faces}
# Opaque slender highlights keep this small glass accessory readable in an item slot.
AIRLOCK = [
    box([6, 0, 6], [10, 1.5, 10], '#cork'),
    box([7.5, 1.5, 7.5], [8.5, 6, 8.5], '#glass'),
    box([7.5, 5, 7.5], [11, 6, 8.5], '#glass'),
    box([10, 5, 7.5], [11, 10, 8.5], '#glass'),
    box([6, 9, 7.5], [11, 10, 8.5], '#glass'),
    box([6, 9, 7.5], [7, 14, 8.5], '#glass'),
    box([5.3, 11, 6.8], [7.7, 13.2, 9.2], '#glass'),
    box([9.3, 6, 6.8], [11.7, 8.2, 9.2], '#glass'),
    box([5.1, 14, 6.6], [7.9, 14.8, 9.4], '#cap'),
    box([9.8, 6.3, 7.3], [11.2, 7.5, 8.7], '#water'),
    box([5.8, 11.3, 7.3], [7.2, 12.5, 8.7], '#water'),
]
textures = {'glass':'minecraft:block/white_stained_glass', 'cork':'minecraft:block/oak_log_top',
            'cap':'minecraft:block/red_terracotta', 'water':'minecraft:block/light_blue_stained_glass',
            'particle':'minecraft:block/white_stained_glass'}
write(ROOT/'models/item/airlock.json', {'parent':'minecraft:item/handheld', 'textures':textures,
    'elements':AIRLOCK, 'display':{'gui':{'rotation':[20,25,0], 'scale':[.9,.9,.9]},
    'ground':{'translation':[0,2,0], 'scale':[.5,.5,.5]}}})
body = json.loads((ROOT/'models/block/carboy.json').read_text())
body['elements'] = [e for e in body['elements'] if all(f['texture'] not in ('#airlock','#cork') for f in e['faces'].values())]
write(ROOT/'models/block/carboy.json', body)
# The same accessory is 45% item size on the neck; no airlock is baked into an empty carboy.
addon = copy.deepcopy(AIRLOCK)
for e in addon:
    for key in ('from','to'):
        e[key] = [round(8+(e[key][0]-8)*.45,4), round(14.4+e[key][1]*.45,4), round(8+(e[key][2]-8)*.45,4)]
    for face in e['faces'].values():
        face['texture'] = {'#glass':'#airlock','#cork':'#cork','#cap':'#cap','#water':'#water'}[face['texture']]
base = copy.deepcopy(body)
base['parent'] = 'minecraft:block/block'
base['textures'].update({'airlock':textures['glass'],'cork':textures['cork'],'cap':textures['cap'],'water':textures['water'],'liquid':'minecraft:block/white_concrete'})
base['display'] = {'gui':{'rotation':[30,225,0], 'translation':[0,-.6,0], 'scale':[.55,.55,.55]},
                   'ground':{'translation':[0,2,0],'scale':[.35,.35,.35]},
                   'fixed':{'rotation':[0,90,0],'scale':[.5,.5,.5]}}
for element in base['elements']:
    for face in element['faces'].values():
        if face['texture']=='#glass': face['uv']=[0,0,16,16]
for installed in (False,True):
    for tier in range(5):
        model=copy.deepcopy(base)
        if tier:
            model['elements'].insert(0,box([3.8,4,4.8],[12.2,4+6.5*tier/4,11.2],'#liquid',0))
        if installed: model['elements'].extend(addon)
        path=f'container/carboy_{tier}' + ('_airlock' if installed else '')
        write(ROOT/f'models/{path}.json',model)
entry=copy.deepcopy(base)
entry['overrides']=[]
for installed in (False,True):
    for tier in range(5):
        if not installed and not tier: continue
        predicate={}
        if tier: predicate['vinum:fill']=.00001 if tier==1 else (tier-1)/4+.00001
        if installed: predicate['vinum:airlock']=1
        entry['overrides'].append({'predicate':predicate,'model':f'vinum:container/carboy_{tier}'+('_airlock' if installed else '')})
write(ROOT/'models/item/carboy.json',entry)
print('Generated airlock accessory and ten portable carboy fill/airlock variants.')
