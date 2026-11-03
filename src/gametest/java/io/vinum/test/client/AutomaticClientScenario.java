package io.vinum.test.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Explicit opt-in creates a fresh disposable world only inside the isolated test-client directory. */
@EventBusSubscriber(modid="vinum_tests",value=Dist.CLIENT)
public final class AutomaticClientScenario {
    private static boolean created,started;
    private static int settled;
    private AutomaticClientScenario() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        String scenario=System.getProperty("vinum.clientScenario","");
        if(scenario.isBlank()||!Boolean.getBoolean("vinum.clientTests"))return;
        var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().getFileName().toString().equals("run-test-client"))
            throw new IllegalStateException("Automatic scenarios require the isolated run-test-client directory");
        if(!created&&mc.level==null&&mc.screen!=null&&mc.getOverlay()==null) {
            com.mojang.logging.LogUtils.getLogger().info("Starting isolated automatic client scenario {} from {}",scenario,mc.screen.getClass().getSimpleName());
            created=true;
            String name="vinum-auto-"+java.util.UUID.randomUUID();
            mc.createWorldOpenFlows().createFreshLevel(name,
                new LevelSettings(name,GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                new WorldOptions(12345L,false,false),WorldPresets::createNormalWorldDimensions,mc.screen);
        }
        if(created&&!started&&mc.player!=null&&mc.level!=null&&mc.getSingleplayerServer()!=null&&++settled>80) {
            started=true;mc.setScreen(null);ClientTestRunner.automatic(scenario);
        }
    }
}
