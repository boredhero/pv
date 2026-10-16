package io.vinum.block;

import io.vinum.common.Defines;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Defines.MODID);
    public static final DeferredHolder<Block, WineBarrelBlock> BARREL = BLOCKS.register("barrel", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, SteelBrazierBlock> STEEL_BRAZIER = BLOCKS.register("steel_brazier",
        () -> new SteelBrazierBlock(steel().lightLevel(state -> state.getValue(SteelBrazierBlock.LIT) ? 15 : 0)));
    public static final DeferredHolder<Block, SteelPotBlock> STEEL_POT = BLOCKS.register("steel_pot", () -> new SteelPotBlock(steel()));
    public static final DeferredHolder<Block, SteelCoilBlock> STEEL_COIL = BLOCKS.register("steel_coil", () -> new SteelCoilBlock(steel()));
    public static final DeferredHolder<Block, StillMultiblockPart1Block> STILL_MULTIBLOCK_PART_1 = BLOCKS.register("still_multiblock_part_1", () -> new StillMultiblockPart1Block(still()));
    public static final DeferredHolder<Block, StillMultiblockPart2Block> STILL_MULTIBLOCK_PART_2 = BLOCKS.register("still_multiblock_part_2", () -> new StillMultiblockPart2Block(still()));
    public static final DeferredHolder<Block, StillMultiblockPart3Block> STILL_MULTIBLOCK_PART_3 = BLOCKS.register("still_multiblock_part_3", () -> new StillMultiblockPart3Block(still()));
    public static final DeferredHolder<Block, StillMultiblockPart4Block> STILL_MULTIBLOCK_PART_4 = BLOCKS.register("still_multiblock_part_4", () -> new StillMultiblockPart4Block(still()));
    public static final DeferredHolder<Block, AgaveCropBlock> CROP_AGAVE = BLOCKS.register("crop_agave", () -> new AgaveCropBlock(
        BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.DESTROY)));

    public static final DeferredHolder<Block, Block> BLOCK_LEAD_ORE = BLOCKS.register("block_lead_ore", () -> new Block(
        BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3.0F, 5.0F).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, Block> BLOCK_LEAD = BLOCKS.register("block_lead", () -> new Block(
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F, 5.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_EMPTY = BLOCKS.register("block_wine_barrel_empty", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_CHARDONNAY = BLOCKS.register("block_wine_barrel_chardonnay", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_SAUVIGNON_BLANC = BLOCKS.register("block_wine_barrel_sauvignon_blanc", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_CABERNET_SAUVIGNON = BLOCKS.register("block_wine_barrel_cabernet_sauvignon", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_MERLOT = BLOCKS.register("block_wine_barrel_merlot", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_PINOT_NOIR = BLOCKS.register("block_wine_barrel_pinot_noir", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, WineBarrelBlock> BLOCK_WINE_BARREL_ZINFANDEL = BLOCKS.register("block_wine_barrel_zinfandel", () -> new WineBarrelBlock(barrel()));
    public static final DeferredHolder<Block, ColoredFallingBlock> BLOCK_LEADED_SAND = BLOCKS.register("block_leaded_sand", () -> new ColoredFallingBlock(new ColorRGBA(0xA6A697), sand()));
    public static final DeferredHolder<Block, ColoredFallingBlock> BLOCK_FERROUS_SAND = BLOCKS.register("block_ferrous_sand", () -> new ColoredFallingBlock(new ColorRGBA(0xB1A596), sand()));
    public static final DeferredHolder<Block, ColoredFallingBlock> BLOCK_REFINED_SAND = BLOCKS.register("block_refined_sand", () -> new ColoredFallingBlock(new ColorRGBA(0xDFD5C1), sand()));
    public static final DeferredHolder<Block, TransparentBlock> BLOCK_DARK_WINE_GLASS = BLOCKS.register("block_dark_wine_glass", () -> new TransparentBlock(glass()));
    public static final DeferredHolder<Block, TransparentBlock> BLOCK_FINE_CRYSTAL_GLASS = BLOCKS.register("block_fine_crystal_glass", () -> new TransparentBlock(glass()));

    private ModBlocks() {}

    private static BlockBehaviour.Properties barrel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F, 3.0F).sound(SoundType.WOOD)
            .noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties sand() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(0.5F, 2.5F).sound(SoundType.SAND);
    }

    private static BlockBehaviour.Properties glass() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.3F, 1.5F).sound(SoundType.GLASS)
            .noOcclusion().isValidSpawn((state, level, pos, entityType) -> false)
            .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
            .isViewBlocking((state, level, pos) -> false);
    }


    private static BlockBehaviour.Properties steel() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion();
    }

    private static BlockBehaviour.Properties still() {
        return steel().pushReaction(PushReaction.BLOCK);
    }

    public static boolean needsItemBlock(Block block) {
        return !(block instanceof StillPartBlock) && block != CROP_AGAVE.get();
    }
}
