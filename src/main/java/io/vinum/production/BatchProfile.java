package io.vinum.production;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

/** Intensive properties only: copying/splitting a fluid stack never changes its composition.
 * Concentrations are grams per litre; ingredient proportions and alcohol are fractions.
 */
public record BatchProfile(double sugarGramsPerLitre, double starchGramsPerLitre,
        double solidsGramsPerLitre, double alcoholFraction,
        Map<ResourceLocation, Double> ingredientProportions, Optional<ResourceLocation> yeast,
        List<ResourceLocation> history) {
    public static final int MAX_HISTORY = 16;
    public static final int MAX_INGREDIENTS = 64;
    private static final Codec<Double> CONCENTRATION = bounded(10000);
    private static final Codec<Double> FRACTION = bounded(1);
    private static final Codec<Map<ResourceLocation, Double>> INGREDIENTS = Codec.unboundedMap(ResourceLocation.CODEC, FRACTION)
            .validate(values -> values.size() <= MAX_INGREDIENTS && (values.isEmpty()
                    || Math.abs(values.values().stream().mapToDouble(Double::doubleValue).sum() - 1) <= 0.000001)
                    ? DataResult.success(values) : DataResult.error(() -> "Ingredient proportions must be bounded and sum to one"));
    public static final Codec<BatchProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CONCENTRATION.fieldOf("sugar_g_per_litre").forGetter(BatchProfile::sugarGramsPerLitre),
            CONCENTRATION.fieldOf("starch_g_per_litre").forGetter(BatchProfile::starchGramsPerLitre),
            CONCENTRATION.fieldOf("solids_g_per_litre").forGetter(BatchProfile::solidsGramsPerLitre),
            FRACTION.fieldOf("alcohol_fraction").forGetter(BatchProfile::alcoholFraction),
            INGREDIENTS.fieldOf("ingredients").forGetter(BatchProfile::ingredientProportions),
            ResourceLocation.CODEC.optionalFieldOf("yeast").forGetter(BatchProfile::yeast),
            ResourceLocation.CODEC.listOf(0, MAX_HISTORY).fieldOf("history").forGetter(BatchProfile::history)
    ).apply(instance, BatchProfile::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatchProfile> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    private static Codec<Double> bounded(double maximum) {
        return Codec.DOUBLE.validate(value -> Double.isFinite(value) && value >= 0 && value <= maximum
                ? DataResult.success(value) : DataResult.error(() -> "Invalid batch concentration or fraction"));
    }

    public BatchProfile {
        concentration(sugarGramsPerLitre); concentration(starchGramsPerLitre); concentration(solidsGramsPerLitre);
        fraction(alcoholFraction);
        ingredientProportions = Map.copyOf(ingredientProportions);
        if (ingredientProportions.size() > MAX_INGREDIENTS) throw new IllegalArgumentException("Too many ingredients");
        double total = 0;
        for (double share : ingredientProportions.values()) { fraction(share); total += share; }
        if (!ingredientProportions.isEmpty() && Math.abs(total - 1) > 0.000001)
            throw new IllegalArgumentException("Ingredient proportions must sum to one");
        yeast = java.util.Objects.requireNonNull(yeast);
        history = List.copyOf(history);
        if (history.size() > MAX_HISTORY) throw new IllegalArgumentException("Batch history exceeds limit");
    }
    private static void concentration(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 10000) throw new IllegalArgumentException("Invalid concentration");
    }
    private static void fraction(double value) {
        if (!Double.isFinite(value) || value < 0 || value > 1) throw new IllegalArgumentException("Invalid fraction");
    }
    public static BatchProfile of(FluidStack fluid) {
        return fluid.getOrDefault(ProductionDataComponents.BATCH_PROFILE.get(), ProductionInputs.defaultProfile(fluid));
    }
    public FluidStack applyTo(FluidStack fluid) {
        if (!fluid.isEmpty()) fluid.set(ProductionDataComponents.BATCH_PROFILE.get(), this);
        return fluid;
    }
    public BatchProfile withAlcoholFraction(double alcohol) {
        return new BatchProfile(sugarGramsPerLitre, starchGramsPerLitre, solidsGramsPerLitre, alcohol, ingredientProportions, yeast, history);
    }
    public BatchProfile withYeast(ResourceLocation culture) {
        return new BatchProfile(sugarGramsPerLitre, starchGramsPerLitre, solidsGramsPerLitre, alcoholFraction, ingredientProportions, Optional.of(culture), history);
    }
    public BatchProfile addHistory(ResourceLocation event) {
        var events = new ArrayList<>(history);
        events.add(event);
        if (events.size() > MAX_HISTORY) events.removeFirst();
        return new BatchProfile(sugarGramsPerLitre, starchGramsPerLitre, solidsGramsPerLitre, alcoholFraction, ingredientProportions, yeast, events);
    }
    public BatchProfile distilled(double alcohol, ResourceLocation event) {
        return new BatchProfile(0, 0, 0, alcohol, ingredientProportions, yeast, history).addHistory(event);
    }
    /** Weighted by fluid amounts, without storing those amounts in the component. */
    public static BatchProfile blend(BatchProfile first, int firstAmount, BatchProfile second, int secondAmount) {
        if (firstAmount <= 0 || secondAmount <= 0) throw new IllegalArgumentException("Blend amounts must be positive");
        double weight = (double) firstAmount / ((long) firstAmount + secondAmount);
        var ingredients = new LinkedHashMap<ResourceLocation, Double>();
        first.ingredientProportions.forEach((key, value) -> ingredients.merge(key, value * weight, Double::sum));
        second.ingredientProportions.forEach((key, value) -> ingredients.merge(key, value * (1 - weight), Double::sum));
        // Keep the known ingredients even when the other input has no recorded provenance.
        ResourceLocation unspecified = ProductionInputs.id("unspecified");
        if (first.ingredientProportions.isEmpty()) ingredients.merge(unspecified, weight, Double::sum);
        if (second.ingredientProportions.isEmpty()) ingredients.merge(unspecified, 1 - weight, Double::sum);
        var events = new ArrayList<>(first.history);
        for (var event : second.history) if (!events.contains(event)) events.add(event);
        while (events.size() > MAX_HISTORY) events.removeFirst();
        return new BatchProfile(first.sugarGramsPerLitre * weight + second.sugarGramsPerLitre * (1 - weight),
                first.starchGramsPerLitre * weight + second.starchGramsPerLitre * (1 - weight),
                first.solidsGramsPerLitre * weight + second.solidsGramsPerLitre * (1 - weight),
                first.alcoholFraction * weight + second.alcoholFraction * (1 - weight), ingredients,
                first.yeast.equals(second.yeast) ? first.yeast : Optional.empty(), events);
    }
}
