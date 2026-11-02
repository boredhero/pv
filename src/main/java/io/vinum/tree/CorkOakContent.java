package io.vinum.tree;

import io.vinum.common.Defines;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.*;

/** Isolated registrations for the renewable cork-oak production resource. */
public final class CorkOakContent {
  public static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, Defines.MODID);
  public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Defines.MODID);
  public static final DeferredRegister<BlockEntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Defines.MODID);
  public static final DeferredRegister<Feature<?>> FEATURES =
      DeferredRegister.create(Registries.FEATURE, Defines.MODID);
  public static final ResourceKey<ConfiguredFeature<?, ?>> TREE_KEY =
      ResourceKey.create(Registries.CONFIGURED_FEATURE, id("cork_oak"));
  private static final TreeGrower GROWER =
      new TreeGrower("vinum:cork_oak", Optional.empty(), Optional.of(TREE_KEY), Optional.empty());
  public static final DeferredHolder<Block, CorkLogBlock> LOG =
      BLOCKS.register(
          "cork_oak_log",
          () ->
              new CorkLogBlock(
                  BlockBehaviour.Properties.of()
                      .mapColor(MapColor.WOOD)
                      .strength(2)
                      .sound(SoundType.WOOD)));
  public static final DeferredHolder<Block, LeavesBlock> LEAVES =
      BLOCKS.register(
          "cork_oak_leaves",
          () ->
              new LeavesBlock(
                  BlockBehaviour.Properties.of()
                      .mapColor(MapColor.PLANT)
                      .strength(.2F)
                      .randomTicks()
                      .sound(SoundType.GRASS)
                      .noOcclusion()
                      .isValidSpawn((state, level, pos, type) -> false)
                      .isSuffocating((state, level, pos) -> false)
                      .isViewBlocking((state, level, pos) -> false)));
  public static final DeferredHolder<Block, SaplingBlock> SAPLING =
      BLOCKS.register(
          "cork_oak_sapling",
          () ->
              new SaplingBlock(
                  GROWER,
                  BlockBehaviour.Properties.of()
                      .mapColor(MapColor.PLANT)
                      .noCollission()
                      .randomTicks()
                      .instabreak()
                      .sound(SoundType.GRASS)));
  public static final DeferredItem<BlockItem> LOG_ITEM =
      ITEMS.register("cork_oak_log", () -> new CorkLogItem(LOG.get(), new Item.Properties()));
  public static final DeferredItem<BlockItem> LEAF_ITEM =
      ITEMS.register("cork_oak_leaves", () -> new BlockItem(LEAVES.get(), new Item.Properties()));
  public static final DeferredItem<BlockItem> SAPLING_ITEM =
      ITEMS.register("cork_oak_sapling", () -> new BlockItem(SAPLING.get(), new Item.Properties()));
  public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorkLogBlockEntity>>
      LOG_ENTITY =
          ENTITIES.register(
              "cork_oak_log",
              () -> BlockEntityType.Builder.of(CorkLogBlockEntity::new, LOG.get()).build(null));
  public static final DeferredHolder<Feature<?>, CorkOakFeature> TREE_FEATURE =
      FEATURES.register("cork_oak", CorkOakFeature::new);

  private CorkOakContent() {}

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(Defines.MODID, path);
  }

  public static void register(IEventBus bus, ModContainer container) {
    CorkWoodContent.register(bus);
    BLOCKS.register(bus);
    ITEMS.register(bus);
    ENTITIES.register(bus);
    FEATURES.register(bus);
    container.registerConfig(
        ModConfig.Type.SERVER, CorkOakConfig.SPEC, "vinum-cork-oak-server.toml");
  }
}
