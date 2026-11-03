package io.vinum.test;

import io.vinum.bar.*;
import io.vinum.beverage.*;
import io.vinum.barrel.BarrelFluidData;
import io.vinum.item.*;
import io.vinum.production.BatchProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BarServiceGameTests {
    private static BarBlockEntity bar(GameTestHelper h,BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState());
        return (BarBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="barrel_space")
    public static void heldBottleFillsPlacedWineGlassAndDrinkwarePersists(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));var bar=bar(h,pos);
        var bottle=(FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get();
        var glass=(FluidContainerItem)ModItems.WINE_GLASS_EMPTY.get();
        var fluid=BarrelFluidData.withAge(WineCatalog.sample(WineCatalog.varieties().getFirst(),317),12345);
        bar.insert(2,new ItemStack(glass));bar.insert(0,new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()));
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,bottle.filled(fluid));
        var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0,1,0),Direction.UP,pos,false);
        bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(glass.contents(bar.bottle(2)).getAmount()==125 && bottle.contents(player.getMainHandItem()).getAmount()==192,"Normal click pours held bottle into placed wine glass");
        var restored=new BarBlockEntity(pos,bar.getBlockState());restored.loadWithComponents(bar.saveWithoutMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(ItemStack.isSameItemSameComponents(restored.bottle(2),bar.bottle(2))
            && restored.bottle(0).is(ModItems.SPICED_APPLE_MARGARITA.get()),"Filled glass and cocktail survive save/load");
        player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        bar.getBlockState().useWithoutItem(h.getLevel(),player,hit);
        h.assertTrue(!bar.bottle(2).isEmpty(),"Ordinary empty-hand click leaves drink on counter for holding gesture");
        player.setShiftKeyDown(true);bar.getBlockState().useWithoutItem(h.getLevel(),player,hit);
        h.assertTrue(bar.bottle(2).isEmpty() && player.getInventory().countItem(glass)==1,"Sneak-click retrieves exactly one glass");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void placedBottleServesPartialGlassAndPreservesBatch(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));var bar=bar(h,pos);
        var bottle=(FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get();var glass=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        var fluid=BarrelFluidData.withAge(WineCatalog.sample(WineCatalog.varieties().getFirst(),317),12345);
        bar.insert(2,bottle.filled(fluid));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,glass.filled(fluid.copyWithAmount(30)));
        var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0,1,0),Direction.UP,pos,false);
        bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        var served=glass.contents(player.getMainHandItem());var remaining=bottle.contents(bar.bottle(2));
        h.assertTrue(served.getAmount()==50 && remaining.getAmount()==297,"Only the missing 20 mB transfers");
        h.assertTrue(BarrelFluidData.getAge(served)==12345 && BatchProfile.of(served).equals(BatchProfile.of(fluid)),"Serving preserves age and chemistry");
        var full=player.getMainHandItem().copy();var before=bar.bottle(2);
        bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(ItemStack.isSameItemSameComponents(full,player.getMainHandItem()) && ItemStack.isSameItemSameComponents(before,bar.bottle(2)),"Full glass cannot drain or replace the displayed bottle");
        h.succeed();
    }
    @GameTest(template="barrel_space",timeoutTicks=100)
    public static void placedRumBottleServesShotWithMaturationAndDrinkingEffects(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));var bar=bar(h,pos);
        var bottle=(FluidContainerItem)ModItems.RUM_BOTTLE.get();var shot=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        long age=io.vinum.config.BarrelConfig.daysToTicks(io.vinum.config.BarrelConfig.rumDarkAgedDays());
        var fluid=BarrelFluidData.withAge(ModItems.spirit(io.vinum.fluid.ModFluids.RUM.source().get(),750),age);
        BatchProfile.of(fluid).withAlcoholFraction(.4).applyTo(fluid);
        bar.insert(2,bottle.filled(fluid));
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "rum-serving-test"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(shot));
        var hit=new BlockHitResult(Vec3.atBottomCenterOf(pos).add(0,1,0),Direction.UP,pos,false);
        bar.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        var served=shot.contents(player.getMainHandItem());
        h.assertTrue(served.getAmount()==50 && bottle.contents(bar.bottle(2)).getAmount()==700,
                "Placed rum bottle serves exactly one shot and retains its remainder");
        h.assertTrue(BarrelFluidData.getAge(served)==age && BatchProfile.of(served).alcoholFraction()==.4,
                "Rum shot keeps dark maturation and actual alcohol strength");
        h.assertTrue(io.vinum.barrel.BeverageCatalog.lookupFluid(served.getFluid()).orElseThrow().aging().stage(age)
                .equals("tooltip.vinum.maturation.rum_dark_aged"),"Serving keeps the same rum stage");
        var emptied=shot.finishUsingItem(player.getMainHandItem(),h.getLevel(),player);
        h.assertTrue(emptied.is(ModItems.SHOT_GLASS.get()) && shot.contents(emptied).isEmpty()
                && player.getData(io.vinum.capability.BACCapability.BAC_STATE).getBACLevel()>0,
                "Drinking a rum shot applies alcohol and returns its reusable empty glass");
        h.succeed();
    }
    @GameTest(template="barrel_space",timeoutTicks=100)
    public static void heldDrinkCancelsThenConsumesAndLeavesGlassOnCounter(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));var bar=bar(h,pos);
        var glass=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        var fluid=ModItems.spirit(io.vinum.fluid.ModFluids.TEQUILA.source().get(),50);
        bar.insert(2,glass.filled(fluid));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);player.setXRot(90);player.setYRot(0);
        h.assertTrue(player.pick(player.blockInteractionRange(),1,false) instanceof BlockHitResult hit && hit.getBlockPos().equals(pos),"Drink target is in reach and visible");
        for(int i=1;i<=12;i++)h.runAfterDelay(i,()->BarDrinking.pulse(player,pos,2,true));
        h.runAfterDelay(13,()->{
            BarDrinking.pulse(player,pos,2,false);
            h.assertTrue(glass.contents(bar.bottle(2)).getAmount()==50,"Early release consumes nothing");
            // Repeated packets in one server tick cannot fast-forward the hold.
            for(int j=0;j<100;j++)BarDrinking.pulse(player,pos,2,true);
            h.assertTrue(glass.contents(bar.bottle(2)).getAmount()==50,"Packet flood cannot shorten drinking duration");
            BarDrinking.pulse(player,pos,2,false);
        });
        for(int i=15;i<=46;i++)h.runAfterDelay(i,()->BarDrinking.pulse(player,pos,2,true));
        h.runAfterDelay(47,()->{
            h.assertTrue(bar.bottle(2).is(ModItems.SHOT_GLASS.get()) && glass.contents(bar.bottle(2)).isEmpty(),"Finished drink leaves empty reusable glass in its slot");
            h.assertTrue(player.getMainHandItem().isEmpty(),"Drinking did not pick up or duplicate the glass");
            h.succeed();
        });
    }
    @GameTest(template="barrel_space",timeoutTicks=60)
    public static void cocktailDrinksInPlaceWithEffectsAndReusableGlass(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(3,1,3));var bar=bar(h,pos);
        bar.insert(2,new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);player.setXRot(90);player.setYRot(0);
        for(int i=1;i<=32;i++)h.runAfterDelay(i,()->BarDrinking.pulse(player,pos,2,true));
        h.runAfterDelay(33,()->{
            h.assertTrue(bar.bottle(2).is(ModItems.MARGARITA_GLASS.get()),"Cocktail returns its empty glass on the counter");
            h.assertTrue(player.hasEffect(net.minecraft.world.effect.MobEffects.LUCK),"Existing cocktail effects still apply");
            h.succeed();
        });
    }
}
