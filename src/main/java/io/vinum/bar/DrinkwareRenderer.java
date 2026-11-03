package io.vinum.bar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.vinum.beverage.BottleGeometry;
import io.vinum.beverage.DrinkwareAppearance;
import io.vinum.beverage.WineCatalog;
import io.vinum.item.FluidContainerItem;
import io.vinum.item.drinks.DrinkItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Placed-only glass meshes. Inventory artwork and item display transforms remain untouched. */
public final class DrinkwareRenderer {
    private static final ResourceLocation MATERIAL =
            ResourceLocation.withDefaultNamespace("textures/block/white_concrete.png");
    private static final double INNER_SCALE = .88;

    private DrinkwareRenderer() {}

    public static void render(ItemStack stack, DrinkwareAppearance appearance, PoseStack poses,
                              MultiBufferSource buffers, int light, int overlay) {
        BottleGeometry shape = appearance.geometry();
        poses.pushPose();
        poses.scale((float) shape.widthScale(), 1, (float) shape.depthScale());
        double fullness = 0;
        int liquidColor = 0xFFE6A137;
        if (stack.getItem() instanceof FluidContainerItem container) {
            var fluid = container.contents(stack);
            fullness = fluid.getAmount() / (double) container.capacity();
            liquidColor = 0xFF000000 | WineCatalog.color(fluid);
        } else if (stack.getItem() instanceof DrinkItem) {
            fullness = 1;
        }

        VertexConsumer opaque = buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
        if (fullness > 0) {
            double surface = shape.fillHeight(fullness);
            // Only the bowl contains liquid: stems and footplates never fill with wine.
            for (int i = 0; i < shape.sections(); i++) {
                double low = Math.max(shape.height(i), shape.liquidBottom());
                double high = Math.min(shape.height(i + 1), surface);
                if (high <= low) continue;
                BarRenderer.wall(opaque, poses.last(), shape, low, high,
                        shape.radius(low) * INNER_SCALE, shape.radius(high) * INNER_SCALE,
                        liquidColor, light, overlay);
            }
            BarRenderer.cap(opaque, poses.last(), shape, surface,
                    shape.radius(surface) * INNER_SCALE, liquidColor, light, overlay);
        }

        double top = shape.height(shape.sections());
        // The blue-white edges and thin stems echo the existing glass item sprites.
        BarRenderer.wall(opaque, poses.last(), shape, 0, .006,
                shape.radius(0), shape.radius(.006), appearance.rimColor(), light, overlay);
        for (int i = 0; i < shape.sections(); i++) {
            double low = shape.height(i), high = shape.height(i + 1);
            if (high <= shape.liquidBottom()
                    && Math.max(shape.radius(low), shape.radius(high)) <= .012) {
                BarRenderer.wall(opaque, poses.last(), shape, low, high,
                        shape.radius(low), shape.radius(high), appearance.rimColor(), light, overlay);
            }
        }
        ring(opaque, poses.last(), shape, top, shape.radius(top),
                shape.radius(top) - .004, appearance.rimColor(), light, overlay);
        BarRenderer.wall(opaque, poses.last(), shape, top - .004, top,
                shape.radius(top - .004), shape.radius(top), appearance.rimColor(), light, overlay);

        VertexConsumer glass = buffers.getBuffer(RenderType.entityTranslucent(MATERIAL));
        for (int i = 0; i < shape.sections(); i++) {
            double low = shape.height(i), high = shape.height(i + 1);
            BarRenderer.wall(glass, poses.last(), shape, low, high,
                    shape.radius(low), shape.radius(high), appearance.glassColor(), light, overlay);
        }
        // A shallow transparent floor closes the base while the top remains visibly open.
        BarRenderer.cap(glass, poses.last(), shape, .006, shape.radius(.006),
                appearance.glassColor(), light, overlay);

        if (appearance.cocktail() && fullness > 0) {
            garnish(poses, buffers, shape, light, overlay);
        }
        poses.popPose();
    }

    private static void ring(VertexConsumer out, PoseStack.Pose pose, BottleGeometry shape,
                             double y, double outer, double inner, int color, int light, int overlay) {
        for (int i = 0; i < shape.sides(); i++) {
            double a = 2 * Math.PI * i / shape.sides(), b = 2 * Math.PI * (i + 1) / shape.sides();
            BarRenderer.quad(out, pose, new double[][] {
                    {Math.cos(a) * inner, y, Math.sin(a) * inner},
                    {Math.cos(b) * inner, y, Math.sin(b) * inner},
                    {Math.cos(b) * outer, y, Math.sin(b) * outer},
                    {Math.cos(a) * outer, y, Math.sin(a) * outer}}, color, 0, 1, 0, light, overlay);
        }
    }

    private static void garnish(PoseStack poses, MultiBufferSource buffers, BottleGeometry shape,
                                int light, int overlay) {
        // Salt-colored lip and a fruit wheel on the rim match the orange cocktail sprite.
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
        double top = shape.height(shape.sections());
        ring(out, poses.last(), shape, top + .001, shape.radius(top) + .001,
                shape.radius(top) - .004, 0xFFF8E8C2, light, overlay);
        poses.pushPose();
        poses.translate(shape.radius(top) * .65, top + .025, 0);
        poses.mulPose(Axis.XP.rotationDegrees(78));
        BarRenderer.wall(out, poses.last(), shape, -.004, .004, .042, .042,
                0xFF86A02F, light, overlay);
        BarRenderer.cap(out, poses.last(), shape, .004, .042, 0xFFC2D45B, light, overlay);
        BarRenderer.cap(out, poses.last(), shape, -.004, .042, 0xFFC2D45B, light, overlay);
        for (int side : new int[]{-1, 1}) {
            for (int i = 0; i < 8; i++) {
                double a = 2 * Math.PI * i / 8 + .045, b = 2 * Math.PI * (i + 1) / 8 - .045;
                double y = side * .0045;
                BarRenderer.quad(out, poses.last(), new double[][] {
                        {0, y, 0}, {0, y, 0},
                        {Math.cos(b) * .034, y, Math.sin(b) * .034},
                        {Math.cos(a) * .034, y, Math.sin(a) * .034}},
                        i % 2 == 0 ? 0xFFD7E58A : 0xFFACC94E, 0, side, 0, light, overlay);
            }
        }
        poses.popPose();
    }
}
