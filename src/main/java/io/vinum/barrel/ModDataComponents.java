package io.vinum.barrel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import io.vinum.common.Defines;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Shared batch metadata travels with both fluids and filled beverage containers. */
public final class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Defines.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> FLUID_AGE =
            COMPONENTS.register("fluid_content_age", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG.validate(age -> age >= 0 ? DataResult.success(age)
                            : DataResult.error(() -> "Fluid content age cannot be negative")))
                    .networkSynchronized(ByteBufCodecs.VAR_LONG).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> CONTAINED_FLUID =
            COMPONENTS.register("contained_fluid", () -> DataComponentType.<SimpleFluidContent>builder()
                    .persistent(SimpleFluidContent.CODEC)
                    .networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());

    private ModDataComponents() {}
}
