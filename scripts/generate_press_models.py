#!/usr/bin/env python3
"""Native block geometry for timber screw press, removable crank and furnace generator."""
import json,math
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'src/main/resources'
assets=root/'assets/vinum'
def write(path,obj):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
def cube(a,b,texture,rotation=None):
 out={'from':a,'to':b,'faces':{f:{'texture':'#'+texture} for f in ['up','down','north','south','east','west']}}
 if rotation:out['rotation']=rotation
 return out
textures={'wood':'minecraft:block/stripped_oak_log','grain':'minecraft:block/oak_planks','dark':'minecraft:block/spruce_planks','iron':'minecraft:block/iron_block','black':'minecraft:block/gray_concrete','copper':'minecraft:block/copper_block','particle':'minecraft:block/oak_planks'}
parts=[]
# Basket screw press: three feet, a drained catch tray, open wooden staves and a central screw.
press_textures=dict(textures,metal='minecraft:block/purple_concrete',edge='minecraft:block/purple_terracotta')
for x,z in [(2,2),(11,2),(6.5,11)]:
 parts.append(cube([x,0,z],[x+2.5,5,z+2.5],'metal'))
parts += [cube([1,4,1],[15,5,15],'metal'),cube([2,5,2],[14,5.4,14],'dark')]
# Continuous hoops and sixteen separated wooden staves keep the top visibly open.
for i in range(16):
 angle=2*math.pi*i/16
 x=8+5.0*math.cos(angle);z=8+5.0*math.sin(angle)
 parts.append(cube([round(x-.85,3),5.5,round(z-.85,3)],[round(x+.85,3),18,round(z+.85,3)],'grain' if i%2 else 'wood'))
def hoop_segment(angle,y):
 # Tangential strips form a continuous polygon instead of a necklace of cubes.
 x=8+5.45*math.cos(math.radians(angle));z=8+5.45*math.sin(math.radians(angle))
 quadrant=round(angle/90);rotation=-(angle-quadrant*90)
 dx,dz=(.32,1.12) if quadrant%2==0 else (1.12,.32)
 return cube([round(x-dx,3),y,round(z-dz,3)],[round(x+dx,3),y+.65,round(z+dz,3)],'metal',
  {'origin':[round(x,3),y,round(z,3)],'axis':'y','angle':rotation})
for y in [6.2,11.4,16.6]:
 for i in range(16):parts.append(hoop_segment(i*22.5,y))
 for x in [4.2,7.8,11.4]:parts.append(cube([x,y+.15,2.15],[x+.3,y+.45,2.35],'iron'))
# Slender screw with fine pitched threads, rather than broad separated washers.
parts.append(cube([7.6,7,7.6],[8.4,26,8.4],'black'))
for i in range(34):
 y=9+i*.5
 for x0,z0,x1,z1,offset in [(7.45,7.45,8.55,7.7,0),(8.3,7.45,8.55,8.55,.125),(7.45,8.3,8.55,8.55,.25),(7.45,7.45,7.7,8.55,.375)]:
  parts.append(cube([x0,y+offset,z0],[x1,y+offset+.12,z1],'iron'))
# Low front spout, open upward so the juice visibly has a path out of the catch tray.
parts += [cube([6.5,4.1,0],[9.5,4.7,3],'metal'),cube([6.5,4.7,0],[7,5.2,3],'edge'),cube([9,4.7,0],[9.5,5.2,3],'edge')]
import copy
for half,lo,hi in [('lower',0,16),('upper',16,32)]:
 clipped=[]
 for part in parts:
  if part['to'][1]<=lo or part['from'][1]>=hi:continue
  piece=copy.deepcopy(part);piece['from'][1]=max(lo,piece['from'][1])-lo;piece['to'][1]=min(hi,piece['to'][1])-lo
  if 'rotation' in piece:piece['rotation']['origin'][1]-=lo
  clipped.append(piece)
 write(assets/f'models/block/wine_press_{half}.json',{'parent':'minecraft:block/block','textures':press_textures,'elements':clipped})
preview_parts=list(parts)
for left,right,inset_left,inset_right in [(3.6,7.92,4.8,7.92),(8.08,12.4,8.08,11.2)]:
 preview_parts += [cube([left,17.6,5.12],[right,18.4,10.88],'grain'),cube([inset_left,17.6,3.76],[inset_right,18.4,5.12],'grain'),cube([inset_left,17.6,10.88],[inset_right,18.4,12.24],'grain')]
preview_parts.append(cube([6.88,18.4,6.88],[9.12,20,9.12],'metal'))
write(assets/'models/block/wine_press.json',{'parent':'minecraft:block/block','textures':press_textures,'elements':preview_parts,'display':{'gui':{'rotation':[25,35,0],'translation':[0,-2,0],'scale':[.4,.4,.4]}}})
# Crank item also uses real geometry instead of a generic icon.
crank=[cube([3,7,7],[13,8,9],'iron'),cube([3,5,7],[5,7,9],'iron'),cube([11.5,8,6.5],[13.5,12,9.5],'wood')]
write(assets/'models/machine/hand_crank.json',{'parent':'minecraft:block/block','textures':dict(textures,iron='minecraft:block/purple_concrete'),'elements':crank,'display':{'gui':{'rotation':[25,35,0],'scale':[.85,.85,.85]}}})
write(assets/'models/item/hand_crank.json',{'parent':'vinum:machine/hand_crank'})
motor=[cube([3,3,3],[13,11,13],'iron'),cube([4,4,2],[12,10,3],'copper'),cube([7,11,7],[9,15,9],'black')]
write(assets/'models/item/press_motor.json',{'parent':'minecraft:block/block','textures':textures,'elements':motor})
# Compact riveted generator: iron housing, front firebox, copper dynamo at rear and short exhaust.
for lit in [False,True]:
 gen=[cube([1,0,1],[15,2,15],'black'),cube([2,2,2],[14,12,14],'iron'),cube([4,3,1.8],[12,9,2.1],'black'),cube([4.5,3.5,1.7],[11.5,8.5,1.8],'fire'),cube([3,10,1.7],[13,11.5,2.1],'black'),cube([3,12,3],[13,13,13],'iron'),cube([11,13,10],[13,16,12],'black'),cube([4,4,13.5],[12,10,15],'copper')]
 for x in [3,12]:
  for y in [2.5,11]:gen.append(cube([x,y,1.6],[x+1,y+1,1.8],'copper'))
 for x in [5,7,9,11]:gen.append(cube([x,3,1.4],[x+.4,9,1.6],'iron'))
 write(assets/f'models/block/basic_generator{"_lit" if lit else ""}.json',{'parent':'minecraft:block/block','textures':dict(textures,fire='minecraft:block/magma' if lit else 'minecraft:block/black_concrete'),'elements':gen})
for name in ['wine_press','basic_generator']:
 variants={}
 for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
  for lit in [False,True]:
   for half in ['lower','upper']:
    suffix='_'+half if name=='wine_press' else '_lit' if lit else ''
    variants[f'facing={facing},lit={str(lit).lower()},half={half}']={'model':f'vinum:block/{name}'+suffix,'y':rotation}
 write(assets/f'blockstates/{name}.json',{'variants':variants})
 write(assets/f'models/item/{name}.json',{'parent':f'vinum:block/{name}'})
 write(root/f'data/vinum/loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':f'vinum:{name}'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
