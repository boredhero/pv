package io.vinum.worldgen;

import java.util.Random;
import java.util.function.Function;

import com.mojang.datafixers.Dynamic;

import io.vinum.block.AgaveCropBlock;
import io.vinum.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationSettings;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;

public class FeatureAgave extends Feature<NoFeatureConfig> {
	
	public FeatureAgave(Function<Dynamic<?>, ? extends NoFeatureConfig> configFactory) {
		super(configFactory);
		
	}
	
	public boolean check(IWorld world, BlockPos pos) {
		
		BlockState bsPos = world.getBlockState(pos);
		BlockState bsPosDown = world.getBlockState(pos.down());

		if (bsPosDown.getBlock().isIn(BlockTags.SAND) && bsPos.getMaterial().isReplaceable() && bsPos == Blocks.AIR.getDefaultState()) {
			
			return true;
			
		} else {
			
			return false;
			
		}
		
	}
	
	public void generateAgaveFeature(IWorld world, BlockPos pos, Random random, int type) {
		
		world.setBlockState(pos, ModBlocks.CROP_AGAVE.get().getDefaultState().with(AgaveCropBlock.AGE, 2), 2);
		
	}
	
	@Override
	public boolean place(IWorld world, ChunkGenerator<? extends GenerationSettings> generator, Random random, BlockPos pos, NoFeatureConfig config) {
		
		int type = (int) ((Math.random() * 4) + 1);
		
		for (int i = 0; i < type; i++) {
			
			if (i == 0) {
				
				int init = (int) ((Math.random() * 2) + 1);
				int off = (int) ((Math.random() * 2) + 1);
				
				if (check(world, pos.north(init).west(off))) {
					
					generateAgaveFeature(world, pos.north(init).west(off), random, type);
					
				}
				
			}
			
			if (i == 1) {
				
				int init = (int) ((Math.random() * 2) + 1);
				int off = (int) ((Math.random() * 2) + 1);
				
				if (check(world, pos.south(init).east(off))) {
					
					generateAgaveFeature(world, pos.south(init).east(off), random, type);
					
				}
				
			}
			
			if (i == 2) {
				
				int init = (int) ((Math.random() * 2) + 1);
				int off = (int) ((Math.random() * 2) + 1);
				
				if (check(world, pos.north(init).east(off))) {
					
					generateAgaveFeature(world, pos.north(init).east(off), random, type);
					
				}
				
			} else {
				
				int init = (int) ((Math.random() * 2) + 1);
				int off = (int) ((Math.random() * 2) + 1);
				
				if (check(world, pos.south(init).west(off))) {
					
					generateAgaveFeature(world, pos.south(init).west(off), random, type);
					
				}
				
			}
			
		}
		
		return true;
		
	}
	
}