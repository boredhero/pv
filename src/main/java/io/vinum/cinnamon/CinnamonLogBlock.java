package io.vinum.cinnamon;

import java.util.function.Supplier;
import io.vinum.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;

/** Peeling exposes a distinct stripped block, so replacing a harvested log cannot reset its bark. */
public final class CinnamonLogBlock extends RotatedPillarBlock {
    private final Supplier<? extends Block> stripped;
    public CinnamonLogBlock(Properties properties,Supplier<? extends Block> stripped){super(properties);this.stripped=stripped;}
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!held.canPerformAction(ItemAbilities.AXE_STRIP))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),held))return ItemInteractionResult.FAIL;
        if(level.isClientSide)return ItemInteractionResult.SUCCESS;
        level.setBlock(pos,stripped.get().defaultBlockState().setValue(AXIS,state.getValue(AXIS)),Block.UPDATE_ALL);
        if(!player.hasInfiniteMaterials()){
            popResource(level,pos,new ItemStack(ModItems.CINNAMON.get(),2));
            held.hurtAndBreak(1,player,hand==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
        }
        level.playSound(null,pos,SoundEvents.AXE_STRIP,SoundSource.BLOCKS,1,1);
        level.gameEvent(player,net.minecraft.world.level.gameevent.GameEvent.BLOCK_CHANGE,pos);
        return ItemInteractionResult.CONSUME;
    }
}
