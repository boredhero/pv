package io.vinum.beverage;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.ModDataComponents;
import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionDataComponents;
import io.vinum.fluid.ModFluids;
import java.util.HashSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Conservative batch mixing. Water contributes volume, never ingredient provenance or maturation. */
public final class FluidBlending {
    public static final TagKey<Fluid> CANE_FEEDSTOCKS = tag("cane_feedstocks");
    public static final TagKey<Fluid> BLENDABLE = tag("blendable_batches");
    public static final TagKey<Fluid> DILUTABLE = tag("dilutable_batches");
    private static TagKey<Fluid> tag(String name) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("vinum", name));
    }
    private FluidBlending() {}
    private static boolean water(FluidStack fluid) { return fluid.getFluid().isSame(Fluids.WATER); }
    private static boolean dilution(FluidStack a, FluidStack b) {
        return water(a) && (b.is(CANE_FEEDSTOCKS) || b.is(DILUTABLE))
                || water(b) && (a.is(CANE_FEEDSTOCKS) || a.is(DILUTABLE));
    }
    public static boolean canBlend(FluidStack first, FluidStack second) {
        if (first.isEmpty() || second.isEmpty()) return true;
        FluidStack a = WineCatalog.normalize(first), b = WineCatalog.normalize(second);
        if (dilution(a, b)) {
            FluidStack water = water(a) ? a : b;
            // Never discard arbitrary metadata from a modded water batch.
            return water.isComponentsPatchEmpty();
        }
        boolean cane = a.is(CANE_FEEDSTOCKS) && b.is(CANE_FEEDSTOCKS);
        if (!cane && !FluidStack.isSameFluid(a, b)) return false;
        a.remove(ModDataComponents.FLUID_AGE.get()); b.remove(ModDataComponents.FLUID_AGE.get());
        if (cane || WineCatalog.isWine(a) || a.is(BLENDABLE)) {
            var keys = new HashSet<>(BatchProfile.of(a).ingredientProportions().keySet());
            keys.addAll(BatchProfile.of(b).ingredientProportions().keySet());
            if (BatchProfile.of(a).ingredientProportions().isEmpty() || BatchProfile.of(b).ingredientProportions().isEmpty())
                keys.add(io.vinum.production.ProductionInputs.id("unspecified"));
            if (keys.size() > BatchProfile.MAX_INGREDIENTS) return false;
            a.remove(ProductionDataComponents.BATCH_PROFILE.get()); b.remove(ProductionDataComponents.BATCH_PROFILE.get());
        }
        return a.getComponentsPatch().equals(b.getComponentsPatch());
    }
    public static FluidStack blend(FluidStack first, FluidStack second) {
        if (!canBlend(first, second)) throw new IllegalArgumentException("Incompatible fluid batches");
        FluidStack a = WineCatalog.normalize(first), b = WineCatalog.normalize(second);
        if (a.isEmpty()) return b;
        if (b.isEmpty()) return a;
        int amount = Math.addExact(a.getAmount(), b.getAmount());
        if (dilution(a, b)) {
            FluidStack source = water(a) ? b : a;
            FluidStack result = new FluidStack(source.is(CANE_FEEDSTOCKS) ? ModFluids.CANE_WASH.source().get() : source.getFluid(), amount);
            result.applyComponents(source.getComponentsPatch());
            BatchProfile profile = BatchProfile.of(source);
            double ratio = (double) source.getAmount() / amount;
            new BatchProfile(profile.sugarGramsPerLitre() * ratio, profile.starchGramsPerLitre() * ratio,
                    profile.solidsGramsPerLitre() * ratio, profile.alcoholFraction() * ratio,
                    profile.ingredientProportions(), profile.yeast(), profile.history()).applyTo(result);
            return result;
        }
        boolean cane = a.is(CANE_FEEDSTOCKS) && b.is(CANE_FEEDSTOCKS);
        FluidStack result = new FluidStack(cane && !FluidStack.isSameFluid(a, b) ? ModFluids.CANE_WASH.source().get() : a.getFluid(), amount);
        result.applyComponents(a.getComponentsPatch());
        if (cane || WineCatalog.isWine(a) || a.is(BLENDABLE))
            BatchProfile.blend(BatchProfile.of(a), a.getAmount(), BatchProfile.of(b), b.getAmount()).applyTo(result);
        return BarrelFluidData.withAge(result, BarrelFluidData.weightedAge(BarrelFluidData.getAge(a), a.getAmount(),
                BarrelFluidData.getAge(b), b.getAmount()));
    }
}
