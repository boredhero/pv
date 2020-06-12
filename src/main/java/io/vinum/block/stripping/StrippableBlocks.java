package io.vinum.block.stripping;

import java.util.HashMap;
import java.util.Map;

import io.vinum.block.ModBlocks;
import net.minecraft.block.Block;

public class StrippableBlocks {
	
	public static final Map<Block, Block> BLOCK_STRIPPING_MAP = new HashMap<>();
	
	public static void registerStrippableBlocks() {
		
		BLOCK_STRIPPING_MAP.put(ModBlocks.CINNAMON_LOG.get(), ModBlocks.CINNAMON_PLANKS.get());
		
	}
	
}