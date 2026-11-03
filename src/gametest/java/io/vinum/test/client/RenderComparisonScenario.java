package io.vinum.test.client;

import io.vinum.bar.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.shelf.*;
import io.vinum.test.client.legacy.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Screenshot;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Same-camera screenshots of frozen reference meshes, optimized meshes, reload and changed fill. */
final class RenderComparisonScenario implements ClientScenario {
    private CompletableFuture<Void> reload;
    private volatile boolean screenshotSaved;
    private int settled;
    private RendererSwap renderers;
    private boolean hideGui;
    private long previousDayTime;
    private boolean previousDayCycle,timeCaptured;
    private int fov;

    public String name(){return "render_comparison";}

    public List<Step> steps(ClientTestContext c) {
        return new Sequence()
            .await("prepare isolated rendering fixture",c::prepare,c::serverDone,200)
            .action("snapshot render state and install reference renderers",()->{
                hideGui=c.mc.options.hideGui;
                fov=c.mc.options.fov().get();
                renderers=new RendererSwap(c.mc.getBlockEntityRenderDispatcher());
                c.onRestore(()->{renderers.restore();c.mc.options.hideGui=hideGui;c.mc.options.fov().set(fov);});
                renderers.reference();
                c.onRestore(()->{if(timeCaptured)c.server(p->{
                    p.serverLevel().setDayTime(previousDayTime);
                    p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(previousDayCycle,p.server);
                });});
                c.mc.options.hideGui=true;
                c.mc.options.fov().set(30);
                c.mc.getToasts().clear();
            })
            .await("place fixed bottle and drinkware comparison scene",()->c.server(p->{
                previousDayTime=p.serverLevel().getDayTime();
                previousDayCycle=p.serverLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
                timeCaptured=true;
                p.serverLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,p.server);
                p.serverLevel().setDayTime(6000);
                var samples=samples();
                for(int x=-2;x<=2;x++) {
                    for(int y=0;y<=3;y++)p.serverLevel().setBlockAndUpdate(c.target.offset(x,y,-2),Blocks.WHITE_CONCRETE.defaultBlockState());
                    p.serverLevel().setBlockAndUpdate(c.target.offset(x,-1,1),Blocks.SEA_LANTERN.defaultBlockState());
                    var pos=c.target.offset(x,0,1);
                    p.serverLevel().setBlockAndUpdate(pos,BarContent.BAR.get().defaultBlockState().setValue(BarBlock.FACING,Direction.SOUTH));
                    var bar=(BarBlockEntity)p.serverLevel().getBlockEntity(pos);
                    var slots=NonNullList.withSize(BarBlockEntity.SLOTS,ItemStack.EMPTY);
                    for(int i=0;i<slots.size();i++)slots.set(i,samples.get(((x+2)*slots.size()+i)%samples.size()));
                    var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,slots,p.registryAccess());bar.loadWithComponents(tag,p.registryAccess());
                    p.serverLevel().sendBlockUpdated(pos,bar.getBlockState(),bar.getBlockState(),2);
                }
                for(int x=-1;x<=1;x++) {
                    var pos=c.target.offset(x,1,-1);
                    p.serverLevel().setBlockAndUpdate(pos,ShelfContent.SHELF.get().defaultBlockState().setValue(WineShelfBlock.FACING,Direction.SOUTH));
                    var shelf=(WineShelfBlockEntity)p.serverLevel().getBlockEntity(pos);
                    var slots=NonNullList.withSize(WineShelfBlockEntity.SLOTS,ItemStack.EMPTY);
                    for(int i=0;i<slots.size();i++)slots.set(i,samples.get(((x+1)*slots.size()+i)%18));
                    var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,slots,p.registryAccess());shelf.loadWithComponents(tag,p.registryAccess());
                    p.serverLevel().sendBlockUpdated(pos,shelf.getBlockState(),shelf.getBlockState(),2);
                }
                p.teleportTo(p.serverLevel(),c.target.getX()+.5,c.target.getY()+.7,c.target.getZ()+6.7,180,5);
                p.setYHeadRot(180);
            }),c::serverDone,200)
            .await("wait for scene synchronization",()->{},()->c.mc.level.getBlockEntity(c.target.offset(-2,0,1)) instanceof BarBlockEntity b
                &&!b.bottle(0).isEmpty()&&c.mc.level.getBlockEntity(c.target.offset(1,1,-1)) instanceof WineShelfBlockEntity s&&!s.bottle(8).isEmpty(),200)
            .await("settle reference scene",()->settled=0,()->settle(c),100)
            .await("capture baseline screenshot",()->capture(c,"render-baseline.png"),()->screenshotSaved,200)
            .action("restore optimized renderers",()->renderers.restore())
            .await("settle optimized scene",()->settled=0,()->settle(c),100)
            .await("capture optimized screenshot",()->capture(c,"render-optimized.png"),()->screenshotSaved,200)
            .action("reload client resources",()->reload=c.mc.reloadResourcePacks())
            .await("finish resource reload",()->{},()->{
                if(!reload.isDone())return false;
                reload.join();return c.mc.getOverlay()==null;
            },1200)
            .await("settle reloaded scene",()->settled=0,()->settle(c),100)
            .await("capture resource reload screenshot",()->capture(c,"render-reloaded.png"),()->screenshotSaved,200)
            .await("change displayed liquid quantity on server",()->c.server(p->{
                var bar=(BarBlockEntity)p.serverLevel().getBlockEntity(c.target.offset(-2,0,1));
                var before=bar.bottle(0);
                var container=(FluidContainerItem)before.getItem();
                if(!bar.replace(0,before,container.filled(container.contents(before).copyWithAmount(50))))
                    throw new AssertionError("Could not change fixture bottle fill");
            }),c::serverDone,100)
            .await("observe changed fill on client",()->{},()->{
                if(!(c.mc.level.getBlockEntity(c.target.offset(-2,0,1)) instanceof BarBlockEntity bar))return false;
                var stack=bar.bottle(0);return stack.getItem() instanceof FluidContainerItem container&&container.contents(stack).getAmount()==50;
            },200)
            .await("settle changed fill scene",()->settled=0,()->settle(c),100)
            .await("capture changed fill screenshot",()->capture(c,"render-fill-changed.png"),()->screenshotSaved,200)
            .build();
    }

    private boolean settle(ClientTestContext c){c.mc.getToasts().clear();return ++settled>40;}

    private void capture(ClientTestContext c,String filename){
        c.mc.getToasts().clear();
        screenshotSaved=false;
        Screenshot.grab(c.mc.gameDirectory,filename,c.mc.getMainRenderTarget(),message->screenshotSaved=true);
    }

    private static List<ItemStack> samples(){
        var samples=new ArrayList<ItemStack>();
        for(var item:List.of(ModItems.FIFTH_BOTTLE_EMPTY.get(),ModItems.RUM_BOTTLE.get(),ModItems.MEAD_BOTTLE.get(),ModItems.HONEYSHINE_BOTTLE.get(),ModItems.BRANDY_BOTTLE.get())) {
            var container=(FluidContainerItem)item;
            var fluid=item==ModItems.FIFTH_BOTTLE_EMPTY.get()?ModFluids.TEQUILA:item==ModItems.RUM_BOTTLE.get()?ModFluids.RUM:
                item==ModItems.MEAD_BOTTLE.get()?ModFluids.MEAD:item==ModItems.HONEYSHINE_BOTTLE.get()?ModFluids.HONEYSHINE:ModFluids.BRANDY;
            samples.add(container.filled(ModItems.spirit(fluid.source().get(),750)));
            samples.add(container.filled(ModItems.spirit(fluid.source().get(),317)));
            samples.add(new ItemStack(item));
        }
        var wine=(FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get();
        samples.add(wine.filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),750)));
        samples.add(wine.filled(WineCatalog.sample(WineCatalog.varieties().getLast(),317)));
        samples.add(new ItemStack(wine));
        var shot=(FluidContainerItem)ModItems.SHOT_GLASS.get();
        samples.add(shot.filled(ModItems.spirit(ModFluids.RUM.source().get(),50)));
        samples.add(shot.filled(ModItems.spirit(ModFluids.TEQUILA.source().get(),20)));
        samples.add(new ItemStack(shot));
        var glass=(FluidContainerItem)ModItems.WINE_GLASS_EMPTY.get();
        samples.add(glass.filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),125)));
        samples.add(glass.filled(WineCatalog.sample(WineCatalog.varieties().getLast(),50)));
        samples.add(new ItemStack(glass));
        samples.add(new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()));
        return samples;
    }

    /** Test-only dispatcher override: no production registration or shipped reflection. */
    private static final class RendererSwap {
        private final BlockEntityRenderDispatcher dispatcher;
        private final Field field;
        private final Map<BlockEntityType<?>,BlockEntityRenderer<?>> original;
        private boolean referenceActive;
        @SuppressWarnings("unchecked") RendererSwap(BlockEntityRenderDispatcher dispatcher){
            this.dispatcher=dispatcher;
            try {
                field=BlockEntityRenderDispatcher.class.getDeclaredField("renderers");field.setAccessible(true);
                original=(Map<BlockEntityType<?>,BlockEntityRenderer<?>>)field.get(dispatcher);
            }catch(ReflectiveOperationException e){throw new AssertionError("Cannot snapshot test renderer registry",e);}
        }
        void reference(){
            var reference=new HashMap<>(original);
            reference.put(BarContent.ENTITY.get(),new LegacyBarRenderer(null));
            reference.put(ShelfContent.ENTITY.get(),new LegacyWineShelfRenderer(null));
            set(reference);referenceActive=true;
        }
        void restore(){if(referenceActive){set(original);referenceActive=false;}}
        private void set(Map<BlockEntityType<?>,BlockEntityRenderer<?>> value){
            try {field.set(dispatcher,value);}catch(IllegalAccessException e){throw new AssertionError("Cannot restore test renderer registry",e);}
        }
    }
}
