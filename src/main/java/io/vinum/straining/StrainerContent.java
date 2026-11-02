package io.vinum.straining;

import com.mojang.serialization.MapCodec;
import io.vinum.common.Defines;
import io.vinum.item.ModItemGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class StrainerContent {
  private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Defines.MODID);
  private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Defines.MODID);
  private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Defines.MODID);
  private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Defines.MODID);
  public static final DeferredHolder<Block, StrainerBlock> BLOCK = BLOCKS.register("strainer", () -> new StrainerBlock(
      BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()));
  public static final DeferredItem<BlockItem> ITEM = ITEMS.register("strainer", () -> new BlockItem(BLOCK.get(), new Item.Properties()));
  public static final DeferredHolder<RecipeType<?>, RecipeType<StrainingRecipe>> TYPE = TYPES.register("straining", () -> new RecipeType<>() {});
  public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StrainingRecipe>> SERIALIZER = SERIALIZERS.register("straining", () -> new RecipeSerializer<>() {
    @Override public MapCodec<StrainingRecipe> codec() { return StrainingRecipe.CODEC; }
    @Override public StreamCodec<RegistryFriendlyByteBuf, StrainingRecipe> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(StrainingRecipe.CODEC.codec()); }
  });
  private StrainerContent() {}
  public static void register(IEventBus bus) {
    BLOCKS.register(bus); ITEMS.register(bus); TYPES.register(bus); SERIALIZERS.register(bus);
    bus.addListener(StrainerContent::creative);
  }
  private static void creative(BuildCreativeModeTabContentsEvent event) {
    if (event.getTab() == ModItemGroup.MAIN.get()) event.accept(ITEM.get());
  }
}
