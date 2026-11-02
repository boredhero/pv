package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class StillMultiblockPart4Block extends StillPartBlock {
    public static final MapCodec<StillMultiblockPart4Block> CODEC = simpleCodec(StillMultiblockPart4Block::new);

    public StillMultiblockPart4Block(Properties properties) {
        super(properties, 4, VoxelShapeHelper.addShapes(VoxelShapeHelper.box( 3.0D, 0.0D, 2.0D, 15.0D, 1.0D, 14.0D), Shapes.or(VoxelShapeHelper.box( 2.0D, 1.0D, 2.0D, 3.0D, 13.0D, 14.0D), VoxelShapeHelper.box( 15.0D, 1.0D, 2.0D, 16.0D, 13.0D, 14.0D), VoxelShapeHelper.box( 3.0D, 1.0D, 1.0D, 15.0D, 13.0D, 2.0D), VoxelShapeHelper.box( 3.0D, 1.0D, 14.0D, 15.0D, 13.0D, 15.0D))));
    }

    @Override
    public MapCodec<StillMultiblockPart4Block> codec() {
        return CODEC;
    }
}
