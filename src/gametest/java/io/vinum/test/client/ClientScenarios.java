package io.vinum.test.client;

import io.vinum.block.ModBlocks;
import io.vinum.block.SteelComponentBlock;
import io.vinum.inventory.container.StillMasterContainer;
import io.vinum.item.ModItems;
import io.vinum.multiblock.StillMultiblock;
import io.vinum.production.vessel.FermentationVesselBlockEntity;
import io.vinum.tileentity.StillMasterTileEntity;
import java.util.List;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

final class ClientScenarios {
    private ClientScenarios() {}
    static List<ClientScenario> all() {
        return List.of(new ClientScenario() {
            public String name() { return "carboy_interaction"; }
            public List<Step> steps(ClientTestContext c) {
                return new Sequence()
                    .await("prepare isolated fixture and snapshot player", c::prepare, c::serverDone, 200)
                    .await("place carboy and give juice", () -> c.server(p -> {
                        p.serverLevel().setBlockAndUpdate(c.target, ModBlocks.CARBOY.get().defaultBlockState());
                        c.equip(p, 0, new ItemStack(ModItems.GRAPE_JUICE_CHARDONNAY.get()));
                    }), c::serverDone, 100)
                    .await("wait for client block and inventory sync", () -> {}, () ->
                        c.mc.level.getBlockState(c.target).is(ModBlocks.CARBOY.get())
                            && c.mc.player.getInventory().getItem(0).is(ModItems.GRAPE_JUICE_CHARDONNAY.get()), 200)
                    .action("select juice and right click actual carboy", () -> { c.select(0); c.useBlock(); })
                    .await("assert client receives filled carboy", () -> {}, () ->
                        c.mc.level.getBlockEntity(c.target) instanceof FermentationVesselBlockEntity vessel
                            && vessel.getFluid().getAmount() > 0, 200)
                    .action("select empty hand and crouch", () -> { c.select(1); c.crouch(true); })
                    .action("right click to start fermentation", c::useBlock)
                    .await("assert client fermentation sync", () -> {}, () ->
                        c.mc.level.getBlockEntity(c.target) instanceof FermentationVesselBlockEntity vessel
                            && vessel.isFermenting(), 200)
                    .action("release crouch and request actual status", () -> { c.crouch(false); c.useBlock(); })
                    .await("assert server received fermentation interaction", () -> c.server(p -> {
                        if (!(p.serverLevel().getBlockEntity(c.target) instanceof FermentationVesselBlockEntity vessel)
                                || !vessel.isFermenting()) throw new AssertionError("Server vessel did not start fermenting");
                    }), c::serverDone, 100).build();
            }
        }, new ClientScenario() {
            public String name() { return "still_menu"; }
            public List<Step> steps(ClientTestContext c) {
                return new Sequence()
                    .await("prepare isolated fixture and snapshot player", c::prepare, c::serverDone, 200)
                    .await("place complete still and give empty bottle", () -> c.server(p -> {
                        var blocks = List.of(ModBlocks.STILL_MULTIBLOCK_PART_1, ModBlocks.STILL_MULTIBLOCK_PART_2,
                                ModBlocks.STILL_MULTIBLOCK_PART_3, ModBlocks.STILL_MULTIBLOCK_PART_4);
                        for (int part = 1; part <= 4; part++) p.serverLevel().setBlock(
                            StillMultiblock.position(c.target, Direction.NORTH, part), blocks.get(part - 1).get().defaultBlockState()
                                .setValue(SteelComponentBlock.FACING, Direction.NORTH), 2);
                        c.equip(p, 1, new ItemStack(ModItems.FIFTH_BOTTLE_EMPTY.get(), 3));
                    }), c::serverDone, 100)
                    .await("wait for client still and bottle", () -> {}, () ->
                        c.mc.level.getBlockState(c.target).is(ModBlocks.STILL_MULTIBLOCK_PART_1.get())
                            && c.mc.player.getInventory().getItem(1).getCount() == 3, 200)
                    .action("open still through real block interaction", () -> { c.select(0); c.useBlock(); })
                    .await("assert real still screen and menu", () -> {}, () ->
                        c.mc.screen instanceof AbstractContainerScreen<?> && c.mc.player.containerMenu instanceof StillMasterContainer, 200)
                    .action("shift click hotbar bottle into machine", () -> c.mc.gameMode.handleInventoryMouseClick(
                        c.mc.player.containerMenu.containerId, 32, 0, ClickType.QUICK_MOVE, c.mc.player))
                    .await("assert synced bottle slot", () -> {}, () ->
                        c.mc.player.containerMenu.getSlot(1).getItem().getCount() == 3
                            && c.mc.player.getInventory().getItem(1).isEmpty(), 200)
                    .await("assert authoritative still inventory", () -> c.server(p -> {
                        if (!(p.serverLevel().getBlockEntity(c.target) instanceof StillMasterTileEntity still)
                                || still.getItem(1).getCount() != 3) throw new AssertionError("Bottle click did not reach server inventory");
                    }), c::serverDone, 100)
                    .action("shift click machine bottles back to player", () -> c.mc.gameMode.handleInventoryMouseClick(
                        c.mc.player.containerMenu.containerId, 1, 0, ClickType.QUICK_MOVE, c.mc.player))
                    .await("assert returned bottles", () -> {}, () -> {
                        int count = 0;
                        for (int i = 0; i < 36; i++) if (c.mc.player.getInventory().getItem(i).is(ModItems.FIFTH_BOTTLE_EMPTY.get()))
                            count += c.mc.player.getInventory().getItem(i).getCount();
                        return count == 3 && c.mc.player.containerMenu.getSlot(1).getItem().isEmpty();
                    }, 200)
                    .action("close actual menu", () -> c.mc.player.closeContainer()).build();
            }
        }, new ClientScenario() {
            public String name() { return "shallow_vat"; }
            public List<Step> steps(ClientTestContext c) {
                return new Sequence()
                    .await("prepare isolated fixture and snapshot player", c::prepare, c::serverDone, 200)
                    .await("place three shallow vat pieces and give fourth", () -> c.server(p -> {
                        var state = ModBlocks.FERMENTATION_VAT.get().defaultBlockState();
                        p.serverLevel().setBlockAndUpdate(c.target, state);
                        p.serverLevel().setBlockAndUpdate(c.target.east(), state);
                        p.serverLevel().setBlockAndUpdate(c.target.east().south(), state);
                        c.equip(p, 0, new ItemStack(ModBlocks.FERMENTATION_VAT.get()));
                    }), c::serverDone, 100)
                    .await("wait for client vat pieces and held block", () -> {}, () ->
                        c.mc.level.getBlockState(c.target).is(ModBlocks.FERMENTATION_VAT.get())
                            && c.mc.level.getBlockState(c.target.east().south()).is(ModBlocks.FERMENTATION_VAT.get())
                            && c.mc.player.getInventory().getItem(0).is(ModBlocks.FERMENTATION_VAT.get().asItem()), 200)
                    .action("place fourth vat through actual held block interaction", () -> { c.select(0); c.useBlock(); })
                    .await("assert shallow vat topology sync on client", () -> {}, () ->
                        c.mc.level.getBlockEntity(c.target.south()) instanceof FermentationVesselBlockEntity vessel
                            && vessel.getStructureSize() == 2 && vessel.getStructureHeight() == 1
                            && vessel.getCapacity() == 64000, 200)
                    .await("assert authoritative shallow vat assembly", () -> c.server(p -> {
                        if (!(p.serverLevel().getBlockEntity(c.target.south()) instanceof FermentationVesselBlockEntity vessel)
                                || vessel.getStructureSize() != 2 || vessel.getStructureHeight() != 1
                                || vessel.getCapacity() != 64000)
                            throw new AssertionError("Actual placement did not assemble a 2×2×1 vat with 64,000 mB capacity");
                    }), c::serverDone, 100).build();
            }
        });
    }
}
