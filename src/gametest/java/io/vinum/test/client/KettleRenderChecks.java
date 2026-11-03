package io.vinum.test.client;

import com.mojang.blaze3d.vertex.*;
import io.vinum.fluid.ModFluids;
import io.vinum.sugar.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

/** Render actual synchronized tank snapshots; verify surfaces are visible inside the copper bowl. */
final class KettleRenderChecks {
    static void run() {
        var registries=RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        int checked=0;
        for(var type:java.util.List.of(Fluids.WATER,ModFluids.CANE_JUICE.source().get(),
                ModFluids.CANE_SYRUP.source().get(),ModFluids.MOLASSES.source().get(),
                ModFluids.CANE_WASH.source().get(),ModFluids.MEAD_MUST.source().get())) {
            float previous=0;
            for(int amount:new int[]{0,1,1000,4000}) {
                var kettle=new SugarKettleBlockEntity(BlockPos.ZERO,SugarContent.KETTLE.get().defaultBlockState());
                var tag=new CompoundTag();
                if(amount>0)tag.put("Fluid",new FluidStack(type,amount).save(registries));
                kettle.loadWithComponents(tag,registries);
                try(var storage=new ByteBufferBuilder(4096)) {
                    BufferBuilder[] builder={null};
                    new SugarKettleRenderer(null).render(kettle,0,new PoseStack(),renderType->{
                        if(builder[0]==null)builder[0]=new BufferBuilder(storage,renderType.mode(),renderType.format());
                        return builder[0];
                    },LightTexture.FULL_BRIGHT,0);
                    if(amount==0) {
                        if(builder[0]!=null)throw new AssertionError("Empty kettle rendered liquid");
                    } else {
                        if(builder[0]==null)throw new AssertionError("Filled kettle did not render liquid");
                        try(var mesh=builder[0].build()) {
                            if(mesh==null||mesh.drawState().vertexCount()!=4)throw new AssertionError("Kettle needs one complete surface quad");
                            var vertices=mesh.vertexBuffer();
                            int stride=mesh.drawState().format().getVertexSize();
                            float height=vertices.getFloat(4);
                            if(height<=previous||height<=4F/16||height>=13F/16)throw new AssertionError("Liquid must rise between floor and rim");
                            for(int i=0;i<4;i++) {
                                float x=vertices.getFloat(i*stride),y=vertices.getFloat(i*stride+4),z=vertices.getFloat(i*stride+8);
                                if(x<=3F/16||x>=13F/16||z<=3F/16||z>=13F/16||y!=height)
                                    throw new AssertionError("Liquid escaped kettle interior");
                            }
                            previous=height;
                        }
                    }
                }
                checked++;
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("Vinum kettle render checks passed: {} empty and filled tank meshes",checked);
    }
}
