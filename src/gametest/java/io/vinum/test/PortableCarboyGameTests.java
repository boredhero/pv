package io.vinum.test;

import io.vinum.block.ModBlocks;
import io.vinum.item.ModItems;
import io.vinum.production.vessel.*;
import io.vinum.barrel.ModDataComponents;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class PortableCarboyGameTests {
    private static FermentationVesselBlockEntity place(GameTestHelper h, Player player, ItemStack stack, BlockPos relative) {
        BlockPos pos=h.absolutePos(relative), floor=pos.below();
        h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var hit=new BlockHitResult(Vec3.atCenterOf(floor).add(0,.5,0),Direction.UP,floor,false);
        h.assertTrue(stack.useOn(new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,stack,hit)).consumesAction(),"Portable carboy places normally");
        return (FermentationVesselBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="barrel_space")
    public static void oneItemPlacesAndPicksUpCompleteBatch(GameTestHelper h) {
        h.assertTrue(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath("vinum","juice_carboy")),"No duplicate portable carboy is registered");
        var item=ModItems.CARBOY.get();
        h.assertTrue(item==ModItems.JUICE_CARBOY.get() && ModBlocks.CARBOY.get().asItem()==item,"Portable and placed forms resolve to the same item");
        FluidStack juice=new FluidStack(io.vinum.fluid.ModFluids.JUICE_CHARDONNAY.source().get(),1750);
        juice.set(DataComponents.CUSTOM_NAME,Component.literal("Single estate batch"));
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var vessel=place(h,player,item.filled(juice),new BlockPos(2,1,2));
        h.assertTrue(vessel.getFluid().getAmount()==1750 && FluidStack.isSameFluidSameComponents(vessel.getFluid(),juice),"Placement transfers all fluid metadata exactly once");
        h.assertTrue(player.getMainHandItem().isEmpty(),"Placement consumes one portable vessel");
        var drops=Block.getDrops(vessel.getBlockState(),h.getLevel(),vessel.getBlockPos(),vessel);
        h.assertTrue(drops.size()==1 && drops.getFirst().is(item) && item.contents(drops.getFirst()).getAmount()==1750,"Breaking returns the same portable item with liquid");
        var clone=vessel.getBlockState().getBlock().getCloneItemStack(vessel.getBlockState(),new BlockHitResult(Vec3.atCenterOf(vessel.getBlockPos()),Direction.UP,vessel.getBlockPos(),false),h.getLevel(),vessel.getBlockPos(),player);
        h.assertTrue(item.contents(clone).getAmount()==1750,"Pick block includes portable contents");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void airlockAndFermentationSurviveCarryingButChangedLiquidResetsPlan(GameTestHelper h) {
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        var item=ModItems.CARBOY.get();
        var vessel=place(h,player,item.filled(new FluidStack(io.vinum.fluid.ModFluids.JUICE_CHARDONNAY.source().get(),1000)),new BlockPos(2,1,2));
        h.assertTrue(!vessel.startFermentation(),"Carboy cannot ferment without an installed airlock");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.AIRLOCK.get(),2));
        var pos=vessel.getBlockPos();
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        vessel.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(vessel.hasAirlock() && player.getMainHandItem().getCount()==1,"Installing airlock consumes one addon");
        vessel.getBlockState().useItemOn(player.getMainHandItem(),h.getLevel(),player,InteractionHand.MAIN_HAND,hit);
        h.assertTrue(player.getMainHandItem().getCount()==1,"Duplicate installation cannot consume an addon");
        h.assertTrue(vessel.startFermentation(),"Airlock enables small-batch fermentation");
        var tag=vessel.saveWithoutMetadata(h.getLevel().registryAccess());tag.putInt("FermentationProgress",100);
        vessel.loadWithComponents(tag,h.getLevel().registryAccess());
        ItemStack carried=new ItemStack(item);vessel.saveToItem(carried,h.getLevel().registryAccess());
        h.getLevel().setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        var restored=place(h,player,carried.copy(),new BlockPos(4,1,2));
        h.assertTrue(restored.hasAirlock() && restored.isFermenting() && restored.saveWithoutMetadata(h.getLevel().registryAccess()).getInt("FermentationProgress")==100,"Untouched pick-up/place preserves installed addon and frozen progress");
        var handler=item.handler(carried);
        h.assertTrue(handler.drain(250,FluidAction.SIMULATE).getAmount()==250 && carried.get(DataComponents.BLOCK_ENTITY_DATA).copyTag().contains("FermentationPlan"),"Simulation preserves carried plan");
        handler.drain(250,FluidAction.EXECUTE);
        var modified=place(h,player,handler.getContainer(),new BlockPos(6,1,2));
        h.assertTrue(modified.hasAirlock() && !modified.isFermenting() && modified.getFluid().getAmount()==750,"Changed contents invalidate frozen fermentation but retain installed hardware");
        h.succeed();
    }
}
