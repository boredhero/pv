package io.vinum.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class VoxelShapeHelper {
    private VoxelShapeHelper() {}

    // Some legacy models specify corners backwards; normalize them before constructing a box.
    public static VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Block.box(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
            Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public static VoxelShape addShapes(VoxelShape... shapes) {
        VoxelShape result = Shapes.empty();
        for (VoxelShape shape : shapes) result = Shapes.or(result, shape);
        return result.optimize();
    }

    public static VoxelShape[] horizontalShapes(VoxelShape north) {
        VoxelShape[] result = new VoxelShape[4];
        VoxelShape current = north.optimize();
        Direction direction = Direction.NORTH;
        for (int i = 0; i < 4; i++) {
            result[direction.get2DDataValue()] = current;
            VoxelShape[] rotated = {Shapes.empty()};
            current.forAllBoxes((x1, y1, z1, x2, y2, z2) -> rotated[0] = Shapes.or(rotated[0],
                Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
            current = rotated[0].optimize();
            direction = direction.getClockWise();
        }
        return result;
    }
}
