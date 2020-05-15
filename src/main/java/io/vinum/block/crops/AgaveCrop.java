package io.vinum.block.crops;

import net.minecraft.block.Block;
import net.minecraft.block.CropsBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;

public class AgaveCrop extends CropsBlock{

    public AgaveCrop(Properties builder) {
        super(builder);
    }

    public AgaveCrop() {
        this(Block.Properties.create(Material.PLANTS).doesNotBlockMovement().tickRandomly()
                .hardnessAndResistance(0.0F, 0.F).sound(SoundType.CROP));
    }

    @Override
    public int getMaxAge(){
        return 2;
    }
    
}