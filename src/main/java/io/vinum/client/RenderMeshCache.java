package io.vinum.client;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Bounded LRU of immutable CPU meshes. Render-thread confined, including statistics and clearing.
 * Keys must be immutable appearance/content values, never mutable ItemStacks or block entities.
 */
@EventBusSubscriber(modid = "vinum", value = Dist.CLIENT)
public final class RenderMeshCache<K> {
    private static final Set<RenderMeshCache<?>> CACHES = Collections.newSetFromMap(new WeakHashMap<>());
    private static long generation;
    private final int maximumEntries;
    private final LinkedHashMap<K, CachedRenderMesh> meshes = new LinkedHashMap<>(16, .75f, true);
    private long hits;
    private long builds;
    private long cachedVertices;

    public RenderMeshCache(int maximumEntries) {
        if (maximumEntries < 1) throw new IllegalArgumentException("Mesh cache must have positive capacity");
        this.maximumEntries = maximumEntries;
        CACHES.add(this);
    }

    public CachedRenderMesh get(K key, Supplier<CachedRenderMesh> factory) {
        Objects.requireNonNull(key, "mesh key");
        var mesh = meshes.get(key);
        if (mesh != null) {
            hits++;
            return mesh;
        }
        mesh = Objects.requireNonNull(factory.get(), "recorded mesh");
        builds++;
        if (meshes.size() == maximumEntries) {
            var iterator = meshes.entrySet().iterator();
            cachedVertices -= iterator.next().getValue().vertexCount();
            iterator.remove();
        }
        meshes.put(key, mesh);
        cachedVertices += mesh.vertexCount();
        return mesh;
    }

    public int size() { return meshes.size(); }
    public long hitCount() { return hits; }
    public long buildCount() { return builds; }
    public long cachedVertexCount() { return cachedVertices; }

    /** Drop geometry while retaining lifetime diagnostic counters. */
    public void clear() {
        meshes.clear();
        cachedVertices = 0;
    }

    public static long generation() { return generation; }

    /** Invalidates both mesh entries and any caller snapshots keyed by the global generation. */
    public static void clearAll() {
        generation++;
        for (var cache : CACHES) cache.clear();
    }

    @SubscribeEvent
    public static void modelsBaked(ModelEvent.BakingCompleted event) {
        // BakingCompleted is delivered on the game thread after resource reload has completed.
        // Weak registration lets short-lived test/preview caches be collected normally.
        clearAll();
    }
}
