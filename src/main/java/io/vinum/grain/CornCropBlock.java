package io.vinum.grain;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Root owns growth and loot. The upper stalk reserves real space and never duplicates a harvest. */
public final class CornCropBlock extends CropBlock {
    public static final MapCodec<CornCropBlock> CODEC=simpleCodec(CornCropBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public CornCropBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(AGE,0).setValue(HALF,DoubleBlockHalf.LOWER));}
    @Override public MapCodec<CornCropBlock> codec(){return CODEC;}
    @Override protected ItemLike getBaseSeedId(){return RyeCornContent.CORN_SEEDS.get();}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(AGE,HALF);}
    private boolean upper(BlockState state){return state.getValue(HALF)==DoubleBlockHalf.UPPER;}
    @Override protected boolean isRandomlyTicking(BlockState state){return !upper(state)&&!isMaxAge(state);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return Block.box(2,0,2,14,upper(state)?Math.max(2,8+(getAge(state)-4)*8.0/3):Math.min(16,3+getAge(state)*3),14);
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        if(!upper(state))return super.canSurvive(state,level,pos);
        var below=level.getBlockState(pos.below());return below.is(this)&&!upper(below)&&getAge(below)>=4;
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(upper(state)||isMaxAge(state)||!level.isAreaLoaded(pos,1)||level.getRawBrightness(pos,0)<9)return;
        if(net.neoforged.neoforge.common.CommonHooks.canCropGrow(level,pos,state,random.nextInt((int)(25F/getGrowthSpeed(state,level,pos))+1)==0)){
            growTo(level,pos,state,getAge(state)+1);
            net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level,pos,state);
        }
    }
    public void growTo(Level level,BlockPos pos,BlockState state,int target){
        if(level.isClientSide)return;
        if(upper(state)){pos=pos.below();state=level.getBlockState(pos);if(!state.is(this))return;}
        int age=Math.clamp(target,getAge(state),7);
        var above=level.getBlockState(pos.above());
        if(age>=4&&!above.isAir()&&!(above.is(this)&&upper(above)))age=Math.min(age,3);
        if(age<getAge(state))return;
        level.setBlock(pos,getStateForAge(age),Block.UPDATE_ALL);
        if(age>=4)level.setBlock(pos.above(),getStateForAge(age).setValue(HALF,DoubleBlockHalf.UPPER),Block.UPDATE_ALL);
    }
    @Override public void growCrops(Level level,BlockPos pos,BlockState state){
        BlockPos root=upper(state)?pos.below():pos;var rootState=level.getBlockState(root);
        if(rootState.is(this))growTo(level,root,rootState,getAge(rootState)+getBonemealAgeIncrease(level));
    }
    @Override public boolean isValidBonemealTarget(LevelReader level,BlockPos pos,BlockState state){
        var root=upper(state)?pos.below():pos;var crop=level.getBlockState(root);
        if(!crop.is(this)||isMaxAge(crop))return false;
        var above=level.getBlockState(root.above());
        return getAge(crop)<3||above.isAir()||above.is(this)&&upper(above);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!replacement.is(this)&&!level.isClientSide){
            BlockPos other=upper(state)?pos.below():pos.above();var partner=level.getBlockState(other);
            if(partner.is(this)&&upper(partner)!=upper(state)){
                if(upper(state))level.destroyBlock(other,true);
                else level.removeBlock(other,false);
            }
        }
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&upper(state)){
            var below=pos.below();if(level.getBlockState(below).is(this))level.removeBlock(below,false);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
}
