#!/usr/bin/env python3
"""Generate a sparse eight-stage 3D agave rosette, splitting geometry at block boundaries."""
import json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
MODELS=RES/'assets/vinum/models/block'
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,indent=2)+'\n')
def leaf_boxes(age):
    scale=[.09,.15,.23,.31,.48,.65,.82,1.0][age]
    boxes=[]
    # Outer splayed blades, a middle crown, and upright inner spears.
    for ring,(count,radius,height,phase) in enumerate([(12,21.8,14,0),(9,14.5,28,.28),(6,6.7,44,.1)]):
        for leaf in range(count):
            angle=leaf*math.tau/count+phase
            c,s=math.cos(angle),math.sin(angle)
            segments=12
            for step in range(segments):
                t0=step/segments;t1=(step+1)/segments
                points=[]
                for t in [t0,t1]:
                    radius_t=radius*(t**1.1)
                    y=.3+height*t+3*math.sin(math.pi*t)
                    width=(1-t)**.8*(2.0 if ring==0 else 1.8)+.06
                    for side in [-1,1]:
                        points.append((8+(c*radius_t-s*width*side)*scale,y*scale,8+(s*radius_t+c*width*side)*scale))
                lower=[min(p[i] for p in points) for i in range(3)]
                upper=[max(p[i] for p in points) for i in range(3)]
                upper[1]+=max(.08,.22*scale)
                boxes.append((lower,upper,leaf%3))
    return boxes
def pina_elements():
    elements=[]
    for layer,width in enumerate([5,8,10.5,12,11.5,9.5,6.5]):
        for dx,dz in [(width*.30,width*.5),(width*.5,width*.30)]:
            a=[8-dx,layer*2,8-dz];b=[8+dx,layer*2+2,8+dz]
            faces={face:{'texture':'#pina_top' if face in ['up','down'] else '#pina'} for face in ['up','down','north','south','east','west']}
            elements.append({'from':a,'to':b,'faces':faces})
    return elements
pina_textures={'pina':'vinum:block/agave_pina','pina_top':'minecraft:block/birch_log_top','particle':'vinum:block/agave_pina'}
write(MODELS/'agave/pina.json',{'parent':'minecraft:block/block','textures':pina_textures,'elements':pina_elements()})
cells_by_age=[];shape_rows=[];connection_rows=[]
for age in range(8):
    parts={}
    for lo,hi,tint in leaf_boxes(age):
        for dx in range(math.floor(lo[0]/16),math.floor((hi[0]-.0001)/16)+1):
            for dy in range(math.floor(lo[1]/16),math.floor((hi[1]-.0001)/16)+1):
                for dz in range(math.floor(lo[2]/16),math.floor((hi[2]-.0001)/16)+1):
                    offset=[dx*16,dy*16,dz*16]
                    a=[round(max(lo[i],offset[i])-offset[i],3) for i in range(3)]
                    b=[round(min(hi[i],offset[i]+16)-offset[i],3) for i in range(3)]
                    if any(b[i]<=a[i] for i in range(3)):continue
                    faces={face:{'texture':'#leaf','tintindex':3 if face=='up' else 4 if face=='down' else tint} for face in ['up','down','north','south','east','west']}
                    parts.setdefault((dx,dy,dz),[]).append({'from':a,'to':b,'faces':faces})
    assert all(-1<=x<=1 and 0<=y<=2 and -1<=z<=1 for x,y,z in parts)
    cells_by_age.append(sorted(parts))
    # Connect only cells whose actual blade geometry touches across their shared face.
    for cell, elements in parts.items():
        for axis in range(3):
            neighbor=list(cell);neighbor[axis]+=1;neighbor=tuple(neighbor)
            if neighbor not in parts:continue
            others=[i for i in range(3) if i!=axis]
            if any(abs(a['to'][axis]-16)<.002 and abs(b['from'][axis])<.002
                   and all(min(a['to'][i],b['to'][i])-max(a['from'][i],b['from'][i])>0 for i in others)
                   for a in elements for b in parts[neighbor]):
                connection_rows.append([age,*cell,*neighbor])
    for (x,y,z),elements in parts.items():
        name=f'crop_agave_stage{age}' if (x,y,z)==(0,0,0) else f'agave/age{age}_{x+1}_{y}_{z+1}'
        write(MODELS/(name+'.json'),{'parent':'minecraft:block/block','ambientocclusion':False,'textures':{'leaf':'minecraft:block/white_concrete',**pina_textures},'elements':elements + (pina_elements() if age==7 and (x,y,z)==(0,0,0) else [])})
        lo=[min(e['from'][i] for e in elements) for i in range(3)]
        hi=[max(e['to'][i] for e in elements) for i in range(3)]
        shape_rows.append([age,x,y,z]+lo+hi)
write(MODELS/'agave/empty.json',{'parent':'minecraft:block/block','textures':{'particle':'minecraft:block/white_concrete'},'elements':[]})
write(RES/'assets/vinum/blockstates/crop_agave.json',{'variants':{f'age={age},stripped={str(stripped).lower()}':{'model':'vinum:block/agave/pina' if age==7 and stripped else f'vinum:block/crop_agave_stage{age}'} for age in range(8) for stripped in [False,True]}})
variants={}
for age in range(8):
    for x in range(-1,2):
        for y in range(3):
            for z in range(-1,2):
                name=f'agave/age{age}_{x+1}_{y}_{z+1}' if (x,y,z) in cells_by_age[age] and (x,y,z)!=(0,0,0) else 'agave/empty'
                variants[f'age={age},root_x={x+1},root_y={y},root_z={z+1}']={'model':'vinum:block/'+name}
write(RES/'assets/vinum/blockstates/agave_leaves.json',{'variants':variants})
write(RES/'data/vinum/loot_table/blocks/agave_leaves.json',{'type':'minecraft:block','pools':[]})
rows=',\n        '.join('{'+','.join(map(str,row))+'}' for row in shape_rows)
connections=',\n        '.join('{'+','.join(map(str,row))+'}' for row in connection_rows)
java='''package io.vinum.block;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.*;

/** Generated from the same leaf geometry as the models; do not edit by hand. */
public final class AgaveGeometry {
    private static final Map<Integer,List<BlockPos>> CELLS=new HashMap<>();
    private static final Map<String,VoxelShape> SHAPES=new HashMap<>();
    private static final Map<String,List<BlockPos>> CONNECTIONS=new HashMap<>();
    static {
        double[][] rows={
        ROWS
        };
        for(double[] row:rows){
            int age=(int)row[0],x=(int)row[1],y=(int)row[2],z=(int)row[3];
            CELLS.computeIfAbsent(age,k->new ArrayList<>()).add(new BlockPos(x,y,z));
            SHAPES.put(key(age,x,y,z),Block.box(row[4],row[5],row[6],row[7],row[8],row[9]));
        }
        CELLS.replaceAll((age,cells)->List.copyOf(cells));
        int[][] edges={CONNECTION_ROWS};
        for(int[] edge:edges){
            BlockPos a=new BlockPos(edge[1],edge[2],edge[3]),b=new BlockPos(edge[4],edge[5],edge[6]);
            CONNECTIONS.computeIfAbsent(key(edge[0],a.getX(),a.getY(),a.getZ()),k->new ArrayList<>()).add(b);
            CONNECTIONS.computeIfAbsent(key(edge[0],b.getX(),b.getY(),b.getZ()),k->new ArrayList<>()).add(a);
        }
        CONNECTIONS.replaceAll((key,cells)->List.copyOf(cells));
    }
    private AgaveGeometry(){}
    private static String key(int age,int x,int y,int z){return age+":"+x+":"+y+":"+z;}
    public static List<BlockPos> cells(int age){return CELLS.getOrDefault(age,List.of());}
    public static List<BlockPos> connections(int age,BlockPos cell){return CONNECTIONS.getOrDefault(key(age,cell.getX(),cell.getY(),cell.getZ()),List.of());}
    public static VoxelShape shape(int age,int x,int y,int z){return SHAPES.getOrDefault(key(age,x,y,z),Shapes.empty());}
}
'''.replace('CONNECTION_ROWS',connections).replace('ROWS',rows)
(ROOT/'src/main/java/io/vinum/block/AgaveGeometry.java').write_text(java)
print('Agave cells by stage:',[len(c) for c in cells_by_age])
