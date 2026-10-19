package io.vinum.block;

import com.mojang.serialization.MapCodec;
import io.vinum.multiblock.BarrelMultiblock;
import io.vinum.barrel.BarrelBottling;
import io.vinum.production.ProductionContainers;
import io.vinum.multiblock.BarrelShapes;
import io.vinum.tileentity.BarrelBlockEntity;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Universal storage barrel; legacy wine variants share this behavior and renderer. */
public class WineBarrelBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<WineBarrelBlock> CODEC = simpleCodec(WineBarrelBlock::new);

    public WineBarrelBlock(Properties properties) {
        super(properties.noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<WineBarrelBlock> codec() { return CODEC; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // A harvested barrel carries only its own escrow. Its former group coordinates
        // must never make placement at a new location claim another reservoir.
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) barrel.setStructure(pos, 1);
        BarrelMultiblock.enqueue(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean moving) {
        super.onPlace(state, level, pos, previous, moving);
        BarrelMultiblock.enqueue(level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        BarrelMultiblock.enqueue(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) BarrelMultiblock.removed(level, pos, barrel);
            super.onRemove(state, level, pos, next, moving);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BarrelBlockEntity barrel) {
            for (int index = 0; index < drops.size(); index++) {
                ItemStack stack = drops.get(index);
                if (!stack.is(asItem())) continue;
                if (stack.getCount() > 1) {
                    // Loot modifiers may add empty barrels, but cannot duplicate the escrow.
                    ItemStack filled = stack.split(1);
                    barrel.saveToItem(filled, params.getLevel().registryAccess());
                    drops.add(filled);
                } else {
                    barrel.saveToItem(stack, params.getLevel().registryAccess());
                }
                break;
            }
        }
        return drops;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // Match shulker-box behavior: creative removal of a nonempty reservoir preserves
        // its local escrow, while empty creative barrels do not litter the world.
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel
                && !barrel.getLocalFluid().isEmpty()) {
            ItemStack stack = new ItemStack(this);
            barrel.saveToItem(stack, level.registryAccess());
            popResource(level, pos, stack);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            ItemInteractionResult production = ProductionContainers.useItemOn(stack, level, player, hand, barrel.getFluidHandler(hit.getDirection()));
            if (production != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return production;
            ItemInteractionResult bottled = BarrelBottling.useItemOn(stack, level, player, hand, barrel.getFluidHandler(hit.getDirection()));
            if (bottled != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return bottled;
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide || FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            if (!level.isClientSide) player.displayClientMessage(barrel.getStatusMessage(), true);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            return BarrelShapes.shape(pos, barrel.getStructureOrigin(), barrel.getStructureSize(), state.getValue(FACING));
        }
        return BarrelShapes.shape(pos, pos, 1, state.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
