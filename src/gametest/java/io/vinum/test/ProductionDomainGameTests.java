package io.vinum.test;

import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.vinum.barrel.ModDataComponents;
import io.vinum.production.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class ProductionDomainGameTests {
    @GameTest(template = "barrel_space")
    public static void invalidRecipeOutputsNeverConsumeOrTransformInput(GameTestHelper helper) {
        var empty = net.minecraft.resources.ResourceLocation.withDefaultNamespace("empty");
        var unknown = ProductionInputs.id("missing_output");
        var inputId = ProductionInputs.id("agave_wash");
        var validFluid = ProductionInputs.id("tequila");
        var validItem = ProductionInputs.id("fifth_silver_tequila");
        var air = net.minecraft.resources.ResourceLocation.withDefaultNamespace("air");
        FluidStack input = profile(200, 100, 40).withAlcoholFraction(0.1).applyTo(fluid("agave_wash", 3000));
        FluidStack original = input.copy();
        for (var invalidFluid : List.of(empty, unknown)) {
            var fermentation = new FermentationRecipe(inputId, invalidFluid, 20, 200, 0.0006, 0);
            var mashing = new MashingRecipe(inputId, invalidFluid, 20, 0.5, true);
            var distillation = new DistillationRecipe(inputId, invalidFluid, validItem, 20, 0.4, 750);
            helper.assertTrue(!fermentation.matches(input) && FermentationEngine.finish(input, fermentation).isEmpty(),
                    "Fermentation must reject empty or unknown output fluids");
            helper.assertTrue(!mashing.matches(input, true) && MashingEngine.finish(input, mashing, true).isEmpty(),
                    "Mashing must reject empty or unknown output fluids");
            helper.assertTrue(!distillation.matches(input) && distillation.output(input).isEmpty(),
                    "Distillation must reject empty or unknown output fluids");
        }
        for (var invalidItem : List.of(air, unknown)) {
            var distillation = new DistillationRecipe(inputId, validFluid, invalidItem, 20, 0.4, 750);
            helper.assertTrue(!distillation.matches(input) && distillation.output(input).isEmpty(),
                    "Distillation must reject air or unknown output items");
        }
        helper.assertTrue(FluidStack.matches(input, original), "Rejected conversions must leave input volume and every component unchanged");
        helper.succeed();
    }
    @GameTest(template = "barrel_space")
    public static void profilePersistenceAndNetworkingPreserveIntensiveProperties(GameTestHelper helper) {
        BatchProfile profile = profile(200, 100, 40).withYeast(ProductionInputs.id("wine_yeast"));
        for (int i = 0; i < 30; i++) profile = profile.addHistory(ProductionInputs.id("event_" + i));
        helper.assertTrue(profile.history().size() == BatchProfile.MAX_HISTORY, "History must stay bounded");
        var encoded = BatchProfile.CODEC.encodeStart(JsonOps.INSTANCE, profile).getOrThrow();
        helper.assertTrue(BatchProfile.CODEC.parse(JsonOps.INSTANCE, encoded).getOrThrow().equals(profile), "Codec must preserve the complete profile");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            BatchProfile.STREAM_CODEC.encode(buffer, profile);
            helper.assertTrue(BatchProfile.STREAM_CODEC.decode(buffer).equals(profile), "Network codec must preserve the complete profile");
        } finally { buffer.release(); }
        FluidStack full = profile.applyTo(fluid("must_merlot", 1000));
        FluidStack split = full.copyWithAmount(125);
        helper.assertTrue(BatchProfile.of(split).equals(profile), "Splitting must preserve all per-litre properties");
        helper.assertTrue(full.getAmount() == 1000, "Splitting a copy must leave the source untouched");
        var invalid = encoded.deepCopy().getAsJsonObject();
        invalid.addProperty("sugar_g_per_litre", -1);
        helper.assertTrue(BatchProfile.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent(), "Malformed concentration must fail decoding");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void recipesLoadAndFermentationConservesAvailableSugar(GameTestHelper helper) {
        var manager = helper.getLevel().getRecipeManager();
        helper.assertTrue(manager.getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).size() == 13,
                "All twelve grape inputs and agave mosto must have fermentation recipes");
        var recipe = manager.getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .map(holder -> holder.value()).filter(value -> value.inputFluid().equals(ProductionInputs.id("must_merlot"))).findFirst().orElseThrow();
        FluidStack input = profile(100, 0, 40).applyTo(fluid("must_merlot", 375));
        input.set(ModDataComponents.FLUID_AGE.get(), 500L);
        FluidStack output = FermentationEngine.finish(input, recipe);
        helper.assertTrue(output.getAmount() == 375 && input.getAmount() == 375, "Conversion must preserve volume and leave input immutable");
        helper.assertTrue(close(BatchProfile.of(output).alcoholFraction(), 0.06), "Only available sugar may turn into alcohol");
        helper.assertTrue(BatchProfile.of(output).sugarGramsPerLitre() == 0, "Consumed sugar must be removed");
        helper.assertTrue(!output.has(ModDataComponents.FLUID_AGE.get()), "Fermentation must begin a fresh maturation clock");
        helper.assertTrue(BatchProfile.of(output).ingredientProportions().equals(BatchProfile.of(input).ingredientProportions()), "Ingredients must survive conversion");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void mashingRequiresHeatAndBlendsRemainWeighted(GameTestHelper helper) {
        var recipe = new MashingRecipe(ProductionInputs.id("agave_mosto"), ProductionInputs.id("agave_mosto"), 20, 0.5, true);
        FluidStack input = profile(20, 100, 40).applyTo(fluid("agave_mosto", 1000));
        helper.assertTrue(MashingEngine.finish(input, recipe, false).isEmpty(), "Unheated mashing must be rejected");
        BatchProfile result = BatchProfile.of(MashingEngine.finish(input, recipe, true));
        helper.assertTrue(result.sugarGramsPerLitre() == 70 && result.starchGramsPerLitre() == 50, "Mashing must convert only the configured starch fraction");
        var blended = BatchProfile.blend(profile(200, 0, 0), 1000, profile(0, 100, 40), 3000);
        helper.assertTrue(blended.sugarGramsPerLitre() == 50 && blended.starchGramsPerLitre() == 75 && blended.solidsGramsPerLitre() == 30,
                "Blending must weight each intensive property by actual volume");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void inputCatalogRejectsForgedAndWrongSizedContainers(GameTestHelper helper) {
        ItemStack juice = new ItemStack(BuiltInRegistries.ITEM.get(ProductionInputs.id("grape_juice_merlot")));
        helper.assertTrue(ProductionInputs.fromItem(juice).getAmount() == 1000, "Prepared inputs represent one litre");
        juice.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(fluid("juice_merlot", 999)));
        helper.assertTrue(ProductionInputs.fromItem(juice).isEmpty(), "Wrong container volume must be rejected");
        juice.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(fluid("agave_wash", 1000)));
        helper.assertTrue(ProductionInputs.fromItem(juice).isEmpty(), "Wrong input fluid must be rejected");
        ItemStack forged = new ItemStack(Items.STICK);
        forged.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(fluid("juice_merlot", 1000)));
        helper.assertTrue(ProductionInputs.fromItem(forged).isEmpty(), "Unregistered items must never masquerade as production containers");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void distillationConservesAlcoholAndWritesBottledFluid(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager().getAllRecipesFor(ProductionRecipes.DISTILLATION_TYPE.get());
        helper.assertTrue(recipes.size() == 1, "Agave distillation recipe must load");
        DistillationRecipe recipe = recipes.getFirst().value();
        FluidStack wash = profile(0, 0, 0).withAlcoholFraction(0.1).applyTo(fluid("agave_wash", 3000));
        helper.assertTrue(recipe.requiredInputAmount(wash) == 3000 && recipe.matches(wash), "750mB at forty percent requires three litres at ten percent");
        helper.assertTrue(!recipe.matches(wash.copyWithAmount(2999)), "Insufficient alcohol volume must not produce a bottle");
        ItemStack bottle = recipe.output(wash);
        FluidStack contents = bottle.get(ModDataComponents.CONTAINED_FLUID.get()).copy();
        helper.assertTrue(contents.getAmount() == 750 && close(BatchProfile.of(contents).alcoholFraction(), 0.4), "Bottle metadata must store exact output volume and strength");
        helper.assertTrue(BatchProfile.of(contents).sugarGramsPerLitre() == 0 && BatchProfile.of(contents).solidsGramsPerLitre() == 0,
                "Distillate must be clear and free of sugar");
        helper.assertTrue(BatchProfile.of(contents).ingredientProportions().equals(BatchProfile.of(wash).ingredientProportions()), "Distillate must preserve source ingredient proportions");
        helper.succeed();
    }
    private static FluidStack fluid(String name, int amount) { return new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id(name)), amount); }
    private static BatchProfile profile(double sugar, double starch, double solids) {
        return new BatchProfile(sugar, starch, solids, 0, Map.of(ProductionInputs.id("grapes_merlot"), 1.0), Optional.empty(), List.of());
    }
    private static boolean close(double first, double second) { return Math.abs(first - second) < 0.0000001; }
}
