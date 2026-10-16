package io.vinum.multiblock;

import io.vinum.block.ModBlocks;
import io.vinum.block.SteelComponentBlock;
import io.vinum.block.StillPartBlock;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Atomic assembly and reversible teardown of the original two-by-two tequila still. */
public final class StillMultiblock {
    private record Assembly(Level level, BlockPos master) {}
    private static final Set<Assembly> DISASSEMBLING = new HashSet<>();

    private StillMultiblock() {}

    public static BlockPos position(BlockPos master, Direction facing, int part) {
        return switch (part) {
            case 1 -> master;
            case 2 -> master.above();
            case 3 -> master.relative(facing.getCounterClockWise()).above();
            case 4 -> master.relative(facing.getCounterClockWise());
            default -> throw new IllegalArgumentException("Invalid still part: " + part);
        };
    }

    public static BlockPos masterPosition(BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof StillPartBlock part)) return pos;
        Direction right = state.getValue(SteelComponentBlock.FACING).getClockWise();
        return switch (part.part()) {
            case 2 -> pos.below();
            case 3 -> pos.relative(right).below();
            case 4 -> pos.relative(right);
            default -> pos;
        };
    }

    private static Block partBlock(int part) {
        return switch (part) {
            case 1 -> ModBlocks.STILL_MULTIBLOCK_PART_1.get();
            case 2 -> ModBlocks.STILL_MULTIBLOCK_PART_2.get();
            case 3 -> ModBlocks.STILL_MULTIBLOCK_PART_3.get();
            case 4 -> ModBlocks.STILL_MULTIBLOCK_PART_4.get();
            default -> throw new IllegalArgumentException("Invalid still part: " + part);
        };
    }

    private static Block rawBlock(int part) {
        return switch (part) {
            case 1 -> ModBlocks.STEEL_BRAZIER.get();
            case 3 -> ModBlocks.STEEL_COIL.get();
            default -> ModBlocks.STEEL_POT.get();
        };
    }

    public static boolean isLoaded(Level level, BlockPos master, Direction facing) {
        for (int part = 1; part <= 4; part++) if (!level.hasChunkAt(position(master, facing, part))) return false;
        return true;
    }

    public static boolean isComplete(Level level, BlockPos master, Direction facing) {
        if (!isLoaded(level, master, facing)) return false;
        for (int part = 1; part <= 4; part++) {
            BlockState state = level.getBlockState(position(master, facing, part));
            if (!state.is(partBlock(part)) || state.getValue(SteelComponentBlock.FACING) != facing) return false;
        }
        return true;
    }

    /** Called after any of the four raw components is placed. Never consumes an existing still. */
    public static boolean build(Level level, BlockPos placedPos) {
        if (level.isClientSide) return false;
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            for (int placedPart = 1; placedPart <= 4; placedPart++) {
                BlockPos offset = position(BlockPos.ZERO, facing, placedPart);
                BlockPos master = placedPos.subtract(offset);
                if (!isLoaded(level, master, facing)) continue;
                boolean matches = true;
                for (int part = 1; part <= 4; part++) {
                    if (!level.getBlockState(position(master, facing, part)).is(rawBlock(part))) {
                        matches = false;
                        break;
                    }
                }
                if (!matches) continue;
                for (int part = 1; part <= 4; part++) {
                    BlockPos pos = position(master, facing, part);
                    boolean water = level.getBlockState(pos).getValue(SteelComponentBlock.WATERLOGGED);
                    level.setBlock(pos, partBlock(part).defaultBlockState().setValue(SteelComponentBlock.FACING, facing)
                        .setValue(SteelComponentBlock.WATERLOGGED, water), Block.UPDATE_CLIENTS);
                }
                notifyNeighbors(level, master, facing);
                return true;
            }
        }
        return false;
    }

    public static BlockState originalState(BlockState partState) {
        StillPartBlock part = (StillPartBlock) partState.getBlock();
        return part.originalBlock().defaultBlockState()
            .setValue(SteelComponentBlock.FACING, partState.getValue(SteelComponentBlock.FACING))
            .setValue(SteelComponentBlock.WATERLOGGED, partState.getValue(SteelComponentBlock.WATERLOGGED));
    }

    /** The removed block drops through its loot table; the other components stay in the world. */
    public static void disassemble(Level level, BlockPos removedPos, BlockState removedState) {
        if (level.isClientSide) return;
        BlockPos master = masterPosition(removedPos, removedState);
        Direction facing = removedState.getValue(SteelComponentBlock.FACING);
        Assembly assembly = new Assembly(level, master);
        if (!DISASSEMBLING.add(assembly)) return;
        try {
            for (int part = 1; part <= 4; part++) {
                BlockPos pos = position(master, facing, part);
                if (pos.equals(removedPos) || !level.hasChunkAt(pos)) continue;
                BlockState state = level.getBlockState(pos);
                if (state.is(partBlock(part)) && state.getValue(SteelComponentBlock.FACING) == facing) {
                    level.setBlock(pos, originalState(state), Block.UPDATE_CLIENTS);
                }
            }
            notifyNeighbors(level, master, facing);
        } finally {
            DISASSEMBLING.remove(assembly);
        }
    }

    private static void notifyNeighbors(Level level, BlockPos master, Direction facing) {
        for (int part = 1; part <= 4; part++) {
            BlockPos pos = position(master, facing, part);
            if (level.hasChunkAt(pos)) level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock());
        }
    }
}
