package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.fluid.ModFluids;
import io.vinum.item.ModItems;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class CarboyVatTransferGameTests {
    @GameTest(template="barrel_space")
    public static void ordinaryCarboyUseFillsVatAndEmptyUseDrawsWithoutReversingFailures(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(2,1,2));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.FERMENTATION_VAT.get().defaultBlockState());
        var vat=(FermentationVesselBlockEntity)h.getLevel().getBlockEntity(pos);
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var item=ModItems.CARBOY.get();
        var juice=new FluidStack(ModFluids.JUICE_MERLOT.source().get(),4000);
        player.setItemInHand(InteractionHand.MAIN_HAND,item.filled(juice));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        var state=h.getLevel().getBlockState(pos);
        h.assertTrue(!player.isShiftKeyDown(),"Fixture exercises ordinary right-click");
        h.assertTrue(state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit).consumesAction()
                && vat.getFluid().getAmount()==4000 && item.contents(player.getMainHandItem()).isEmpty(),
                "Ordinary filled carboy pours its 4L into vat");
        state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(vat.getFluid().isEmpty() && item.contents(player.getMainHandItem()).getAmount()==4000,
                "Ordinary empty carboy draws liquid out");
        vat.getFluidHandler(null).fill(juice.copyWithAmount(15000),FluidAction.EXECUTE);
        state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(vat.getFluid().getAmount()==16000 && item.contents(player.getMainHandItem()).getAmount()==3000,
                "Nearly full vat accepts only free capacity, leaving exact carboy remainder");
        var before=player.getMainHandItem().copy();
        h.assertTrue(state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==ItemInteractionResult.FAIL
                && ItemStack.isSameItemSameComponents(before,player.getMainHandItem()) && vat.getFluid().getAmount()==16000,
                "Full vat fails without reversing direction and drawing more wine");
        h.assertTrue(vat.startFermentation(),"Vat starts without requiring carboy airlock");
        h.assertTrue(state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==ItemInteractionResult.FAIL
                && ItemStack.isSameItemSameComponents(before,player.getMainHandItem()),"Active vat rejects pours atomically");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        h.assertTrue(state.useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit)==ItemInteractionResult.FAIL
                && item.contents(player.getMainHandItem()).isEmpty(),"Active vat also rejects draws");
        h.succeed();
    }
}
