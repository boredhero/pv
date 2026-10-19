package io.vinum.test.harness;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** Server-only recovery journal stored in the current world's player data. */
public final class PlayerTestRecovery {
    public static final String RECOVERY = "VinumClientTestRecovery";
    private PlayerTestRecovery() {}

    public static boolean hasSnapshot(ServerPlayer player) {
        return player.getPersistentData().contains(RECOVERY);
    }

    public static void capture(ServerPlayer player, BlockPos target) {
        if (hasSnapshot(player)) throw new IllegalStateException("Unfinished test recovery exists; run /vinumclienttest restore first");
        CompoundTag recovery = new CompoundTag();
        recovery.putUUID("Owner", player.getUUID());
        recovery.putString("Dimension", player.serverLevel().dimension().location().toString());
        recovery.put("Inventory", player.getInventory().save(new ListTag()));
        recovery.putDouble("X", player.getX()); recovery.putDouble("Y", player.getY()); recovery.putDouble("Z", player.getZ());
        recovery.putFloat("Yaw", player.getYRot()); recovery.putFloat("Pitch", player.getXRot());
        recovery.putInt("Selected", player.getInventory().selected);
        recovery.putInt("GameType", player.gameMode.getGameModeForPlayer().getId());
        recovery.putBoolean("Flying", player.getAbilities().flying);
        recovery.putLong("Fixture", target.asLong());
        player.getPersistentData().put(RECOVERY, recovery);
        // Persist the original state before any fixture or inventory mutation.
        player.server.getPlayerList().saveAll();
    }

    public static void restore(ServerPlayer player) {
        CompoundTag recovery = player.getPersistentData().getCompound(RECOVERY);
        if (!recovery.hasUUID("Owner") || !recovery.getUUID("Owner").equals(player.getUUID()))
            throw new IllegalStateException("No recovery snapshot for this player in this world");
        var server = player.server;
        var dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(recovery.getString("Dimension")));
        var level = server.getLevel(dimension);
        if (level == null) throw new IllegalStateException("Original recovery dimension is unavailable");
        BlockPos target = BlockPos.of(recovery.getLong("Fixture"));
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 3, 3)))
            if (!level.hasChunkAt(pos)) throw new IllegalStateException("Move near the original fixture before restoring; recovery snapshot retained");
        player.closeContainer();
        // Journal belongs to this world's player file; never import another world's snapshot.
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 3, 3))) {
            if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) container.clearContent();
        }
        for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 3, 3)))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        player.getInventory().load(recovery.getList("Inventory", 10));
        player.getInventory().selected = recovery.getInt("Selected");
        player.setGameMode(GameType.byId(recovery.getInt("GameType")));
        player.getAbilities().flying = recovery.getBoolean("Flying");
        player.onUpdateAbilities();
        player.teleportTo(level, recovery.getDouble("X"), recovery.getDouble("Y"), recovery.getDouble("Z"),
                recovery.getFloat("Yaw"), recovery.getFloat("Pitch"));
        player.connection.send(new ClientboundSetCarriedItemPacket(player.getInventory().selected));
        player.inventoryMenu.broadcastChanges();
        // First persist the restored player with its journal, then commit journal removal.
        server.getPlayerList().saveAll();
        player.getPersistentData().remove(RECOVERY);
        try { server.getPlayerList().saveAll(); }
        catch (RuntimeException error) { player.getPersistentData().put(RECOVERY, recovery); throw error; }
    }

}
