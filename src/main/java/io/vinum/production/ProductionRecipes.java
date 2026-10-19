package io.vinum.production;

import com.mojang.serialization.MapCodec;
import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ProductionRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Defines.MODID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Defines.MODID);
    public static final DeferredHolder<RecipeType<?>, RecipeType<FermentationRecipe>> FERMENTATION_TYPE = type("fermentation");
    public static final DeferredHolder<RecipeType<?>, RecipeType<MashingRecipe>> MASHING_TYPE = type("mashing");
    public static final DeferredHolder<RecipeType<?>, RecipeType<DistillationRecipe>> DISTILLATION_TYPE = type("distillation");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FermentationRecipe>> FERMENTATION_SERIALIZER = serializer("fermentation", FermentationRecipe.CODEC);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MashingRecipe>> MASHING_SERIALIZER = serializer("mashing", MashingRecipe.CODEC);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DistillationRecipe>> DISTILLATION_SERIALIZER = serializer("distillation", DistillationRecipe.CODEC);
    private ProductionRecipes() {}
    private static <R extends Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<R>> type(String name) {
        return TYPES.register(name, () -> new RecipeType<>() { @Override public String toString() { return "vinum:" + name; } });
    }
    private static <R extends Recipe<?>> DeferredHolder<RecipeSerializer<?>, RecipeSerializer<R>> serializer(String name, MapCodec<R> codec) {
        return SERIALIZERS.register(name, () -> new RecipeSerializer<>() {
            @Override public MapCodec<R> codec() { return codec; }
            @Override public StreamCodec<RegistryFriendlyByteBuf, R> streamCodec() { return ByteBufCodecs.fromCodecWithRegistries(codec.codec()); }
        });
    }
    public static void register(IEventBus bus) { TYPES.register(bus); SERIALIZERS.register(bus); }
}
