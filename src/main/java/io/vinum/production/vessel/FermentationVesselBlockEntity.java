package io.vinum.production.vessel;

import io.vinum.production.BatchProfile;
import io.vinum.production.FermentationEngine;
import io.vinum.production.FermentationRecipe;
import io.vinum.production.ProductionRecipes;
import io.vinum.tileentity.ModTileEntities;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Each physical vessel owns its fluid and frozen fermentation plan, including after a split. */
public final class FermentationVesselBlockEntity extends BlockEntity {
    public static final int CARBOY_CAPACITY = 4000;
    public static final int VAT_CAPACITY = 16000;
    private FluidStack fluid = FluidStack.EMPTY;
    private BlockPos origin;
    private int size = 1;
    private int height = 1;
    private int progress;
    private boolean starter;
    private FermentationRecipe recipe;
    private final IFluidHandler handler = new Handler();

    public FermentationVesselBlockEntity(BlockPos pos, BlockState state) {
        super(state.getBlock() instanceof FermentationVatBlock ? ModTileEntities.FERMENTATION_VAT.get()
                : ModTileEntities.CARBOY.get(), pos, state);
        origin = pos.immutable();
    }

    public boolean isVat() { return getBlockState().getBlock() instanceof FermentationVatBlock; }
    public int localCapacity() { return isVat() ? VAT_CAPACITY : CARBOY_CAPACITY; }
    public int getCapacity() { return localCapacity() * size * size * height; }
    public BlockPos getStructureOrigin() { return origin; }
    /** Square footprint width/depth; retained for compatibility with cube callers. */
    public int getStructureSize() { return size; }
    public int getStructureHeight() { return height; }
    public boolean isController() { return origin.equals(worldPosition); }
    public FluidStack getLocalFluid() { return fluid.copy(); }
    public FluidStack getFluid() { return handler.getFluidInTank(0); }
    public IFluidHandler getFluidHandler(@Nullable Direction side) { return handler; }
    public boolean isFermenting() { return recipe != null; }
    public int getProgress() { return recipe == null ? 0 : (int) Math.min(100L, progress * 100L / recipe.durationTicks()); }

    public void setStructure(BlockPos newOrigin, int newSize) {
        setStructure(newOrigin, newSize, newSize);
    }

    public void setStructure(BlockPos newOrigin, int newSize, int newHeight) {
        if (newSize < 1 || newSize > 3 || newHeight < 1 || newHeight > 3) throw new IllegalArgumentException("Vat dimensions must be between one and three");
        if (origin.equals(newOrigin) && size == newSize && height == newHeight) return;
        origin = newOrigin.immutable();
        size = newSize;
        height = newHeight;
        invalidateCapabilities();
        changed();
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && isVat()) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    /** Loaded parts only: automation must never reach across an unavailable chunk. */
    @Nullable public List<FermentationVesselBlockEntity> members() {
        if (level == null || isRemoved()) return null;
        List<FermentationVesselBlockEntity> members = new ArrayList<>(size * size * height);
        for (BlockPos cursor : BlockPos.betweenClosed(origin, origin.offset(size - 1, height - 1, size - 1))) {
            if (!level.hasChunkAt(cursor) || !(level.getBlockEntity(cursor) instanceof FermentationVesselBlockEntity part)
                    || part.isRemoved() || part.size != size || part.height != height || !part.origin.equals(origin) || part.isVat() != isVat()) return null;
            members.add(part);
        }
        return members;
    }

    public boolean addStarter() {
        List<FermentationVesselBlockEntity> parts = members();
        if (parts == null || parts.stream().allMatch(p -> p.fluid.isEmpty() || p.starter)) return false;
        for (var part : parts) {
            if (!part.fluid.isEmpty() && part.recipe == null && level.getRecipeManager()
                    .getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream().noneMatch(holder -> holder.value().matches(part.fluid))) return false;
        }
        for (var part : parts) {
            if (part.fluid.isEmpty()) continue;
            part.starter = true;
            part.fluid = BatchProfile.of(part.fluid).withYeast(ResourceLocation.fromNamespaceAndPath("vinum", "wine_yeast")).applyTo(part.fluid);
            part.changed();
        }
        return true;
    }

    /** Press all local escrows together; skins are abstracted away without losing liquid or alcohol. */
    public boolean press() {
        if (level == null || level.isClientSide) return false;
        var parts = members();
        FluidStack sample = getFluid();
        if (parts == null || sample.isEmpty() || parts.stream().anyMatch(part -> part.recipe != null)) return false;
        BatchProfile profile = BatchProfile.of(sample);
        if (profile.solidsGramsPerLitre() <= 0 || !profile.history().contains(ResourceLocation.fromNamespaceAndPath("vinum", "fermentation"))) return false;
        BatchProfile pressed = new BatchProfile(profile.sugarGramsPerLitre(), profile.starchGramsPerLitre(), 0,
                profile.alcoholFraction(), profile.ingredientProportions(), profile.yeast(), profile.history())
                .addHistory(ResourceLocation.fromNamespaceAndPath("vinum", "pressing"));
        for (var part : parts) {
            if (part.fluid.isEmpty()) continue;
            part.fluid = pressed.applyTo(part.fluid);
            part.changed();
        }
        return true;
    }

    public Component getStatusMessage() {
        if (members() == null) return Component.translatable("vessel.vinum.unavailable");
        FluidStack contents = getFluid();
        String dimensions = size + "×" + height + "×" + size;
        int percentage = 100;
        boolean active = false;
        int totalAmount = 0;
        FluidStack representative = FluidStack.EMPTY;
        for (var part : members()) {
            totalAmount += part.fluid.getAmount();
            if (representative.isEmpty() && !part.fluid.isEmpty()) representative = part.fluid;
            if (part.recipe != null) { active = true; percentage = Math.min(percentage, part.getProgress()); }
        }
        if (active) return Component.translatable("vessel.vinum.fermenting", representative.getHoverName(), totalAmount, getCapacity(), percentage);
        if (contents.isEmpty()) return Component.translatable("vessel.vinum.empty", getBlockState().getBlock().getName(), getCapacity() / 1000, dimensions);
        if (BatchProfile.of(contents).solidsGramsPerLitre() > 0 && BatchProfile.of(contents).history()
                .contains(ResourceLocation.fromNamespaceAndPath("vinum", "fermentation"))) {
            return Component.translatableWithFallback("vessel.vinum.needs_press", "%s · %s / %s mB · Use a Wine Press to remove skins before bottling",
                    contents.getHoverName(), contents.getAmount(), getCapacity());
        }
        return Component.translatable("vessel.vinum.contents", contents.getHoverName(), contents.getAmount(), getCapacity(), dimensions);
    }

    /** Validate the entire loaded reservoir before freezing any plan. No partial starts. */
    public boolean startFermentation() {
        if (level == null || level.isClientSide) return false;
        var parts = members();
        FluidStack sample = getFluid();
        if (parts == null || sample.isEmpty() || parts.stream().anyMatch(part -> part.recipe != null)) return false;
        FermentationRecipe selected = level.getRecipeManager().getAllRecipesFor(ProductionRecipes.FERMENTATION_TYPE.get()).stream()
                .sorted(java.util.Comparator.comparing(holder -> holder.id().toString()))
                .map(net.minecraft.world.item.crafting.RecipeHolder::value).filter(candidate -> candidate.matches(sample)).findFirst().orElse(null);
        if (selected == null) return false;
        for (var part : parts) {
            if (part.fluid.isEmpty()) continue;
            part.recipe = selected;
            part.progress = 0;
            part.changed();
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FermentationVesselBlockEntity vessel) {
        if (vessel.fluid.isEmpty() || level.getGameTime() % 20 != 0) return;
        if (vessel.recipe == null) return;
        vessel.progress = (int) Math.min((long) vessel.recipe.durationTicks(), vessel.progress + (vessel.starter ? 40L : 20L));
        if (vessel.progress >= vessel.recipe.durationTicks()) {
            FluidStack result = FermentationEngine.finish(vessel.fluid, vessel.recipe);
            // A frozen plan may become invalid after data-pack/mod changes. Keep the original batch.
            if (!result.isEmpty()) vessel.fluid = result;
            vessel.recipe = null;
            vessel.progress = 0;
            vessel.starter = false;
            vessel.changed();
            if (vessel.isVat()) level.scheduleTick(pos, state.getBlock(), 1);
        } else {
            vessel.setChanged();
            // Keep visible bubbles/progress fresh without a packet on every tick.
            if (level.getGameTime() % 200 == 0) vessel.changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Fluid", fluid.saveOptional(registries));
        tag.putLong("StructureOrigin", origin.asLong());
        tag.putInt("StructureSize", size);
        tag.putInt("StructureHeight", height);
        tag.putInt("FermentationProgress", progress);
        tag.putBoolean("Starter", starter);
        if (recipe != null) FermentationRecipe.CODEC.codec().encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), recipe)
                .result().ifPresent(encoded -> tag.put("FermentationPlan", encoded));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = FluidStack.parseOptional(registries, tag.getCompound("Fluid"));
        origin = tag.contains("StructureOrigin") ? BlockPos.of(tag.getLong("StructureOrigin")) : worldPosition;
        size = isVat() ? Math.clamp(tag.getInt("StructureSize"), 1, 3) : 1;
        // Old saves only stored a cube size.
        height = isVat() ? Math.clamp(tag.contains("StructureHeight") ? tag.getInt("StructureHeight") : size, 1, 3) : 1;
        if (!VesselMultiblock.contains(origin, size, height, worldPosition)) { origin = worldPosition; size = 1; height = 1; }
        recipe = tag.contains("FermentationPlan") ? FermentationRecipe.CODEC.codec()
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), tag.get("FermentationPlan")).result().orElse(null) : null;
        progress = recipe == null ? 0 : Math.clamp(tag.getInt("FermentationProgress"), 0, recipe.durationTicks());
        starter = tag.getBoolean("Starter");
    }

    @Override public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        if (fluid.isEmpty()) { stack.remove(DataComponents.BLOCK_ENTITY_DATA); return; }
        CompoundTag tag = saveCustomOnly(registries);
        tag.remove("StructureOrigin");
        tag.remove("StructureSize");
        tag.remove("StructureHeight");
        BlockItem.setBlockEntityData(stack, getType(), tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    private final class Handler implements IFluidHandler {
        @Override public int getTanks() { return 1; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? getCapacity() : 0; }
        @Override public FluidStack getFluidInTank(int tank) {
            if (tank != 0) return FluidStack.EMPTY;
            var parts = members();
            if (parts == null) return FluidStack.EMPTY;
            FluidStack combined = FluidStack.EMPTY;
            for (var part : parts) {
                if (part.fluid.isEmpty()) continue;
                if (combined.isEmpty()) combined = part.fluid.copy();
                else if (!FluidStack.isSameFluidSameComponents(combined, part.fluid)) return FluidStack.EMPTY;
                else combined.grow(part.fluid.getAmount());
            }
            return combined;
        }
        @Override public boolean isFluidValid(int tank, FluidStack resource) {
            return tank == 0 && !resource.isEmpty() && (isVat() || BatchProfile.of(resource).solidsGramsPerLitre() <= 0);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!isFluidValid(0, resource)) return 0;
            var parts = members();
            if (parts == null) return 0;
            int available = 0;
            for (var part : parts) {
                if (part.recipe != null || !part.fluid.isEmpty() && !FluidStack.isSameFluidSameComponents(part.fluid, resource)) return 0;
                available += part.localCapacity() - part.fluid.getAmount();
            }
            int accepted = Math.min(available, resource.getAmount());
            if (action.execute() && accepted > 0) {
                int remaining = accepted;
                for (var part : parts) {
                    int portion = Math.min(remaining, part.localCapacity() - part.fluid.getAmount());
                    if (portion == 0) continue;
                    part.fluid = resource.copyWithAmount(part.fluid.getAmount() + portion);
                    part.starter = BatchProfile.of(resource).yeast().isPresent();
                    part.changed();
                    remaining -= portion;
                }
                if (isVat() && level != null) level.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
            }
            return accepted;
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) {
            FluidStack sample = getFluidInTank(0);
            if (!FluidStack.isSameFluidSameComponents(sample, resource)) return FluidStack.EMPTY;
            return drain(resource.getAmount(), action);
        }
        @Override public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) return FluidStack.EMPTY;
            var parts = members();
            if (parts == null || parts.stream().anyMatch(part -> part.recipe != null)) return FluidStack.EMPTY;
            FluidStack result = getFluidInTank(0);
            if (result.isEmpty()) return result;
            result.setAmount(Math.min(maxDrain, result.getAmount()));
            if (action.execute()) {
                int remaining = result.getAmount();
                for (var part : parts) {
                    int portion = Math.min(remaining, part.fluid.getAmount());
                    if (portion == 0) continue;
                    part.fluid.shrink(portion);
                    if (part.fluid.isEmpty()) { part.starter = false; part.progress = 0; }
                    part.changed();
                    remaining -= portion;
                }
            }
            return result;
        }
    }
}
