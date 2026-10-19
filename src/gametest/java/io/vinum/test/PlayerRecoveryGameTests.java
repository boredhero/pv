package io.vinum.test;

import io.vinum.test.harness.PlayerTestRecovery;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class PlayerRecoveryGameTests {
    @GameTest(template = "barrel_space")
    public static void persistedRecoveryRestoresPlayerAndCleansOnlyOwnedFixture(GameTestHelper helper) {
        var player = VinumGameTests.serverPlayer(helper);
        BlockPos target = helper.absolutePos(new BlockPos(7, 3, 7));
        Vec3 originalPosition = Vec3.atCenterOf(target.above(5));
        player.setPos(originalPosition);
        player.setYRot(41); player.setXRot(-18);
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = true;
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Recovery component sentinel"));
        sword.set(DataComponents.DAMAGE, 17);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET);
        ItemStack offhand = new ItemStack(Items.GOLDEN_APPLE, 3);
        player.getInventory().setItem(7, sword.copy());
        player.getInventory().setItem(39, helmet.copy());
        player.getInventory().setItem(40, offhand.copy());
        player.getInventory().selected = 7;
        PlayerTestRecovery.capture(player, target);

        // Exercise the actual player save/load boundary, including NeoForge persistent data.
        CompoundTag persistedPlayer = player.saveWithoutId(new CompoundTag());
        player.getPersistentData().remove(PlayerTestRecovery.RECOVERY);
        player.load(persistedPlayer);
        helper.assertTrue(PlayerTestRecovery.hasSnapshot(player), "Player NBT must retain the recovery journal across load");

        player.getInventory().clearContent();
        player.getInventory().setItem(0, new ItemStack(Items.DIRT, 31));
        player.getInventory().selected = 0;
        player.setPos(Vec3.atCenterOf(target));
        player.setGameMode(GameType.SURVIVAL);
        player.getAbilities().flying = false;
        helper.getLevel().setBlockAndUpdate(target, Blocks.CHEST.defaultBlockState());
        ((ChestBlockEntity) helper.getLevel().getBlockEntity(target)).setItem(0, new ItemStack(Items.DIAMOND, 9));
        BlockPos outside = target.offset(4, 0, 0);
        helper.getLevel().setBlockAndUpdate(outside, Blocks.GOLD_BLOCK.defaultBlockState());

        PlayerTestRecovery.restore(player);
        helper.assertTrue(ItemStack.matches(player.getInventory().getItem(7), sword)
                && ItemStack.matches(player.getInventory().getItem(39), helmet)
                && ItemStack.matches(player.getInventory().getItem(40), offhand)
                && player.getInventory().getItem(0).isEmpty(), "Restore must replace temporary inventory and retain item components, armor and offhand");
        helper.assertTrue(player.getInventory().selected == 7 && player.position().distanceToSqr(originalPosition) < 1e-8
                && player.getYRot() == 41 && player.getXRot() == -18, "Restore must recover selected slot, position and rotation");
        helper.assertTrue(player.gameMode.getGameModeForPlayer() == GameType.CREATIVE && player.getAbilities().flying,
                "Restore must recover game mode and flying ability");
        helper.assertTrue(helper.getLevel().getBlockState(target).isAir()
                && helper.getLevel().getBlockState(outside).is(Blocks.GOLD_BLOCK), "Cleanup must remove owned fixture and preserve blocks outside its bounds");
        helper.assertTrue(!PlayerTestRecovery.hasSnapshot(player), "Successful recovery must commit journal removal");
        helper.succeed();
    }

    @GameTest(template = "barrel_space")
    public static void missingAndWrongOwnerRecoveryRejectWithoutMutation(GameTestHelper helper) {
        var player = VinumGameTests.serverPlayer(helper);
        BlockPos target = helper.absolutePos(new BlockPos(7, 3, 7));
        player.setPos(Vec3.atCenterOf(target.above(5)));
        ItemStack sentinel = new ItemStack(Items.EMERALD, 11);
        player.getInventory().setItem(0, sentinel.copy());
        helper.getLevel().setBlockAndUpdate(target, Blocks.DIAMOND_BLOCK.defaultBlockState());
        boolean missingRejected = false;
        try { PlayerTestRecovery.restore(player); } catch (IllegalStateException expected) { missingRejected = true; }
        helper.assertTrue(missingRejected && !PlayerTestRecovery.hasSnapshot(player), "Missing journal must be rejected without creating a snapshot");

        PlayerTestRecovery.capture(player, target);
        CompoundTag journal = player.getPersistentData().getCompound(PlayerTestRecovery.RECOVERY);
        journal.putUUID("Owner", UUID.randomUUID());
        CompoundTag rejectedJournal = journal.copy();
        Vec3 unchangedPosition = player.position();
        boolean wrongOwnerRejected = false;
        try { PlayerTestRecovery.restore(player); } catch (IllegalStateException expected) { wrongOwnerRejected = true; }
        helper.assertTrue(wrongOwnerRejected && ItemStack.matches(player.getInventory().getItem(0), sentinel)
                && player.position().equals(unchangedPosition) && helper.getLevel().getBlockState(target).is(Blocks.DIAMOND_BLOCK),
                "Wrong owner must be rejected before changing inventory, position or world");
        helper.assertTrue(player.getPersistentData().getCompound(PlayerTestRecovery.RECOVERY).equals(rejectedJournal),
                "Rejected recovery must retain its journal unchanged");
        helper.succeed();
    }
}
