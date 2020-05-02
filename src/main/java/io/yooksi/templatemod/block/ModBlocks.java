package io.yooksi.templatemod.block;

import io.yooksi.templatemod.common.Defines;
import net.minecraft.block.Block;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Set;
import java.util.function.Supplier;

public final class ModBlocks {

	public static final DeferredRegister<Block> BLOCKS =
			new DeferredRegister<>(ForgeRegistries.BLOCKS, Defines.MODID);

	private static final Set<RegistryObject<Block>> PURE_BLOCKS = new java.util.HashSet<>();

	/**
	 * @return {@code true} if the given {@code Block} requires an associated {@code BlockItem}.
	 * 			Most blocks will want to have {@code BlockItems} as they want to be in inventory.
	 */
	public static boolean needsItemBlock(Block block) {

		for (RegistryObject<Block> pureBlock : PURE_BLOCKS) {
			if (pureBlock.get() == block) {
				return false;
			}
		} return true;
	}

	/**
	 * Adds the given supplier associated with the given name to the list of entries to be registered.
	 *
	 * @param name the new entry's name, it will automatically be prefixed.
	 * @param sup a factory for the new entry, it should return a new instance every time it is called.
	 * @param hasItem whether this {@code Block} is intended to have an associated {@code BlockItem}.
	 *
	 * @return {@code RegistryObject} that will be updated with when the entries in the registry change.
	 */
	private static RegistryObject<Block> register(String name, Supplier<? extends Block> sup, boolean hasItem) {

		RegistryObject<Block> registryObject = BLOCKS.register(name, sup);
		if (!hasItem) PURE_BLOCKS.add(registryObject);

		return registryObject;
	}

	/**
	 * Default registration method that registers blocks as {@code BlockItem} owners.
	 * @see #register(String, Supplier, boolean)
	 */
	private static RegistryObject<Block> register(String name, Supplier<? extends Block> sup) {
		return register(name, sup, true);
	}
}
