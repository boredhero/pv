package io.vinum.block.crops;

import net.minecraft.block.CropsBlock;

public class AgaveCrop extends CropsBlock{

    public AgaveCrop(Properties builder) {
        super(builder);
    }

    @Override
    public int getMaxAge(){
        return 2;
    }
    
}