package io.vinum.production;

import io.vinum.common.Defines;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProductionDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Defines.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BatchProfile>> BATCH_PROFILE =
            COMPONENTS.register("batch_profile", () -> DataComponentType.<BatchProfile>builder()
                    .persistent(BatchProfile.CODEC).networkSynchronized(BatchProfile.STREAM_CODEC).build());
    private ProductionDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
