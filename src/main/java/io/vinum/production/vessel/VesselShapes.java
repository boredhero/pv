package io.vinum.production.vessel;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Cached pixel-step cylindrical walls follow the shared rendered vat, with no internal walls. */
final class VesselShapes {
    private static final Map<Integer, VoxelShape> CACHE = new HashMap<>();
    private VesselShapes() {}
    static VoxelShape shape(int size, int x, int y, int z) {
        int key = size * 1000 + x * 100 + y * 10 + z;
        return CACHE.computeIfAbsent(key, ignored -> build(size, x, y, z));
    }
    private static VoxelShape build(int size, int x, int y, int z) {
        double center = size / 2D, radius = size * .46D, inner = radius - .09D;
        double top = Math.clamp(size * .94D - y, 0, 1);
        VoxelShape shape = Shapes.empty();
        for (int row = 0; row < 16; row++) {
            double dz = z + (row + .5D) / 16 - center;
            if (Math.abs(dz) >= radius) continue;
            double outerX = Math.sqrt(radius * radius - dz * dz);
            double innerX = Math.abs(dz) >= inner ? 0 : Math.sqrt(inner * inner - dz * dz);
            if (y == 0) shape = add(shape, center - outerX - x, center + outerX - x, row, 0, .12D);
            if (top <= .12D && y == 0) continue;
            shape = add(shape, center - outerX - x, center - innerX - x, row, y == 0 ? .12D : 0, top);
            shape = add(shape, center + innerX - x, center + outerX - x, row, y == 0 ? .12D : 0, top);
        }
        return shape.optimize();
    }
    private static VoxelShape add(VoxelShape shape, double left, double right, int row, double bottom, double top) {
        left = Math.clamp(left, 0, 1); right = Math.clamp(right, 0, 1);
        return left >= right || bottom >= top ? shape : Shapes.or(shape, Shapes.box(left, bottom, row / 16D, right, top, (row + 1) / 16D));
    }
}
