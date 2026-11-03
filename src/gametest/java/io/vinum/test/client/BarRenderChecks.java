package io.vinum.test.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.logging.LogUtils;
import io.vinum.bar.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.shelf.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

/** Execute the block-entity meshes, not just model baking, with real BufferBuilder lifetimes. */
public final class BarRenderChecks {
    private BarRenderChecks() {}
    public static void run() {
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        var samples=new ItemStack[]{
            ((FluidContainerItem)ModItems.FIFTH_BOTTLE_EMPTY.get()).filled(ModItems.spirit(ModFluids.TEQUILA.source().get(),317)),
            ((FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get()).filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),400)),
            ((FluidContainerItem)ModItems.SHOT_GLASS.get()).filled(ModItems.spirit(ModFluids.TEQUILA.source().get(),25)),
            ((FluidContainerItem)ModItems.WINE_GLASS_EMPTY.get()).filled(WineCatalog.sample(WineCatalog.varieties().getFirst(),60)),
            new ItemStack(ModItems.SPICED_APPLE_MARGARITA.get()),
            ((FluidContainerItem)ModItems.RUM_BOTTLE.get()).filled(ModItems.spirit(ModFluids.RUM.source().get(),375)),
            ((FluidContainerItem)ModItems.RUM_BOTTLE.get()).filled(io.vinum.barrel.BarrelFluidData.withAge(
                ModItems.spirit(ModFluids.RUM.source().get(),750), 90L*24000)),
            ((FluidContainerItem)ModItems.RUM_BOTTLE.get()).filled(io.vinum.barrel.BarrelFluidData.withAge(
                ModItems.spirit(ModFluids.RUM.source().get(),750), 365L*24000))};
        try(var buffers=new SwitchingBuffers()) {
            for(Direction facing:Direction.Plane.HORIZONTAL) {
                for(boolean filled:new boolean[]{true,false}) {
                  for(int offset:new int[]{0,1,2,3}) {
                    var bar=new BarBlockEntity(BlockPos.ZERO,BarContent.BAR.get().defaultBlockState().setValue(BarBlock.FACING,facing));
                    var slots=NonNullList.withSize(BarBlockEntity.SLOTS,ItemStack.EMPTY);
                    for(int i=0;i<slots.size();i++) {
                        var sample=samples[(i+offset)%samples.length];
                        slots.set(i,filled?sample:new ItemStack(sample.getItem()));
                    }
                    var tag=new CompoundTag();ContainerHelper.saveAllItems(tag,slots,registries);bar.loadWithComponents(tag,registries);
                    new BarRenderer(null).render(bar,0,new PoseStack(),buffers,LightTexture.FULL_BRIGHT,0);
                    var shelf=new WineShelfBlockEntity(BlockPos.ZERO,ShelfContent.SHELF.get().defaultBlockState().setValue(WineShelfBlock.FACING,facing));
                    shelf.insert(0,slots.get(0));shelf.insert(1,slots.get(1));
                    shelf.insert(2,filled?samples[5]:new ItemStack(ModItems.RUM_BOTTLE.get()));
                    shelf.insert(3,filled?samples[6]:new ItemStack(ModItems.RUM_BOTTLE.get()));
                    shelf.insert(4,filled?samples[7]:new ItemStack(ModItems.RUM_BOTTLE.get()));
                    new WineShelfRenderer(null).render(shelf,0,new PoseStack(),buffers,LightTexture.FULL_BRIGHT,0);
                  }
                }
            }
            buffers.finish();
            if(buffers.meshes<16)throw new AssertionError("Expected textured bottle, label, closure, glass and drinkware meshes");
            LogUtils.getLogger().info("Vinum render-path checks passed: {} real meshes across filled/empty bars and shelves in every facing",buffers.meshes);
        }
    }
    /** Switching an unbuffered render type ends its previous builder, just like BufferSource. */
    private static final class SwitchingBuffers implements MultiBufferSource,AutoCloseable {
        private final ByteBufferBuilder storage=new ByteBufferBuilder(65536);
        private BufferBuilder active;
        private RenderType type;
        private int meshes;
        @Override public VertexConsumer getBuffer(RenderType next) {
            if(active==null || type!=next) {
                finish();type=next;active=new BufferBuilder(storage,next.mode(),next.format());
            }
            return active;
        }
        void finish() {
            if(active!=null) {
                var mesh=active.build();active=null;
                if(mesh!=null){meshes++;mesh.close();}
            }
        }
        @Override public void close(){finish();storage.close();}
    }
}
