package io.vinum.test;

import io.vinum.bar.*;
import io.vinum.item.*;
import io.vinum.fluid.ModFluids;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BarRayInteractionGameTests {
    @GameTest(template="barrel_space")
    public static void horizontalBottleAimServesRumInBothGameModes(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));
        var bottle=(FluidContainerItem)ModItems.RUM_BOTTLE.get();
        var shot=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}) for(double height:new double[]{1.3,1.55}) {
            h.getLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState());
            var bar=(BarBlockEntity)h.getLevel().getBlockEntity(pos);
            bar.remove(2);bar.insert(2,bottle.filled(ModItems.spirit(ModFluids.RUM.source().get(),750)));
            var player=h.makeMockPlayer(mode);
            player.setPos(pos.getX()+.5,pos.getY()+height-player.getEyeHeight(),pos.getZ()+3);
            player.setYRot(180);player.setYHeadRot(180);player.setXRot(0);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shot));
            var vanilla=player.pick(player.blockInteractionRange(),1,false);
            h.assertTrue(vanilla.getType()!=HitResult.Type.BLOCK
                || !((net.minecraft.world.phys.BlockHitResult)vanilla).getBlockPos().equals(pos),
                "Reproduces vanilla picking missing a bottle in the cell above the bar");
            var target=BarInteractions.target(player).orElseThrow(()->new AssertionError("Above-bar bottle must be targetable: eye="+player.getEyePosition()+" view="+player.getViewVector(1)+" vanilla="+vanilla+" direct="+BarBlock.displayShape(bar.getBlockState(),bar,2).clip(player.getEyePosition(),player.getEyePosition().add(player.getViewVector(1).scale(player.blockInteractionRange())),pos)));
            h.assertTrue(target.slot()==2&&target.hit().getBlockPos().equals(pos),"Ray selects the visible bottle");
            BarInteractions.use(player,InteractionHand.MAIN_HAND);
            h.assertTrue(shot.contents(player.getMainHandItem()).getAmount()==50
                &&bottle.contents(bar.bottle(2)).getAmount()==700,"Actual horizontal ray serves once in "+mode);
            BarInteractions.use(player,InteractionHand.MAIN_HAND);
            h.assertTrue(bottle.contents(bar.bottle(2)).getAmount()==700,"Full glass cannot drain a second serving");
        }
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void displayRayCannotServeThroughWallsOrOutsideReach(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));
        h.getLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState());
        var bar=(BarBlockEntity)h.getLevel().getBlockEntity(pos);
        var bottle=(FluidContainerItem)ModItems.RUM_BOTTLE.get();
        bar.insert(2,bottle.filled(ModItems.spirit(ModFluids.RUM.source().get(),750)));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX()+.5,pos.getY()+1.3-player.getEyeHeight(),pos.getZ()+3);
        player.setYRot(180);player.setYHeadRot(180);player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SHOT_GLASS.get()));
        h.getLevel().setBlockAndUpdate(pos.above().south(),Blocks.STONE.defaultBlockState());
        h.assertTrue(BarInteractions.target(player).isEmpty(),"Opaque obstruction blocks bottle ray");
        BarInteractions.use(player,InteractionHand.MAIN_HAND);
        h.assertTrue(bottle.contents(bar.bottle(2)).getAmount()==750,"Obstructed interaction consumes nothing");
        h.getLevel().removeBlock(pos.above().south(),false);
        player.setPos(pos.getX()+.5,pos.getY()+1.3-player.getEyeHeight(),pos.getZ()+10);
        h.assertTrue(BarInteractions.target(player).isEmpty(),"Bottle outside interaction reach is rejected");
        BarInteractions.use(player,InteractionHand.MAIN_HAND);
        h.assertTrue(bottle.contents(bar.bottle(2)).getAmount()==750,"Out-of-range interaction consumes nothing");
        h.succeed();
    }
}
