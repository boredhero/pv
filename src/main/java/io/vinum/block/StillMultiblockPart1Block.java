package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.util.VoxelShapeHelper;
import net.minecraft.world.phys.shapes.Shapes;

public class StillMultiblockPart1Block extends StillPartBlock implements net.minecraft.world.level.block.EntityBlock {
    public static final MapCodec<StillMultiblockPart1Block> CODEC = simpleCodec(StillMultiblockPart1Block::new);

    public StillMultiblockPart1Block(Properties properties) {
        super(properties, 1, VoxelShapeHelper.addShapes(VoxelShapeHelper.box(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D), VoxelShapeHelper.box(0.0D, 1.0D, 0.0D, 16.0D, 15.0D, 16.0D)));
    }

    @Override
    public MapCodec<StillMultiblockPart1Block> codec() {
        return CODEC;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        return new io.vinum.tileentity.StillMasterTileEntity(pos, state);
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return !level.isClientSide && type == io.vinum.tileentity.ModTileEntities.STILL_MASTER.get()
            ? (world, pos, blockState, entity) -> io.vinum.tileentity.StillMasterTileEntity.serverTick(world, pos, blockState, (io.vinum.tileentity.StillMasterTileEntity) entity)
            : null;
    }
}
