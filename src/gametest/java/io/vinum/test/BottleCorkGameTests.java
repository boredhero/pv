package io.vinum.test;

import io.vinum.barrel.ModDataComponents;
import io.vinum.beverage.*;
import io.vinum.item.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BottleCorkGameTests {
    private static FluidStack wine(int amount) { return WineCatalog.sample(WineCatalog.varieties().getFirst(), amount); }
    @GameTest(template="barrel_space")
    public static void firstFillChargesOneCorkAndFailedFillIsAtomic(GameTestHelper h) {
        var p = h.makeMockPlayer(GameType.SURVIVAL);
        var tank = new FluidTank(2000); tank.fill(wine(400), FluidAction.EXECUTE);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get()));
        h.assertTrue(ContainerTransfers.use(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,tank)==ItemInteractionResult.FAIL,
                "No cork means no wine bottling");
        h.assertTrue(tank.getFluidAmount()==400 && !BottleCorks.hasCork(p.getMainHandItem()),"Failed fill changes neither tank nor bottle");
        p.getInventory().add(new ItemStack(ModItems.WINE_CORK.get(),2));
        ContainerTransfers.use(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,tank);
        h.assertTrue(p.getInventory().countItem(ModItems.WINE_CORK.get())==1 && BottleCorks.hasCork(p.getMainHandItem()),"First partial filling consumes exactly one cork");
        tank.fill(wine(500),FluidAction.EXECUTE);
        ContainerTransfers.use(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,tank);
        h.assertTrue(p.getInventory().countItem(ModItems.WINE_CORK.get())==1 && tank.getFluidAmount()==150,"Top-up doesn't charge another cork");
        var item=(FluidContainerItem)p.getMainHandItem().getItem();
        h.assertTrue(item.contents(p.getMainHandItem()).getAmount()==750,"Bottle reaches capacity without lost wine");
        p.setShiftKeyDown(true);
        ContainerTransfers.use(p.getMainHandItem(),h.getLevel(),p,InteractionHand.MAIN_HAND,tank);
        h.assertTrue(!BottleCorks.hasCork(p.getMainHandItem()),"Emptying consumes this filling's cork entitlement");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void automationRequiresPreparedEmptyBottle(GameTestHelper h) {
        var item=(FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get();
        var empty=new ItemStack(item);
        h.assertTrue(item.handler(empty).fill(wine(750),FluidAction.EXECUTE)==0,"Generic capabilities cannot bypass corks");
        var recipe=h.getLevel().getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("vinum","crafting/prepare_wine_bottle_empty")).orElseThrow().value();
        var corkRecipe=(CorkBottleRecipe)recipe;
        var input=CraftingInput.of(2,1,List.of(empty,new ItemStack(ModItems.WINE_CORK.get())));
        h.assertTrue(corkRecipe.matches(input,h.getLevel()),"Bottle and cork prepare for automation");
        var prepared=corkRecipe.assemble(input,h.getLevel().registryAccess());
        var handler=item.handler(prepared);
        h.assertTrue(handler.fill(wine(750),FluidAction.SIMULATE)==750 && handler.getFluidInTank(0).isEmpty(),"Simulation accepts preparation without filling");
        h.assertTrue(handler.fill(wine(750),FluidAction.EXECUTE)==750,"Automation fills a prepared bottle");
        h.assertTrue(!corkRecipe.matches(CraftingInput.of(2,1,List.of(handler.getContainer(),new ItemStack(ModItems.WINE_CORK.get()))),h.getLevel()),"Recipe cannot destroy full bottles");
        handler.drain(750,FluidAction.EXECUTE);
        h.assertTrue(handler.fill(wine(750),FluidAction.EXECUTE)==0,"Empty bottle must receive a new cork for reuse");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void handTransferChargesRecipientButServingDoesNot(GameTestHelper h) {
        var p=h.makeMockPlayer(GameType.SURVIVAL);
        var carboy=(FluidContainerItem)ModItems.JUICE_CARBOY.get();
        p.setItemInHand(InteractionHand.OFF_HAND,carboy.filled(wine(1000)));
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
        h.assertTrue(!ContainerTransfers.betweenHands(h.getLevel(),p,InteractionHand.MAIN_HAND),"Other bottle shapes also require a cork for wine");
        p.getInventory().add(new ItemStack(ModItems.WINE_CORK.get()));
        h.assertTrue(ContainerTransfers.betweenHands(h.getLevel(),p,InteractionHand.MAIN_HAND),"Cork permits carboy-to-bottle transfer");
        h.assertTrue(p.getInventory().countItem(ModItems.WINE_CORK.get())==0,"Exactly one cork consumed between hands");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.WINE_GLASS_EMPTY.get()));
        h.assertTrue(ContainerTransfers.betweenHands(h.getLevel(),p,InteractionHand.MAIN_HAND),"Serving into a glass needs no cork");
        h.assertTrue(carboy.contents(p.getOffhandItem()).getAmount()==125,"All transfers conserve the source remainder");
        h.succeed();
    }
}
