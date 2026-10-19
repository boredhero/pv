package io.vinum.fluid;

import java.util.ArrayList;
import java.util.List;
import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Tank fluids: bottles, glasses and fluid handlers transfer these without placing world fluid blocks. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Defines.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Defines.MODID);
    private static final List<FluidEntry> ENTRIES = new ArrayList<>();
    public static final FluidEntry TEQUILA = registerBeverage("tequila", 0xFFECE7CF);
    public static final FluidEntry GOLDEN_TEQUILA = registerBeverage("golden_tequila", 0xFFCA933B);
    public static final FluidEntry CHARDONNAY = registerBeverage("chardonnay", 0xFFEBD17F);
    public static final FluidEntry SAUVIGNON_BLANC = registerBeverage("sauvignon_blanc", 0xFFD8D99A);
    public static final FluidEntry CABERNET_SAUVIGNON = registerBeverage("cabernet_sauvignon", 0xFF64182D);
    public static final FluidEntry MERLOT = registerBeverage("merlot", 0xFF842D40);
    public static final FluidEntry PINOT_NOIR = registerBeverage("pinot_noir", 0xFF9A3C4C);
    public static final FluidEntry ZINFANDEL = registerBeverage("zinfandel", 0xFF76213A);

    public static final FluidEntry AGAVE_MOSTO = registerBeverage("agave_mosto", 0xFFD5BA79);
    public static final FluidEntry AGAVE_WASH = registerBeverage("agave_wash", 0xFFCEC09A);
    public static final FluidEntry JUICE_CHARDONNAY = registerBeverage("juice_chardonnay", 0xFFEBD17F);
    public static final FluidEntry MUST_CHARDONNAY = registerBeverage("must_chardonnay", 0xFFEBD17F);
    public static final FluidEntry JUICE_SAUVIGNON_BLANC = registerBeverage("juice_sauvignon_blanc", 0xFFD8D99A);
    public static final FluidEntry MUST_SAUVIGNON_BLANC = registerBeverage("must_sauvignon_blanc", 0xFFD8D99A);
    public static final FluidEntry JUICE_CABERNET_SAUVIGNON = registerBeverage("juice_cabernet_sauvignon", 0xFF64182D);
    public static final FluidEntry MUST_CABERNET_SAUVIGNON = registerBeverage("must_cabernet_sauvignon", 0xFF64182D);
    public static final FluidEntry JUICE_MERLOT = registerBeverage("juice_merlot", 0xFF842D40);
    public static final FluidEntry MUST_MERLOT = registerBeverage("must_merlot", 0xFF842D40);
    public static final FluidEntry JUICE_PINOT_NOIR = registerBeverage("juice_pinot_noir", 0xFF9A3C4C);
    public static final FluidEntry MUST_PINOT_NOIR = registerBeverage("must_pinot_noir", 0xFF9A3C4C);
    public static final FluidEntry JUICE_ZINFANDEL = registerBeverage("juice_zinfandel", 0xFF76213A);
    public static final FluidEntry MUST_ZINFANDEL = registerBeverage("must_zinfandel", 0xFF76213A);

    private ModFluids() {}
    public static void register(IEventBus bus) { FLUID_TYPES.register(bus); FLUIDS.register(bus); }
    public static List<FluidEntry> entries() { return List.copyOf(ENTRIES); }
    private static FluidEntry registerBeverage(String name, int color) {
        FluidEntry entry = new FluidEntry(name, color);
        ENTRIES.add(entry);
        return entry;
    }

    public static final class FluidEntry {
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<Fluid, FlowingFluid> source;
        private final DeferredHolder<Fluid, FlowingFluid> flowing;
        private final int color;
        private FluidEntry(String name, int color) {
            this.color = color;
            type = FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.vinum." + name).canConvertToSource(false)) {});
            source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(properties()));
            flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(properties()));
        }
        private BaseFlowingFluid.Properties properties() { return new BaseFlowingFluid.Properties(type, source, flowing); }
        public DeferredHolder<FluidType, FluidType> type() { return type; }
        public DeferredHolder<Fluid, FlowingFluid> source() { return source; }
        public DeferredHolder<Fluid, FlowingFluid> flowing() { return flowing; }
        public int color() { return color; }
    }
}
