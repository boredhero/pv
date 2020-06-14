package io.vinum.block;

import net.minecraft.block.WoodType;

public class ModWoodType extends WoodType {
	
	public static final WoodType CINNAMON = new ModWoodType("cinnamon");
	
	public ModWoodType(String nameIn) {
		super(nameIn);
		
	}
	
}