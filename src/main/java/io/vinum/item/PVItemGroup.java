/*
    Project Vinum - PVItemGroup.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package io.vinum.item;

import java.util.function.Supplier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

import io.vinum.common.PVDefines;

/**
 * ModItemGroup is an ItemGroup (previously called {@code CreativeTab} child for our mod
 * that gives us a dedicated tab in the creative menu that contains all our mod items.
 * @see
 * <a href="https://cadiboo.github.io/tutorials/1.15.1/forge/1.7-itemgroup/#setup--util-code">
 *  ItemGroup - Setup & Util code (Cadiboo)</a>
 */
@mcp.MethodsReturnNonnullByDefault
public class PVItemGroup extends ItemGroup {

	/** Main item group for this mod used for all items */
	public static final ItemGroup MAIN = new PVItemGroup(PVDefines.MODID, () -> new ItemStack(PVItems.SPICED_APPLE_MARGARITA.get()));

	public static final Item.Properties PROPERTIES = new Item.Properties().tab(PVItemGroup.MAIN);

	/**
	 * Here we use a {@link java.util.function.Supplier Supplier} because need to delay
	 * creation of icon {@link ItemStack} because ItemGroups are created before any Items are
	 * registered. The icon is not needed before the first time the {@code ItemGroup} is rendered
	 * (by which time Items will have been registered). If we try to make a new ItemStack with an
	 * Item when we create our ItemGroup, we will get an error because all Items are {@code null}
	 * at this time and trying to use them will cause a {@code NullPointerException}.
	 */
	private final Supplier<ItemStack> iconSupplier;

	public PVItemGroup(final String label, Supplier<ItemStack> iconSupplier) {

		super(label);
		this.iconSupplier = iconSupplier;
	}

	@Override
	public ItemStack makeIcon() {
		return iconSupplier.get();
	}
}
