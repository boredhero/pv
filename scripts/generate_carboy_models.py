#!/usr/bin/env python3
"""Keep the portable carboy's fill/airlock variants consistent with its placed body."""
import copy
import json
import math
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
# Twin-bubble S airlock with a tapered gray bung and a white vented cap.
# Glass tubes retain thin opaque highlights so their silhouette survives GUI scaling.
AIRLOCK = []
def tube(a,b):
    AIRLOCK.append(box(a,b,'#glass'))
    AIRLOCK.append(box([a[0],a[1],b[2]],[min(a[0]+.12,b[0]),b[1],b[2]+.025],'#edge'))
def bulb(cx):
    AIRLOCK.extend([
        box([cx-.8,7.2,7.1],[cx+.8,7.7,8.9],'#glass'),
        box([cx-1.1,7.7,6.8],[cx+1.1,10.8,9.2],'#glass'),
        box([cx-.8,10.8,7.1],[cx+.8,11.5,8.9],'#glass'),
        box([cx-.45,11.5,7.55],[cx+.45,12,8.45],'#glass'),
        box([cx-.65,7.8,7.4],[cx+.65,8.8,8.6],'#water'),
        box([cx-.95,7.9,9.2],[cx-.82,10.6,9.23],'#edge'),
    ])
for width,low,high in ((3.3,0,.7),(3.8,.7,1.4),(4.3,1.4,2.1),(4.8,2.1,2.8)):
    AIRLOCK.append(box([8-width/2,low,8-width/2],[8+width/2,high,8+width/2],'#cork'))
tube([7.55,2.8,7.55],[8.45,4.3,8.45])
diagonal=box([7.55,4,7.55],[8.45,8.2,8.45],'#glass')
diagonal['rotation']={'origin':[8,4,8],'axis':'z','angle':45}
AIRLOCK.append(diagonal)
tube([4.6,6.7,7.55],[5.5,12.6,8.45])
tube([5.1,12.3,7.55],[6.2,13.2,8.45])
tube([5.8,12.8,7.55],[7.3,13.7,8.45])
tube([6.8,11.6,7.55],[7.7,13.2,8.45])
bulb(7.1); bulb(10.5)
tube([6.65,6.5,7.55],[7.55,7.4,8.45])
tube([7.1,6.1,7.55],[10.5,7,8.45])
tube([10.05,6.5,7.55],[10.95,7.4,8.45])
diagonal=box([10.05,11.8,7.55],[10.95,14.5,8.45],'#glass')
diagonal['rotation']={'origin':[10.5,11.8,8],'axis':'z','angle':45}
AIRLOCK.append(diagonal)
tube([8.15,13.6,7.55],[9.05,14.7,8.45])
AIRLOCK.append(box([7.2,14.5,6.6],[10,15.7,9.4],'#cap'))
AIRLOCK.append(box([7.45,15.7,6.85],[9.75,16,9.15],'#cap'))
for x,z in ((7.8,7.2),(9.2,7.2),(7.8,8.8),(9.2,8.8)):
    AIRLOCK.append(box([x-.12,16,z-.12],[x+.12,16.025,z+.12],'#cork'))
textures = {'glass':'minecraft:block/glass', 'cork':'minecraft:block/light_gray_concrete',
            'cap':'minecraft:block/white_concrete', 'water':'minecraft:block/light_blue_stained_glass',
            'particle':'minecraft:block/glass', 'edge':'minecraft:block/white_concrete'}
write(ROOT/'models/item/airlock.json', {'parent':'minecraft:block/block', 'render_type':'minecraft:cutout', 'textures':textures,
    'elements':AIRLOCK, 'display':{'gui':{'rotation':[20,25,0], 'scale':[.9,.9,.9]},
    'ground':{'translation':[0,2,0], 'scale':[.5,.5,.5]}, 'firstperson_righthand':{'rotation':[0,-90,25], 'translation':[1,2,1], 'scale':[.6,.6,.6]}, 'thirdperson_righthand':{'rotation':[0,-90,30], 'scale':[.65,.65,.65]}}})
def glass_ring(radius, low, high, rib=False):
    """Eight exterior panels; diagonal panels use legal native 45-degree rotations."""
    panels=[]
    half=radius*math.tan(math.pi/8)
    for angle in (0,45):
        for side in range(4):
            if side==0: a,b=[8-half,low,8-radius],[8+half,high,8-radius+.035]
            elif side==1: a,b=[8-half,low,8+radius-.035],[8+half,high,8+radius]
            elif side==2: a,b=[8-radius,low,8-half],[8-radius+.035,high,8+half]
            else: a,b=[8+radius-.035,low,8-half],[8+radius,high,8+half]
            e=box([round(v,4) for v in a],[round(v,4) for v in b],'#glass')
            if angle: e['rotation']={'origin':[8,8,8],'axis':'y','angle':45}
            panels.append(e)
    return panels
body={'parent':'minecraft:block/block', 'render_type':'minecraft:cutout',
      'ambientocclusion':False, 'textures':{'glass':'minecraft:block/glass',
      'particle':'minecraft:block/glass'}, 'elements':[]}
# Ribbed bottle body and a bevelled base, without a crate, wicker or wood.
body['elements'] += [box([4.5,0,3.3],[11.5,.18,12.7],'#glass'), box([3.3,0,4.5],[12.7,.18,11.5],'#glass')]
body['elements'] += glass_ring(4.7,0,.55)
for low,high in ((.55,3.5),(3.5,6.7),(6.7,10.2)):
    body['elements'] += glass_ring(5,low,high)
for y in (.55,3.5,6.7,10.2):
    body['elements'] += glass_ring(5.10,y,y+.16,True)
# Short graduated facets form sloping shoulders beneath a narrow bottle neck.
for radius,low,high in ((4.65,10.36,10.9),(4,10.9,11.5),(3.25,11.5,12.1),
                        (2.5,12.1,12.7),(1.75,12.7,13.3),(1.4,13.3,15.5)):
    body['elements'] += glass_ring(radius,low,high)
body['elements'] += glass_ring(1.65,15.35,15.7,True)
write(ROOT/'models/block/carboy.json',body)
# The same accessory is 45% item size on the neck; no airlock is baked into an empty carboy.
addon = copy.deepcopy(AIRLOCK)
for e in addon:
    for key in ('from','to'):
        e[key] = [round(8+(e[key][0]-8)*.45,4), round(15.45+e[key][1]*.45,4), round(8+(e[key][2]-8)*.45,4)]
    if 'rotation' in e:
        x,y,z=e['rotation']['origin']
        e['rotation']['origin']=[round(8+(x-8)*.45,4),round(15.45+y*.45,4),round(8+(z-8)*.45,4)]
    for face in e['faces'].values():
        face['texture'] = {'#glass':'#airlock','#cork':'#cork','#cap':'#cap','#water':'#water','#edge':'#edge'}[face['texture']]
base = copy.deepcopy(body)
base['parent'] = 'minecraft:block/block'
base['textures'].update({'airlock':textures['glass'],'cork':textures['cork'],'cap':textures['cap'],'water':textures['water'],'liquid':'minecraft:block/white_concrete','edge':textures['edge']})
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
            # Narrow slices approximate the same octagonal interior as the placed liquid.
            for index in range(10):
                z0=-4.65+index*.93; z1=z0+.93
                half=min(4.65, 4.65*(1+math.tan(math.pi/8))-max(abs(z0),abs(z1)))
                model['elements'].insert(0,box([8-half,.65,8+z0],[8+half,.65+9.1*tier/4,8+z1],'#liquid',0))
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
