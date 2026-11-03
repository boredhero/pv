package io.vinum.test;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.BottleCorks;
import io.vinum.beverage.WineCatalog;
import io.vinum.config.BarrelConfig;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItemGroup;
import io.vinum.item.ModItems;
import io.vinum.production.BatchProfile;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class CreativeBeverageGameTests {
    @GameTest(template = "empty")
    public static void creativeWineBottlesAreReadyToServe(GameTestHelper h) {
        var tab = ModItemGroup.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), true,
                h.getLevel().registryAccess()));
        var bottle = (FluidContainerItem) ModItems.WINE_BOTTLE_EMPTY.get();
        var displayed = tab.getDisplayItems().stream().filter(stack -> stack.is(bottle)).toList();
        h.assertTrue(displayed.stream().filter(stack -> bottle.contents(stack).isEmpty()).count() == 1,
                "Keep one empty canonical wine bottle available");
        for (var variety : WineCatalog.varieties()) {
            var examples = displayed.stream().filter(stack -> BatchProfile.of(bottle.contents(stack))
                    .ingredientProportions().containsKey(variety.ingredient())).toList();
            h.assertTrue(examples.size() == 1, "One filled Creative bottle per wine variety");
            var stack = examples.getFirst();
            var fluid = bottle.contents(stack);
            h.assertTrue(fluid.getAmount() == 750 && BottleCorks.hasCork(stack), "Creative wine is full and corked");
            h.assertTrue(BarrelFluidData.getAge(fluid) == BarrelConfig.daysToTicks(BarrelConfig.wineMatureDays()),
                    "Creative wine is mature rather than unaged must");
            h.assertTrue(Math.abs(BatchProfile.of(fluid).alcoholFraction() - variety.defaultAlcohol()) < 1e-9,
                    "Creative wine preserves the variety's alcohol content");
            h.assertTrue(WineCatalog.color(fluid) == variety.color(),
                    "Shared bottle artwork must not replace red and white wine colors with one fixed tint");
            h.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(other ->
                    net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack, other)),
                    "Filled wine can also be found through Creative search");
        }
        h.succeed();
    }
    @GameTest(template = "barrel_space")
    public static void creativeWineGlassesCanBePlacedOnBar(GameTestHelper h) {
        var tab = ModItemGroup.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(h.getLevel().enabledFeatures(), true,
                h.getLevel().registryAccess()));
        var glass = (FluidContainerItem) ModItems.WINE_GLASS_EMPTY.get();
        var pos = h.absolutePos(new net.minecraft.core.BlockPos(3, 1, 3));
        h.getLevel().setBlockAndUpdate(pos, io.vinum.bar.BarContent.BAR.get().defaultBlockState());
        var bar = (io.vinum.bar.BarBlockEntity) h.getLevel().getBlockEntity(pos);
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        for (var variety : WineCatalog.varieties()) {
            var examples = tab.getDisplayItems().stream().filter(stack -> stack.is(glass)
                    && BatchProfile.of(glass.contents(stack)).ingredientProportions().containsKey(variety.ingredient())).toList();
            h.assertTrue(examples.size() == 1, "One Creative wine glass per variety");
            var stack = examples.getFirst();
            h.assertTrue(glass.contents(stack).getAmount() == 125, "Wine glass contains a full serving");
            h.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(other ->
                    net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack, other)), "Glass is searchable");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
            double[] center = io.vinum.bar.BarBlock.center(bar.getBlockState(), 2);
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(
                    pos.getX() + center[0], pos.getY() + 1, pos.getZ() + center[1]),
                    net.minecraft.core.Direction.UP, pos, false);
            h.assertTrue(bar.getBlockState().useItemOn(player.getMainHandItem(), h.getLevel(), player,
                    net.minecraft.world.InteractionHand.MAIN_HAND, hit).consumesAction(), "Right-click places the glass");
            h.assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(stack, bar.bottle(2)),
                    "Placed wine glass preserves the full batch and variety");
            bar.remove(2);
        }
        h.succeed();
    }
}
