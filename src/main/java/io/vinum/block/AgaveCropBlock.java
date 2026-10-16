package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AgaveCropBlock extends CropBlock {
    public static final MapCodec<AgaveCropBlock> CODEC = simpleCodec(AgaveCropBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    private static final VoxelShape[] SHAPES = {
        Block.box(0, 0, 0, 16, 6, 16), Block.box(0, 0, 0, 16, 11, 16), Block.box(0, 0, 0, 16, 14, 16)
    };

    public AgaveCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AgaveCropBlock> codec() { return CODEC; }

    @Override
    public IntegerProperty getAgeProperty() { return AGE; }

    @Override
    public int getMaxAge() { return 2; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.SAND) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected ItemLike getBaseSeedId() { return ModItems.AGAVE_SEEDS.get(); }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
