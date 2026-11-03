package io.vinum.test.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import io.vinum.bar.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.shelf.*;
import io.vinum.test.client.legacy.*;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Frozen pre-cache geometry is the oracle; compare output attributes, not implementation details. */
public final class RenderParityChecks {
    private static final RenderType LIQUID=RenderType.entityCutoutNoCull(ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png"));
    private static final double EPSILON=1e-6;
    private static final RegistryAccess REGISTRIES=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    private RenderParityChecks(){}
    public static void run(){
        int cases=0;
        for(Direction facing:Direction.Plane.HORIZONTAL)for(int transform=0;transform<3;transform++)for(var stack:samples()) {
            var bar=bar(facing,stack);var shelf=shelf(facing,stack);
            var oldBar=new Capture();var newBar=new Capture();
            new LegacyBarRenderer(null).render(bar,0,pose(transform),oldBar,0x00700030,0x00020003);
            new BarRenderer(null).render(bar,0,pose(transform),newBar,0x00700030,0x00020003);
            compare(oldBar,newBar,false,"bar "+stack+" facing="+facing+" transform="+transform);
            if(stack.getItem() instanceof FluidContainerItem container&&container.capacity()==750){
                var oldShelf=new Capture();var newShelf=new Capture();
                new LegacyWineShelfRenderer(null).render(shelf,0,pose(transform),oldShelf,0x00700030,0x00020003);
                new WineShelfRenderer(null).render(shelf,0,pose(transform),newShelf,0x00700030,0x00020003);
                compare(oldShelf,newShelf,true,"shelf "+stack+" facing="+facing+" transform="+transform);
                if(transform==0&&facing==Direction.NORTH&&stack.is(ModItems.WINE_BOTTLE_EMPTY.get())&&container.contents(stack).getAmount()==750){
                    var glass=RenderType.entityTranslucent(io.vinum.beverage.BeverageAppearance.resolve(container.contents(stack),stack).glassMaterial().texture());
                    int oldBody=oldShelf.vertices.get(LIQUID).size()+oldShelf.vertices.get(glass).size();
                    int newBody=newShelf.vertices.get(LIQUID).size()+newShelf.vertices.get(glass).size();
                    int oldRuns=oldShelf.requests.get(LIQUID)+oldShelf.requests.get(glass);
                    int newRuns=newShelf.requests.get(LIQUID)+newShelf.requests.get(glass);
                    if(oldBody!=3840||newBody!=2688||oldRuns!=20||newRuns!=2)
                        throw new AssertionError("Wine rack body optimization changed unexpectedly: vertices "+oldBody+" -> "+newBody+", runs "+oldRuns+" -> "+newRuns);
                }
            }
            cases++;
        }
        cacheLifecycle();
        changingContents();
        LogUtils.getLogger().info("Vinum cached render parity passed: {} cases, exact UV/color/light/overlay and positions within max(4ULP,{}), normals within1e-6",cases,EPSILON);
        benchmark();
    }
    private static PoseStack pose(int transform){
        var p=new PoseStack();
        if(transform>0){p.translate(-12.125,2.75,6.0625);p.mulPose(Axis.YP.rotationDegrees(37));}
        if(transform>1){p.scale(.75f,1.25f,.9f);p.mulPose(Axis.XP.rotationDegrees(17));}
        return p;
    }
    static List<ItemStack> samples(){
        var result=new ArrayList<ItemStack>();
        var wine=WineCatalog.sample(WineCatalog.varieties().getFirst(),750);
        var items=new net.minecraft.world.item.Item[]{ModItems.FIFTH_BOTTLE_EMPTY.get(),ModItems.RUM_BOTTLE.get(),
            ModItems.WINE_BOTTLE_EMPTY.get(),ModItems.MEAD_BOTTLE.get(),ModItems.HONEYSHINE_BOTTLE.get(),ModItems.BRANDY_BOTTLE.get(),
            ModItems.SHOT_GLASS.get(),ModItems.WINE_GLASS_EMPTY.get()};
        var fluids=new net.minecraft.world.level.material.Fluid[]{ModFluids.TEQUILA.source().get(),ModFluids.RUM.source().get(),wine.getFluid(),
            ModFluids.MEAD.source().get(),ModFluids.HONEYSHINE.source().get(),ModFluids.BRANDY.source().get(),
            ModFluids.RUM.source().get(),wine.getFluid()};
        for(int i=0;i<items.length;i++){
            var item=(FluidContainerItem)items[i];result.add(new ItemStack(item));
            for(int amount:new int[]{1,item.capacity()/2,item.capacity()}) {
                var fluid=i==2||i==7?wine.copyWithAmount(amount):ModItems.spirit(fluids[i],amount);
                result.add(item.filled(fluid));
            }
        }
        result.add(new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()));
        return result;
    }
    static BarBlockEntity bar(Direction facing,ItemStack stack){
        var bar=new BarBlockEntity(BlockPos.ZERO,BarContent.BAR.get().defaultBlockState().setValue(BarBlock.FACING,facing));
        load(bar,BarBlockEntity.SLOTS,stack);return bar;
    }
    static WineShelfBlockEntity shelf(Direction facing,ItemStack stack){
        var shelf=new WineShelfBlockEntity(BlockPos.ZERO,ShelfContent.SHELF.get().defaultBlockState().setValue(WineShelfBlock.FACING,facing));
        load(shelf,WineShelfBlockEntity.SLOTS,stack);return shelf;
    }
    static void load(net.minecraft.world.level.block.entity.BlockEntity entity,int count,ItemStack stack){
        var slots=NonNullList.withSize(count,ItemStack.EMPTY);slots.set(0,stack.copy());
        var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,slots,REGISTRIES);entity.loadWithComponents(tag,REGISTRIES);
    }
    private static void changingContents(){
        var item=(FluidContainerItem)ModItems.RUM_BOTTLE.get();
        var bar=bar(Direction.NORTH,item.filled(ModItems.spirit(ModFluids.RUM.source().get(),750)));
        var renderer=new BarRenderer(null);
        for(int amount:new int[]{750,375,0,1,750}){
            load(bar,BarBlockEntity.SLOTS,amount==0?new ItemStack(item):item.filled(ModItems.spirit(ModFluids.RUM.source().get(),amount)));
            var expected=new Capture();var actual=new Capture();
            new LegacyBarRenderer(null).render(bar,0,new PoseStack(),expected,0x00200030,17);
            renderer.render(bar,0,new PoseStack(),actual,0x00200030,17);
            compare(expected,actual,false,"changed existing bar fill "+amount);
            var repeated=new Capture();renderer.render(bar,0,new PoseStack(),repeated,0x00200030,17);repeated.finish();
            for(var type:actual.vertices.keySet()){
                var first=actual.vertices.get(type);var second=repeated.vertices.get(type);
                if(second==null||first.size()!=second.size())throw new AssertionError("Cache hit changed material geometry");
                for(int i=0;i<first.size();i++)if(!Arrays.equals(first.get(i),second.get(i)))
                    throw new AssertionError("Cache hit must be bit-identical to initial playback");
            }
            var relit=new Capture();var relitExpected=new Capture();
            renderer.render(bar,0,new PoseStack(),relit,0x00D000E0,29);
            new LegacyBarRenderer(null).render(bar,0,new PoseStack(),relitExpected,0x00D000E0,29);
            compare(relitExpected,relit,false,"same cached mesh with changed lighting "+amount);
        }
    }
    private static void compare(Capture expected,Capture actual,boolean rack,String context){
        expected.finish();actual.finish();
        if(!expected.vertices.keySet().equals(actual.vertices.keySet()))throw new AssertionError("Material mismatch: "+context);
        for(var type:expected.vertices.keySet()){
            var old=expected.vertices.get(type);var current=actual.vertices.get(type);
            if(rack&&type==LIQUID){
                // Opaque end caps between adjoining sections are intentionally removed. Every retained
                // quad must still match an original quad; glass, artwork and closures use exact sequences.
                if(current.size()>old.size()||current.size()%4!=0)throw new AssertionError("Invalid rack reduction: "+context);
                boolean[] used=new boolean[old.size()/4];
                for(int n=0;n<current.size();n+=4){
                    boolean found=false;
                    for(int o=0;o<old.size();o+=4)if(!used[o/4]&&quadMatches(old,o,current,n)){
                        used[o/4]=true;found=true;break;
                    }
                    if(!found)throw new AssertionError("Rack introduced changed liquid quad "+n+": "+context);
                }
            }else{
                if(old.size()!=current.size())throw new AssertionError("Vertex count "+old.size()+" -> "+current.size()+": "+context+" "+type);
                for(int n=0;n<old.size();n++)if(!matches(old.get(n),current.get(n)))
                    throw new AssertionError("Vertex "+n+" changed: "+context+" "+type+" expected="+Arrays.toString(old.get(n))+" actual="+Arrays.toString(current.get(n)));
            }
        }
    }
    private static boolean quadMatches(List<int[]> a,int ai,List<int[]> b,int bi){for(int k=0;k<4;k++)if(!matches(a.get(ai+k),b.get(bi+k)))return false;return true;}
    private static boolean matches(int[] a,int[] b){
        for(int i=0;i<a.length;i++)if(a[i]!=b[i]){
            // Baked local transforms reassociate floating-point matrix operations. At translated
            // world coordinates an absolute epsilon alone is smaller than one representable step.
            float expected=Float.intBitsToFloat(a[i]),actual=Float.intBitsToFloat(b[i]);
            if(i<=2&&Math.abs(expected-actual)<=Math.max(EPSILON,4*Math.ulp(expected)))continue;
            if(i>=11&&Math.abs(expected-actual)<=EPSILON)continue;
            return false;
        }
        return true;
    }
    static final class Capture implements MultiBufferSource {
        final Map<RenderType,List<int[]>> vertices=new LinkedHashMap<>();
        final Map<RenderType,Integer> requests=new HashMap<>();
        private final Map<RenderType,Probe> probes=new HashMap<>();
        public VertexConsumer getBuffer(RenderType type){requests.merge(type,1,Integer::sum);return probes.computeIfAbsent(type,t->new Probe(vertices.computeIfAbsent(t,k->new ArrayList<>())));}
        void finish(){for(var p:probes.values())p.flush();}
    }
    private static final class Probe implements VertexConsumer {
        private final List<int[]> target;private int[] vertex;
        Probe(List<int[]> target){this.target=target;}
        void flush(){if(vertex!=null){target.add(vertex);vertex=null;}}
        public VertexConsumer addVertex(float x,float y,float z){flush();vertex=new int[14];vertex[0]=Float.floatToIntBits(x);vertex[1]=Float.floatToIntBits(y);vertex[2]=Float.floatToIntBits(z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){vertex[3]=r;vertex[4]=g;vertex[5]=b;vertex[6]=a;return this;}
        public VertexConsumer setUv(float u,float v){vertex[7]=Float.floatToIntBits(u);vertex[8]=Float.floatToIntBits(v);return this;}
        public VertexConsumer setUv1(int u,int v){vertex[9]=u|(v<<16);return this;}
        public VertexConsumer setUv2(int u,int v){vertex[10]=u|(v<<16);return this;}
        public VertexConsumer setNormal(float x,float y,float z){vertex[11]=Float.floatToIntBits(x);vertex[12]=Float.floatToIntBits(y);vertex[13]=Float.floatToIntBits(z);return this;}
    }
    private static void cacheLifecycle(){
        var cache=new io.vinum.client.RenderMeshCache<Integer>(2);
        int[] calls={0};
        java.util.function.Supplier<io.vinum.client.CachedRenderMesh> factory=()->{
            calls[0]++;
            return io.vinum.client.CachedRenderMesh.record((poses,buffers)->{
                var out=buffers.getBuffer(LIQUID);
                for(int i=0;i<4;i++)out.addVertex(i%2,0,i/2).setColor(0x7F123456)
                    .setUv(i%2,i/2).setOverlay(0).setLight(0).setNormal(0,1,0);
            });
        };
        var first=cache.get(1,factory);
        if(cache.get(1,factory)!=first||calls[0]!=1||cache.hitCount()!=1||first.vertexCount()!=4)
            throw new AssertionError("Unchanged mesh must hit cache without rebuilding");
        cache.get(2,factory);cache.get(1,factory);cache.get(3,factory);
        if(cache.size()!=2||cache.cachedVertexCount()!=8||cache.buildCount()!=3)
            throw new AssertionError("Mesh cache must remain bounded and account for evicted vertices");
        cache.get(2,factory);
        if(calls[0]!=4)throw new AssertionError("Least recently used mesh was not evicted");
        var capture=new Capture();first.render(new PoseStack().last(),capture,0x00120034,0x00560078);capture.finish();
        for(var vertex:capture.vertices.get(LIQUID))if(vertex[9]!=0x00560078||vertex[10]!=0x00120034
                ||vertex[3]!=0x12||vertex[4]!=0x34||vertex[5]!=0x56||vertex[6]!=0x7F)
            throw new AssertionError("Cached playback must preserve color and apply current light/overlay");
        long generation=io.vinum.client.RenderMeshCache.generation();
        io.vinum.client.RenderMeshCache.clearAll();
        if(io.vinum.client.RenderMeshCache.generation()<=generation||cache.size()!=0||cache.cachedVertexCount()!=0)
            throw new AssertionError("Resource reload must invalidate caches and slot generations");
        cache.get(2,factory);if(calls[0]!=5)throw new AssertionError("Invalidated geometry must rebuild");
    }
    private static void benchmark(){
        var stack=samples().get(7);
        var bars=new BarBlockEntity[1000];
        for(int i=0;i<bars.length;i++)bars[i]=bar(Direction.NORTH,stack);
        var legacy=new LegacyBarRenderer(null);var current=new BarRenderer(null);
        var sink=new CountingSink();MultiBufferSource buffers=type->sink;
        var pose=new PoseStack();
        for(var bar:bars){legacy.render(bar,0,pose,buffers,LightTexture.FULL_BRIGHT,0);current.render(bar,0,pose,buffers,LightTexture.FULL_BRIGHT,0);}
        var bean=java.lang.management.ManagementFactory.getThreadMXBean();
        var allocations=bean instanceof com.sun.management.ThreadMXBean b&&b.isThreadAllocatedMemorySupported()?b:null;
        if(allocations!=null&&!allocations.isThreadAllocatedMemoryEnabled())allocations.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();
        var measurements=new ArrayList<Map<String,Object>>();
        for(int count:new int[]{1,100,1000}){
            long[] before=new long[7],after=new long[7],oldBytes=new long[7],newBytes=new long[7];
            for(int round=0;round<7;round++){
                long bytes=allocations==null?0:allocations.getThreadAllocatedBytes(thread),begin=System.nanoTime();
                for(int i=0;i<count;i++)legacy.render(bars[i],0,pose,buffers,LightTexture.FULL_BRIGHT,0);
                before[round]=System.nanoTime()-begin;
                oldBytes[round]=allocations==null?-1:allocations.getThreadAllocatedBytes(thread)-bytes;
                bytes=allocations==null?0:allocations.getThreadAllocatedBytes(thread);begin=System.nanoTime();
                for(int i=0;i<count;i++)current.render(bars[i],0,pose,buffers,LightTexture.FULL_BRIGHT,0);
                after[round]=System.nanoTime()-begin;
                newBytes[round]=allocations==null?-1:allocations.getThreadAllocatedBytes(thread)-bytes;
            }
            Arrays.sort(before);Arrays.sort(after);Arrays.sort(oldBytes);Arrays.sort(newBytes);
            measurements.add(Map.of("objects",count,"legacyMedianCpuNanos",before[3],"cachedMedianCpuNanos",after[3],
                "legacyMedianAllocatedBytes",oldBytes[3],"cachedMedianAllocatedBytes",newBytes[3]));
        }
        try{
            var directory=net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath().resolve("client-test-results");
            java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve("render-benchmark.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("scope","1000 distinct warmed block entities, one rum bottle each, Java mesh submission to counting sink; not FPS or GPU/shader performance", "measurements",measurements,"checksum",sink.checksum)));
        }catch(java.io.IOException error){throw new RuntimeException(error);}
        LogUtils.getLogger().info("Vinum warmed CPU mesh benchmark (not FPS): {}",measurements);
    }
    private static final class CountingSink implements VertexConsumer {
        long checksum;
        public VertexConsumer addVertex(float x,float y,float z){checksum+=Float.floatToIntBits(x)^Float.floatToIntBits(y)^Float.floatToIntBits(z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){return this;}
        public VertexConsumer setUv(float u,float v){return this;}
        public VertexConsumer setUv1(int u,int v){return this;}
        public VertexConsumer setUv2(int u,int v){return this;}
        public VertexConsumer setNormal(float x,float y,float z){return this;}
    }
}
