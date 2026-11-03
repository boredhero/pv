package io.vinum.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Vector3f;

/**
 * Immutable local-space vertices. Only geometry/materials are retained: never a live vertex consumer,
 * GPU buffer, world, item stack, light value or overlay. Record and replay on the render thread.
 */
public final class CachedRenderMesh {
    // XYZ, UV, normal XYZ; color stays an int to preserve all eight bits per channel.
    private static final int STRIDE = 8;
    private record MaterialRun(RenderType type, float[] vertices, int[] colors) {}
    private final List<MaterialRun> runs;
    private final int vertexCount;

    private CachedRenderMesh(List<MaterialRun> runs) {
        this.runs = List.copyOf(runs);
        this.vertexCount = runs.stream().mapToInt(run -> run.colors.length).sum();
    }

    /** Executes an existing renderer against identity poses and a CPU-only recording source. */
    public static CachedRenderMesh record(BiConsumer<PoseStack, MultiBufferSource> renderer) {
        var recording = new Recording();
        renderer.accept(new PoseStack(), recording);
        var runs = new ArrayList<MaterialRun>(recording.runs.size());
        for (var run : recording.runs) {
            runs.add(new MaterialRun(run.type, Arrays.copyOf(run.vertices, run.size * STRIDE),
                Arrays.copyOf(run.colors, run.size)));
        }
        return new CachedRenderMesh(runs);
    }

    public int vertexCount() { return vertexCount; }
    public int materialRuns() { return runs.size(); }

    /** Preserves material order, reacquiring the consumer after every material switch. */
    public void render(PoseStack.Pose pose, MultiBufferSource buffers, int light, int overlay) {
        var transformed = new Vector3f();
        for (var run : runs) {
            var out = buffers.getBuffer(run.type);
            float[] values = run.vertices;
            for (int vertex = 0, offset = 0; vertex < run.colors.length; vertex++, offset += STRIDE) {
                pose.pose().transformPosition(values[offset], values[offset+1], values[offset+2], transformed);
                out.addVertex(transformed.x, transformed.y, transformed.z).setColor(run.colors[vertex])
                    .setUv(values[offset+3], values[offset+4]).setOverlay(overlay).setLight(light);
                pose.transformNormal(values[offset+5], values[offset+6], values[offset+7], transformed);
                out.setNormal(transformed.x, transformed.y, transformed.z);
            }
        }
    }

    private static final class Recording implements MultiBufferSource {
        final List<RunBuilder> runs = new ArrayList<>();
        RunBuilder current;
        @Override public VertexConsumer getBuffer(RenderType type) { return new Recorder(this, type); }
        RunBuilder begin(RenderType type) {
            // Group actual vertex emission order, not getBuffer order. A renderer may obtain a
            // consumer early and use it later; recording must not reorder translucent geometry.
            if (current == null || current.type != type) {
                current = new RunBuilder(type);
                runs.add(current);
            }
            return current;
        }
    }

    private static final class RunBuilder {
        final RenderType type;
        float[] vertices = new float[64 * STRIDE];
        int[] colors = new int[64];
        int size;
        RunBuilder(RenderType type) { this.type = type; }
        int add(float x, float y, float z) {
            if (size == colors.length) {
                colors = Arrays.copyOf(colors, size * 2);
                vertices = Arrays.copyOf(vertices, size * 2 * STRIDE);
            }
            int vertex = size++;
            int offset = vertex * STRIDE;
            vertices[offset] = x;
            vertices[offset+1] = y;
            vertices[offset+2] = z;
            colors[vertex] = -1;
            return vertex;
        }
    }

    private static final class Recorder implements VertexConsumer {
        private final Recording recording;
        private final RenderType type;
        private RunBuilder run;
        private int vertex;
        Recorder(Recording recording, RenderType type) { this.recording = recording; this.type = type; }
        @Override public VertexConsumer addVertex(float x, float y, float z) {
            run = recording.begin(type);
            vertex = run.add(x,y,z);
            return this;
        }
        private void requireVertex() {
            if (run == null) throw new IllegalStateException("Vertex attributes before addVertex");
        }
        @Override public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            requireVertex();
            run.colors[vertex] = (alpha & 255)<<24 | (red & 255)<<16 | (green & 255)<<8 | (blue & 255);
            return this;
        }
        @Override public VertexConsumer setUv(float u, float v) {
            requireVertex();
            run.vertices[vertex*STRIDE+3] = u;
            run.vertices[vertex*STRIDE+4] = v;
            return this;
        }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) {
            requireVertex();
            run.vertices[vertex*STRIDE+5] = x;
            run.vertices[vertex*STRIDE+6] = y;
            run.vertices[vertex*STRIDE+7] = z;
            return this;
        }
    }
}
