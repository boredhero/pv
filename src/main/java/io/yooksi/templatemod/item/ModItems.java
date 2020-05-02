package io.yooksi.templatemod.item;

import io.yooksi.templatemod.common.Defines;
import net.minecraft.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModItems {

	public static final DeferredRegister<Item> ITEMS =
			new DeferredRegister<>(ForgeRegistries.ITEMS, Defines.MODID);
}
