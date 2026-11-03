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
        int liquidColor = 0xFFED8D16;
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
                    shape.radius(surface) * INNER_SCALE,
                    appearance.cocktail() ? 0xFFFFAD28 : liquidColor, light, overlay);
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
        // The item sprite has granular salt, ice/spice in orange liquid, and TWO apple wedges.
        // Finish every opaque detail together after the glass pass; never reuse an ended builder.
        VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(MATERIAL));
        double top = shape.height(shape.sections());
        ring(out, poses.last(), shape, top + .001, shape.radius(top) + .001,
                shape.radius(top) - .004, 0xFFF5E8CF, light, overlay);
        for (int i = 0; i < 40; i++) {
            double angle = i * Math.PI * 2 / 40;
            double radius = shape.radius(top) + (i % 3 - 1) * .0018;
            double size = i % 4 == 0 ? .006 : .004;
            box(out, poses.last(), Math.cos(angle) * radius, top + .002,
                    Math.sin(angle) * radius, size, .003 + (i % 3) * .001, size,
                    i % 3 == 0 ? 0xFFE4D9C6 : 0xFFFFF9E9, light, overlay);
        }

        double surface = shape.fillHeight(1);
        // Three irregular, partly submerged cubes with pale faces and blue-white edges.
        ice(out, poses, -.045, surface + .007, -.025, .035, 24, light, overlay);
        ice(out, poses, .036, surface + .010, -.040, .030, -18, light, overlay);
        ice(out, poses, -.012, surface + .005, .050, .033, 62, light, overlay);
        // Seeded positions are fixed per glass: no flicker and no world/random state mutation.
        for (int i = 0; i < 27; i++) {
            double angle = i * 2.399963;
            double radius = .098 * Math.sqrt((i + .5) / 27);
            double x = Math.cos(angle) * radius, z = Math.sin(angle) * radius;
            box(out, poses.last(), x, surface + .0015, z, i % 3 == 0 ? .004 : .0027,
                    .0015, .0025, i % 4 == 0 ? 0xFFB96B27 : 0xFF713B1A, light, overlay);
        }

        apple(out, poses, .060, top - .005, .020, -15, 0xFF7D9E2C, light, overlay);
        apple(out, poses, .087, top - .014, -.014, -32, 0xFFB93420, light, overlay);
    }

    private static void ice(VertexConsumer out, PoseStack poses, double x, double y, double z,
                            double size, float rotation, int light, int overlay) {
        poses.pushPose();
        poses.translate(x, y, z);
        poses.mulPose(Axis.YP.rotationDegrees(rotation));
        box(out, poses.last(), 0, 0, 0, size, .017, size * .82, 0xFFC6E4E5, light, overlay);
        box(out, poses.last(), -.001, .009, -.001, size * .76, .002, size * .58, 0xFFF5FCED, light, overlay);
        box(out, poses.last(), -size * .42, .002, 0, .003, .016, size * .82, 0xFFEAF9F2, light, overlay);
        poses.popPose();
    }

    /** Thick semicircular apple slice: continuous cream flesh and colored peel, no citrus segments. */
    private static void apple(VertexConsumer out, PoseStack poses, double x, double y, double z,
                              float angle, int peel, int light, int overlay) {
        poses.pushPose();
        poses.translate(x, y, z);
        poses.mulPose(Axis.ZP.rotationDegrees(angle));
        double radius = .058, flesh = .051, thickness = .009;
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * i / 12, b = Math.PI * (i + 1) / 12;
            double ax = Math.cos(a), ay = Math.sin(a), bx = Math.cos(b), by = Math.sin(b);
            for (int side : new int[]{-1, 1}) {
                double depth = side * thickness;
                BarRenderer.quad(out, poses.last(), new double[][] {
                        {0, 0, depth}, {ax * flesh, ay * flesh, depth},
                        {bx * flesh, by * flesh, depth}, {0, 0, depth}},
                        0xFFFFE3A1, 0, 0, side, light, overlay);
                BarRenderer.quad(out, poses.last(), new double[][] {
                        {ax * flesh, ay * flesh, depth}, {ax * radius, ay * radius, depth},
                        {bx * radius, by * radius, depth}, {bx * flesh, by * flesh, depth}},
                        peel, 0, 0, side, light, overlay);
            }
            BarRenderer.quad(out, poses.last(), new double[][] {
                    {ax * radius, ay * radius, -thickness}, {ax * radius, ay * radius, thickness},
                    {bx * radius, by * radius, thickness}, {bx * radius, by * radius, -thickness}},
                    peel, (float)Math.cos((a+b)/2), (float)Math.sin((a+b)/2), 0, light, overlay);
        }
        // The straight cut edge exposes pale flesh; tiny dark seeds distinguish apple from citrus.
        box(out, poses.last(), 0, -.001, 0, radius * 2, .003, thickness * 2, 0xFFF7D594, light, overlay);
        box(out, poses.last(), -.009, .009, -thickness-.001, .003, .006, .001, 0xFF704323, light, overlay);
        box(out, poses.last(), .008, .012, -thickness-.001, .003, .005, .001, 0xFF704323, light, overlay);
        poses.popPose();
    }

    private static void box(VertexConsumer out, PoseStack.Pose pose, double x, double y, double z,
                            double width, double height, double depth, int color, int light, int overlay) {
        double x0=x-width/2,x1=x+width/2,y0=y-height/2,y1=y+height/2,z0=z-depth/2,z1=z+depth/2;
        BarRenderer.quad(out,pose,new double[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}},color,0,0,-1,light,overlay);
        BarRenderer.quad(out,pose,new double[][]{{x1,y0,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}},color,0,0,1,light,overlay);
        BarRenderer.quad(out,pose,new double[][]{{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}},color,-1,0,0,light,overlay);
        BarRenderer.quad(out,pose,new double[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}},color,1,0,0,light,overlay);
        BarRenderer.quad(out,pose,new double[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}},color,0,1,0,light,overlay);
        BarRenderer.quad(out,pose,new double[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}},color,0,-1,0,light,overlay);
    }
}
