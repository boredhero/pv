package io.vinum.blender;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public final class BlenderBlock extends BaseEntityBlock {
    public static final MapCodec<BlenderBlock> CODEC=simpleCodec(BlenderBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public BlenderBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.rotate(mirror.getRotation(state.getValue(FACING)));}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new BlenderBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,BlenderContent.ENTITY.get(),BlenderBlockEntity::tick);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!player.mayBuild()||!level.mayInteract(player,pos)||!(level.getBlockEntity(pos) instanceof BlenderBlockEntity plant))return InteractionResult.PASS;
        if(player instanceof ServerPlayer server)server.openMenu(plant,buf->buf.writeBlockPos(pos));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        var drops=new ArrayList<>(super.getDrops(state,params));
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BlenderBlockEntity plant)
            for(var drop:drops)if(drop.is(asItem())){plant.saveToItem(drop,params.getLevel().registryAccess());break;}
        return drops;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&level.getBlockEntity(pos) instanceof BlenderBlockEntity plant){
            boolean filled=!plant.a().isEmpty()||!plant.output().isEmpty()||plant.processing()||!plant.b().isEmpty();
            for(int i=0;i<plant.inventory.getSlots();i++)filled|=!plant.inventory.getStackInSlot(i).isEmpty();
            if(filled){var drop=new ItemStack(this);plant.saveToItem(drop,level.registryAccess());popResource(level,pos,drop);}
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public net.minecraft.world.level.material.PushReaction getPistonPushReaction(BlockState state){return net.minecraft.world.level.material.PushReaction.BLOCK;}
}
