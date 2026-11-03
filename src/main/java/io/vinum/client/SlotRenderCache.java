package io.vinum.client;

import java.util.WeakHashMap;
import java.util.function.IntFunction;

/** Render-thread snapshots: no per-frame ItemStack copies, fluid decoding, or appearance lookup.
 * Weak owner keys allow unloaded block entities to be collected; values never retain their owner.
 * Resource reload generations invalidate snapshots as well as the shared bounded mesh caches.
 */
public final class SlotRenderCache {
    private record Entry(long revision,long generation,CachedRenderMesh[] meshes) {}
    private static final WeakHashMap<Object,Entry> ENTRIES=new WeakHashMap<>();
    private SlotRenderCache() {}
    public static CachedRenderMesh[] get(Object owner,long revision,int slots,IntFunction<CachedRenderMesh> build) {
        long generation=RenderMeshCache.generation();
        var entry=ENTRIES.get(owner);
        if(entry==null||entry.revision()!=revision||entry.generation()!=generation) {
            var meshes=new CachedRenderMesh[slots];
            for(int slot=0;slot<slots;slot++)meshes[slot]=build.apply(slot);
            entry=new Entry(revision,generation,meshes);
            ENTRIES.put(owner,entry);
        }
        return entry.meshes();
    }
}
