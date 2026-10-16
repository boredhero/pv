package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class StillMultiblockPart3Block extends StillPartBlock {
    public static final MapCodec<StillMultiblockPart3Block> CODEC = simpleCodec(StillMultiblockPart3Block::new);

    public StillMultiblockPart3Block(Properties properties) {
        super(properties, 3, VoxelShapeHelper.addShapes(VoxelShapeHelper.box( 12.0D, 1.0D, 6.0D, 14.0D, 3.0D, 8.0D), VoxelShapeHelper.box( 8.0D, 3.0D, 5.0D, 13.0D, 5.0D, 7.0D), VoxelShapeHelper.box( 6.0D, 4.0D, 6.0D, 8.0D, 6.0D, 8.0D), VoxelShapeHelper.box( 5.0D, 5.0D, 8.0D, 7.0D, 7.0D, 13.0D), VoxelShapeHelper.box( 6.0D, 6.0D, 13.0D, 8.0D, 8.0D, 15.0D), VoxelShapeHelper.box( 8.0D, 7.0D, 14.0D, 13.0D, 9.0D, 16.0D), VoxelShapeHelper.box( 13.0D, 8.0D, 13.0D, 15.0D, 10.0D, 15.0D), VoxelShapeHelper.box( 14.0D, 9.0D, 8.0D, 16.0D, 11.0D, 13.0D)));
    }

    @Override
    public MapCodec<StillMultiblockPart3Block> codec() {
        return CODEC;
    }
}
