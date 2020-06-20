/*
    Project Vinum - ModWoodType.java
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
package io.vinum.block;

import net.minecraft.block.WoodType;

public class ModWoodType extends WoodType {
	
	public static final WoodType CINNAMON = new ModWoodType("cinnamon");
	
	public ModWoodType(String nameIn) {
		super(nameIn);
		
	}
	
}