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

  @GameTest(template = "barrel_space", timeoutTicks = 60)
  public static void rawMustStrainsIntoJuiceAndFermentsInSmallCarboy(GameTestHelper h) {
    BlockPos pos = place(h);
    var player = h.makeMockPlayer(GameType.SURVIVAL);
    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.GRAPE_MUST_MERLOT.get()));
    use(h, pos, player);
    h.assertTrue(player.getMainHandItem().is(ModItems.GRAPE_MUST_MERLOT.get()), "Missing receiver cannot consume must");
    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.JUICE_CARBOY.get()));
    use(h, pos, player);
    FluidStack juice = contents(player.getOffhandItem());
    h.assertTrue(player.getMainHandItem().is(Items.BOWL) && juice.getAmount() == 1000
        && BuiltInRegistries.FLUID.getKey(juice.getFluid()).equals(ProductionInputs.id("juice_merlot"))
        && BatchProfile.of(juice).solidsGramsPerLitre() == 0
        && BatchProfile.of(juice).sugarGramsPerLitre() == 200
        && BatchProfile.of(juice).alcoholFraction() == 0,
        "A bowl strains to a litre of fermentable juice without creating alcohol, returning its bowl");
    BlockPos carboyPos = pos.offset(2, 0, 0);
    h.getLevel().setBlockAndUpdate(carboyPos, ModBlocks.CARBOY.get().defaultBlockState());
    var vessel = (FermentationVesselBlockEntity) h.getLevel().getBlockEntity(carboyPos);
    player.setItemInHand(InteractionHand.MAIN_HAND, player.getOffhandItem());
    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
    player.setShiftKeyDown(true);
    ProductionContainers.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, vessel.getFluidHandler(null));
    h.assertTrue(vessel.installAirlock(), "Install the small fermentation airlock");
    h.assertTrue(vessel.getFluid().getAmount() == 1000 && vessel.startFermentation(), "Strained juice supports small-carboy fermentation");
    h.assertTrue(vessel.getFluidHandler(null).drain(1000, FluidAction.SIMULATE).isEmpty(), "Active batches cannot be extracted for straining");
    var saved = vessel.saveWithoutMetadata(h.getLevel().registryAccess());
    saved.putInt("FermentationProgress", 23980);
    vessel.loadWithComponents(saved, h.getLevel().registryAccess());
    h.runAfterDelay(21, () -> {
      player.setShiftKeyDown(false);
      ProductionContainers.useItemOn(player.getMainHandItem(), h.getLevel(), player, InteractionHand.MAIN_HAND, vessel.getFluidHandler(null));
      FluidStack wine = contents(player.getMainHandItem());
      h.assertTrue(wine.getAmount() == 1000 && WineCatalog.isWine(wine)
          && Math.abs(BatchProfile.of(wine).alcoholFraction() - .12) < 1e-9
          && BatchProfile.of(wine).solidsGramsPerLitre() == 0,
          "Small carboy produces clear wine ready for bottling or aging");
      h.succeed();
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
