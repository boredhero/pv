package io.vinum.sugar;

import com.mojang.serialization.MapCodec;
import io.vinum.beverage.ContainerTransfers;
import io.vinum.block.SteelBrazierBlock;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class SugarKettleBlock extends BaseEntityBlock {
    public static final MapCodec<SugarKettleBlock> CODEC=simpleCodec(SugarKettleBlock::new);
    public SugarKettleBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(1,0,1,15,14,15);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SugarKettleBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,SugarContent.ENTITY.get(),SugarKettleBlockEntity::tick);}
    public static boolean heated(Level level,BlockPos pos){
        var below=level.getBlockState(pos.below());
        return below.getBlock() instanceof CampfireBlock&&below.getValue(CampfireBlock.LIT)
            ||below.getBlock() instanceof SteelBrazierBlock&&below.getValue(SteelBrazierBlock.LIT)&&!below.getValue(BlockStateProperties.WATERLOGGED);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!player.mayBuild()||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,hit.getDirection(),stack)||!(level.getBlockEntity(pos) instanceof SugarKettleBlockEntity kettle))return ItemInteractionResult.FAIL;
        if(stack.is(Items.HONEY_BOTTLE)) {
            if(!level.isClientSide) {
                if(kettle.addHoney()) {
                    // Consume exactly one input in both game modes, returning its physical container.
                    stack.shrink(1);
                    var empty=new ItemStack(Items.GLASS_BOTTLE);
                    if(stack.isEmpty())player.setItemInHand(hand,empty);
                    else if(!player.getInventory().add(empty))player.drop(empty,false);
                } else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vinum.honey_mixing_failed"),true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(stack.is(SugarContent.SEPARATION_INSERT.get())) {
            if(!level.isClientSide&&kettle.installInsert())stack.shrink(1);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(stack.is(Items.WATER_BUCKET)) {
            if(!level.isClientSide&&kettle.fluids().fill(new FluidStack(Fluids.WATER,1000),FluidAction.SIMULATE)==1000){
                kettle.fluids().fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE);
                player.setItemInHand(hand,ItemUtils.createFilledResult(stack,player,new ItemStack(Items.BUCKET)));
            }return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(stack.is(SugarContent.PANELA.get())||stack.is(Items.SUGAR)) {
            if(!level.isClientSide){if(kettle.dissolve(stack))stack.shrink(1);else player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vinum.kettle_dissolve_failed"),true);}
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ContainerTransfers.use(stack,level,player,hand,kettle.fluids());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!player.mayBuild()||!level.mayInteract(player,pos)||!(level.getBlockEntity(pos) instanceof SugarKettleBlockEntity kettle))return InteractionResult.PASS;
        if(!level.isClientSide){
            if(player.isShiftKeyDown())kettle.cycleMode();
            else if(!kettle.solidOutput().isEmpty()){var result=kettle.takeOutput();if(!player.getInventory().add(result))player.drop(result,false);}
            else if(!kettle.processing()&&!kettle.start())player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.vinum.kettle_start_failed"),false);
            player.displayClientMessage(kettle.status(),true);
        }return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=new ArrayList<>(super.getDrops(state,params));
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof SugarKettleBlockEntity kettle)
            for(var drop:drops)if(drop.is(asItem())){kettle.saveToItem(drop,params.getLevel().registryAccess());break;}
        return drops;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&level.getBlockEntity(pos) instanceof SugarKettleBlockEntity kettle
            &&(!kettle.contents().isEmpty()||!kettle.solidOutput().isEmpty()||kettle.hasInsert())){
            var drop=new ItemStack(this);kettle.saveToItem(drop,level.registryAccess());popResource(level,pos,drop);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.world.level.material.PushReaction.BLOCK;}
}
