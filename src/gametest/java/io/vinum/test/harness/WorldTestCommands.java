package io.vinum.test.harness;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Convenience commands for the existing in-game GameTest runner. Present only in the test mod. */
@EventBusSubscriber(modid = "vinum_tests")
public final class WorldTestCommands {
    private WorldTestCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("vinumtest").requires(source -> source.hasPermission(2))
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Scripted world tests (use a disposable test world): /vinumtest list | run <name> | runall | failed | report. Real client scenarios: /vinumclienttest."), false);
                    return 1;
                })
                .then(Commands.literal("list").executes(context -> {
                    var tests = GameTestRegistry.getAllTestFunctions().stream().filter(test -> test.structureName().startsWith("vinum_tests:"))
                            .map(test -> test.testName()).sorted().toList();
                    context.getSource().sendSuccess(() -> Component.literal(String.join("\n", tests)), false);
                    return tests.size();
                }))
                .then(Commands.literal("runall").executes(context -> dispatcher.execute("test runall", context.getSource())))
                .then(Commands.literal("failed").executes(context -> dispatcher.execute("test runfailed", context.getSource())))
                .then(Commands.literal("run").then(Commands.argument("name", StringArgumentType.greedyString())
                        .suggests((context, builder) -> {
                            GameTestRegistry.getAllTestFunctions().stream().map(test -> test.testName()).filter(name -> name.startsWith(builder.getRemaining())).forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> dispatcher.execute("test run " + StringArgumentType.getString(context, "name"), context.getSource()))))
                .then(Commands.literal("report").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal(StructuredTestReporter.summary()), false);
                    return 1;
                })));
    }
}
