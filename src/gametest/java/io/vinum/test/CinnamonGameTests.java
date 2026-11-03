package io.vinum.test;

import io.vinum.cinnamon.*;
import io.vinum.item.ModItems;
import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class CinnamonGameTests {
    private static final BlockPos ROOT = new BlockPos(7, 1, 7);
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("vinum", path); }
    private static Block block(String name) { return BuiltInRegistries.BLOCK.get(id(name)); }
    private static com.google.gson.JsonObject resource(GameTestHelper h, String path) {
        try (var stream = h.getLevel().getServer().getResourceManager().getResource(id(path + ".json")).orElseThrow().open()) {
            if (stream == null) throw new AssertionError("Missing cinnamon worldgen resource: " + path);
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(stream,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) { throw new AssertionError(e); }
    }
    private static int cinnamonDrops(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                .filter(e -> e.getItem().is(ModItems.CINNAMON.get())).mapToInt(e -> e.getItem().getCount()).sum();
    }

    @GameTest(template = "barrel_space")
    public static void cinnamonAxeHarvestStripsOnceAndPreservesAxisAndTool(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var axe = new ItemStack(Items.IRON_AXE);
        axe.setDamageValue(19);
        axe.set(DataComponents.CUSTOM_NAME, Component.literal("Bark harvester"));
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        int index = 0;
        for (String source : List.of("cinnamon_log", "cinnamon_wood")) {
            BlockPos pos = h.absolutePos(new BlockPos(2 + 6 * index++, 1, 2));
            level.setBlockAndUpdate(pos, block(source).defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
            int beforeDamage = axe.getDamageValue();
            level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(level.getBlockState(pos).is(block("stripped_" + source))
                    && level.getBlockState(pos).getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                    "Harvest converts bark-covered wood to its axis-preserving stripped form");
            h.assertTrue(cinnamonDrops(h, pos) == 2 && axe.getDamageValue() == beforeDamage + 1,
                    "Harvest supplies exactly two cinnamon and wears the axe once");
            level.getBlockState(pos).useItemOn(axe, level, player, InteractionHand.MAIN_HAND, hit);
            h.assertTrue(cinnamonDrops(h, pos) == 2 && axe.getDamageValue() == beforeDamage + 1,
                    "Stripped wood cannot be harvested again");
            var lumber = Block.getDrops(level.getBlockState(pos), level, pos, null);
            h.assertTrue(lumber.size() == 1 && lumber.getFirst().is(block("stripped_" + source).asItem()),
                    "Breaking harvested lumber retains the stripped form instead of resetting bark");
        }
        h.assertTrue(axe.getHoverName().equals(Component.literal("Bark harvester")), "Harvest keeps axe components");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void cinnamonHarvestRejectsWrongToolAndCreativeDoesNotFarmDrops(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(ROOT);
        level.setBlockAndUpdate(pos, block("cinnamon_log").defaultBlockState());
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var shears = new ItemStack(Items.SHEARS);
        player.setItemInHand(InteractionHand.MAIN_HAND, shears);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
        level.getBlockState(pos).useItemOn(shears, level, player, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(level.getBlockState(pos).is(block("cinnamon_log")) && cinnamonDrops(h, pos) == 0,
                "Shears cannot turn bark into spice");
        var creative = h.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        var axe = new ItemStack(Items.IRON_AXE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, axe);
        level.getBlockState(pos).useItemOn(axe, level, creative, InteractionHand.MAIN_HAND, hit);
        h.assertTrue(level.getBlockState(pos).is(block("stripped_cinnamon_log")) && cinnamonDrops(h, pos) == 0
                && axe.getDamageValue() == 0, "Creative stripping changes appearance without free spice drops or tool wear");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void cinnamonTreePlacementPreflightsStructuresAndWater(GameTestHelper h) {
        h.setBlock(ROOT.below(), Blocks.DIRT);
        h.setBlock(ROOT.above(2), Blocks.STONE);
        var config = new CinnamonTreeFeature.Configuration(8, 10);
        h.assertTrue(!CinnamonTreeFeature.placeTree(h.getLevel(), RandomSource.create(42), h.absolutePos(ROOT), config)
                && h.getBlockState(ROOT).isAir() && h.getBlockState(ROOT.above(2)).is(Blocks.STONE),
                "Blocked tree does not overwrite a structure or leave a partial trunk");
        h.setBlock(ROOT.above(2), Blocks.AIR);
        h.setBlock(ROOT, Blocks.WATER);
        h.assertTrue(!CinnamonTreeFeature.placeTree(h.getLevel(), RandomSource.create(42), h.absolutePos(ROOT), config)
                && h.getBlockState(ROOT).is(Blocks.WATER), "Cinnamon does not overwrite standing water");
        h.setBlock(ROOT, Blocks.AIR);
        h.setBlock(ROOT.below(), Blocks.STONE);
        h.assertTrue(!CinnamonTreeFeature.placeTree(h.getLevel(), RandomSource.create(42), h.absolutePos(ROOT), config),
                "Tree requires suitable soil");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void cinnamonSaplingGrowsRegisteredTreeWithDecayingLeaves(GameTestHelper h) {
        var level = h.getLevel();
        var root = h.absolutePos(ROOT);
        for (BlockPos p : BlockPos.betweenClosed(root.offset(-6, 0, -6), root.offset(6, 14, 6)))
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
        h.setBlock(ROOT.below(), Blocks.DIRT);
        h.setBlock(ROOT, block("cinnamon_sapling").defaultBlockState().setValue(SaplingBlock.STAGE, 1));
        ((SaplingBlock) block("cinnamon_sapling")).performBonemeal(level, RandomSource.create(42), root, level.getBlockState(root));
        h.assertTrue(level.getBlockState(root).is(block("cinnamon_log")), "Bonemeal resolves the configured cinnamon tree");
        int logs = 0, leaves = 0;
        for (BlockPos p : BlockPos.betweenClosed(root.offset(-6, 0, -6), root.offset(6, 14, 6))) {
            var state = level.getBlockState(p);
            if (state.is(block("cinnamon_log"))) logs++;
            if (state.is(block("cinnamon_leaves"))) {
                leaves++;
                h.assertTrue(!state.getValue(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.DISTANCE) < 7,
                        "Generated foliage has support distances and can decay when felled");
            }
        }
        h.assertTrue(logs >= 8 && leaves >= 40, "Sapling produces a trunk and substantial evergreen crown");
        h.assertTrue(block("cinnamon_log").defaultBlockState().is(BlockTags.LOGS), "Trunks support normal leaf decay");
        var leafPos = h.absolutePos(new BlockPos(1, 1, 1));
        var unsupported = block("cinnamon_leaves").defaultBlockState().setValue(LeavesBlock.DISTANCE, 7);
        level.setBlockAndUpdate(leafPos, unsupported);
        unsupported.randomTick(level, leafPos, RandomSource.create(3));
        h.assertTrue(level.getBlockState(leafPos).isAir(), "Unsupported natural leaves decay");
        var shearDrops = Block.getDrops(block("cinnamon_leaves").defaultBlockState(), level, leafPos, null,
                h.makeMockPlayer(GameType.SURVIVAL), new ItemStack(Items.SHEARS));
        h.assertTrue(shearDrops.stream().anyMatch(s -> s.is(block("cinnamon_leaves").asItem())), "Shears collect leaf blocks");
        h.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void cinnamonWoodRecipesFuelAndMultiBlockLootWork(GameTestHelper h) {
        var level = h.getLevel();
        for (String lumber : List.of("cinnamon_log", "cinnamon_wood", "stripped_cinnamon_log", "stripped_cinnamon_wood")) {
            var input = CraftingInput.of(1, 1, List.of(new ItemStack(block(lumber))));
            var output = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow()
                    .value().assemble(input, level.registryAccess());
            h.assertTrue(output.is(block("cinnamon_planks").asItem()) && output.getCount() == 4,
                    "Every lumber form crafts four planks");
        }
        for (String name : List.of("planks", "stairs", "slab", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate")) {
            var construction = block("cinnamon_" + name);
            h.assertTrue(level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                    .anyMatch(r -> r.value().getResultItem(level.registryAccess()).is(construction.asItem())),
                    "Every cinnamon construction piece has a crafting recipe: " + name);
            h.assertTrue(new ItemStack(construction).getBurnTime(RecipeType.SMELTING) > 0,
                    "Cinnamon construction pieces are ordinary wood fuel: " + name);
        }
        var sticksInput = CraftingInput.of(1, 2, List.of(new ItemStack(block("cinnamon_planks")), new ItemStack(block("cinnamon_planks"))));
        h.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, sticksInput, level).orElseThrow().value()
                .assemble(sticksInput, level.registryAccess()).is(Items.STICK), "Cinnamon planks work in vanilla tagged recipes");
        var strippedInput = CraftingInput.of(2, 2, java.util.Collections.nCopies(4, new ItemStack(block("stripped_cinnamon_log"))));
        var strippedOutput = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, strippedInput, level).orElseThrow()
                .value().assemble(strippedInput, level.registryAccess());
        h.assertTrue(strippedOutput.is(block("stripped_cinnamon_wood").asItem()),
                "Crafting stripped logs cannot restore harvestable bark");
        var pos = h.absolutePos(ROOT);
        var doubleSlab = block("cinnamon_slab").defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE);
        h.assertTrue(Block.getDrops(doubleSlab, level, pos, null).stream().mapToInt(ItemStack::getCount).sum() == 2,
                "Double slab returns two pieces");
        var door = block("cinnamon_door").defaultBlockState();
        h.assertTrue(Block.getDrops(door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), level, pos, null).size() == 1
                && Block.getDrops(door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), level, pos.above(), null).isEmpty(),
                "Door drops exactly once across its two blocks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void cinnamonWorldgenIsRestrictedToWarmForestAndUsesRareAttempts(GameTestHelper h) {
        h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).containsKey(id("cinnamon_tree")),
                "Cinnamon configured feature loads in the server registry");
        for (String biome : List.of("jungle", "sparse_jungle")) {
            String feature = "cinnamon_" + biome;
            h.assertTrue(h.getLevel().registryAccess().registryOrThrow(Registries.PLACED_FEATURE).containsKey(id(feature)),
                    "Natural cinnamon placement is registered: " + feature);
            var modifier = resource(h, "neoforge/biome_modifier/add_" + feature);
            h.assertTrue(modifier.get("biomes").getAsString().equals("minecraft:" + biome),
                    "Worldgen does not expand into swamp, cold or dry biomes");
            var placement = resource(h, "worldgen/placed_feature/" + feature).getAsJsonArray("placement");
            boolean rarity = false, biomeFilter = false;
            for (var entry : placement) {
                var value = entry.getAsJsonObject();
                if (value.get("type").getAsString().equals("minecraft:rarity_filter"))
                    rarity = value.get("chance").getAsInt() == (biome.equals("jungle") ? 8 : 12);
                if (value.get("type").getAsString().equals("minecraft:biome")) biomeFilter = true;
            }
            h.assertTrue(rarity && biomeFilter, "Placement uses selected rarity and biome boundary filtering");
        }
        h.succeed();
    }
}
