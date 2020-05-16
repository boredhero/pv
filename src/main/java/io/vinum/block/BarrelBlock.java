package io.vinum.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.IBlockReader;

public class BarrelBlock extends Block {
	
	public BarrelBlock(Block.Properties propertiesIn) {
		super(propertiesIn);
        
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		//Return a new instance of the barrel tile entity.
		return new TileEntity(TileEntityType.FURNACE){};
	}
	
}