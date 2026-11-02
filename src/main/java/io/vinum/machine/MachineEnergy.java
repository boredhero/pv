package io.vinum.machine;

import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * Bounded FE storage with separate external transfer limits and internal production/consumption.
 */
public final class MachineEnergy extends EnergyStorage {
  private final Runnable changed;

  public MachineEnergy(int capacity, int receive, int extract, Runnable changed) {
    super(capacity, receive, extract);
    this.changed = changed;
  }

  @Override
  public int receiveEnergy(int amount, boolean simulate) {
    int accepted = super.receiveEnergy(Math.max(0, amount), simulate);
    if (accepted > 0 && !simulate) changed.run();
    return accepted;
  }

  @Override
  public int extractEnergy(int amount, boolean simulate) {
    int extracted = super.extractEnergy(Math.max(0, amount), simulate);
    if (extracted > 0 && !simulate) changed.run();
    return extracted;
  }

  public int generate(int amount) {
    int accepted = Math.clamp(amount, 0, capacity - energy);
    if (accepted > 0) {
      energy += accepted;
      changed.run();
    }
    return accepted;
  }

  public int consume(int amount) {
    int used = Math.clamp(amount, 0, energy);
    if (used > 0) {
      energy -= used;
      changed.run();
    }
    return used;
  }

  public void load(int value) {
    energy = Math.clamp(value, 0, capacity);
  }
}
