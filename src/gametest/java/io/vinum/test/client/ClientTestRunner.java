package io.vinum.test.client;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/** Loaded only from the development test mod; never included in the production JAR. */
@EventBusSubscriber(modid = "vinum_tests", value = Dist.CLIENT)
public final class ClientTestRunner {
    private static final List<ClientScenario> SCENARIOS = ClientScenarios.all();
    private static ClientTestRunner active;
    private static final Deque<ClientScenario> queued = new ArrayDeque<>();
    private static final List<Map<String, String>> suiteResults = new ArrayList<>();
    private static int settleTicks;
    private final ClientTestContext context;
    private final String scenario;
    private final List<ClientScenario.Step> steps;
    private final List<String> completed = new ArrayList<>();
    private int index, ticks;
    private boolean started;
    private String outcome, detail;
    private CompletableFuture<Void> restoring;
    private long restoreStarted;

    private ClientTestRunner(ClientScenario scenario) {
        this.context = new ClientTestContext();
        this.scenario = scenario.name();
        this.steps = scenario.steps(context);
    }

    @SubscribeEvent
    public static void commands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("vinumclienttest")
            .executes(c -> { message("list | run <scenario> | runall | cancel | status | restore. Run temporarily changes inventory and an empty air fixture in your Creative test world, then restores both. Restore recovers an interrupted run from this world's saved player data."); return 1; })
            .then(Commands.literal("list").executes(c -> { message(String.join(", ", SCENARIOS.stream().map(ClientScenario::name).toList())); return 1; }))
            .then(Commands.literal("status").executes(c -> { message(active == null ? "Idle" : active.status()); return 1; }))
            .then(Commands.literal("runall").executes(c -> runAll()))
            .then(Commands.literal("cancel").executes(c -> { queued.clear(); if (active != null) active.finish("cancelled", "Cancelled by user"); return 1; }))
            .then(Commands.literal("restore").executes(c -> {
                if (active != null) { message("Wait for the current test restoration first"); return 0; }
                try {
                    ClientTestContext.recover().whenComplete((ignored, error) -> Minecraft.getInstance().execute(() ->
                        message(error == null ? "Saved player state and fixture restored" : "Recovery failed; snapshot retained: " + error)));
                    return 1;
                } catch (Throwable error) { message("Cannot recover: " + error); return 0; }
            }))
            .then(Commands.literal("run").then(Commands.argument("scenario", StringArgumentType.word())
                .suggests((c, b) -> { SCENARIOS.forEach(s -> b.suggest(s.name())); return b.buildFuture(); })
                .executes(c -> run(StringArgumentType.getString(c, "scenario"))))));
    }

    private static int run(String name) {
        if (active != null || !queued.isEmpty()) { message("A test or restoration is already running; use status or cancel."); return 0; }
        suiteResults.clear();
        return start(name);
    }

    private static int runAll() {
        if (active != null || !queued.isEmpty()) { message("A suite is already running."); return 0; }
        suiteResults.clear(); queued.addAll(SCENARIOS); settleTicks = 1;
        message("Queued " + queued.size() + " real-client scenarios; cancel stops the suite.");
        return 1;
    }

    private static int start(String name) {
        try {
            ClientScenario scenario = SCENARIOS.stream().filter(s -> s.name().equals(name)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown scenario: " + name));
            active = new ClientTestRunner(scenario);
            message("Running " + name + "; keep the test world open until restoration finishes.");
            return 1;
        } catch (Throwable error) { queued.clear(); message("Cannot run: " + error); return 0; }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (active != null) active.advance();
        else if (!queued.isEmpty() && --settleTicks <= 0 && Minecraft.getInstance().screen == null) {
            start(queued.removeFirst().name());
        }
    }

    private void advance() {
        if (restoring != null) {
            if (!restoring.isDone()) {
                if (context.mc.getSingleplayerServer() == null || System.nanoTime() - restoreStarted > 15_000_000_000L) {
                    outcome = "restore_failed";
                    detail += "; restoration could not finish. Recovery snapshot remains in this world's player NBT; reconnect and run /vinumclienttest restore";
                    queued.clear(); report(); active = null;
                }
                return;
            }
            try { restoring.join(); context.finishClientRestore(); } catch (Throwable error) { outcome = "restore_failed"; detail += "; " + error; }
            report();
            if (!"passed".equals(outcome)) queued.clear();
            settleTicks = 5; active = null; return;
        }
        try {
            if (context.mc.player == null || context.mc.level == null) throw new IllegalStateException("World closed during test");
            if (context.mc.screen != null && !(context.mc.screen instanceof ChatScreen)
                    && !(context.mc.screen instanceof AbstractContainerScreen<?>
                        && context.mc.player.containerMenu instanceof io.vinum.inventory.container.StillMasterContainer
                        && scenario.equals("still_menu")))
                throw new IllegalStateException("Unexpected screen opened during test");
            // Chat remains available for status/cancel; never issue clicks while the user is typing.
            if (context.mc.screen instanceof ChatScreen) return;
            ClientScenario.Step step = steps.get(index);
            if (!started) { started = true; ticks = 0; step.start().run(); }
            boolean complete = step.complete().getAsBoolean();
            // Give the player time to observe each action before the next step or cleanup.
            int delay = Math.clamp(Integer.getInteger("vinum.clientTestStepDelayTicks", 10), 0, 40);
            if (complete && ticks >= delay) {
                completed.add(step.name()); index++; started = false;
                if (index == steps.size()) finish("passed", "All client steps passed");
            } else if (++ticks > Math.max(delay, step.timeoutTicks())) throw new AssertionError("Timed out: " + step.name());
        } catch (Throwable error) { finish("failed", status() + ": " + error); }
    }

    private String status() { return scenario + ": " + (restoring != null ? "restoring" : steps.get(Math.min(index, steps.size() - 1)).name()); }

    private void finish(String result, String reason) {
        if (restoring != null) return;
        outcome = result; detail = reason;
        restoreStarted = System.nanoTime();
        try { restoring = context.restore(); }
        catch (Throwable error) { restoring = CompletableFuture.failedFuture(error); }
    }

    private void report() {
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("scenario", scenario); report.put("outcome", outcome);
            report.put("time", Instant.now().toString()); report.put("detail", detail);
            report.put("completedSteps", completed);
            Path directory = context.mc.gameDirectory.toPath().resolve("client-test-results");
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(scenario + ".json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
            suiteResults.add(Map.of("scenario", scenario, "outcome", outcome, "detail", detail));
            Files.writeString(directory.resolve("suite.json"), new GsonBuilder().setPrettyPrinting().create().toJson(suiteResults));
            message(scenario + ": " + outcome + ". " + detail + ". Report: " + directory);
        } catch (Exception error) { message(scenario + ": " + outcome + "; report failed: " + error); }
    }

    private static void message(String text) { Minecraft.getInstance().gui.getChat().addMessage(Component.literal("[Vinum client test] " + text)); }
}
