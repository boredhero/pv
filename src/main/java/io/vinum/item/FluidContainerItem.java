package io.vinum.item;

import io.vinum.barrel.BarrelFluidData;
import io.vinum.barrel.ModDataComponents;
import io.vinum.beverage.FluidBlending;
import io.vinum.beverage.WineCatalog;
import io.vinum.production.BatchProfile;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** Reusable, partial-fill container. Capacity, acceptance and drinking are independent of beverage identity. */
public class FluidContainerItem extends Item {
    public static final TagKey<Fluid> CONTENTS = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("vinum", "container_contents"));
    public static final TagKey<Fluid> DRINKABLE = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("vinum", "drinkable"));
    private final int capacity;
    private final boolean drinking;
    private final Supplier<FluidStack> legacyContents;
    private final Supplier<? extends Item> canonicalItem;
    public FluidContainerItem(Properties properties, int capacity, boolean drinking) {
        this(properties, capacity, drinking, () -> FluidStack.EMPTY, null);
    }
    public FluidContainerItem(Properties properties, int capacity, boolean drinking,
                              Supplier<FluidStack> legacyContents, Supplier<? extends Item> canonicalItem) {
        super((canonicalItem == null ? properties.component(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.EMPTY) : properties).stacksTo(1));
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity; this.drinking = drinking; this.legacyContents = legacyContents; this.canonicalItem = canonicalItem;
    }
    public int capacity() { return capacity; }
    public boolean legacy() { return canonicalItem != null; }
    public FluidStack contents(ItemStack stack) {
        var stored = stack.get(ModDataComponents.CONTAINED_FLUID.get());
        FluidStack fluid = stored == null ? legacyContents.get() : stored.copy();
        if (fluid.getAmount() > capacity) return FluidStack.EMPTY;
        if (stack.has(ModDataComponents.FLUID_AGE.get())) fluid = BarrelFluidData.withAge(fluid, stack.get(ModDataComponents.FLUID_AGE.get()));
        return WineCatalog.normalize(fluid);
    }
    protected void contentsChanged(ItemStack stack) {}
    public Handler handler(ItemStack stack) { return new Handler(stack); }
    public ItemStack filled(FluidStack fluid) {
        ItemStack stack = new ItemStack(canonicalItem == null ? this : canonicalItem.get());
        if (fluid.getAmount() > capacity) throw new IllegalArgumentException("Container overflow");
        stack.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(WineCatalog.normalize(fluid)));
        if (io.vinum.beverage.BottleCorks.isBottle(stack) && WineCatalog.isWine(fluid))
            stack.set(ModDataComponents.BOTTLE_CORK.get(), true);
        return stack;
    }
    @Override public Component getName(ItemStack stack) {
        FluidStack fluid = contents(stack);
        Component container = Component.translatable((canonicalItem == null ? this : canonicalItem.get()).getDescriptionId());
        return fluid.isEmpty() ? container : Component.translatable("item.vinum.container_filled", container, WineCatalog.name(fluid));
    }
    @Override public boolean isBarVisible(ItemStack stack) { return !contents(stack).isEmpty(); }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * contents(stack).getAmount() / capacity); }
    @Override public int getBarColor(ItemStack stack) { return WineCatalog.color(contents(stack)) & 0xFFFFFF; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = contents(stack);
        tooltip.add(Component.translatable("tooltip.vinum.container_capacity", fluid.getAmount(), capacity).withStyle(ChatFormatting.GRAY));
        if (io.vinum.beverage.BottleCorks.isBottle(stack))
            tooltip.add(Component.translatable(io.vinum.beverage.BottleCorks.hasCork(stack)
                    ? "tooltip.vinum.cork_supplied" : "tooltip.vinum.cork_required").withStyle(ChatFormatting.GRAY));
        if (fluid.isEmpty()) return;
        io.vinum.barrel.BeverageCatalog.lookupFluid(fluid.getFluid()).ifPresent(beverage -> {
            long age=BarrelFluidData.getAge(fluid);
            tooltip.add(Component.translatable("tooltip.vinum.barrel_age",age/io.vinum.config.BarrelConfig.ticksPerDay()).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.vinum.maturation",Component.translatable(beverage.aging().stage(age))).withStyle(ChatFormatting.GOLD));
        });
        BatchProfile profile = BatchProfile.of(fluid);
        tooltip.add(Component.translatable("tooltip.vinum.alcohol", String.format(java.util.Locale.ROOT, "%.1f", profile.alcoholFraction() * 100)).withStyle(ChatFormatting.GRAY));
        profile.ingredientProportions().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry ->
            tooltip.add(Component.translatable("tooltip.vinum.blend_share", Component.translatable("item." + entry.getKey().getNamespace() + "." + entry.getKey().getPath()),
                    String.format(java.util.Locale.ROOT, "%.1f", entry.getValue() * 100)).withStyle(ChatFormatting.DARK_GRAY)));
        tooltip.add(Component.translatable("tooltip.vinum.container_controls").withStyle(ChatFormatting.DARK_GRAY));
    }
    public boolean canDrink(ItemStack stack) {
        var fluid = contents(stack);
        return drinking && !fluid.isEmpty() && fluid.is(DRINKABLE) && BatchProfile.of(fluid).solidsGramsPerLitre() == 0;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(io.vinum.beverage.ContainerTransfers.betweenHands(level,player,hand))return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
        return canDrink(player.getItemInHand(hand)) ? ItemUtils.startUsingInstantly(level, player, hand) : InteractionResultHolder.pass(player.getItemInHand(hand));
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!canDrink(stack) || level.isClientSide) return stack;
        if (entity instanceof net.minecraft.server.level.ServerPlayer player) {
            net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        }
        if (entity instanceof Player player && player.hasInfiniteMaterials()) return stack;
        FluidStack fluid = contents(stack);
        // A normal 125 mB glass at 12% ABV corresponds to the existing one-point serving.
        if (entity instanceof Player player) io.vinum.item.drinks.IDrink.addBACLevel(level, player,
                player.getData(io.vinum.capability.BACCapability.BAC_STATE).acceptAlcohol(fluid.getAmount() * BatchProfile.of(fluid).alcoholFraction()));
        var emptied = handler(stack.copyWithCount(1));
        emptied.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        ItemStack result = emptied.getContainer();
        stack.shrink(1);
        if (stack.isEmpty()) return result;
        if (entity instanceof Player player) { if (!player.getInventory().add(result)) player.drop(result, false); }
        else entity.spawnAtLocation(result);
        return stack;
    }
    public final class Handler implements IFluidHandlerItem {
        private ItemStack stack;
        private Handler(ItemStack stack) { this.stack = stack; }
        @Override public ItemStack getContainer() { return stack; }
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return tank == 0 ? contents(stack) : FluidStack.EMPTY; }
        @Override public int getTankCapacity(int tank) { return tank == 0 ? capacity : 0; }
        @Override public boolean isFluidValid(int tank, FluidStack fluid) {
            return tank == 0 && !fluid.isEmpty() && WineCatalog.normalize(fluid).is(CONTENTS)
                    && (!drinking || BatchProfile.of(fluid).solidsGramsPerLitre() <= 0);
        }
        private void write(FluidStack fluid) {
            if (canonicalItem != null) {
                ItemStack migrated = new ItemStack(canonicalItem.get());
                migrated.applyComponents(stack.getComponentsPatch()); stack = migrated;
            }
            stack.set(ModDataComponents.CONTAINED_FLUID.get(), SimpleFluidContent.copyOf(fluid));
            stack.remove(ModDataComponents.FLUID_AGE.get());
            if (fluid.isEmpty()) stack.remove(ModDataComponents.BOTTLE_CORK.get());
            if (stack.getItem() instanceof FluidContainerItem item) item.contentsChanged(stack);
        }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (stack.getCount() != 1 || !isFluidValid(0, resource)) return 0;
            if (io.vinum.beverage.BottleCorks.requiresCork(stack, resource)) return 0;
            FluidStack current = contents(stack);
            if (!FluidBlending.canBlend(current, resource)) return 0;
            int accepted = Math.min(resource.getAmount(), capacity - current.getAmount());
            if (accepted > 0 && action.execute()) write(FluidBlending.blend(current, resource.copyWithAmount(accepted)));
            return accepted;
        }
        @Override public FluidStack drain(FluidStack requested, FluidAction action) {
            if (requested.isEmpty() || !BarrelFluidData.sameFluid(contents(stack), WineCatalog.normalize(requested))) return FluidStack.EMPTY;
            return drain(requested.getAmount(), action);
        }
        @Override public FluidStack drain(int maximum, FluidAction action) {
            if (stack.getCount() != 1 || maximum <= 0) return FluidStack.EMPTY;
            FluidStack current = contents(stack);
            if (current.isEmpty()) return FluidStack.EMPTY;
            FluidStack result = current.copyWithAmount(Math.min(maximum, current.getAmount()));
            if (action.execute()) { current.shrink(result.getAmount()); write(current); }
            return result;
        }
    }
}
