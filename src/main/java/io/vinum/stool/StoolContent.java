package io.vinum.stool;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;

public final class StoolContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "vinum");
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("vinum");
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "vinum");
    public static final DeferredHolder<Block, BarstoolBlock> BARSTOOL = BLOCKS.register("barstool",
        () -> new BarstoolBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).noOcclusion()));
    public static final DeferredItem<BlockItem> ITEM = ITEMS.register("barstool", () -> new BlockItem(BARSTOOL.get(), new Item.Properties()));
    public static final DeferredHolder<EntityType<?>, EntityType<StoolSeat>> SEAT = ENTITIES.register("barstool_seat",
        () -> EntityType.Builder.<StoolSeat>of(StoolSeat::new, MobCategory.MISC).sized(.1F,.1F)
            .noSave().noSummon().clientTrackingRange(8).updateInterval(20).build("vinum:barstool_seat"));
    private StoolContent() {}
    public static void register(IEventBus bus) {
        BLOCKS.register(bus); ITEMS.register(bus); ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTab() == io.vinum.item.ModItemGroup.MAIN.get()) event.accept(ITEM.get());
        });
    }
}
