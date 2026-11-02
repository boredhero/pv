package io.vinum.barrel;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

/** Add one definition to support a new beverage across barrel pouring, tapping and age tooltips. */
public record BeverageDefinition(ResourceLocation id, Supplier<? extends Fluid> fluid,
                                 AgingProfile aging, List<Serving> servings) {
    public BeverageDefinition { servings = List.copyOf(servings); }
}
