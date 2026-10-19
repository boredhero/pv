package io.vinum.test.client;

import io.vinum.test.harness.PlayerTestRecovery;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The server bridge is the only place scenarios may inspect or mutate authoritative state. */
public final class ClientTestContext {
    public final Minecraft mc = Minecraft.getInstance();
    public final BlockPos target;
    private final net.minecraft.client.server.IntegratedServer server;
    private final UUID playerId;
    private boolean captured;
    private int selected;
    private final boolean shiftWasDown;
    private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    public ClientTestContext() {
        if (!Boolean.getBoolean("vinum.clientTests")) throw new IllegalStateException("Use the dedicated testClient development run.");
        server = mc.getSingleplayerServer();
        if (server == null || mc.player == null || !mc.player.isCreative() || !mc.player.hasPermissions(2))
            throw new IllegalStateException("Requires a singleplayer Creative test world with cheats enabled.");
        if (mc.player.containerMenu != mc.player.inventoryMenu) throw new IllegalStateException("Close the current container first.");
        playerId = mc.player.getUUID();
        target = mc.player.blockPosition().above(8);
        shiftWasDown = mc.options.keyShift.isDown();
    }

    public void server(Consumer<ServerPlayer> action) {
        pending = server.submit(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) throw new IllegalStateException("Test player disconnected");
            action.accept(player);
        });
    }

    public boolean serverDone() {
        if (!pending.isDone()) return false;
        pending.join();
        return true;
    }

    public void prepare() {
        server(player -> {
            if (!player.isCreative() || !player.hasPermissions(2)) throw new IllegalStateException("Creative and cheats required");
            if (PlayerTestRecovery.hasSnapshot(player))
                throw new IllegalStateException("Unfinished test recovery exists; run /vinumclienttest restore first");
            // Require a loaded, completely empty volume before taking ownership of any blocks.
            for (BlockPos pos : BlockPos.betweenClosed(target.offset(-3, -1, -3), target.offset(3, 3, 3))) {
                if (!player.serverLevel().hasChunkAt(pos) || !player.serverLevel().isInWorldBounds(pos)
                        || !player.serverLevel().getBlockState(pos).isAir())
                    throw new IllegalStateException("Fixture requires a loaded empty 7×5×7 air volume eight blocks above the player");
            }
            if (!player.serverLevel().getEntities(player, new net.minecraft.world.phys.AABB(Vec3.atLowerCornerOf(target.offset(-3, -1, -3)), Vec3.atLowerCornerOf(target.offset(4, 4, 4)))).isEmpty())
                throw new IllegalStateException("Fixture volume contains entities");
            selected = player.getInventory().selected;
            PlayerTestRecovery.capture(player, target);
            captured = true;
            player.getInventory().clearContent();
            player.getInventory().selected = 0;
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                player.serverLevel().setBlockAndUpdate(target.offset(x, -1, z), Blocks.GLASS.defaultBlockState());
            player.teleportTo(target.getX() + .5, target.getY(), target.getZ() + 2.5);
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
            player.inventoryMenu.broadcastChanges();
        });
    }

    public void equip(ServerPlayer player, int slot, ItemStack stack) {
        player.getInventory().setItem(slot, stack);
        player.inventoryMenu.broadcastChanges();
    }

    public void select(int slot) { mc.player.getInventory().selected = slot; }

    public void crouch(boolean value) {
        mc.options.keyShift.setDown(value);
        mc.player.input.shiftKeyDown = value;
        mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, value
                ? ServerboundPlayerCommandPacket.Action.PRESS_SHIFT_KEY : ServerboundPlayerCommandPacket.Action.RELEASE_SHIFT_KEY));
    }

    public void useBlock() {
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(target).add(0, 0, .5), Direction.SOUTH, target, false));
    }

    public void useItem() { mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); }

    public CompletableFuture<Void> restore() {
        mc.options.keyShift.setDown(shiftWasDown);
        if (mc.player != null) mc.player.closeContainer();
        // Queue after all earlier server work, even if preparation or an assertion failed.
        return server.submit(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (!captured) return;
            if (player == null) throw new IllegalStateException("Recovery saved in this world's player NBT under " + PlayerTestRecovery.RECOVERY
                    + "; reconnect to the same world and run /vinumclienttest restore");
            PlayerTestRecovery.restore(player);
        });
    }

    public static CompletableFuture<Void> recover() {
        Minecraft mc = Minecraft.getInstance();
        var server = mc.getSingleplayerServer();
        if (!Boolean.getBoolean("vinum.clientTests") || server == null || mc.player == null)
            throw new IllegalStateException("Reconnect to the original singleplayer test world using testClient");
        UUID owner = mc.player.getUUID();
        mc.player.closeContainer();
        return server.submit(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            if (player == null) throw new IllegalStateException("Player disconnected before recovery");
            PlayerTestRecovery.restore(player);
        });
    }

    public void finishClientRestore() {
        if (captured && mc.player != null) {
            mc.player.getInventory().selected = selected;
            crouch(shiftWasDown);
        }
    }
}
