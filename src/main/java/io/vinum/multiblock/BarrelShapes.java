package io.vinum.multiblock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Shared barrel profile and cached local collision clips for the body and its two cradles. */
public final class BarrelShapes {
    // Normalized geometry is also consumed by the renderer, keeping both representations aligned.
    public static final double BODY_CENTER_X = 0.5;
    public static final double BODY_CENTER_Y = 0.55;
    public static final double MAX_RADIUS = 0.45;
    public static final double SADDLE_MIN_X = 0.11;
    public static final double SADDLE_MAX_X = 0.89;
    public static final double FRONT_SADDLE_START = 0.17;
    public static final double REAR_SADDLE_START = 0.73;
    public static final double SADDLE_DEPTH = 0.10;
    public static final double SADDLE_OVERLAP = 0.008;

    private record Key(int size, int x, int y, int z, boolean alongX) {}
    private static final Map<Key, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private BarrelShapes() {}

    public static double radius(double axial) {
        return MAX_RADIUS * (0.86 + 0.14 * Math.sin(Math.PI * axial));
    }

    public static double saddleTop(double lateral, double axial) {
        double radius = radius(axial);
        double offset = lateral - BODY_CENTER_X;
        return BODY_CENTER_Y - Math.sqrt(Math.max(0, radius * radius - offset * offset)) + SADDLE_OVERLAP;
    }

    public static VoxelShape shape(BlockPos pos, BlockPos origin, int size, Direction facing) {
        if (size < 1 || size > BarrelMultiblock.MAX_SIZE || !BarrelMultiblock.contains(origin, size, pos)) {
            origin = pos;
            size = 1;
        }
        Key key = new Key(size, pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ(),
            facing.getAxis() == Direction.Axis.X);
        return CACHE.computeIfAbsent(key, BarrelShapes::create);
    }

    private static VoxelShape create(Key key) {
        double centerX = key.size * BODY_CENTER_X;
        double centerY = key.size * BODY_CENTER_Y;
        double lateralOrigin = key.alongX ? key.z : key.x;
        double axialOrigin = key.alongX ? key.x : key.z;
        VoxelShape shape = Shapes.empty();
        for (int axialSlice = 0; axialSlice < 4; axialSlice++) {
            double a0 = axialSlice / 4.0;
            double a1 = (axialSlice + 1) / 4.0;
            double globalAxis = axialOrigin + (a0 + a1) / 2;
            double radius = key.size * radius(globalAxis / key.size);
            for (int row = 0; row < 8; row++) {
                double y0 = row / 8.0;
                double y1 = (row + 1) / 8.0;
                double dy = key.y + (y0 + y1) / 2 - centerY;
                if (Math.abs(dy) >= radius) continue;
                double width = Math.sqrt(radius * radius - dy * dy);
                double l0 = Math.max(0, centerX - width - lateralOrigin);
                double l1 = Math.min(1, centerX + width - lateralOrigin);
                if (l1 <= l0) continue;
                shape = Shapes.or(shape, box(key, l0, y0, a0, l1, y1, a1));
            }
        }
        shape = addSaddle(shape, key, lateralOrigin, axialOrigin, FRONT_SADDLE_START);
        shape = addSaddle(shape, key, lateralOrigin, axialOrigin, REAR_SADDLE_START);
        return shape.optimize();
    }

    private static VoxelShape addSaddle(VoxelShape shape, Key key, double lateralOrigin, double axialOrigin, double start) {
        double axialMin = Math.max(0, start * key.size - axialOrigin);
        double axialMax = Math.min(1, (start + SADDLE_DEPTH) * key.size - axialOrigin);
        double lateralMin = Math.max(0, SADDLE_MIN_X * key.size - lateralOrigin);
        double lateralMax = Math.min(1, SADDLE_MAX_X * key.size - lateralOrigin);
        if (axialMax <= axialMin || lateralMax <= lateralMin) return shape;
        // Narrow lateral strips follow the half-moon cutout. Each local box stays
        // within its own block; only the shape of a physical supporting slab is filled.
        for (int axialSlice = 0; axialSlice < 4; axialSlice++) {
            double a0 = Math.max(axialMin, axialSlice / 4.0);
            double a1 = Math.min(axialMax, (axialSlice + 1) / 4.0);
            if (a1 <= a0) continue;
            for (int strip = 0; strip < 16; strip++) {
                double l0 = Math.max(lateralMin, strip / 16.0);
                double l1 = Math.min(lateralMax, (strip + 1) / 16.0);
                if (l1 <= l0) continue;
                double x0 = (lateralOrigin + l0) / key.size;
                double x1 = (lateralOrigin + l1) / key.size;
                double z0 = (axialOrigin + a0) / key.size;
                double z1 = (axialOrigin + a1) / key.size;
                double top = Math.max(Math.max(saddleTop(x0, z0), saddleTop(x0, z1)),
                    Math.max(saddleTop(x1, z0), saddleTop(x1, z1)));
                double y1 = Math.min(1, top * key.size - key.y);
                if (y1 <= 0) continue;
                shape = Shapes.or(shape, box(key, l0, 0, a0, l1, y1, a1));
            }
        }
        return shape;
    }

    private static VoxelShape box(Key key, double lateralMin, double yMin, double axialMin,
                                   double lateralMax, double yMax, double axialMax) {
        return key.alongX ? Shapes.box(axialMin, yMin, lateralMin, axialMax, yMax, lateralMax)
            : Shapes.box(lateralMin, yMin, axialMin, lateralMax, yMax, axialMax);
    }
}
