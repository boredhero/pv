package io.vinum.cinnamon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/** Medium evergreen with a slender trunk and irregular rounded crown, not a coppiced plantation bush. */
public final class CinnamonTreeFeature extends Feature<CinnamonTreeFeature.Configuration>{
    public record Configuration(int minHeight,int maxHeight) implements FeatureConfiguration{
        public static final Codec<Configuration> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(4,16).optionalFieldOf("min_height",8).forGetter(Configuration::minHeight),
            Codec.intRange(4,16).optionalFieldOf("max_height",12).forGetter(Configuration::maxHeight)).apply(i,Configuration::new));
        public Configuration{if(minHeight<4||maxHeight>16||maxHeight<minHeight)throw new IllegalArgumentException("Invalid cinnamon height range");}
    }
    public CinnamonTreeFeature(){super(Configuration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<Configuration> context){return placeTree(context.level(),context.random(),context.origin(),context.config());}
    private static boolean loaded(WorldGenLevel level,BlockPos pos){
        return level.getChunk(SectionPos.blockToSectionCoord(pos.getX()),SectionPos.blockToSectionCoord(pos.getZ()),ChunkStatus.EMPTY,false)!=null;
    }
    private static boolean replaceable(BlockState state){
        return state.getFluidState().isEmpty()&&(state.isAir()||state.is(CinnamonContent.SAPLING.get())||state.is(BlockTags.REPLACEABLE_BY_TREES)
            &&!(state.getBlock() instanceof LeavesBlock&&state.getValue(LeavesBlock.PERSISTENT)));
    }
    public static boolean placeTree(WorldGenLevel level,RandomSource random,BlockPos origin,Configuration config){
        if(!loaded(level,origin.below())||!(level.getBlockState(origin.below()).is(BlockTags.DIRT)||level.getBlockState(origin.below()).is(Blocks.FARMLAND)))return false;
        int height=config.minHeight()+random.nextInt(config.maxHeight()-config.minHeight()+1);
        Map<BlockPos,BlockState> logs=new LinkedHashMap<>();Set<BlockPos> leaves=new LinkedHashSet<>();
        for(int y=0;y<height-1;y++)logs.put(origin.above(y),log(Direction.Axis.Y));
        crown(leaves,origin.above(height-2),3,random);
        for(Direction direction:Direction.Plane.HORIZONTAL){
            int y=height-4+random.nextInt(2);BlockPos branch=origin.above(y);
            int reach=1+random.nextInt(2);
            for(int step=0;step<reach;step++){branch=branch.relative(direction);logs.put(branch,log(direction.getAxis()));}
            logs.put(branch.above(),log(Direction.Axis.Y));crown(leaves,branch.above(),2,random);
        }
        leaves.removeAll(logs.keySet());Set<BlockPos> footprint=new HashSet<>(leaves);footprint.addAll(logs.keySet());
        for(BlockPos pos:footprint)if(pos.getY()<level.getMinBuildHeight()||pos.getY()>=level.getMaxBuildHeight()||!loaded(level,pos)||!level.ensureCanWrite(pos)||!replaceable(level.getBlockState(pos)))return false;
        logs.forEach((pos,state)->level.setBlock(pos,state,Block.UPDATE_CLIENTS));
        for(BlockPos pos:leaves){
            int distance=7;for(BlockPos log:logs.keySet())distance=Math.min(distance,pos.distManhattan(log));
            if(distance<7)level.setBlock(pos,CinnamonContent.LEAVES.get().defaultBlockState().setValue(LeavesBlock.DISTANCE,Math.max(1,distance)),Block.UPDATE_CLIENTS);
        }
        return true;
    }
    private static BlockState log(Direction.Axis axis){return CinnamonContent.LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,axis);}
    private static void crown(Set<BlockPos> leaves,BlockPos center,int radius,RandomSource random){
        for(int y=-2;y<=2;y++)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            double distance=(x*x+z*z)/(double)(radius*radius)+y*y/6.0;
            if(distance<=1.25&&(distance<.9||random.nextInt(4)!=0))leaves.add(center.offset(x,y,z));
        }
    }
}
