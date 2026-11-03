#!/usr/bin/env python3
"""Compile data-defined bottle shapes into tinted geometry without altering original PNG pixels."""
import json
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]/'src/main/resources'
ASSETS=ROOT/'assets/vinum'
shapes=json.loads((ROOT/'vinum/bottle_shapes.json').read_text())
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,indent=2)+'\n')
def inside(x,y,rect):return rect is not None and rect[0]<=x<rect[2] and rect[1]<=y<rect[3]
def pixel(x,y,size,texture,tint=None):
 unit=16/size;face={'texture':texture,'uv':[x*unit,y*unit,(x+1)*unit,(y+1)*unit]}
 if tint is not None:face['tintindex']=tint
 return {'from':[x*unit,16-(y+1)*unit,8],'to':[(x+1)*unit,16-y*unit,8.01],
  'faces':{'south':face,'north':dict(face,uv=[(x+1)*unit,y*unit,x*unit,(y+1)*unit])},'shade':False}
overrides=[]
for name,shape in sorted(shapes.items(),key=lambda p:p[1]['modelValue']):
 namespace,path=shape['texture'].split(':',1)
 im=Image.open(ROOT/f'assets/{namespace}/textures/{path}.png').convert('RGBA')
 assert im.width==im.height
 for step in range(0 if shape.get('emptyModel') else 1,5):
  fraction=step/4;elements=[];bounds=shape['liquidBounds'];surface=bounds[3]-(bounds[3]-bounds[1])*fraction
  for y in range(im.height):
   for x in range(im.width):
    r,g,b,a=im.getpixel((x,y));role=None;shade=255
    if step==0 and inside(x,y,shape.get('closureBounds')):continue
    if inside(x,y,shape.get('labelPrimaryBounds')) and a:
     role=1;shade=round(max(r,g,b)/150*255)
    elif inside(x,y,shape.get('labelSecondaryBounds')) and a:
     role=2;shade=round(max(r,g,b)/153*255)
    elif inside(x,y,bounds) and not inside(x,y,shape.get('labelArtworkBounds')) and (not shape.get('liquidNeutralOnly') or (a and max(r,g,b)-min(r,g,b)<shape.get('liquidNeutralTolerance',12) and min(r,g,b)>=shape.get('liquidNeutralMinimum',0) and max(r,g,b)<=shape.get('liquidNeutralMaximum',255))):
     if shape.get('preserveOpaquePixels') and a:pass
     elif y>=surface:role=0
     elif shape.get('emptyGlassTint'):
      role=3;shade=round(max(r,g,b)/160*255)
     else:continue
    if role is not None:elements.append(pixel(x,y,im.width,'#white',(role<<8)|max(1,min(255,shade))))
    elif a:elements.append(pixel(x,y,im.width,'#frame'))
  model=f'container/bottles/{name}_{step}'
  write(ASSETS/f'models/{model}.json',{'gui_light':'front','textures':{'frame':shape['texture'],'white':'minecraft:block/white_concrete','particle':shape['texture']},
    'display':{'gui':{'rotation':[0,0,0],'scale':[1,1,1]},'ground':{'scale':[.5,.5,.5]},'firstperson_righthand':{'rotation':[0,-90,25],'scale':[.68,.68,.68]},'thirdperson_righthand':{'rotation':[0,-90,55],'scale':[.85,.85,.85]}},'elements':elements})
  if step>0:overrides.append({'predicate':{'vinum:bottle_shape':shape['modelValue'],'vinum:fill':.00001 if step==1 else (step-1)/4+.00001},'model':'vinum:'+model})
base={'parent':'minecraft:item/generated','textures':{'layer0':'vinum:item/fifth_bottle_empty'},'overrides':overrides}
for name in ['fifth_bottle_empty','fifth_silver_tequila','fifth_golden_tequila','fifth_reposado_tequila','fifth_anejo_tequila','fifth_extra_anejo_tequila']:
 write(ASSETS/f'models/item/{name}.json',base)
for name,shape in shapes.items():
 for item in shape.get('items',[]):
  write(ASSETS/f'models/item/{item}.json',{'parent':f'vinum:container/bottles/{name}_0','overrides':overrides})
