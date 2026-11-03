package io.vinum.test;

import io.vinum.grain.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class GrainCropGameTests {
    private static BlockPos plant(GameTestHelper h,Block block,int x){
        var pos=h.absolutePos(new BlockPos(x,2,3));
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.FARMLAND.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());return pos;
    }
    private static int drops(GameTestHelper h,BlockPos pos,Item item){
        return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    @GameTest(template="barrel_space")
    public static void ryeGrowsAndHarvestsRenewableSeeds(GameTestHelper h){
        var rye=RyeCornContent.RYE_CROP.get();var pos=plant(h,rye,3);
        for(int i=0;i<8;i++)rye.performBonemeal(h.getLevel(),h.getLevel().random,pos,h.getLevel().getBlockState(pos));
        h.assertTrue(rye.isMaxAge(h.getLevel().getBlockState(pos)),"Rye matures through vanilla bonemeal growth");
        h.getLevel().destroyBlock(pos,true);
        h.assertTrue(drops(h,pos,RyeCornContent.RYE.get())==1,"Mature rye drops grain");
        h.assertTrue(drops(h,pos,RyeCornContent.RYE_SEEDS.get())>=1,"Rye always returns replantable seeds");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void cornReservesSpaceAndBonemealWorksFromEitherHalf(GameTestHelper h){
        var corn=RyeCornContent.CORN_CROP.get();var pos=plant(h,corn,3);
        h.getLevel().setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
        corn.growTo(h.getLevel(),pos,h.getLevel().getBlockState(pos),7);
        h.assertTrue(corn.getAge(h.getLevel().getBlockState(pos))==3,"Corn waits for upper stalk space");
        h.assertTrue(h.getLevel().getBlockState(pos.above()).is(Blocks.STONE),"Growth cannot overwrite blocks");
        h.getLevel().removeBlock(pos.above(),false);
        corn.growTo(h.getLevel(),pos,h.getLevel().getBlockState(pos),4);
        h.assertTrue(h.getLevel().getBlockState(pos.above()).is(corn),"Tall stage creates upper stalk");
        for(int i=0;i<4;i++)corn.performBonemeal(h.getLevel(),h.getLevel().random,pos.above(),h.getLevel().getBlockState(pos.above()));
        h.assertTrue(corn.isMaxAge(h.getLevel().getBlockState(pos))&&corn.isMaxAge(h.getLevel().getBlockState(pos.above())),"Either half advances one shared age");
        h.assertTrue(h.getLevel().getBlockState(pos.above()).getValue(CornCropBlock.HALF)==DoubleBlockHalf.UPPER,"Upper identity retained");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void breakingEitherCornHalfHarvestsExactlyOnce(GameTestHelper h){
        var corn=RyeCornContent.CORN_CROP.get();
        for(int half=0;half<2;half++){
            var pos=plant(h,corn,3+half*6);corn.growTo(h.getLevel(),pos,h.getLevel().getBlockState(pos),7);
            h.getLevel().destroyBlock(half==0?pos:pos.above(),true);
            h.assertTrue(h.getLevel().getBlockState(pos).isAir()&&h.getLevel().getBlockState(pos.above()).isAir(),"Harvest removes both stalk halves");
            h.assertTrue(drops(h,pos,RyeCornContent.CORN.get())==2,"One harvest yields exactly two ears from either half");
            h.assertTrue(drops(h,pos,RyeCornContent.CORN_SEEDS.get())>=1,"Corn harvest returns seeds");
        }
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void cornSoilLossAndCreativeRemovalCleanUpBothHalves(GameTestHelper h){
        var corn=RyeCornContent.CORN_CROP.get();var pos=plant(h,corn,3);
        corn.growTo(h.getLevel(),pos,h.getLevel().getBlockState(pos),7);
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        h.assertTrue(h.getLevel().getBlockState(pos).isAir()&&h.getLevel().getBlockState(pos.above()).isAir(),"Lost farmland removes stalk");
        var creative=plant(h,corn,9);corn.growTo(h.getLevel(),creative,h.getLevel().getBlockState(creative),7);
        var player=h.makeMockPlayer(GameType.CREATIVE);var top=creative.above();
        corn.playerWillDestroy(h.getLevel(),top,h.getLevel().getBlockState(top),player);
        h.getLevel().removeBlock(top,false);
        h.assertTrue(h.getLevel().getBlockState(creative).isAir()&&h.getLevel().getBlockState(top).isAir(),"Creative top removal cleans root");
        h.assertTrue(drops(h,creative,RyeCornContent.CORN.get())==0&&drops(h,creative,RyeCornContent.CORN_SEEDS.get())==0,"Creative removal does not harvest loot");
        h.succeed();
    }
}
