package io.vinum.test.client;

import io.vinum.fluid.ModFluids;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.ModItems;
import io.vinum.sugar.SugarContent;
import io.vinum.sugar.SugarKettleBlockEntity;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Vanilla secondary-use packets must reach the carboy item, not place it beside the kettle. */
final class KettleTransferScenario implements ClientScenario {
    public String name() { return "kettle_transfer"; }
    public List<Step> steps(ClientTestContext c) {
        var sequence = new Sequence().await("prepare isolated fixture", c::prepare, c::serverDone, 200);
        var carboy = (FluidContainerItem) ModItems.CARBOY.get();
        for (var mode : List.of(GameType.CREATIVE, GameType.SURVIVAL)) {
            for (int existing : new int[]{0, 3500}) {
                int expectedKettle = Math.min(4000, existing + 1250);
                int expectedHeld = existing + 1250 - expectedKettle;
                String label = mode.getName() + " initial " + existing;
                sequence.await(label + " equip filled carboy and kettle", () -> c.server(p -> {
                    p.setGameMode(mode);
                    p.serverLevel().removeBlock(c.target, false);
                    p.serverLevel().setBlockAndUpdate(c.target, SugarContent.KETTLE.get().defaultBlockState());
                    var kettle = (SugarKettleBlockEntity)p.serverLevel().getBlockEntity(c.target);
                    if (existing > 0) kettle.fluids().fill(new FluidStack(ModFluids.CANE_JUICE.source().get(), existing), FluidAction.EXECUTE);
                    c.equip(p, 0, carboy.filled(new FluidStack(ModFluids.CANE_JUICE.source().get(), 1250)));
                }), c::serverDone, 100)
                .await(label + " await client synchronization", () -> {}, () ->
                    c.mc.level.getBlockEntity(c.target) instanceof SugarKettleBlockEntity kettle
                        && kettle.contents().getAmount() == existing
                        && carboy.contents(c.mc.player.getInventory().getItem(0)).getAmount() == 1250, 200)
                .action(label + " crouch and vanilla useItemOn", () -> { c.select(0); c.crouch(true); c.useBlock(); })
                .await(label + " synchronized transfer", () -> {}, () ->
                    c.mc.level.getBlockEntity(c.target) instanceof SugarKettleBlockEntity kettle
                        && kettle.contents().getAmount() == expectedKettle
                        && carboy.contents(c.mc.player.getMainHandItem()).getAmount() == expectedHeld, 200)
                .await(label + " authoritative conservation", () -> c.server(p -> {
                    var kettle = (SugarKettleBlockEntity)p.serverLevel().getBlockEntity(c.target);
                    if (kettle.contents().getAmount() != expectedKettle || carboy.contents(p.getMainHandItem()).getAmount() != expectedHeld)
                        throw new AssertionError("Carboy/kettle transfer lost fluid");
                }), c::serverDone, 100);
            }
            sequence.action(mode.getName() + " release crouch", () -> c.crouch(false))
            .await(mode.getName() + " reset for cold honey preparation", () -> c.server(p -> {
                var kettle = (SugarKettleBlockEntity)p.serverLevel().getBlockEntity(c.target);
                kettle.fluids().drain(4000, FluidAction.EXECUTE);
                c.equip(p, 0, new ItemStack(Items.WATER_BUCKET));
            }), c::serverDone, 100)
            .await("wait for water bucket", () -> {}, () -> c.mc.player.getInventory().getItem(0).is(Items.WATER_BUCKET), 200)
            .action("vanilla water bucket use", c::useBlock)
            .await("one litre water synchronized", () -> {}, () -> c.mc.level.getBlockEntity(c.target) instanceof SugarKettleBlockEntity kettle && kettle.contents().getAmount() == 1000, 200)
            .await("equip honey bottle", () -> c.server(p -> c.equip(p, 0, new ItemStack(Items.HONEY_BOTTLE))), c::serverDone, 100)
            .await("wait for honey bottle", () -> {}, () -> c.mc.player.getMainHandItem().is(Items.HONEY_BOTTLE), 200)
            .action("vanilla honey bottle use", c::useBlock)
            .await("must and returned bottle synchronized", () -> {}, () ->
                c.mc.level.getBlockEntity(c.target) instanceof SugarKettleBlockEntity kettle
                    && kettle.contents().getAmount() == 1250 && kettle.contents().getFluid() == ModFluids.MEAD_MUST.source().get()
                    && c.mc.player.getMainHandItem().is(Items.GLASS_BOTTLE), 200);
        }
        return sequence.build();
    }
}
