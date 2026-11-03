package io.vinum.test;

import io.vinum.item.ModItems;
import io.vinum.production.vessel.CarboyItem;
import io.vinum.sugar.*;
import io.vinum.fluid.ModFluids;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class KettleTransferGameTests {
    @GameTest(template="barrel_space")
    public static void sneakingCarboyPoursIntoKettleThroughItemFallback(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));
        h.getLevel().setBlockAndUpdate(pos,SugarContent.KETTLE.get().defaultBlockState());
        var kettle=(SugarKettleBlockEntity)h.getLevel().getBlockEntity(pos);
        var carboy=(CarboyItem)ModItems.CARBOY.get();
        for(var mode:new GameType[]{GameType.CREATIVE,GameType.SURVIVAL}) {
            kettle.fluids().drain(Integer.MAX_VALUE,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            kettle.fluids().fill(new FluidStack(ModFluids.CANE_JUICE.source().get(),1000),
                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            var player=h.makeMockPlayer(mode);player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND,carboy.filled(new FluidStack(ModFluids.CANE_JUICE.source().get(),4000)));
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
            h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,Direction.UP)!=null,
                "Sneak-use must find the kettle capability when vanilla bypasses block interaction");
            var result=carboy.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            h.assertTrue(result.consumesAction(),"Sneaking item fallback pours into kettle in "+mode);
            h.assertTrue(kettle.contents().getAmount()==4000&&carboy.contents(player.getMainHandItem()).getAmount()==1000,
                "Partial pour fills kettle and preserves carboy remainder");
            h.assertTrue(h.getLevel().getBlockState(pos.above()).isAir(),"Pouring must not place the carboy on kettle");
            carboy.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            h.assertTrue(kettle.contents().getAmount()==4000&&carboy.contents(player.getMainHandItem()).getAmount()==1000,
                "Full kettle cannot lose fluid");
        }
        h.succeed();
    }
}
