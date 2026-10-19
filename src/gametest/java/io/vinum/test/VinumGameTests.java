package io.vinum.test;

import com.mojang.authlib.GameProfile;
import io.vinum.block.AgaveCropBlock;
import io.vinum.block.ModBlocks;
import io.vinum.capability.BAC;
import io.vinum.capability.BACCapability;
import io.vinum.item.ModItems;
import io.vinum.item.drinks.IDrink;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.LogTestReporter;
import net.minecraft.gametest.framework.TestReporter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/** Server integration tests: real registries, recipes, loot, world updates, and block entities. */
@Mod("vinum_tests")
@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class VinumGameTests {
    private static final BlockPos ORIGIN = new BlockPos(4, 1, 4);

    public VinumGameTests() throws javax.xml.parsers.ParserConfigurationException {
        TestReporter xml = new JUnitLikeTestReporter(new java.io.File("vinum-gametest-results.xml"));
        TestReporter log = new LogTestReporter();
        GlobalTestReporter.replaceWith(new TestReporter() {
            @Override
            public void onTestFailed(GameTestInfo test) { log.onTestFailed(test); xml.onTestFailed(test); }
            @Override
            public void onTestSuccess(GameTestInfo test) { log.onTestSuccess(test); xml.onTestSuccess(test); }
            @Override
            public void finish() { log.finish(); xml.finish(); }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 280)
    public static void stillRunsThroughWorldTicker(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.COAL);
        helper.runAfterDelay(241, () -> {
            assertStack(helper, still.getItem(3), ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
            helper.assertTrue(still.getItem(0).isEmpty(), "Distillation must consume one wort");
            helper.assertTrue(still.getItem(1).isEmpty(), "Distillation must consume one bottle");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void stillRequiresExactly240HeatedTicks(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.COAL);
        tick(helper, still, 239);
        helper.assertTrue(still.getItem(3).isEmpty(), "Still must not finish before 240 ticks");
        assertStack(helper, still.getItem(0), ModItems.FERMENTED_AGAVE_WORT.get(), 1);
        tick(helper, still, 1);
        assertStack(helper, still.getItem(3), ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
        helper.assertTrue(still.progressTime == 0, "Completed batch must reset progress");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void missingFuelCannotProduceOrConsumeIngredients(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.AIR);
        tick(helper, still, 500);
        helper.assertTrue(still.getItem(3).isEmpty(), "Fuel-free still must not produce tequila");
        helper.assertTrue(still.progressTime == 0, "Zero fuel must not advance progress");
        assertStack(helper, still.getItem(0), ModItems.FERMENTED_AGAVE_WORT.get(), 1);
        assertStack(helper, still.getItem(1), ModItems.FIFTH_BOTTLE_EMPTY.get(), 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blockedOutputDoesNotOverwriteItemsOrConsumeFuel(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.COAL);
        still.setItem(3, new ItemStack(Items.DIAMOND, 3));
        tick(helper, still, 300);
        assertStack(helper, still.getItem(3), Items.DIAMOND, 3);
        assertStack(helper, still.getItem(0), ModItems.FERMENTED_AGAVE_WORT.get(), 1);
        assertStack(helper, still.getItem(1), ModItems.FIFTH_BOTTLE_EMPTY.get(), 1);
        assertStack(helper, still.getItem(2), Items.COAL, 1);
        still.setItem(3, new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get(), 64));
        tick(helper, still, 300);
        assertStack(helper, still.getItem(3), ModItems.FIFTH_SILVER_TEQUILA.get(), 64);
        assertStack(helper, still.getItem(2), Items.COAL, 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidWortOrBottleDoesNotConsumeAnything(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.COAL);
        still.setItem(0, new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()));
        tick(helper, still, 300);
        assertStack(helper, still.getItem(0), ModItems.UNFERMENTED_AGAVE_WORT.get(), 1);
        assertStack(helper, still.getItem(2), Items.COAL, 1);
        still.setItem(0, new ItemStack(ModItems.FERMENTED_AGAVE_WORT.get()));
        still.setItem(1, new ItemStack(Items.GLASS_BOTTLE));
        tick(helper, still, 300);
        assertStack(helper, still.getItem(0), ModItems.FERMENTED_AGAVE_WORT.get(), 1);
        assertStack(helper, still.getItem(1), Items.GLASS_BOTTLE, 1);
        assertStack(helper, still.getItem(2), Items.COAL, 1);
        helper.assertTrue(still.getItem(3).isEmpty(), "Invalid ingredients cannot produce tequila");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void lavaFuelReturnsBucket(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        fill(still, Items.LAVA_BUCKET);
        tick(helper, still, 240);
        assertStack(helper, still.getItem(2), Items.BUCKET, 1);
        assertStack(helper, still.getItem(3), ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void automationFeedsCorrectSidesAndExtractsOnlyFinishedItems(GameTestHelper helper) {
        StillMasterTileEntity still = assemble(helper, ORIGIN, Direction.NORTH);
        IItemHandler top = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, still.getBlockPos(), Direction.UP);
        IItemHandler side = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, still.getBlockPos(), Direction.EAST);
        IItemHandler bottom = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, still.getBlockPos(), Direction.DOWN);
        helper.assertTrue(top != null && side != null && bottom != null, "Still must expose item capabilities on all sides");
        assertStack(helper, top.insertItem(0, new ItemStack(Items.DIRT), false), Items.DIRT, 1);
        helper.assertTrue(top.insertItem(0, new ItemStack(ModItems.FERMENTED_AGAVE_WORT.get()), false).isEmpty(), "Top accepts wort");
        helper.assertTrue(top.insertItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()), false).isEmpty(), "Top accepts bottle");
        helper.assertTrue(side.insertItem(0, new ItemStack(Items.LAVA_BUCKET), false).isEmpty(), "Side accepts fuel");
        helper.assertTrue(top.extractItem(0, 1, false).isEmpty(), "Automation must not steal unprocessed wort");
        helper.assertTrue(side.extractItem(0, 1, false).isEmpty(), "Automation must not steal unused fuel");
        tick(helper, still, 240);
        assertStack(helper, bottom.extractItem(0, 64, false), ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
        assertStack(helper, bottom.extractItem(1, 64, false), Items.BUCKET, 1);
        helper.assertTrue(still.isEmpty(), "Automated batch must leave no duplicate items");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stillResumesSavedInventoryAndProgress(GameTestHelper helper) {
        StillMasterTileEntity original = assemble(helper, ORIGIN, Direction.NORTH);
        fill(original, Items.COAL);
        tick(helper, original, 117);
        CompoundTag saved = original.saveWithFullMetadata(helper.getLevel().registryAccess());
        StillMasterTileEntity restored = new StillMasterTileEntity(original.getBlockPos(), original.getBlockState());
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel());
        helper.assertTrue(restored.progressTime == original.progressTime, "Progress must survive save/load");
        helper.assertTrue(restored.fuelTime == original.fuelTime, "Fuel must survive save/load");
        tick(helper, restored, 123);
        assertStack(helper, restored.getItem(3), ModItems.FIFTH_SILVER_TEQUILA.get(), 1);
        helper.assertTrue(restored.getItem(0).isEmpty() && restored.getItem(1).isEmpty(), "Restored batch consumes each input once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void multiblockAssemblesInAllDirections(GameTestHelper helper) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            StillMasterTileEntity still = assemble(helper, ORIGIN, facing);
            BlockPos master = still.getBlockPos();
            helper.assertTrue(StillMultiblock.isComplete(helper.getLevel(), master, facing), "Still must assemble facing " + facing);
            for (BlockPos part : parts(master, facing)) {
                helper.assertTrue(StillMultiblock.masterPosition(part, helper.getLevel().getBlockState(part)).equals(master), "Every part must resolve master facing " + facing);
            }
            for (BlockPos part : parts(master, facing)) helper.getLevel().setBlockAndUpdate(part, Blocks.AIR.defaultBlockState());
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingAnyPartReturnsComponentsAndInventoryOnce(GameTestHelper helper) {
        BlockPos[] origins = {new BlockPos(3, 1, 3), new BlockPos(9, 1, 3), new BlockPos(3, 1, 9), new BlockPos(9, 1, 9)};
        Block[] raw = {ModBlocks.STEEL_BRAZIER.get(), ModBlocks.STEEL_POT.get(), ModBlocks.STEEL_COIL.get(), ModBlocks.STEEL_POT.get()};
        for (int broken = 0; broken < 4; broken++) {
            StillMasterTileEntity still = assemble(helper, origins[broken], Direction.NORTH);
            still.setItem(0, new ItemStack(ModItems.FERMENTED_AGAVE_WORT.get(), 3));
            still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get(), 5));
            BlockPos[] positions = parts(still.getBlockPos(), Direction.NORTH);
            helper.getLevel().destroyBlock(positions[broken], true);
            for (int index = 0; index < 4; index++) {
                Block expected = index == broken ? Blocks.AIR : raw[index];
                helper.assertTrue(helper.getLevel().getBlockState(positions[index]).is(expected), "Breaking part " + broken + " must restore component " + index);
            }
            AABB area = new AABB(still.getBlockPos()).inflate(2);
            List<ItemStack> drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getItem).toList();
            helper.assertTrue(count(drops, ModItems.FERMENTED_AGAVE_WORT.get()) == 3, "Wort inventory must drop exactly once");
            helper.assertTrue(count(drops, ModItems.FIFTH_BOTTLE_EMPTY.get()) == 5, "Bottle inventory must drop exactly once");
            helper.assertTrue(count(drops, raw[broken].asItem()) == 1, "Broken component must drop once");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void agaveGrowsOnSandAndFarmlandAndHasMaturityLoot(GameTestHelper helper) {
        AgaveCropBlock crop = (AgaveCropBlock) ModBlocks.CROP_AGAVE.get();
        BlockPos pos = helper.absolutePos(ORIGIN);
        for (Block substrate : List.of(Blocks.SAND, Blocks.RED_SAND, Blocks.FARMLAND)) {
            helper.getLevel().setBlockAndUpdate(pos.below(), substrate.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pos, crop.getStateForAge(0));
            helper.assertTrue(crop.getStateForAge(0).canSurvive(helper.getLevel(), pos), "Agave must survive on " + substrate);
            List<ItemStack> youngDrops = Block.getDrops(crop.getStateForAge(0), helper.getLevel(), pos, null);
            helper.assertTrue(count(youngDrops, ModItems.AGAVE_SEEDS.get()) >= 1, "Young agave must return seed");
            helper.assertTrue(count(youngDrops, ModItems.AGAVE.get()) == 0, "Young agave must not drop mature produce");
            crop.performBonemeal(helper.getLevel(), helper.getLevel().random, pos, helper.getLevel().getBlockState(pos));
            BlockState mature = helper.getLevel().getBlockState(pos);
            helper.assertTrue(mature.getValue(crop.getAgeProperty()) == 2, "Bonemeal must mature agave");
            List<ItemStack> drops = Block.getDrops(mature, helper.getLevel(), pos, null);
            helper.assertTrue(count(drops, ModItems.AGAVE_SEEDS.get()) >= 1 && count(drops, ModItems.AGAVE.get()) >= 1, "Mature agave must drop seed and produce");
        }
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        helper.assertTrue(!crop.getStateForAge(0).canSurvive(helper.getLevel(), pos), "Agave must reject stone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void instantFermentationIsUnavailableAndReusableCraftingToolsRemain(GameTestHelper helper) {
        assertCraft(helper, List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(ModItems.COOKED_AGAVE_PULP.get())), ModItems.UNFERMENTED_AGAVE_WORT.get(), 1, Items.BUCKET);
        CraftingInput instantFerment = CraftingInput.of(2, 1, List.of(new ItemStack(ModItems.UNFERMENTED_AGAVE_WORT.get()), new ItemStack(ModItems.BARREL.get())));
        helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, instantFerment, helper.getLevel()).isEmpty(),
                "Wort must ferment over time in a production vessel, not bypass fermentation through a crafting shortcut");
        assertCraft(helper, List.of(new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get()), new ItemStack(ModItems.BARREL.get())), ModItems.FIFTH_GOLDEN_TEQUILA.get(), 1, ModItems.BARREL.get());
        assertCraft(helper, List.of(new ItemStack(Items.APPLE), new ItemStack(ModItems.JUICER.get()), new ItemStack(Items.GLASS_BOTTLE)), ModItems.APPLE_JUICE.get(), 1, ModItems.JUICER.get());
        assertCraft(helper, List.of(new ItemStack(ModItems.FIFTH_SILVER_TEQUILA.get()), new ItemStack(ModItems.PACK_OF_SHOT_GLASSES.get())), ModItems.SHOT_SILVER_TEQUILA.get(), 25, null);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void drinkingReturnsContainerForSingleAndStackedShots(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack single = new ItemStack(ModItems.SHOT_SILVER_TEQUILA.get());
        assertStack(helper, single.finishUsingItem(helper.getLevel(), player), Items.GLASS_BOTTLE, 1);
        ItemStack stacked = new ItemStack(ModItems.SHOT_SILVER_TEQUILA.get(), 2);
        assertStack(helper, stacked.finishUsingItem(helper.getLevel(), player), ModItems.SHOT_SILVER_TEQUILA.get(), 1);
        helper.assertTrue(player.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "Stacked drinks must put empty container in inventory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cocktailAppliesEffectsAndCreativeDrinkKeepsStack(GameTestHelper helper) {
        Player survival = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack cocktail = new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get());
        assertStack(helper, cocktail.finishUsingItem(helper.getLevel(), survival), ModItems.MARGARITA_GLASS.get(), 1);
        helper.assertTrue(survival.hasEffect(MobEffects.LUCK) && survival.hasEffect(MobEffects.JUMP)
                && survival.hasEffect(MobEffects.MOVEMENT_SPEED), "Cocktail must grant its three advertised effects");
        Player creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.getAbilities().instabuild = true;
        ItemStack shots = new ItemStack(ModItems.SHOT_SILVER_TEQUILA.get(), 4);
        assertStack(helper, shots.finishUsingItem(helper.getLevel(), creative), ModItems.SHOT_SILVER_TEQUILA.get(), 4);
        helper.assertTrue(creative.getInventory().countItem(Items.GLASS_BOTTLE) == 0, "Creative drinking must not generate containers");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void alcoholAttachmentSurvivesPlayerSaveAndDecaysAtBoundary(GameTestHelper helper) {
        Player original = helper.makeMockPlayer(GameType.SURVIVAL);
        BAC originalState = original.getData(BACCapability.BAC_STATE);
        originalState.setBACLevel(4);
        originalState.setBACTicks(BAC.DECAY_TICKS - 2);
        CompoundTag saved = new CompoundTag();
        original.saveWithoutId(saved);
        Player restored = helper.makeMockPlayer(GameType.SURVIVAL);
        restored.load(saved);
        BAC restoredState = restored.getData(BACCapability.BAC_STATE);
        helper.assertTrue(restoredState.getBACLevel() == 4 && restoredState.getBACTicks() == BAC.DECAY_TICKS - 2,
                "Player save/load must preserve alcohol level and metabolism timer");
        helper.assertTrue(!restoredState.tick() && restoredState.getBACLevel() == 4, "Alcohol must not decay early");
        helper.assertTrue(restoredState.tick() && restoredState.getBACLevel() == 3 && restoredState.getBACTicks() == 0,
                "Alcohol must decay exactly one unit at the boundary");
        restoredState.setBACLevel(0);
        helper.assertTrue(!restoredState.tick() && restoredState.getBACLevel() == 0, "Sober players must never decay below zero");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void alcoholEffectsAndPoisoningUseRegisteredDamageType(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        IDrink.updatePlayerBAC(player, 9);
        helper.assertTrue(player.hasEffect(MobEffects.CONFUSION) && player.hasEffect(MobEffects.BLINDNESS)
                && player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "High BAC must apply negative effects");
        IDrink.updatePlayerBAC(player, 10);
        helper.assertTrue(player.isDeadOrDying(), "Maximum BAC must cause alcohol poisoning");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void maintainingAlcoholEffectsStillAllowsRegenerationToHeal(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(ORIGIN);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        player.setHealth(10.0F);
        player.getFoodData().setFoodLevel(10);
        for (int tick = 0; tick < 60; tick++) {
            if (tick % 20 == 0) IDrink.updatePlayerBAC(player, 1);
            player.tick();
        }
        helper.assertTrue(player.getHealth() > 10.0F, "Refreshing BAC effects must not reset regeneration before it can heal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeAdvancementsLoadAndIronUnlocksSteelSmelting(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        long recipes = server.getAdvancements().getAllAdvancements().stream()
                .filter(advancement -> advancement.id().getNamespace().equals("vinum") && advancement.id().getPath().startsWith("recipes/")).count();
        helper.assertTrue(recipes >= 28, "All original Vinum recipe unlock advancements must remain loaded; got " + recipes);
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            if (recipe.id().getNamespace().equals("vinum") && !(recipe.value() instanceof io.vinum.production.FluidProcessRecipe)) {
                ResourceLocation unlock = ResourceLocation.fromNamespaceAndPath("vinum", "recipes/" + recipe.id().getPath());
                helper.assertTrue(server.getAdvancements().get(unlock) != null, "Vinum recipe must be discoverable through an unlock advancement: " + recipe.id());
            }
        }
        ServerPlayer player = serverPlayer(helper);
        ResourceLocation recipe = ResourceLocation.fromNamespaceAndPath("vinum", "smelting/steel_ingot");
        helper.assertTrue(!player.getRecipeBook().contains(recipe), "New player must begin without steel recipe");
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        player.getInventory().add(iron.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), iron);
        helper.assertTrue(player.getRecipeBook().contains(recipe), "Acquiring iron must unlock steel smelting in the recipe book");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverDrinkingChangesBacAndPersistsWholeBottleDose(GameTestHelper helper) {
        ServerPlayer player = serverPlayer(helper);
        ItemStack shot = new ItemStack(ModItems.SHOT_SILVER_TEQUILA.get());
        assertStack(helper, shot.finishUsingItem(helper.getLevel(), player), Items.GLASS_BOTTLE, 1);
        helper.assertTrue(player.getData(BACCapability.BAC_STATE).getBACLevel() == 1, "Server drink must increment player BAC");
        helper.assertTrue(player.hasEffect(MobEffects.REGENERATION), "First shot must apply regeneration");
        BAC state = player.getData(BACCapability.BAC_STATE);
        state.setBACLevel(25);
        BAC restored = new BAC();
        restored.deserializeNBT(helper.getLevel().registryAccess(), state.serializeNBT(helper.getLevel().registryAccess()));
        helper.assertTrue(restored.getBACLevel() == 25, "BAC persistence must preserve doses above lethal threshold");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void fakePlayerDrinksWithoutRequiringNetworkConnection(GameTestHelper helper) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "vinum-automation"));
        ItemStack shot = new ItemStack(ModItems.SHOT_SILVER_TEQUILA.get());
        assertStack(helper, shot.finishUsingItem(helper.getLevel(), player), Items.GLASS_BOTTLE, 1);
        helper.assertTrue(player.getData(BACCapability.BAC_STATE).getBACLevel() == 1, "Automated drink use must update fake player BAC without transport");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldgenFeaturesLoadAndTargetOnlyIntendedBiomes(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var placed = registries.registryOrThrow(Registries.PLACED_FEATURE);
        var configured = registries.registryOrThrow(Registries.CONFIGURED_FEATURE);
        ResourceLocation agaveId = ResourceLocation.fromNamespaceAndPath("vinum", "patch_agave");
        ResourceLocation leadId = ResourceLocation.fromNamespaceAndPath("vinum", "lead_ore");
        var agave = placed.getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, agaveId));
        var lead = placed.getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, leadId));
        configured.getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, agaveId));
        configured.getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, leadId));
        var biomes = registries.registryOrThrow(Registries.BIOME);
        for (var key : List.of(Biomes.DESERT, Biomes.BADLANDS, Biomes.WOODED_BADLANDS, Biomes.ERODED_BADLANDS)) {
            var settings = biomes.getOrThrow(key).getGenerationSettings();
            helper.assertTrue(settings.hasFeature(agave.value()), "Arid biome must generate agave: " + key.location());
            helper.assertTrue(settings.hasFeature(lead.value()), "Overworld biome must generate lead: " + key.location());
        }
        helper.assertTrue(!biomes.getOrThrow(Biomes.PLAINS).getGenerationSettings().hasFeature(agave.value()), "Agave must not generate in plains");
        helper.assertTrue(biomes.getOrThrow(Biomes.PLAINS).getGenerationSettings().hasFeature(lead.value()), "Lead must generate in ordinary overworld biomes");
        var nether = biomes.getOrThrow(Biomes.NETHER_WASTES).getGenerationSettings();
        helper.assertTrue(!nether.hasFeature(agave.value()) && !nether.hasFeature(lead.value()), "Vinum overworld features must not leak into Nether");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void configuredLeadFeatureActuallyReplacesStone(GameTestHelper helper) {
        var chunks = new java.util.HashSet<net.minecraft.world.level.chunk.ChunkAccess>();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 0, 1), new BlockPos(10, 4, 10))) {
            helper.setBlock(pos, Blocks.STONE);
            chunks.add(helper.getLevel().getChunk(helper.absolutePos(pos)));
        }
        // The test world is already generated; recreate the heightmap normally present during the ore generation stage.
        for (var chunk : chunks) net.minecraft.world.level.levelgen.Heightmap.primeHeightmaps(chunk,
                java.util.Set.of(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG));
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("vinum", "lead_ore"))).value();
        BlockPos center = helper.absolutePos(new BlockPos(6, 2, 6));
        // Fixed seeds exercise the configured engine deterministically, independent of terrain or biome placement height.
        for (int seed = 0; seed < 16; seed++) feature.place(helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(seed), center);
        int ore = 0;
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(1, 0, 1), new BlockPos(10, 4, 10))) {
            if (helper.getLevel().getBlockState(helper.absolutePos(pos)).is(ModBlocks.BLOCK_LEAD_ORE.get())) ore++;
        }
        helper.assertTrue(ore > 0, "Configured lead ore feature must produce actual lead blocks in replaceable stone");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wineBarrelsRotateAndDropTheirOwnVariant(GameTestHelper helper) {
        List<Block> barrels = List.of(ModBlocks.BLOCK_WINE_BARREL_EMPTY.get(), ModBlocks.BLOCK_WINE_BARREL_CHARDONNAY.get(),
                ModBlocks.BLOCK_WINE_BARREL_SAUVIGNON_BLANC.get(), ModBlocks.BLOCK_WINE_BARREL_CABERNET_SAUVIGNON.get(),
                ModBlocks.BLOCK_WINE_BARREL_MERLOT.get(), ModBlocks.BLOCK_WINE_BARREL_PINOT_NOIR.get(), ModBlocks.BLOCK_WINE_BARREL_ZINFANDEL.get());
        BlockPos pos = helper.absolutePos(ORIGIN);
        for (Block barrel : barrels) {
            BlockState state = barrel.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            helper.getLevel().setBlockAndUpdate(pos, state);
            helper.assertTrue(state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING) == Direction.EAST,
                    "Wine barrel must rotate with structures: " + barrel);
            List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), pos, null);
            helper.assertTrue(count(drops, barrel.asItem()) == 1, "Wine barrel must retain its variant when harvested: " + barrel);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void wineProcessingSandsFallAndLandWithoutLosingVariant(GameTestHelper helper) {
        List<Block> sands = List.of(ModBlocks.BLOCK_LEADED_SAND.get(), ModBlocks.BLOCK_FERROUS_SAND.get(), ModBlocks.BLOCK_REFINED_SAND.get());
        for (int index = 0; index < sands.size(); index++) {
            BlockPos floor = new BlockPos(3 + index * 3, 0, 4);
            helper.setBlock(floor, Blocks.STONE);
            helper.setBlock(floor.above(3), sands.get(index));
        }
        helper.runAfterDelay(40, () -> {
            for (int index = 0; index < sands.size(); index++) {
                BlockPos floor = new BlockPos(3 + index * 3, 0, 4);
                helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(floor.above())).is(sands.get(index)), "Sand must land with original variant: " + sands.get(index));
                helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(floor.above(3))).isAir(), "Sand must leave unsupported position");
            }
            helper.succeed();
        });
    }

    private static void assertCraft(GameTestHelper helper, List<ItemStack> ingredients, Item output, int quantity, Item remainder) {
        CraftingInput input = CraftingInput.of(ingredients.size(), 1, ingredients);
        CraftingRecipe recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
        assertStack(helper, recipe.assemble(input, helper.getLevel().registryAccess()), output, quantity);
        if (remainder != null) helper.assertTrue(count(recipe.getRemainingItems(input), remainder) == 1, "Crafting must preserve " + remainder);
    }

    private static ServerPlayer serverPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "vinum-test-player");
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), profile, ClientInformation.createDefault());
        // Keep real player advancements/attachments; suppress transport because this harness has no client handshake.
        player.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet) {}
        };
        return player;
    }

    private static int count(List<ItemStack> stacks, Item item) {
        return stacks.stream().filter(stack -> stack.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    private static StillMasterTileEntity assemble(GameTestHelper helper, BlockPos relative, Direction facing) {
        BlockPos master = helper.absolutePos(relative);
        BlockPos[] positions = parts(master, facing);
        Block[] components = {ModBlocks.STEEL_BRAZIER.get(), ModBlocks.STEEL_POT.get(), ModBlocks.STEEL_COIL.get(), ModBlocks.STEEL_POT.get()};
        for (int index = 0; index < 4; index++) helper.getLevel().setBlockAndUpdate(positions[index], components[index].defaultBlockState());
        StillMultiblock.build(helper.getLevel(), positions[3]);
        helper.assertTrue(helper.getLevel().getBlockState(master).is(ModBlocks.STILL_MULTIBLOCK_PART_1.get()), "Raw components must assemble a still");
        helper.assertTrue(helper.getLevel().getBlockState(master).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing, "Assembled still must face " + facing);
        return (StillMasterTileEntity) helper.getLevel().getBlockEntity(master);
    }

    private static BlockPos[] parts(BlockPos master, Direction facing) {
        BlockPos output = master.relative(facing.getCounterClockWise());
        return new BlockPos[]{master, master.above(), output.above(), output};
    }

    private static void fill(StillMasterTileEntity still, Item fuel) {
        still.setItem(0, new ItemStack(ModItems.FERMENTED_AGAVE_WORT.get()));
        still.setItem(1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get()));
        still.setItem(2, new ItemStack(fuel));
    }

    private static void tick(GameTestHelper helper, StillMasterTileEntity still, int ticks) {
        for (int tick = 0; tick < ticks; tick++) StillMasterTileEntity.serverTick(helper.getLevel(), still.getBlockPos(), still.getBlockState(), still);
    }

    private static void assertStack(GameTestHelper helper, ItemStack actual, Item expected, int count) {
        helper.assertTrue(actual.is(expected) && actual.getCount() == count, "Expected " + count + " " + expected + ", got " + actual);
    }
}
