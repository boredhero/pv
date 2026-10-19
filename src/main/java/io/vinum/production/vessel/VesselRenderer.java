package io.vinum.production.vessel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vinum.common.Defines;
import io.vinum.tileentity.ModTileEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Upright open stave vats and a rising liquid surface, using the vanilla block atlas. */
@EventBusSubscriber(modid = Defines.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VesselRenderer implements BlockEntityRenderer<FermentationVesselBlockEntity> {
    private static final ResourceLocation OAK = ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");
    private static final ResourceLocation IRON = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    public VesselRenderer(BlockEntityRendererProvider.Context context) {}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTileEntities.CARBOY.get(), VesselRenderer::new);
        event.registerBlockEntityRenderer(ModTileEntities.FERMENTATION_VAT.get(), VesselRenderer::new);
    }
    @Override public void render(FermentationVesselBlockEntity vessel, float partialTick, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        if (!vessel.isController()) return;
        int size = vessel.getStructureSize();
        float center = size / 2F;
        float radius = vessel.isVat() ? size * .46F : .285F;
        if (vessel.isVat()) {
            var wood = buffers.getBuffer(RenderType.entityCutoutNoCull(OAK));
            // Individual narrow staves have a subtly alternating value, visible at every scale.
            for (int i = 0; i < 20; i++) {
                float a = (float) (i * Math.PI * 2 / 20), b = (float) ((i + .97) * Math.PI * 2 / 20);
                int tint = i % 2 == 0 ? 0xFFFFFFFF : 0xFFE5D7BE;
                wall(wood, poses.last(), center, radius, .08F, size * .94F, a, b, tint, light, overlay, false);
                wall(wood, poses.last(), center, radius - .09F, .12F, size * .94F, a, b, 0xFFC1A17A, light, overlay, true);
                float[] p = point(center, radius, a), q = point(center, radius, b);
                float[] r = point(center, radius - .09F, a), s = point(center, radius - .09F, b);
                quad(wood, poses.last(), p[0], size * .94F, p[1], q[0], size * .94F, q[1], s[0], size * .94F, s[1], r[0], size * .94F, r[1], 0xFFDDC394, light, overlay, 0, 1, 0);
                quad(wood, poses.last(), center, .12F, center, r[0], .12F, r[1], s[0], .12F, s[1], center, .12F, center, 0xFFAC8762, light, overlay, 0, 1, 0);
            }
            var hoops = buffers.getBuffer(RenderType.entityCutoutNoCull(IRON));
            for (float height : new float[] {size * .20F, size * .77F}) for (int i = 0; i < 20; i++) {
                wall(hoops, poses.last(), center, radius + .012F, height, height + .045F * size,
                        (float) (i * Math.PI * 2 / 20), (float) ((i + 1) * Math.PI * 2 / 20), 0xFF544F48, light, overlay, false);
            }
        }
        var fluid = vessel.getFluid();
        if (fluid.isEmpty()) return;
        var extension = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation texture = extension.getStillTexture(fluid);
        if (texture == null) return;
        var sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
        var out = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        float fill = Math.min(1F, fluid.getAmount() / (float) vessel.getCapacity());
        float height = vessel.isVat() ? .14F + fill * (size * .87F - .14F) : .13F + fill * .56F;
        int tint = extension.getTintColor(fluid);
        tint = (tint & 0x00FFFFFF) | 0xDF000000;
        float liquidRadius = radius - (vessel.isVat() ? .095F : .02F);
        for (int i = 0; i < 20; i++) {
            float[] p = point(center, liquidRadius, (float) (i * Math.PI * 2 / 20));
            float[] q = point(center, liquidRadius, (float) ((i + 1) * Math.PI * 2 / 20));
            vertex(out, poses.last(), center, height, center, sprite.getU(.5F), sprite.getV(.5F), tint, light, overlay, 0, 1, 0);
            vertex(out, poses.last(), p[0], height, p[1], sprite.getU((p[0] - center) / size + .5F), sprite.getV((p[1] - center) / size + .5F), tint, light, overlay, 0, 1, 0);
            vertex(out, poses.last(), q[0], height, q[1], sprite.getU((q[0] - center) / size + .5F), sprite.getV((q[1] - center) / size + .5F), tint, light, overlay, 0, 1, 0);
            vertex(out, poses.last(), center, height, center, sprite.getU(.5F), sprite.getV(.5F), tint, light, overlay, 0, 1, 0);
            if (!vessel.isVat()) {
                float nx = (p[0] + q[0] - 2 * center) / (2 * liquidRadius);
                float nz = (p[1] + q[1] - 2 * center) / (2 * liquidRadius);
                vertex(out, poses.last(), p[0], .13F, p[1], sprite.getU0(), sprite.getV1(), tint, light, overlay, nx, 0, nz);
                vertex(out, poses.last(), p[0], height, p[1], sprite.getU0(), sprite.getV0(), tint, light, overlay, nx, 0, nz);
                vertex(out, poses.last(), q[0], height, q[1], sprite.getU1(), sprite.getV0(), tint, light, overlay, nx, 0, nz);
                vertex(out, poses.last(), q[0], .13F, q[1], sprite.getU1(), sprite.getV1(), tint, light, overlay, nx, 0, nz);
            }
        }
    }
    private static float[] point(float center, float radius, float angle) { return new float[] {center + radius * (float) Math.cos(angle), center + radius * (float) Math.sin(angle)}; }
    private static void wall(VertexConsumer out, PoseStack.Pose pose, float center, float radius, float low, float high, float a, float b, int color, int light, int overlay, boolean inward) {
        float[] p = point(center, radius, inward ? b : a), q = point(center, radius, inward ? a : b);
        float normalX = (float) Math.cos((a + b) / 2) * (inward ? -1 : 1), normalZ = (float) Math.sin((a + b) / 2) * (inward ? -1 : 1);
        quad(out, pose, p[0], low, p[1], p[0], high, p[1], q[0], high, q[1], q[0], low, q[1], color, light, overlay, normalX, 0, normalZ);
    }
    private static void quad(VertexConsumer out, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, int color, int light, int overlay, float nx, float ny, float nz) {
        vertex(out, pose, ax, ay, az, 0, 1, color, light, overlay, nx, ny, nz);
        vertex(out, pose, bx, by, bz, 0, 0, color, light, overlay, nx, ny, nz);
        vertex(out, pose, cx, cy, cz, 1, 0, color, light, overlay, nx, ny, nz);
        vertex(out, pose, dx, dy, dz, 1, 1, color, light, overlay, nx, ny, nz);
    }
    private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light, int overlay, float nx, float ny, float nz) {
        out.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
    @Override public AABB getRenderBoundingBox(FermentationVesselBlockEntity vessel) {
        var origin = vessel.getStructureOrigin();
        int size = vessel.getStructureSize();
        return new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + size, origin.getY() + size, origin.getZ() + size);
    }
}
