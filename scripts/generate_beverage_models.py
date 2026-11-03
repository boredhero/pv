#!/usr/bin/env python3
"""Build flat item geometry over the shared 64px artwork; liquids are tinted at runtime.
Reads artwork to fit each liquid surface inside its container, without editing PNGs.
"""
import json
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parents[1] / 'src/main/resources/assets/vinum'

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def plane(x0,y0,x1,y1,z,texture,tint=None,uv=None):
    face={'texture':texture,'uv':uv or [x0/4,y0/4,x1/4,y1/4]}
    if tint is not None: face['tintindex']=tint
    return {'from':[x0/4,16-y1/4,z],'to':[x1/4,16-y0/4,z+.01],
            'faces':{'south':face,'north':dict(face,uv=[face['uv'][2],face['uv'][1],face['uv'][0],face['uv'][3]])},'shade':False}

def model(texture,elements):
    return {'display':{'gui':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'ground':{'translation':[0,2,0],'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[.68,.68,.68]},'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,4,.5],'scale':[.85,.85,.85]}},'textures':{'particle':texture,'frame':texture,'liquid':'minecraft:block/white_concrete'},
            'gui_light':'front','elements':elements}

for name,top,bottom in [('wine_glass_empty',14,35),('shot_glass',28,46)]:
    # Bottle retains its existing art; rendered volume is also shown by its capacity bar.
    im=Image.open(ROOT/f'textures/item/{name}.png').convert('RGBA')
    # Use native coordinate scale for the existing 32px bottle.
    im=im.resize((64,64),Image.Resampling.NEAREST)
    texture=f'vinum:item/{name}'
    frame=[plane(0,0,64,64,8.1,'#frame')]
    base=model(texture,frame)
    base['overrides']=[]
    for level in range(1,5):
        fraction=level/4
        start=round(bottom-(bottom-top)*fraction)
        liquid=[]
        for y in range(start,bottom):
            xs=[x for x in range(64) if im.getpixel((x,y))[3]>150]
            if not xs: continue
            lo,hi=(20,44) if name=="shot_glass" else (min(xs)+2,max(xs)-1)
            if hi>lo: liquid.append(plane(lo,y,hi,y+1,8.0,'#liquid',0))
        path=f'container/{name}_{level}'
        write(ROOT/f'models/{path}.json',model(texture,liquid+frame))
        base['overrides'].append({'predicate':{'vinum:fill':.00001 if level==1 else (level-1)/4+.00001},'model':f'vinum:{path}'})
    write(ROOT/f'models/item/{name}.json',base)

# Pulp uses the original grayscale detail. Only neutral pixels on the exposed surface are tinted.
im=Image.open(ROOT/'textures/item/grape_must.png').convert('RGBA')
parts=[plane(0,0,64,64,8,'#frame')]
for y in range(18,43):
    xs=[x for x in range(64) if (lambda p: p[3]>150 and max(p[:3])-min(p[:3])<28)(im.getpixel((x,y)))]
    runs=[]
    for x in xs:
        if runs and runs[-1][-1]==x-1: runs[-1].append(x)
        else: runs.append([x])
    for run in runs: parts.append(plane(run[0],y,run[-1]+1,y+1,8.03,'#frame',0))
write(ROOT/'models/container/grape_must.json',model('vinum:item/grape_must',parts))
for definition in json.loads((ROOT.parents[1]/'vinum/wines.json').read_text()):
    name=definition['id']
    write(ROOT/f'models/item/grape_must_{name}.json',{'parent':'vinum:container/grape_must'})
    for prefix,parent in [('grape_juice','carboy'),('wine_glass','wine_glass_empty')]:
        write(ROOT/f'models/item/{prefix}_{name}.json',json.loads((ROOT/f'models/item/{parent}.json').read_text()))

for legacy in ['shot_silver_tequila','shot_golden_tequila']:
    write(ROOT/f'models/item/{legacy}.json',json.loads((ROOT/'models/item/shot_glass.json').read_text()))
