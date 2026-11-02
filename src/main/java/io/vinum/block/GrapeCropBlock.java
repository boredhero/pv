package io.vinum.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.vinum.crop.GrapeVariety;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low grape vines grow on cultivated farmland and occur naturally on temperate soil. */
public final class GrapeCropBlock extends CropBlock {
    public static final MapCodec<GrapeCropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            GrapeVariety.CODEC.fieldOf("variety").forGetter(GrapeCropBlock::getVariety),
            propertiesCodec()).apply(instance, (variety, properties) -> new GrapeCropBlock(properties, variety)));
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {
            Block.box(3, 0, 3, 13, 4, 13), Block.box(2, 0, 2, 14, 8, 14),
            Block.box(1, 0, 1, 15, 12, 15), Block.box(1, 0, 1, 15, 16, 15)
    };
    private final GrapeVariety variety;

    public GrapeCropBlock(Properties properties, GrapeVariety variety) {
        super(properties);
        this.variety = variety;
    }

    @Override
    public MapCodec<GrapeCropBlock> codec() { return CODEC; }
    public GrapeVariety getVariety() { return variety; }
    @Override
    public IntegerProperty getAgeProperty() { return AGE; }
    @Override
    public int getMaxAge() { return 3; }
    @Override
    protected ItemLike getBaseSeedId() { return variety.seedItem(); }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) { return 1; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }
}
