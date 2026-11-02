package io.vinum.machine;

import io.vinum.production.BatchProfile;
import io.vinum.production.ProductionDataComponents;
import io.vinum.production.ProductionRecipes;
import io.vinum.tileentity.ModTileEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;
import org.jetbrains.annotations.Nullable;

public final class WinePressBlockEntity extends BlockEntity implements MenuProvider {
  public static final int TANK_CAPACITY = 8000,
      ENERGY_CAPACITY = 8000,
      FE_PER_TICK = 10,
      CRANK_WORK = 250,
      CRANK_COOLDOWN = 10;
  private PressingRecipe plan;
  private int progress;
  private BlockPos clarificationTarget;
  private FluidStack clarificationBatch = FluidStack.EMPTY;
  private boolean crank;
  private boolean motor;
  private long lastCrank = -CRANK_COOLDOWN;
  private int crankTurns;
  public final MachineEnergy energy = new MachineEnergy(ENERGY_CAPACITY, 80, 0, this::setChanged);
  public final ItemStackHandler inventory =
      new ItemStackHandler(3) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return switch (slot) {
            case 0 -> acceptsInput(stack);
            case 1 -> FluidUtil.getFluidHandler(stack).isPresent();
            default -> false;
          };
        }

        @Override
        public int getSlotLimit(int slot) {
          return slot == 0 ? 64 : 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
        }
      };
  private final FluidTank tank =
      new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
          sync();
        }
      };
  private final IFluidHandler output =
      new IFluidHandler() {
        @Override
        public int getTanks() {
          return 1;
        }

        @Override
        public FluidStack getFluidInTank(int index) {
          return index == 0 ? tank.getFluid().copy() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int index) {
          return index == 0 ? TANK_CAPACITY : 0;
        }

        @Override
        public boolean isFluidValid(int index, FluidStack fluid) {
          return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
          return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
          return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int amount, FluidAction action) {
          return tank.drain(amount, action);
        }
      };
  private final IItemHandler top = new SidedItemView(inventory, true, false, 0);
  private final IItemHandler bottom = new SidedItemView(inventory, false, true, 2);
  // Side automation loads empty containers and extracts the completed container.
  private final IItemHandler sides =
      new IItemHandler() {
        @Override
        public int getSlots() {
          return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
          return inventory.getStackInSlot(slot + 1);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
          return slot == 0 ? inventory.insertItem(1, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
          return slot == 1 ? inventory.extractItem(2, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
          return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
          return slot == 0 && inventory.isItemValid(1, stack);
        }
      };

  public WinePressBlockEntity(BlockPos pos, BlockState state) {
    super(ModTileEntities.WINE_PRESS.get(), pos, state);
  }

  public IItemHandler items(@Nullable Direction side) {
    return side == Direction.UP ? top : side == Direction.DOWN ? bottom : sides;
  }

  public IFluidHandler fluids() {
    return output;
  }

  public boolean acceptsInput(ItemStack stack) {
    return level != null
        && level.getRecipeManager().getAllRecipesFor(ProductionRecipes.PRESSING_TYPE.get()).stream()
            .anyMatch(r -> r.value().accepts(stack));
  }

  public boolean hasMotor() {
    return motor;
  }

  public boolean attachMotor() {
    if (motor) return false;
    motor = true;
    sync();
    if (level != null) {
      level.invalidateCapabilities(worldPosition);
      level.invalidateCapabilities(worldPosition.above());
    }
    return true;
  }

  public boolean hasCrank() {
    return crank;
  }

  public int crankTurns() {
    return crankTurns;
  }

  public long lastCrank() {
    return lastCrank;
  }

  public float progressFraction() {
    return requiredWork() == 0 ? 0 : progress / (float) requiredWork();
  }

  public boolean attachCrank() {
    if (crank) return false;
    crank = true;
    sync();
    return true;
  }

  public int requiredWork() {
    return clarificationTarget != null ? 2000 : plan == null ? 0 : plan.energy();
  }

  public boolean turnCrank() {
    if (level == null
        || level.isClientSide
        || !crank
        || level.getGameTime() - lastCrank < CRANK_COOLDOWN
        || !prepareWork()) return false;
    // A turn is mechanical work, never stored energy. No later automatic processing in manual mode.
    advanceWork(Math.min(CRANK_WORK, requiredWork() - progress));
    lastCrank = level.getGameTime();
    crankTurns++;
    sync();
    level.playSound(
        null,
        worldPosition,
        net.minecraft.sounds.SoundEvents.WOODEN_TRAPDOOR_CLOSE,
        net.minecraft.sounds.SoundSource.BLOCKS,
        .35F,
        1.25F);
    return true;
  }

  /** Select a finished neighboring vat; manual turns or the motor then perform the separation. */
  public boolean clarifyAdjacentVat() {
    if (level == null
        || level.isClientSide
        || (!crank && !motor)
        || progress > 0
        || clarificationTarget != null) return false;
    for (Direction direction : Direction.Plane.HORIZONTAL) {
      BlockPos target = worldPosition.relative(direction);
      if (!level.hasChunk(
          SectionPos.blockToSectionCoord(target.getX()),
          SectionPos.blockToSectionCoord(target.getZ()))) continue;
      if (level.getBlockEntity(target)
              instanceof io.vinum.production.vessel.FermentationVesselBlockEntity vat
          && vat.isVat()
          && !vat.isFermenting()) {
        FluidStack batch = vat.getFluid();
        BatchProfile profile = BatchProfile.of(batch);
        if (batch.isEmpty()
            || profile.solidsGramsPerLitre() <= 0
            || !profile.history().contains(io.vinum.production.ProductionInputs.id("fermentation")))
          continue;
        clarificationTarget = target;
        clarificationBatch = batch.copy();
        plan = null;
        progress = 0;
        sync();
        return true;
      }
    }
    return false;
  }

  private io.vinum.production.vessel.FermentationVesselBlockEntity clarificationVat() {
    if (clarificationTarget == null
        || worldPosition.distManhattan(clarificationTarget) != 1
        || level == null) return null;
    if (!level.hasChunk(
        SectionPos.blockToSectionCoord(clarificationTarget.getX()),
        SectionPos.blockToSectionCoord(clarificationTarget.getZ()))) return null;
    if (level.getBlockEntity(clarificationTarget)
            instanceof io.vinum.production.vessel.FermentationVesselBlockEntity vat
        && vat.isVat()
        && !vat.isFermenting()) {
      FluidStack current = vat.getFluid();
      if (current.getAmount() == clarificationBatch.getAmount()
          && FluidStack.isSameFluidSameComponents(current, clarificationBatch)) return vat;
    }
    return null;
  }

  private boolean prepareWork() {
    if (clarificationTarget != null) {
      if (clarificationVat() != null) return true;
      clarificationTarget = null;
      clarificationBatch = FluidStack.EMPTY;
      progress = 0;
      sync();
      return false;
    }
    ItemStack input = inventory.getStackInSlot(0);
    if (plan != null && !plan.canProcess(input)) {
      plan = null;
      progress = 0;
      sync();
    }
    if (plan == null)
      plan =
          level.getRecipeManager().getAllRecipesFor(ProductionRecipes.PRESSING_TYPE.get()).stream()
              .map(r -> r.value())
              .filter(r -> r.canProcess(input))
              .findFirst()
              .orElse(null);
    if (plan == null) return false;
    FluidStack output = result(plan);
    return tank.fill(output, IFluidHandler.FluidAction.SIMULATE) == output.getAmount();
  }

  private void advanceWork(int work) {
    if (work <= 0) return;
    progress += work;
    if (progress >= requiredWork()) {
      if (clarificationTarget != null) {
        var vat = clarificationVat();
        if (vat != null) vat.press();
        clarificationTarget = null;
        clarificationBatch = FluidStack.EMPTY;
      } else {
        tank.fill(result(plan), IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(0, plan.count(), false);
        plan = null;
      }
      progress = 0;
      sync();
    }
    setChanged();
  }

  private FluidStack result(PressingRecipe recipe) {
    FluidStack result = recipe.output();
    if (!result.has(ProductionDataComponents.BATCH_PROFILE.get()))
      BatchProfile.of(result).applyTo(result);
    return result;
  }

  public static void tick(Level level, BlockPos pos, BlockState state, WinePressBlockEntity press) {
    if (level.isClientSide) return;
    press.fillContainer();
    if (!press.prepareWork() || !press.motor) return;
    int work = press.energy.consume(Math.min(FE_PER_TICK, press.requiredWork() - press.progress));
    press.advanceWork(work);
    if (work > 0 && level.getGameTime() % 5 == 0) press.sync();
  }

  private void fillContainer() {
    if (tank.isEmpty()
        || inventory.getStackInSlot(1).isEmpty()
        || !inventory.getStackInSlot(2).isEmpty()) return;
    ItemStack candidate = inventory.getStackInSlot(1).copyWithCount(1);
    var handler = FluidUtil.getFluidHandler(candidate).orElse(null);
    if (handler == null) return;
    int amount = handler.fill(tank.getFluid().copy(), IFluidHandler.FluidAction.SIMULATE);
    if (amount <= 0) return;
    int filled =
        handler.fill(tank.getFluid().copyWithAmount(amount), IFluidHandler.FluidAction.EXECUTE);
    if (filled <= 0) return;
    tank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
    inventory.extractItem(1, 1, false);
    inventory.setStackInSlot(2, handler.getContainer());
  }

  public final ContainerData data =
      new ContainerData() {
        @Override
        public int get(int index) {
          return switch (index) {
            case 0 -> energy.getEnergyStored();
            case 1 -> progress;
            case 2 -> requiredWork();
            case 3 -> tank.getFluidAmount();
            case 4 -> (crank ? 1 : 0) | (motor ? 2 : 0) | (clarificationTarget != null ? 4 : 0);
            case 5 -> io.vinum.beverage.WineCatalog.color(tank.getFluid()) & 65535;
            case 6 -> io.vinum.beverage.WineCatalog.color(tank.getFluid()) >>> 16;
            default -> 0;
          };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
          return 7;
        }
      };

  public boolean usable(Player player) {
    return level != null
        && level.getBlockEntity(worldPosition) == this
        && player.distanceToSqr(
                worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5)
            <= 64;
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.vinum.wine_press");
  }

  @Override
  public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
    return new WinePressMenu(id, inventory, this);
  }

  private void sync() {
    setChanged();
    if (level != null && !level.isClientSide)
      level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
  }

  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.saveAdditional(tag, registries);
    tag.put("Inventory", inventory.serializeNBT(registries));
    tag.put("Juice", tank.writeToNBT(registries, new CompoundTag()));
    tag.putInt("Energy", energy.getEnergyStored());
    tag.putInt("Progress", progress);
    if (clarificationTarget != null) {
      tag.putLong("ClarificationTarget", clarificationTarget.asLong());
      tag.put("ClarificationBatch", clarificationBatch.save(registries));
    }
    tag.putBoolean("Crank", crank);
    tag.putBoolean("Motor", motor);
    tag.putInt("CrankTurns", crankTurns);
    tag.putLong("LastCrank", lastCrank);
    if (plan != null)
      PressingRecipe.CODEC
          .codec()
          .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), plan)
          .result()
          .ifPresent(p -> tag.put("Plan", p));
  }

  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
    super.loadAdditional(tag, registries);
    inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    tank.readFromNBT(registries, tag.getCompound("Juice"));
    energy.load(tag.getInt("Energy"));
    plan =
        PressingRecipe.CODEC
            .codec()
            .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.getCompound("Plan"))
            .result()
            .orElse(null);
    clarificationTarget =
        tag.contains("ClarificationTarget")
            ? BlockPos.of(tag.getLong("ClarificationTarget"))
            : null;
    clarificationBatch =
        FluidStack.parseOptional(registries, tag.getCompound("ClarificationBatch"));
    progress = Math.clamp(tag.getInt("Progress"), 0, requiredWork());
    crank = tag.getBoolean("Crank");
    motor = tag.getBoolean("Motor");
    crankTurns = tag.getInt("CrankTurns");
    lastCrank = tag.contains("LastCrank") ? tag.getLong("LastCrank") : -CRANK_COOLDOWN;
  }

  @Override
  public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
    return saveCustomOnly(registries);
  }

  @Override
  public ClientboundBlockEntityDataPacket getUpdatePacket() {
    return ClientboundBlockEntityDataPacket.create(this);
  }

  @Override
  public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
    BlockItem.setBlockEntityData(stack, getType(), saveCustomOnly(registries));
  }
}
