package io.yooksi.templatemod.util;

import io.yooksi.templatemod.common.Defines;
import net.minecraft.util.ResourceLocation;

/**
 * Tiny utility class to help find the right {@code ResourceLocation}.
 */
public final class RLHelper {

	private RLHelper() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @return {@code ResourceLocation} pointing to provided path with {@code MODID} as namespace.
	 */
	public static ResourceLocation getModResourceLocation(String path) {
		return new ResourceLocation(Defines.MODID, path);
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
