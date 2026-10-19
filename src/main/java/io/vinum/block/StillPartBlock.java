package io.vinum.block;

import io.vinum.multiblock.StillMultiblock;
import io.vinum.tileentity.StillMasterTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Only part one owns an inventory; other parts resolve it from their position and facing. */
public abstract class StillPartBlock extends SteelComponentBlock {
    private final int part;

    protected StillPartBlock(Properties properties, int part, VoxelShape northShape) {
        super(properties, northShape);
        this.part = part;
    }

    public int part() { return part; }

    public Block originalBlock() {
        return switch (part) {
            case 1 -> ModBlocks.STEEL_BRAZIER.get();
            case 3 -> ModBlocks.STEEL_COIL.get();
            default -> ModBlocks.STEEL_POT.get();
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos masterPos = StillMultiblock.masterPosition(pos, state);
        if (level.getBlockEntity(masterPos) instanceof StillMasterTileEntity master) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.openMenu(master, masterPos);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(originalBlock());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState nextState, boolean moving) {
        if (!state.is(nextState.getBlock())) {
            if (!level.isClientSide) {
                if (level.getBlockEntity(pos) instanceof StillMasterTileEntity master) {
                    master.dropWash();
                    Containers.dropContents(level, pos, master);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
                StillMultiblock.disassemble(level, pos, state);
            }
            super.onRemove(state, level, pos, nextState, moving);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        super.onPlace(state, level, pos, oldState, moving);
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide) level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos master = StillMultiblock.masterPosition(pos, state);
        if (!StillMultiblock.isLoaded(level, master, state.getValue(FACING))) {
            level.scheduleTick(pos, this, 20);
        } else if (!StillMultiblock.isComplete(level, master, state.getValue(FACING))) {
            // Also repairs orphan pieces created by commands or separated across a chunk boundary.
            StillMultiblock.disassemble(level, pos, state);
            if (level.getBlockState(pos).is(this)) level.setBlock(pos, StillMultiblock.originalState(state), UPDATE_ALL);
        } else {
            // A pending block tick survives chunk saves, so a slave whose master was removed
            // while this chunk was unloaded also repairs itself after the chunk returns.
            level.scheduleTick(pos, this, 100);
        }
    }
}
