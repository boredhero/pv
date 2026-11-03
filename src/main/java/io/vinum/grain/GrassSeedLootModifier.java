package io.vinum.grain;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.*;

/** Additive seed discovery: does not replace vanilla grass loot or award seeds for shearing. */
public final class GrassSeedLootModifier extends LootModifier {
    public static final MapCodec<GrassSeedLootModifier> CODEC=RecordCodecBuilder.mapCodec(instance->codecStart(instance).apply(instance,GrassSeedLootModifier::new));
    public GrassSeedLootModifier(LootItemCondition[] conditions){super(conditions);}
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot,LootContext context){
        var state=context.getParamOrNull(LootContextParams.BLOCK_STATE);
        var tool=context.getParamOrNull(LootContextParams.TOOL);
        if(state==null||!state.is(Blocks.SHORT_GRASS)||tool!=null&&tool.is(Items.SHEARS))return loot;
        int roll=context.getRandom().nextInt(40);
        if(roll==0)loot.add(new ItemStack(RyeCornContent.RYE_SEEDS.get()));
        if(roll==1)loot.add(new ItemStack(RyeCornContent.CORN_SEEDS.get()));
        return loot;
    }
    @Override public MapCodec<? extends IGlobalLootModifier> codec(){return CODEC;}
}
