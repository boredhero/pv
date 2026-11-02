package io.vinum.machine;

import io.vinum.tileentity.ModTileEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;
import org.jetbrains.annotations.Nullable;

/** Furnace-fuel FE source. Pauses fuel burn when storage is full and preserves fuel containers. */
public final class BasicGeneratorBlockEntity extends BlockEntity implements MenuProvider {
  public static final int CAPACITY = 32000, FE_PER_TICK = 20, TRANSFER_PER_TICK = 80;
  private int burnRemaining, burnDuration;
  public final MachineEnergy energy =
      new MachineEnergy(CAPACITY, 0, TRANSFER_PER_TICK, this::setChanged);
  public final ItemStackHandler inventory =
      new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return slot == 0 && stack.getBurnTime(RecipeType.SMELTING) > 0;
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
        }
      };
  private final IItemHandler input = new SidedItemView(inventory, true, false, 0);
  private final IItemHandler output = new SidedItemView(inventory, false, true, 1);

  public BasicGeneratorBlockEntity(BlockPos pos, BlockState state) {
    super(ModTileEntities.BASIC_GENERATOR.get(), pos, state);
  }

  public IItemHandler items(@Nullable Direction side) {
    return side == Direction.DOWN ? output : input;
  }

  private boolean startFuel() {
    ItemStack fuel = inventory.getStackInSlot(0);
    int duration = fuel.getBurnTime(RecipeType.SMELTING);
    if (duration <= 0) return false;
    ItemStack remainder = fuel.getCraftingRemainingItem();
    ItemStack stored = inventory.getStackInSlot(1);
    if (!remainder.isEmpty()
        && !stored.isEmpty()
        && (!ItemStack.isSameItemSameComponents(stored, remainder)
            || stored.getCount() + remainder.getCount() > stored.getMaxStackSize())) return false;
    inventory.extractItem(0, 1, false);
    if (!remainder.isEmpty()) {
      if (stored.isEmpty()) inventory.setStackInSlot(1, remainder.copyWithCount(1));
      else {
        ItemStack result = stored.copy();
        result.grow(1);
        inventory.setStackInSlot(1, result);
      }
    }
    burnRemaining = burnDuration = duration;
    setChanged();
    return true;
  }

  public static void tick(
      Level level, BlockPos pos, BlockState state, BasicGeneratorBlockEntity generator) {
    if (level.isClientSide) return;
    int transferLeft = TRANSFER_PER_TICK;
    for (Direction direction : Direction.values()) {
      if (transferLeft <= 0 || generator.energy.getEnergyStored() == 0) break;
      BlockPos neighbor = pos.relative(direction);
      if (!level.hasChunk(
          SectionPos.blockToSectionCoord(neighbor.getX()),
          SectionPos.blockToSectionCoord(neighbor.getZ()))) continue;
      var target =
          level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, direction.getOpposite());
      if (target == null || !target.canReceive()) continue;
      int offered = Math.min(transferLeft, generator.energy.getEnergyStored());
      int accepted = target.receiveEnergy(offered, false);
      generator.energy.consume(accepted);
      transferLeft -= accepted;
    }
    boolean working = false;
    if (generator.energy.getEnergyStored() <= CAPACITY - FE_PER_TICK) {
      if (generator.burnRemaining <= 0) generator.startFuel();
      if (generator.burnRemaining > 0) {
        generator.energy.generate(FE_PER_TICK);
        generator.burnRemaining--;
        generator.setChanged();
        working = true;
      }
    }
    if (state.getValue(MachineBlock.LIT) != working)
      level.setBlock(pos, state.setValue(MachineBlock.LIT, working), 3);
  }

  public final ContainerData data =
      new ContainerData() {
        @Override
        public int get(int index) {
          return switch (index) {
            case 0 -> energy.getEnergyStored();
            case 1 -> burnRemaining & 65535;
            case 2 -> burnRemaining >>> 16;
            case 3 -> burnDuration & 65535;
            case 4 -> burnDuration >>> 16;
            default -> 0;
          };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
          return 5;
        }
      };

  public int burnRemaining() {
    return burnRemaining;
  }

  public boolean usable(Player player) {
    return level != null
        && level.getBlockEntity(worldPosition) == this
        && player.distanceToSqr(
                worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5)
            <= 64;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.vinum.basic_generator");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new BasicGeneratorMenu(id, inventory, this);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("Inventory", inventory.serializeNBT(registries));
    tag.putInt("Energy", energy.getEnergyStored());
    tag.putInt("BurnRemaining", burnRemaining);
    tag.putInt("BurnDuration", burnDuration);
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    energy.load(tag.getInt("Energy"));
    burnDuration = Math.max(0, tag.getInt("BurnDuration"));
    burnRemaining = Math.clamp(tag.getInt("BurnRemaining"), 0, burnDuration);
  }

  @Override
  public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
    BlockItem.setBlockEntityData(stack, getType(), saveCustomOnly(registries));
  }
}
