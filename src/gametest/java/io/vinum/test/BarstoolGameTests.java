package io.vinum.test;

import io.vinum.stool.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class BarstoolGameTests {
    private static BlockPos place(GameTestHelper h) {
        BlockPos pos=h.absolutePos(new BlockPos(3,1,3));
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,StoolContent.BARSTOOL.get().defaultBlockState());
        return pos;
    }
    @GameTest(template="barrel_space")
    public static void stoolSeatsOnePlayerAndCleansUpAfterDismount(GameTestHelper h) {
        BlockPos pos=place(h);
        var first=h.makeMockPlayer(GameType.SURVIVAL);var second=h.makeMockPlayer(GameType.SURVIVAL);
        first.setPos(Vec3.atCenterOf(pos.east()));second.setPos(Vec3.atCenterOf(pos.west()));
        var state=h.getLevel().getBlockState(pos);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        h.assertTrue(state.useWithoutItem(h.getLevel(),first,hit).consumesAction(),"Right click seats a player");
        h.assertTrue(first.getVehicle() instanceof StoolSeat,"Player rides the transient stool seat");
        h.assertTrue(!state.useWithoutItem(h.getLevel(),second,hit).consumesAction()&&!second.isPassenger(),"Occupied stool rejects a second player");
        var seat=(StoolSeat)first.getVehicle();
        h.assertTrue(!seat.getType().canSerialize(),"Seat entity is never persisted to world/player save data");
        first.stopRiding();seat.tick();
        h.assertTrue(seat.isRemoved()&&!first.isPassenger(),"Dismount removes its unused seat");
        h.assertTrue(first.position().distanceTo(Vec3.atCenterOf(pos))<2,"Dismount remains next to the stool");
        h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void breakingStoolDismountsAndDropsExactlyOneItem(GameTestHelper h) {
        BlockPos pos=place(h);var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(pos.east()));
        var state=h.getLevel().getBlockState(pos);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        state.useWithoutItem(h.getLevel(),player,hit);
        var seat=player.getVehicle();h.assertTrue(seat instanceof StoolSeat,"Stool starts occupied");
        h.getLevel().destroyBlock(pos,true);
        h.assertTrue(!player.isPassenger()&&seat.isRemoved(),"Breaking furniture ejects rider and removes seat immediately");
        int drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).stream()
            .filter(e->e.getItem().is(StoolContent.ITEM.get())).mapToInt(e->e.getItem().getCount()).sum();
        h.assertTrue(drops==1,"Breaking drops one stool and no seat entity item");h.succeed();
    }
    @GameTest(template="barrel_space")
    public static void stoolDoesNotSeatThroughSolidCeiling(GameTestHelper h) {
        BlockPos pos=place(h);h.getLevel().setBlockAndUpdate(pos.above(),Blocks.STONE.defaultBlockState());
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(Vec3.atCenterOf(pos.east()));
        var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false);
        h.assertTrue(!h.getLevel().getBlockState(pos).useWithoutItem(h.getLevel(),player,hit).consumesAction()
            && !player.isPassenger(),"Stool refuses mounting when rider head space is blocked");h.succeed();
    }
}
