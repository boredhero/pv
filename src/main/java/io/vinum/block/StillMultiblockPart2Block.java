package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class StillMultiblockPart2Block extends StillPartBlock {
    public static final MapCodec<StillMultiblockPart2Block> CODEC = simpleCodec(StillMultiblockPart2Block::new);

    public StillMultiblockPart2Block(Properties properties) {
        super(properties, 2, VoxelShapeHelper.addShapes(VoxelShapeHelper.box( 0.0D, 10.0D, 7.0D, 2.0D, 12.0D, 9.0D), VoxelShapeHelper.box(2.0D, 0.0D, 3.0D, 14.0D, 14.0D, 13.0D), VoxelShapeHelper.box(3.0D, 0.0D, 2.0D, 13.0D, 14.0D, 14.0D)));
    }

    @Override
    public MapCodec<StillMultiblockPart2Block> codec() {
        return CODEC;
    }
}
