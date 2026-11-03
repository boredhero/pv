package io.vinum.test;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.beverage.WineCatalog;
import io.vinum.block.ModBlocks;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionContainers;
import io.vinum.production.ProductionInputs;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.straining.StrainerContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class StrainerGameTests {
  private static BlockPos place(GameTestHelper h) {
    BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
    h.getLevel().setBlockAndUpdate(pos, StrainerContent.BLOCK.get().defaultBlockState());
    return pos;
  }
  private static void use(GameTestHelper h, BlockPos pos, Player player) {
    h.getLevel().getBlockState(pos).useItemOn(player.getMainHandItem(), h.getLevel(), player,
        InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
  }
  private static FluidStack contents(ItemStack stack) {
    return ((FluidContainerItem) stack.getItem()).contents(stack);
  }

  @GameTest(template = "barrel_space")
  public static void strainingAgedWinePreservesContainersAgeAndUnknownComponents(GameTestHelper h) {
    BlockPos pos = place(h);
    FluidStack wine = WineCatalog.sample(WineCatalog.varieties().getFirst(), 613);
    BatchProfile base = BatchProfile.of(wine);
    new BatchProfile(7, 0, 100, base.alcoholFraction(), base.ingredientProportions(), base.yeast(),
        List.of(ProductionInputs.id("fermentation"))).applyTo(wine);
    wine = BarrelFluidData.withAge(wine, 123456L);
    wine.set(DataComponents.CUSTOM_NAME, Component.literal("Unrecognized batch metadata"));
    var item = (FluidContainerItem) ModItems.WINE_BOTTLE_EMPTY.get();
    ItemStack bottle = item.filled(wine);
    bottle.set(DataComponents.CUSTOM_NAME, Component.literal("Cellar bottle"));
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
    use(h, pos, player);
    FluidStack clear = contents(player.getMainHandItem());
    h.assertTrue(clear.getAmount() == 613 && BarrelFluidData.getAge(clear) == 123456L
        && clear.get(DataComponents.CUSTOM_NAME).equals(wine.get(DataComponents.CUSTOM_NAME)),
        "Straining preserves partial volume, maturation age and arbitrary fluid components");
    h.assertTrue(BatchProfile.of(clear).solidsGramsPerLitre() == 0
        && BatchProfile.of(clear).sugarGramsPerLitre() == 7
        && BatchProfile.of(clear).alcoholFraction() == base.alcoholFraction()
        && BatchProfile.of(clear).ingredientProportions().equals(base.ingredientProportions())
        && player.getMainHandItem().get(DataComponents.CUSTOM_NAME).equals(bottle.get(DataComponents.CUSTOM_NAME)),
        "Only solids change; bottle metadata, sugar, alcohol and provenance survive");
    h.assertTrue(io.vinum.beverage.BottleCorks.hasCork(player.getMainHandItem()),
        "Straining keeps the cork already supplied for this bottle filling");
    ItemStack once = player.getMainHandItem().copy();
    use(h, pos, player);
    h.assertTrue(ItemStack.isSameItemSameComponents(once, player.getMainHandItem()),
        "Repeated use cannot duplicate contents or add phantom process history");
    var glass = ((FluidContainerItem) ModItems.WINE_GLASS_EMPTY.get()).handler(new ItemStack(ModItems.WINE_GLASS_EMPTY.get()));
    h.assertTrue(glass.fill(clear, FluidAction.EXECUTE) == 125, "Strained aged wine is ready to serve");
    h.succeed();
  }

  @GameTest(template = "barrel_space", timeoutTicks = 100)
  public static void mustFermentsThenHandheldStrainerClearsAndAirlockAllowsAging(GameTestHelper h) {
    BlockPos pos = h.absolutePos(new BlockPos(2, 1, 2));
    h.getLevel().setBlockAndUpdate(pos, ModBlocks.CARBOY.get().defaultBlockState());
    var vessel = (FermentationVesselBlockEntity) h.getLevel().getBlockEntity(pos);
    FluidStack must = ProductionInputs.fromItem(new ItemStack(ModItems.GRAPE_MUST_MERLOT.get()));
    h.assertTrue(vessel.getFluidHandler(null).fill(must, FluidAction.EXECUTE) == 1000, "Carboy accepts raw must");
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(StrainerContent.ITEM.get()));
    use(h, pos, player);
    h.assertTrue(FluidStack.isSameFluidSameComponents(must, vessel.getFluid()), "Raw must cannot be strained");
    h.assertTrue(!vessel.startFermentation(), "Airlock required for fermentation");
    vessel.installAirlock();
    h.assertTrue(vessel.startFermentation(), "Airlocked must starts fermenting");
    h.assertTrue(!vessel.strain(), "Active batches cannot be strained");
    var saved = vessel.saveWithoutMetadata(h.getLevel().registryAccess());
    saved.putInt("FermentationProgress", 23980);
    vessel.loadWithComponents(saved, h.getLevel().registryAccess());
    h.runAfterDelay(21, () -> {
      FluidStack cloudy = vessel.getFluid();
      h.assertTrue(WineCatalog.isWine(cloudy) && BatchProfile.of(cloudy).solidsGramsPerLitre() == 100,
          "Fermentation leaves fermented must with skins");
      h.assertTrue(BarrelFluidData.getAge(cloudy) == 0, "Unstrained must does not age in glass");
      use(h, pos, player);
      FluidStack clear = vessel.getFluid();
      h.assertTrue(clear.getAmount() == 1000 && BatchProfile.of(clear).solidsGramsPerLitre() == 0
          && Math.abs(BatchProfile.of(clear).alcoholFraction() - .12) < 1e-9,
          "Handheld strainer preserves volume and alcohol while clearing young wine");
      h.assertTrue(player.getMainHandItem().is(StrainerContent.ITEM.get()) && !vessel.strain(), "Reusable strainer and repeat no-op");
      h.runAfterDelay(21, () -> {
        h.assertTrue(BarrelFluidData.getAge(vessel.getFluid()) > 0, "Airlocked placed carboy ages clear wine");
        var noAirlock = vessel.saveWithoutMetadata(h.getLevel().registryAccess());
        noAirlock.putBoolean("Airlock", false);
        vessel.loadWithComponents(noAirlock, h.getLevel().registryAccess());
        long age = BarrelFluidData.getAge(vessel.getFluid());
        h.runAfterDelay(21, () -> {
          h.assertTrue(BarrelFluidData.getAge(vessel.getFluid()) == age, "Without airlock maturation pauses");
          h.succeed();
        });
      });
    });
  }

  @GameTest(template = "barrel_space")
  public static void insufficientReceivingCapacityDoesNotConsumeMust(GameTestHelper h) {
    BlockPos pos = place(h);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAPE_MUST_MERLOT.get(), 2));
    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.WINE_BOTTLE_EMPTY.get()));
    use(h, pos, player);
    h.assertTrue(player.getMainHandItem().getCount() == 2 && contents(player.getOffhandItem()).isEmpty(),
        "A too-small receiver cannot lose any of the prepared litre");
    h.succeed();
  }
}
