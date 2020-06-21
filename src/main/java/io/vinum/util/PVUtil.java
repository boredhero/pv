/*
    Project Vinum - PVUtil.java
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
package io.vinum.util;

import net.minecraft.util.ResourceLocation;

import io.vinum.common.PVDefines;
/**
 * Tiny utility class to help find the right {@code ResourceLocation}.
 */
public final class PVUtil {

	private PVUtil() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return {@code ResourceLocation} pointing to provided path with {@code MODID} as namespace.
	 */
	public static ResourceLocation getModResourceLocation(String path) {
		return new ResourceLocation(PVDefines.MODID, path);
	}

	/**
	 * @return {@code ResourceLocation} pointing to provided path with
	 * {@code minecraft} as namespace. Registering blocks and items with
	 * this location will create a registry override (replace).
	 */
	public static ResourceLocation getOverrideResourceLocation(String path) {
		return new ResourceLocation("minecraft", path);
	}

	/**
	 * @return {@code ResourceLocation} pointing to provided path with namespace depending
	 * on whether we want an override resource location ({@code minecraft}) or not ({@code MODID}).
	 *
	 * @see #getOverrideResourceLocation(String)
	 * @see #getModResourceLocation(String)
	 */
	public static ResourceLocation getResourceLocation(String path, boolean override) {
		return override ? getOverrideResourceLocation(path) : getModResourceLocation(path);
	}
}
