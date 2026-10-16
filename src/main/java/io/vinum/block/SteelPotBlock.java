package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class SteelPotBlock extends SteelComponentBlock {
    public static final MapCodec<SteelPotBlock> CODEC = simpleCodec(SteelPotBlock::new);

    public SteelPotBlock(Properties properties) {
        super(properties, Shapes.or(Shapes.or(Shapes.or(VoxelShapeHelper.box(1.0D, 2.0D, 1.0D, 15.0D, 14.0D, 15.0D), VoxelShapeHelper.box(3.0D, 1.0D, 3.0D, 13.0D, 2.0D, 13.0D)), Shapes.or(Shapes.or(VoxelShapeHelper.box(4.0D, 1.0D, 2.0D, 12.0D, 2.0D, 3.0D), VoxelShapeHelper.box(2.0D, 1.0D, 4.0D, 3.0D, 2.0D, 12.0D)), Shapes.or(VoxelShapeHelper.box(4.0D, 1.0D, 14.0D, 12.0D, 2.0D, 13.0D), VoxelShapeHelper.box(14.0D, 1.0D, 4.0D, 13.0D, 2.0D, 12.0D))), Shapes.or(Shapes.or(VoxelShapeHelper.box(2.0D, 0.0D, 2.0D, 4.0D, 1.0D, 4.0D), VoxelShapeHelper.box(14.0D, 0.0D, 2.0D, 12.0D, 1.0D, 4.0D)), Shapes.or(VoxelShapeHelper.box(2.0D, 0.0D, 14.0D, 4.0D, 1.0D, 12.0D), VoxelShapeHelper.box(14.0D, 0.0D, 14.0D, 12.0D, 1.0D, 12.0D))))));
    }

    @Override
    public MapCodec<SteelPotBlock> codec() {
        return CODEC;
    }
}
