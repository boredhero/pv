package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class SteelCoilBlock extends SteelComponentBlock {
    public static final MapCodec<SteelCoilBlock> CODEC = simpleCodec(SteelCoilBlock::new);

    public SteelCoilBlock(Properties properties) {
        super(properties, VoxelShapeHelper.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D));
    }

    @Override
    public MapCodec<SteelCoilBlock> codec() {
        return CODEC;
    }
}
