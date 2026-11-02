package io.vinum.test;

import io.vinum.barrel.*;
import io.vinum.beverage.WineCatalog;
import io.vinum.fluid.ModFluids;
import io.vinum.item.*;
import io.vinum.shelf.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("vinum_tests")
@PrefixGameTestTemplate(false)
public final class WineShelfGameTests {
  private static WineShelfBlockEntity place(GameTestHelper h, BlockPos pos) {
    h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
    var player=h.makeMockPlayer(GameType.SURVIVAL);player.setYRot(0);
    var item=new ItemStack(ShelfContent.ITEM.get());player.setItemInHand(InteractionHand.MAIN_HAND,item);
    var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
    h.assertTrue(item.useOn(new UseOnContext(h.getLevel(),player,InteractionHand.MAIN_HAND,item,hit)).consumesAction(),"Shelf places through ordinary item interaction");
    return (WineShelfBlockEntity)h.getLevel().getBlockEntity(pos);
  }
  private static ItemStack wine() {
    return ((FluidContainerItem)ModItems.WINE_BOTTLE_EMPTY.get()).filled(BarrelFluidData.withAge(WineCatalog.sample(WineCatalog.varieties().getFirst(),625),123));
  }
  @GameTest(template="barrel_space")
  public static void fiveShelfSlotsPreserveBottlesAndDropOnce(GameTestHelper h) {
    BlockPos pos=h.absolutePos(new BlockPos(3,1,3));var shelf=place(h,pos);
    var bottle=wine();bottle.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Cellar batch"));
    for(int i=0;i<5;i++){h.assertTrue(shelf.bottle(i).isEmpty(),"New shelves start completely empty");h.assertTrue(shelf.insert(i,bottle),"Each position accepts one bottle");}
    h.assertTrue(!shelf.insert(0,bottle)&&!shelf.insert(5,bottle)&&!shelf.insert(-1,bottle),"A shelf never exceeds five bottles");
    var saved=shelf.saveWithoutMetadata(h.getLevel().registryAccess());
    var restored=new WineShelfBlockEntity(pos,shelf.getBlockState());restored.loadWithComponents(saved,h.getLevel().registryAccess());
    for(int i=0;i<5;i++)h.assertTrue(ItemStack.isSameItemSameComponents(restored.bottle(i),bottle),"All bottle metadata and partial volume survive persistence");
    h.getLevel().destroyBlock(pos,true);
    var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2));
    h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.WINE_BOTTLE_EMPTY.get())).mapToInt(e->e.getItem().getCount()).sum()==5,"Breaking shelf returns exactly its five bottles");
    h.assertTrue(drops.stream().filter(e->e.getItem().is(ShelfContent.ITEM.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Breaking shelf returns one empty shelf");h.succeed();
  }
  @GameTest(template="barrel_space")
  public static void connectedShelfWallSharesTrimAndClickTargets(GameTestHelper h) {
    BlockPos pos=h.absolutePos(new BlockPos(3,1,3));var left=place(h,pos);var right=place(h,pos.east());
    h.assertTrue(h.getLevel().getBlockState(pos).getValue(WineShelfBlock.RIGHT)&&h.getLevel().getBlockState(pos.east()).getValue(WineShelfBlock.LEFT),"Matching shelf edges connect in both directions");
    var player=h.makeMockPlayer(GameType.SURVIVAL);
    for(int slot=0;slot<5;slot++) {
      left.insert(slot,wine());double[] center=WineShelfBlock.CENTERS[slot];
      var hit=new BlockHitResult(new Vec3(pos.getX()+center[0],pos.getY()+center[1],pos.getZ()),Direction.NORTH,pos,false);
      h.assertTrue(WineShelfBlock.slot(left.getBlockState(),pos,hit)==slot,"Every visible bottle position has its own click target");
      left.getBlockState().useWithoutItem(h.getLevel(),player,hit);
      h.assertTrue(left.bottle(slot).isEmpty(),"Click removes only the selected bottle");
    }
    h.assertTrue(player.getInventory().countItem(ModItems.WINE_BOTTLE_EMPTY.get())==5,"Extracted bottles are returned to the player");
    h.getLevel().destroyBlock(pos.east(),false);
    h.assertTrue(!h.getLevel().getBlockState(pos).getValue(WineShelfBlock.RIGHT),"Outside trim returns when a neighbor is removed");
    h.succeed();
  }
  @GameTest(template="barrel_space")
  public static void shelfAgesWineButNeverCreatesBarrelAgedSpirits(GameTestHelper h) {
    BlockPos pos=h.absolutePos(new BlockPos(3,1,3));var shelf=place(h,pos);
    var wine=wine();var spirit=((FluidContainerItem)ModItems.FIFTH_BOTTLE_EMPTY.get()).filled(ModItems.spirit(ModFluids.TEQUILA.source().get(),750));
    shelf.insert(0,wine);shelf.insert(1,spirit);
    h.assertTrue(!shelf.insert(2,new ItemStack(ModItems.SHOT_GLASS.get()))&&!shelf.insert(2,new ItemStack(ModItems.JUICE_CARBOY.get())),"Only bottles fit the shelf");
    for(int tick=0;tick<20;tick++)WineShelfBlockEntity.tick(h.getLevel(),pos,shelf.getBlockState(),shelf);
    var aged=((FluidContainerItem)wine.getItem()).contents(shelf.bottle(0));
    h.assertTrue(BarrelFluidData.getAge(aged)==143&&aged.getAmount()==625,"Wine gains exactly loaded time without changing volume");
    h.assertTrue(ItemStack.isSameItemSameComponents(shelf.bottle(1),spirit),"Glass storage cannot turn blanco tequila into barrel-aged tequila");
    h.succeed();
  }
}
