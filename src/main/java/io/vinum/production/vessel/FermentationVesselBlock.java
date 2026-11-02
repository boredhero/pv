package io.vinum.production.vessel;

import io.vinum.barrel.BarrelBottling;
import io.vinum.item.ModItems;
import io.vinum.production.ProductionContainers;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

public abstract class FermentationVesselBlock extends Block implements EntityBlock {
    protected FermentationVesselBlock(Properties properties) {
        super(properties.noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK));
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new FermentationVesselBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (world, pos, blockState, entity) -> {
            if (entity instanceof FermentationVesselBlockEntity vessel) FermentationVesselBlockEntity.serverTick(world, pos, blockState, vessel);
        };
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel) vessel.setStructure(pos, 1);
        schedule(level, pos);
    }
    private void schedule(Level level, BlockPos pos) {
        if (!level.isClientSide && this instanceof FermentationVatBlock) level.scheduleTick(pos, this, 1);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) {
        super.onPlace(state, level, pos, old, moving);
        schedule(level, pos);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) { schedule(level, pos); }
    @Override protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { VesselMultiblock.reconcile(level, pos); }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel && vessel.isVat()) VesselMultiblock.removed(level, vessel);
            super.onRemove(state, level, pos, next, moving);
        }
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FermentationVesselBlockEntity vessel) {
            for (ItemStack drop : List.copyOf(drops)) {
                if (!drop.is(asItem())) continue;
                ItemStack owned = drop.getCount() > 1 ? drop.split(1) : drop;
                vessel.saveToItem(owned, params.getLevel().registryAccess());
                if (owned != drop) drops.add(owned);
                break;
            }
        }
        return drops;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel && !vessel.getLocalFluid().isEmpty()) {
            ItemStack drop = new ItemStack(this);
            vessel.saveToItem(drop, level.registryAccess());
            popResource(level, pos, drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(ModItems.WINE_PRESS.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!vessel.press()) return ItemInteractionResult.FAIL;
            player.displayClientMessage(vessel.getStatusMessage(), true);
            return ItemInteractionResult.CONSUME;
        }
        if (held.is(ModItems.WINE_YEAST.get())) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (!vessel.addStarter()) return ItemInteractionResult.FAIL;
            if (!player.hasInfiniteMaterials()) held.shrink(1);
            player.displayClientMessage(vessel.getStatusMessage(), true);
            return ItemInteractionResult.CONSUME;
        }
        var handler = vessel.getFluidHandler(hit.getDirection());
        ItemInteractionResult result = ProductionContainers.useItemOn(held, level, player, hand, handler);
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return result;
        result = BarrelBottling.useItemOn(held, level, player, hand, handler);
        if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return result;
        if (FluidUtil.getFluidHandler(held).isPresent()) return level.isClientSide || FluidUtil.interactWithFluidHandler(player, hand, level, pos, hit.getDirection())
                ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.FAIL;
        if (held.getItem() instanceof net.minecraft.world.item.BlockItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) player.displayClientMessage(vessel.getStatusMessage(), true);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown() && !vessel.startFermentation()) player.displayClientMessage(net.minecraft.network.chat.Component.translatable("vessel.vinum.cannot_start"), true);
                else player.displayClientMessage(vessel.getStatusMessage(), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return this instanceof FermentationVatBlock ? RenderShape.INVISIBLE : RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (this instanceof CarboyBlock) return Shapes.or(box(3, 0, 3, 13, 11, 13), box(6, 11, 6, 10, 16, 10));
        int size = 1, height = 1, x = 0, y = 0, z = 0;
        if (world.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel) {
            size = vessel.getStructureSize();
            height = vessel.getStructureHeight();
            x = pos.getX() - vessel.getStructureOrigin().getX();
            y = pos.getY() - vessel.getStructureOrigin().getY();
            z = pos.getZ() - vessel.getStructureOrigin().getZ();
        }
        return VesselShapes.shape(size, height, x, y, z);
    }
}
