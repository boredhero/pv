package io.vinum.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;

public class VoxelShapeHelper {
	
	public static VoxelShape createRotatableShape(BlockState state, double x1, double y1, double z1, double x2, double y2, double z2) {
		
		VoxelShape[] buffer = new VoxelShape[] {Block.makeCuboidShape(x1, y1, z1, x2, y2, z2), VoxelShapes.empty()};
		
		int times = (state.get(BlockStateProperties.HORIZONTAL_FACING).getHorizontalIndex() - Direction.NORTH.getHorizontalIndex() + 4) % 4;
		
		for (int i = 0; i < times; i++) {
			
			buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1], VoxelShapes.create(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
			buffer[0] = buffer[1];
			buffer[1] = VoxelShapes.empty();
			
		}
		
		return buffer[0];
			
	}
	
	public static VoxelShape createRotatableShape(BlockState state, Direction baseDirection, double x1, double y1, double z1, double x2, double y2, double z2) {
		
		VoxelShape[] buffer = new VoxelShape[] {Block.makeCuboidShape(x1, y1, z1, x2, y2, z2), VoxelShapes.empty()};
		
		int times = (state.get(BlockStateProperties.HORIZONTAL_FACING).getHorizontalIndex() - baseDirection.getHorizontalIndex() + 4) % 4;
		
		for (int i = 0; i < times; i++) {
			
			buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1], VoxelShapes.create(1-maxZ, minY, minX, 1-minZ, maxY, maxX)));
			buffer[0] = buffer[1];
			buffer[1] = VoxelShapes.empty();
			
		}
		
		return buffer[0];
			
	}
	
	public static VoxelShape addShapes(VoxelShape... shapes) {
		
		VoxelShape returnShape = Block.makeCuboidShape(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
		
		for (VoxelShape shape : shapes) {
			
			returnShape = VoxelShapes.or(returnShape, shape);
			
		}
		
		return returnShape;
		
	}
	
}