#!/usr/bin/env python3
"""Generate the native Minecraft inventory mesh for Vinum's unlabelled oak barrel.

The in-world renderer supplies the gently bowed reservoir mesh. This inventory
approximation uses legal vanilla cuboid rotations, preserving resource-pack support.
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/vinum'
elements = []

def box(start, end, texture, rotation=None):
    element = {'from': [round(v, 5) for v in start], 'to': [round(v, 5) for v in end],
               'faces': {face: {'uv': [0, 0, 16, 16], 'texture': '#' + texture}
                         for face in ('north', 'south', 'east', 'west', 'up', 'down')}}
    if texture == 'planks':
        x0,y0,z0 = start; x1,y1,z1 = end
        for face in ('north','south'): element['faces'][face]['uv'] = [x0,16-y1,x1,16-y0]
        for face in ('east','west'): element['faces'][face]['uv'] = [z0,16-y1,z1,16-y0]
        for face in ('up','down'): element['faces'][face]['uv'] = [x0,z0,x1,z1]
    if rotation:
        element['rotation'] = rotation
    elements.append(element)

def ring(z0, z1, radius, texture, width, thickness):
    for segment in range(16):
        degrees = segment * 22.5
        angle = math.radians(degrees)
        x, y = 8 + radius * math.cos(angle), 8.8 + radius * math.sin(angle)
        rotation = (degrees + 90) % 180 - 90
        horizontal = abs(rotation) > 45
        if rotation > 45: rotation -= 90
        if rotation < -45: rotation += 90
        dx, dy = (width / 2, thickness / 2) if horizontal else (thickness / 2, width / 2)
        box([x-dx,y-dy,z0],[x+dx,y+dy,z1],texture,
            {'origin': [round(x, 5), round(y, 5), (z0+z1)/2], 'axis': 'z', 'angle': rotation})

for axial in range(4):
    z0, z1 = axial * 4, (axial+1) * 4
    radius = 7.2 * (.86 + .14 * math.sin(math.pi * (axial+.5)/4))
    ring(z0, z1, radius-.35, 'oak', 2.75, .75)
# Circular heads made from upright oak planks, with small gaps instead of labels.
radius = 5.904
for i in range(12):
    x0, x1 = -radius + 2*radius*i/12, -radius + 2*radius*(i+1)/12
    y = math.sqrt(max(0, radius*radius - max(abs(x0), abs(x1))**2))
    if y < .1: y = 2.6
    for z in (.56, 15.3):
        box([8+x0,8.8-y,z],[8+x1,8.8+y,z+.14],'planks')
for z in (0,15.45):
    ring(z,z+.55,6.02,'planks',2.45,.45)
for center in (1.44, 4.64, 11.36, 14.56):
    radius = 7.2 * (.86 + .14 * math.sin(math.pi * center/16))
    ring(center-.3, center+.3, radius+.015,'metal',3.05,.24)
# Concave saddles share the raised body's dimensions; narrow cuboids give the
# inventory model a clear half-moon silhouette within vanilla's model format.
for z0,z1 in ((2.72,4.0),(4.0,4.32),(11.68,12.0),(12.0,13.28)):
    axial_segment = int(((z0+z1)/2)/4)
    r = 7.2 * (.86 + .14*math.sin(math.pi*(axial_segment+.5)/4)) + .025
    for i in range(24):
        x0,x1 = 1.76 + 12.48*i/24, 1.76 + 12.48*(i+1)/24
        offset = max(abs(x0-8),abs(x1-8))
        height = 8.8 - math.sqrt(max(0,r*r-offset*offset)) + .128
        box([x0,0,z0],[x1,height,z1],'planks')
box([7.58,4.08,-.88],[8.42,4.88,.4],'metal')
box([7.68,3.44,-1.28],[8.32,4.4,-.64],'metal')
box([7.85,4.88,-.56],[8.15,5.6,-.24],'metal')
box([7.28,5.44,-.64],[8.72,5.7,-.16],'metal')
model={'parent':'minecraft:block/block','ambientocclusion':True,
       'textures':{'oak':'vinum:block/barrel_oak','planks':'minecraft:block/oak_planks','metal':'minecraft:block/deepslate_tiles','particle':'vinum:block/barrel_oak'},
       'display':{'gui':{'rotation':[25,135,0],'translation':[0,0,0],'scale':[.65,.65,.65]},
                  'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},
                  'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.5,.5,.5]},
                  'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},
                  'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[.4,.4,.4]}},
       'elements':elements}
path=ROOT/'models/block/oak_barrel.json';path.parent.mkdir(parents=True,exist_ok=True)
path.write_text(json.dumps(model,indent=2)+'\n')
print(f'Generated oak barrel inventory model with {len(elements)} native cuboid elements.')
