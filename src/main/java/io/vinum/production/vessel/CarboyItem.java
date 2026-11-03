package io.vinum.production.vessel;

import io.vinum.item.FluidContainerItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import java.util.List;

/** One portable liquid container which also places the small-batch fermentation vessel. */
public final class CarboyItem extends FluidContainerItem {
    private final Block block;
    public CarboyItem(Block block, Properties properties) {
        super(properties, FermentationVesselBlockEntity.CARBOY_CAPACITY, false);
        this.block = block;
        // Bind block drops/pick-block to this single registered portable item.
        Item.BY_BLOCK.put(block, this);
    }
    @Override public String getDescriptionId() { return block.getDescriptionId(); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer();
        if (player != null && (!player.mayBuild() || !context.getLevel().mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(), context.getClickedFace(), context.getItemInHand())))
            return InteractionResult.FAIL;
        var tank = context.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                context.getClickedPos(), context.getClickedFace());
        boolean emptyPlacement = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                && contents(context.getItemInHand()).isEmpty();
        if (tank != null && context.getPlayer() != null && !emptyPlacement) {
            var result = io.vinum.beverage.ContainerTransfers.use(context.getItemInHand(), context.getLevel(),
                    context.getPlayer(), context.getHand(), tank,
                    context.getLevel().getBlockEntity(context.getClickedPos()) instanceof FermentationVesselBlockEntity
                        ? !contents(context.getItemInHand()).isEmpty() : context.getPlayer().isShiftKeyDown());
            return result.consumesAction() ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.FAIL;
        }
        return place(new BlockPlaceContext(context));
    }
    private InteractionResult place(BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (!block.isEnabled(level.enabledFeatures()) || !context.canPlace()
                || player != null && (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack))) return InteractionResult.FAIL;
        var state = block.getStateForPlacement(context);
        var collision = player == null ? net.minecraft.world.phys.shapes.CollisionContext.empty()
                : net.minecraft.world.phys.shapes.CollisionContext.of(player);
        if (state == null || !state.canSurvive(level, pos) || !level.isUnobstructed(state, pos, collision))
            return InteractionResult.FAIL;
        if (!level.setBlock(pos, state, 11)) return InteractionResult.FAIL;
        BlockItem.updateCustomBlockEntityTag(level, player, pos, stack);
        if (level.getBlockEntity(pos) instanceof FermentationVesselBlockEntity vessel) {
            vessel.applyComponentsFromItemStack(stack);
            vessel.setChanged();
        }
        block.setPlacedBy(level, pos, state, player, stack);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
            net.minecraft.advancements.CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        var sound = state.getSoundType(level, pos, player);
        level.playSound(player, pos, sound.getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS,
                (sound.getVolume() + 1F) / 2F, sound.getPitch() * .8F);
        level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_PLACE, pos,
                net.minecraft.world.level.gameevent.GameEvent.Context.of(player, state));
        stack.consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void contentsChanged(ItemStack stack) {
        // A carried fermentation snapshot is only valid for its original, untouched batch.
        var saved = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (saved == null) return;
        var tag = saved.copyTag();
        tag.remove("Fluid");
        tag.remove("FermentationPlan");
        tag.remove("FermentationProgress");
        tag.remove("Starter");
        stack.set(DataComponents.BLOCK_ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.vinum.carboy").withStyle(ChatFormatting.GRAY));
        var saved = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (saved != null && saved.copyTag().getBoolean("Airlock"))
            lines.add(Component.translatable("tooltip.vinum.carboy_airlock").withStyle(ChatFormatting.GRAY));
        if (saved != null && saved.copyTag().contains("FermentationPlan"))
            lines.add(Component.translatable("tooltip.vinum.carboy_paused").withStyle(ChatFormatting.GOLD));
    }
}
