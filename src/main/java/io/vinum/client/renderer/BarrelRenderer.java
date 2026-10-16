package io.vinum.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.vinum.block.WineBarrelBlock;
import io.vinum.common.Defines;
import io.vinum.multiblock.BarrelShapes;
import io.vinum.tileentity.BarrelBlockEntity;
import io.vinum.tileentity.ModTileEntities;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** One coopered barrel mesh per reservoir, independent of its stored contents. */
@EventBusSubscriber(modid = Defines.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BarrelRenderer implements BlockEntityRenderer<BarrelBlockEntity> {
    private static final ResourceLocation OAK = ResourceLocation.fromNamespaceAndPath("vinum", "textures/block/barrel_oak.png");
    private static final ResourceLocation PLANKS = ResourceLocation.withDefaultNamespace("textures/block/oak_planks.png");
    private static final ResourceLocation METAL = ResourceLocation.withDefaultNamespace("textures/block/iron_block.png");
    private static final int STAVES = 24;
    private static final int SEGMENTS = 4;
    private static final Map<Key, Mesh> MESHES = new HashMap<>();

    public BarrelRenderer(BlockEntityRendererProvider.Context context) {}

    @SubscribeEvent
    public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModTileEntities.BARREL.get(), BarrelRenderer::new);
    }

    @Override
    public void render(BarrelBlockEntity barrel, float partialTick, PoseStack poses, MultiBufferSource buffers,
                       int light, int overlay) {
        if (!barrel.isController()) return;
        int size = Math.clamp(barrel.getStructureSize(), 1, 9);
        Direction facing = barrel.getBlockState().getValue(WineBarrelBlock.FACING);
        Mesh mesh = MESHES.computeIfAbsent(new Key(size, facing), BarrelRenderer::build);
        draw(mesh.wood, buffers.getBuffer(RenderType.entityCutoutNoCull(OAK)), poses.last(), light, overlay, false);
        draw(mesh.planks, buffers.getBuffer(RenderType.entityCutoutNoCull(PLANKS)), poses.last(), light, overlay, true);
        draw(mesh.metal, buffers.getBuffer(RenderType.entityCutoutNoCull(METAL)), poses.last(), light, overlay, false);
    }

    @Override
    public AABB getRenderBoundingBox(BarrelBlockEntity barrel) {
        BlockPos origin = barrel.getStructureOrigin();
        int size = barrel.getStructureSize();
        return new AABB(origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + size, origin.getY() + size, origin.getZ() + size).inflate(size * .12);
    }

    @Override
    public boolean shouldRender(BarrelBlockEntity barrel, Vec3 camera) {
        if (!barrel.isController()) return false;
        double distance = getViewDistance() + barrel.getStructureSize();
        return getRenderBoundingBox(barrel).getCenter().distanceToSqr(camera) < distance * distance;
    }

    @Override
    public int getViewDistance() { return 128; }

    private static void draw(List<Quad> quads, VertexConsumer out, PoseStack.Pose pose, int light, int overlay, boolean planar) {
        for (Quad quad : quads) {
            vertex(out, pose, quad.a, quad.u0, quad.v0, quad, light, overlay, planar);
            vertex(out, pose, quad.b, quad.u1, quad.v0, quad, light, overlay, planar);
            vertex(out, pose, quad.c, quad.u1, quad.v1, quad, light, overlay, planar);
            vertex(out, pose, quad.d, quad.u0, quad.v1, quad, light, overlay, planar);
        }
    }

    private static void vertex(VertexConsumer out, PoseStack.Pose pose, Point p, float u, float v,
                               Quad quad, int light, int overlay, boolean planar) {
        if (planar) {
            // Continuous face-space UVs keep the vanilla planks square, including
            // circular end faces and the sloped surfaces of the saddle.
            if (Math.abs(quad.normal.z) > .5f) { u = p.x; v = 1 - p.y; }
            else if (Math.abs(quad.normal.x) > .5f) { u = p.z; v = 1 - p.y; }
            else { u = p.x; v = p.z; }
        }
        out.addVertex(pose, p.x, p.y, p.z).setColor(quad.color).setUv(u, v).setOverlay(overlay)
                .setLight(light).setNormal(pose, quad.normal.x, quad.normal.y, quad.normal.z);
    }

    private static Mesh build(Key key) {
        List<Quad> wood = new ArrayList<>(), planks = new ArrayList<>(), metal = new ArrayList<>();
        // A dark backing shows through the narrow gaps between separate oak staves.
        for (int stave = 0; stave < STAVES; stave++) {
            double start = stave * Math.PI * 2 / STAVES;
            double end = (stave + 1) * Math.PI * 2 / STAVES;
            int shade = 222 + (stave * 13 % 31);
            int oakColor = 0xff000000 | shade << 16 | shade << 8 | shade;
            for (int segment = 0; segment < SEGMENTS; segment++) {
                float z0 = (float) segment / SEGMENTS, z1 = (float) (segment + 1) / SEGMENTS;
                surface(wood, start, end, z0, z1, -.001f, 0xff302014, 0, 1);
                surface(wood, start + .007, end - .007, z0, z1, 0, oakColor,
                        (stave % 4) * .25f, (stave % 4 + 1) * .25f);
            }
        }
        endcap(planks, .035f, false);
        endcap(planks, .965f, true);
        headRim(planks, false);
        headRim(planks, true);
        // Broad forged hoops follow the same bowed profile as the wood.
        for (float center : new float[]{.09f, .29f, .71f, .91f}) {
            float z0 = center - .019f, z1 = center + .019f;
            for (int stave = 0; stave < STAVES; stave++) {
                double a = stave * Math.PI * 2 / STAVES, b = (stave + 1) * Math.PI * 2 / STAVES;
                surface(metal, a, b, z0, z1, .009f, 0xff393c3e, 0, 1);
                ringEdge(metal, a, b, z0, .009f, false);
                ringEdge(metal, a, b, z1, .009f, true);
                if (stave % 3 == 0) {
                    double angle = (a + b) / 2;
                    float r = radius(center) + .012f;
                    float x = .5f + r * (float) Math.cos(angle), y = (float) BarrelShapes.BODY_CENTER_Y + r * (float) Math.sin(angle);
                    box(metal, x - .009f, y - .009f, center - .009f,
                            x + .009f, y + .009f, center + .009f, 0xff737778);
                }
            }
        }
        // Substantial concave saddles meet the floor and embrace the barrel.
        saddle(planks, (float) BarrelShapes.FRONT_SADDLE_START);
        saddle(planks, (float) BarrelShapes.REAR_SADDLE_START);
        box(metal, .474f, .255f, -.055f, .526f, .305f, .025f, 0xffb19a58);
        box(metal, .48f, .215f, -.08f, .52f, .275f, -.04f, 0xffb19a58);
        box(metal, .491f, .305f, -.035f, .509f, .35f, -.015f, 0xff515557);
        box(metal, .455f, .34f, -.04f, .545f, .356f, -.01f, 0xff515557);
        return new Mesh(transform(wood, key), transform(planks, key), transform(metal, key));
    }

    private static float radius(float axis) { return (float) BarrelShapes.radius(axis); }

    private static Point radial(double angle, float axis, float offset) {
        float r = radius(axis) + offset;
        return new Point(.5f + r * (float) Math.cos(angle), (float) BarrelShapes.BODY_CENTER_Y + r * (float) Math.sin(angle), axis);
    }

    private static void surface(List<Quad> out, double a, double b, float z0, float z1,
                                float offset, int color, float u0, float u1) {
        Point p = radial(a, z0, offset), q = radial(b, z0, offset);
        float slope = (radius(z1) - radius(z0)) / (z1 - z0);
        double middle = (a + b) / 2;
        Point normal = new Point((float) Math.cos(middle), (float) Math.sin(middle), -slope).normalized();
        out.add(new Quad(p, q, radial(b, z1, offset), radial(a, z1, offset), normal, color, u0, u1, z0, z1));
    }

    private static void ringEdge(List<Quad> out, double a, double b, float z, float offset, boolean back) {
        out.add(new Quad(radial(a, z, 0), radial(b, z, 0), radial(b, z, offset), radial(a, z, offset),
                new Point(0, 0, back ? 1 : -1), 0xff242729, 0, 1, 0, 1));
    }

    private static void endcap(List<Quad> out, float z, boolean back) {
        float r = .369f, centerY = (float) BarrelShapes.BODY_CENTER_Y;
        Point center = new Point(.5f, centerY, z), normal = new Point(0, 0, back ? 1 : -1);
        // Triangle fans use the same planar oak-plank UVs across the entire head.
        for (int i = 0; i < STAVES; i++) {
            double a = i * Math.PI * 2 / STAVES, b = (i + 1) * Math.PI * 2 / STAVES;
            face(out, center, new Point(.5f + r * (float)Math.cos(a), centerY + r * (float)Math.sin(a), z),
                    new Point(.5f + r * (float)Math.cos(b), centerY + r * (float)Math.sin(b), z),
                    center, normal, 0xffffffff);
        }
    }

    private static void headRim(List<Quad> out, boolean back) {
        float z = back ? 1 : 0, inset = back ? .965f : .035f;
        float inner = .365f, outer = radius(z), centerY = (float)BarrelShapes.BODY_CENTER_Y;
        for (int i = 0; i < STAVES; i++) {
            double a = i * Math.PI * 2 / STAVES, b = (i + 1) * Math.PI * 2 / STAVES;
            Point ia = new Point(.5f + inner*(float)Math.cos(a), centerY + inner*(float)Math.sin(a), z);
            Point ib = new Point(.5f + inner*(float)Math.cos(b), centerY + inner*(float)Math.sin(b), z);
            Point oa = new Point(.5f + outer*(float)Math.cos(a), centerY + outer*(float)Math.sin(a), z);
            Point ob = new Point(.5f + outer*(float)Math.cos(b), centerY + outer*(float)Math.sin(b), z);
            face(out, ia, ib, ob, oa, new Point(0,0,back ? 1 : -1), 0xffddc8a7);
            face(out, ia, ib, new Point(ib.x,ib.y,inset), new Point(ia.x,ia.y,inset),
                    new Point(-(float)Math.cos((a+b)/2),-(float)Math.sin((a+b)/2),0), 0xffac8d63);
        }
    }

    private static void saddle(List<Quad> out, float z0) {
        float z1 = z0 + (float)BarrelShapes.SADDLE_DEPTH;
        float left = (float)BarrelShapes.SADDLE_MIN_X, right = (float)BarrelShapes.SADDLE_MAX_X;
        int strips = 24;
        for (int i = 0; i < strips; i++) {
            float x0 = left + (right-left)*i/strips, x1 = left + (right-left)*(i+1)/strips;
            float h00 = (float)BarrelShapes.saddleTop(x0,z0), h10 = (float)BarrelShapes.saddleTop(x1,z0);
            float h01 = (float)BarrelShapes.saddleTop(x0,z1), h11 = (float)BarrelShapes.saddleTop(x1,z1);
            Point a = new Point(x0,0,z0), b = new Point(x1,0,z0), c = new Point(x1,h10,z0), d = new Point(x0,h00,z0);
            Point e = new Point(x0,0,z1), f = new Point(x1,0,z1), g = new Point(x1,h11,z1), h = new Point(x0,h01,z1);
            face(out,a,b,c,d,new Point(0,0,-1),0xffdfc6a0);
            face(out,f,e,h,g,new Point(0,0,1),0xffdfc6a0);
            face(out,e,f,b,a,new Point(0,-1,0),0xffb49a72);
            Point topNormal = new Point(-((h10+h11)-(h00+h01))/(2*(x1-x0)),1,
                    -((h01+h11)-(h00+h10))/(2*(z1-z0))).normalized();
            face(out,d,c,g,h,topNormal,0xffead4b4);
            if (i==0) face(out,e,a,d,h,new Point(-1,0,0),0xffd5b98e);
            if (i==strips-1) face(out,b,f,g,c,new Point(1,0,0),0xffd5b98e);
        }
    }

    private static void box(List<Quad> out, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
        Point a = new Point(x0,y0,z0), b = new Point(x1,y0,z0), c = new Point(x1,y1,z0), d = new Point(x0,y1,z0);
        Point e = new Point(x0,y0,z1), f = new Point(x1,y0,z1), g = new Point(x1,y1,z1), h = new Point(x0,y1,z1);
        face(out,a,b,c,d,new Point(0,0,-1),color); face(out,f,e,h,g,new Point(0,0,1),color);
        face(out,e,a,d,h,new Point(-1,0,0),color); face(out,b,f,g,c,new Point(1,0,0),color);
        face(out,d,c,g,h,new Point(0,1,0),color); face(out,e,f,b,a,new Point(0,-1,0),color);
    }

    private static void face(List<Quad> out, Point a, Point b, Point c, Point d, Point normal, int color) {
        out.add(new Quad(a,b,c,d,normal,color,0,1,0,1));
    }

    private static List<Quad> transform(List<Quad> quads, Key key) {
        return quads.stream().map(q -> new Quad(rotate(q.a,key,true), rotate(q.b,key,true),
                rotate(q.c,key,true), rotate(q.d,key,true), rotate(q.normal,key,false),
                q.color,q.u0,q.u1,q.v0,q.v1)).toList();
    }

    private static Point rotate(Point p, Key key, boolean position) {
        float s = position ? key.size : 1, offset = position ? 1 : 0;
        return switch (key.facing) {
            case SOUTH -> new Point((offset-p.x)*s,p.y*s,(offset-p.z)*s);
            case EAST -> new Point((offset-p.z)*s,p.y*s,p.x*s);
            case WEST -> new Point(p.z*s,p.y*s,(offset-p.x)*s);
            default -> new Point(p.x*s,p.y*s,p.z*s);
        };
    }

    private record Key(int size, Direction facing) {}
    private record Mesh(List<Quad> wood, List<Quad> planks, List<Quad> metal) {}
    private record Point(float x, float y, float z) {
        Point normalized() { float length = (float)Math.sqrt(x*x+y*y+z*z); return new Point(x/length,y/length,z/length); }
    }
    private record Quad(Point a, Point b, Point c, Point d, Point normal, int color,
                        float u0, float u1, float v0, float v1) {}
}
