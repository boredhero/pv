package io.vinum.barrel;

import io.vinum.block.WineBarrelBlock;
import io.vinum.common.Defines;
import io.vinum.multiblock.BarrelMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** Adds empty storage entities to decorative barrels saved before barrels had storage. */
@EventBusSubscriber(modid = Defines.MODID)
public final class BarrelLegacyMigration {
    private BarrelLegacyMigration() {}

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel) {
            repair(chunk);
        }
    }

    /**
     * Repairs only the supplied chunk before its initial client packet is constructed.
     * Chunk load events can precede FULL status, so this must not request chunks through
     * the level or perform multiblock reconciliation synchronously.
     *
     * @return the number of newly created barrel entities
     */
    public static int repair(LevelChunk chunk) {
        if (!(chunk.getLevel() instanceof ServerLevel level)) return 0;
        int created = 0;
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof WineBarrelBlock)) continue;
            int minY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!(section.getBlockState(x, y, z).getBlock() instanceof WineBarrelBlock)) continue;
                        cursor.set(minX + x, minY + y, minZ + z);
                        if (chunk.getBlockEntity(cursor, LevelChunk.EntityCreationType.CHECK) != null) continue;
                        BlockPos pos = cursor.immutable();
                        if (chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) != null) {
                            created++;
                            BarrelMultiblock.enqueue(level, pos);
                        }
                    }
                }
            }
        }
        if (created > 0) chunk.setUnsaved(true);
        return created;
    }
}
