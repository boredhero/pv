package io.vinum.sugar;

import io.vinum.beverage.FluidBlending;
import io.vinum.beverage.WineCatalog;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.*;
import org.jetbrains.annotations.Nullable;

/** Two isolated tanks and a reserved batch: processing can never overwrite or discard either output. */
public final class SugarPlantBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY=8000, INVENTORY_SIZE=7, DATA_COUNT=14;
    public static final int ENERGY_CAPACITY=32000, FE_PER_TICK=20;
    public final io.vinum.machine.MachineEnergy energy=new io.vinum.machine.MachineEnergy(ENERGY_CAPACITY,80,0,this::setChanged);
    // Slot 5 remains extraction-only so old ingredient stacks can be recovered without remapping saves.
    public static final int INPUT_CONTAINER=0, RETURNED_CONTAINER=1, OUTPUT_CONTAINER=2,
        FILLED_CONTAINER=3, FUEL=4, LEGACY_RETURN=5, SOLID_OUTPUT=6;
    public static final int MODE_COUNT=3;
    public static final int STATUS_READY=0, STATUS_RUNNING=1, STATUS_INPUT=2,
        STATUS_OUTPUT=3, STATUS_SOLID=4, STATUS_FUEL=5;
    private FluidStack input=FluidStack.EMPTY,output=FluidStack.EMPTY,pendingFluid=FluidStack.EMPTY;
    private ItemStack pendingSolid=ItemStack.EMPTY;
    private int mode,progress,duration,burnTime,burnTotal;
    private boolean processing,heatedBatch,automatic;

    public final ItemStackHandler inventory=new ItemStackHandler(INVENTORY_SIZE) {
        @Override public boolean isItemValid(int slot,ItemStack stack) {
            return switch(slot) {
                case INPUT_CONTAINER,OUTPUT_CONTAINER -> FluidUtil.getFluidHandler(stack).isPresent();
                case FUEL -> stack.getBurnTime(RecipeType.SMELTING)>0;
                default -> false;
            };
        }
        @Override public int getSlotLimit(int slot){return slot==INPUT_CONTAINER||slot==OUTPUT_CONTAINER||slot==FILLED_CONTAINER?1:64;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };

    public SugarPlantBlockEntity(BlockPos pos,BlockState state){super(SugarContent.PLANT_ENTITY.get(),pos,state);}
    public FluidStack input(){return input.copy();}
    public FluidStack output(){return output.copy();}
    public int mode(){return mode;}
    public boolean processing(){return processing;}
    public boolean automatic(){return automatic;}
    /** Disabling repetition finishes the reserved batch; it never discards its inputs. */
    public boolean toggleAutomatic(){
        if(level==null||level.isClientSide)return false;
        automatic=!automatic;sync();return true;
    }
    public int progress(){return progress;}
    public int requiredTicks(){return duration;}
    public int burnTime(){return burnTime;}
    public int burnTotal(){return burnTotal;}
    public boolean selectMode(int selected){
        if(processing||selected<0||selected>=MODE_COUNT)return false;
        mode=selected;sync();return true;
    }
    private static boolean accepted(FluidStack value){
        if(value.isEmpty())return false;
        var id=BuiltInRegistries.FLUID.getKey(value.getFluid());
        return SugarProcess.recipes().stream().anyMatch(recipe->recipe.input().equals(id));
    }
    private final IFluidHandler intake=new TankPort(false);
    private final IFluidHandler outlet=new TankPort(true);
    public IFluidHandler inputFluids(){return intake;}
    public IFluidHandler outputFluids(){return outlet;}
    public IFluidHandler fluids(@Nullable Direction side){return side==Direction.DOWN?outlet:intake;}
    private final class TankPort implements IFluidHandler {
        private final boolean out;
        TankPort(boolean out){this.out=out;}
        private FluidStack contents(){return out?output:input;}
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return tank==0?contents().copy():FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank==0?TANK_CAPACITY:0;}
        public boolean isFluidValid(int tank,FluidStack resource){return tank==0&&!out&&accepted(resource);}
        public int fill(FluidStack resource,FluidAction action){
            if(out||processing||!accepted(resource)||!FluidBlending.canBlend(input,resource))return 0;
            int amount=Math.min(TANK_CAPACITY-input.getAmount(),resource.getAmount());
            if(amount>0&&action.execute()){input=FluidBlending.blend(input,resource.copyWithAmount(amount));sync();}
            return amount;
        }
        public FluidStack drain(FluidStack resource,FluidAction action){
            return FluidStack.isSameFluidSameComponents(resource,contents())?drain(resource.getAmount(),action):FluidStack.EMPTY;
        }
        public FluidStack drain(int amount,FluidAction action){
            if(amount<=0||contents().isEmpty()||!out&&processing)return FluidStack.EMPTY;
            var result=contents().copyWithAmount(Math.min(amount,contents().getAmount()));
            if(action.execute()){contents().shrink(result.getAmount());sync();}
            return result;
        }
    }
    /** Output-only automation remains able to unblock a paused batch. */
    public IItemHandler items(@Nullable Direction side){
        return new IItemHandler(){
            private final int[] slots=side==Direction.DOWN?new int[]{RETURNED_CONTAINER,FILLED_CONTAINER,SOLID_OUTPUT,LEGACY_RETURN}:
                new int[]{INPUT_CONTAINER,OUTPUT_CONTAINER,FUEL};
            public int getSlots(){return slots.length;}
            public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slots[slot]);}
            public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return side==Direction.DOWN?stack:inventory.insertItem(slots[slot],stack,simulate);}
            public ItemStack extractItem(int slot,int amount,boolean simulate){return side==Direction.DOWN?inventory.extractItem(slots[slot],amount,simulate):ItemStack.EMPTY;}
            public int getSlotLimit(int slot){return inventory.getSlotLimit(slots[slot]);}
            public boolean isItemValid(int slot,ItemStack stack){return side!=Direction.DOWN&&inventory.isItemValid(slots[slot],stack);}
        };
    }
    private record Plan(int inputAmount,FluidStack liquid,ItemStack solid,int ticks){}
    private Plan plan(){
        if(mode<0||mode>=MODE_COUNT||mode>=SugarProcess.recipes().size())return null;
        var recipe=SugarProcess.recipes().get(mode);
        if(input.getAmount()<recipe.inputAmount())return null;
        var batch=input.copyWithAmount(recipe.inputAmount());
        if(recipe.batches(batch)!=1)return null;
        return new Plan(recipe.inputAmount(),recipe.liquid(batch),recipe.solids(batch),recipe.ticks());
    }
    private boolean fits(int slot,ItemStack next){
        if(next.isEmpty())return true;
        var stored=inventory.getStackInSlot(slot);
        return (stored.isEmpty()||ItemStack.isSameItemSameComponents(stored,next))
            &&stored.getCount()+next.getCount()<=Math.min(inventory.getSlotLimit(slot),next.getMaxStackSize());
    }
    private boolean fitsLiquid(FluidStack next){return next.isEmpty()||output.getAmount()+next.getAmount()<=TANK_CAPACITY&&(output.isEmpty()||output.getFluid().isSame(next.getFluid()))&&FluidBlending.canBlend(output,next);}
    private void store(int slot,ItemStack value){
        if(value.isEmpty())return;
        var old=inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot,old.isEmpty()?value.copy():old.copyWithCount(old.getCount()+value.getCount()));
    }
    private boolean heatAvailable(){return burnTime>0||energy.getEnergyStored()>=FE_PER_TICK||fuelAvailable();}
    private boolean fuelAvailable(){
        if(burnTime>0)return true;
        var fuel=inventory.getStackInSlot(FUEL);
        if(fuel.getBurnTime(RecipeType.SMELTING)<=0)return false;
        var remainder=fuel.getCraftingRemainingItem();
        return fuel.getCount()==1||fits(RETURNED_CONTAINER,remainder);
    }
    public int status(){
        if(processing){
            if(!fitsLiquid(pendingFluid))return STATUS_OUTPUT;
            if(!fits(SOLID_OUTPUT,pendingSolid))return STATUS_SOLID;
            return heatedBatch&&!heatAvailable()?STATUS_FUEL:STATUS_RUNNING;
        }
        var plan=plan();
        if(plan==null)return STATUS_INPUT;
        if(!fitsLiquid(plan.liquid()))return STATUS_OUTPUT;
        if(!fits(SOLID_OUTPUT,plan.solid()))return STATUS_SOLID;
        if(!heatAvailable())return STATUS_FUEL;
        return STATUS_READY;
    }
    public boolean start(){
        if(level==null||level.isClientSide||processing||status()!=STATUS_READY)return false;
        var plan=plan();
        // Reserve exactly one batch and its results. Its complete snapshot survives saves and pickup.
        input.shrink(plan.inputAmount());
        pendingFluid=plan.liquid().copy();pendingSolid=plan.solid().copy();
        duration=plan.ticks();progress=0;heatedBatch=true;processing=true;sync();return true;
    }
    public boolean recycleOutput(){
        if(level==null||level.isClientSide||processing||output.isEmpty()||!accepted(output)
            ||input.getAmount()+output.getAmount()>TANK_CAPACITY||!FluidBlending.canBlend(input,output))return false;
        input=FluidBlending.blend(input,output);output=FluidStack.EMPTY;sync();return true;
    }
    /** Explicit GUI recovery drains input into the output tank without destroying material. */
    public boolean recoverInput(){
        if(level==null||level.isClientSide||processing||input.isEmpty()||!fitsLiquid(input))return false;
        output=FluidBlending.blend(output,input);input=FluidStack.EMPTY;sync();return true;
    }
    private void consumeFuel(){
        if(burnTime>0)return;
        var fuel=inventory.getStackInSlot(FUEL);
        burnTime=burnTotal=fuel.getBurnTime(RecipeType.SMELTING);
        var remainder=fuel.getCraftingRemainingItem();
        inventory.extractItem(FUEL,1,false);
        if(!remainder.isEmpty()){
            if(inventory.getStackInSlot(FUEL).isEmpty())inventory.setStackInSlot(FUEL,remainder);
            else store(RETURNED_CONTAINER,remainder);
        }
    }
    private void transferContainers(){
        if(!processing){
            var source=inventory.getStackInSlot(INPUT_CONTAINER);
            if(!source.isEmpty()){
                var handler=FluidUtil.getFluidHandler(source.copyWithCount(1)).orElse(null);
                if(handler!=null){
                    var sample=handler.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);
                    int amount=intake.fill(sample,IFluidHandler.FluidAction.SIMULATE);
                    if(amount>0){
                        var drained=handler.drain(amount,IFluidHandler.FluidAction.EXECUTE);
                        if(drained.getAmount()==amount&&fits(RETURNED_CONTAINER,handler.getContainer())){
                            intake.fill(drained,IFluidHandler.FluidAction.EXECUTE);
                            inventory.extractItem(INPUT_CONTAINER,1,false);store(RETURNED_CONTAINER,handler.getContainer());
                        }
                    }
                }
            }
        }
        var container=inventory.getStackInSlot(OUTPUT_CONTAINER);
        if(container.isEmpty()||output.isEmpty()||!inventory.getStackInSlot(FILLED_CONTAINER).isEmpty())return;
        var handler=FluidUtil.getFluidHandler(container.copyWithCount(1)).orElse(null);
        if(handler==null)return;
        int amount=handler.fill(output.copy(),IFluidHandler.FluidAction.SIMULATE);
        if(amount>0&&handler.fill(output.copyWithAmount(amount),IFluidHandler.FluidAction.EXECUTE)==amount){
            output.shrink(amount);inventory.extractItem(OUTPUT_CONTAINER,1,false);
            inventory.setStackInSlot(FILLED_CONTAINER,handler.getContainer());sync();
        }
    }
    public static void tick(Level level,BlockPos pos,BlockState state,SugarPlantBlockEntity plant){
        if(level.isClientSide)return;
        plant.transferContainers();
        // Reuse the same admission checks as a manual batch. Missing inputs/fuel or blocked
        // outputs leave repetition armed without consuming anything; replenishment resumes it.
        if(plant.automatic&&!plant.processing)plant.start();
        if(!plant.processing||plant.status()!=STATUS_RUNNING)return;
        if(plant.heatedBatch){
            // Finish already-lit fuel before spending FE; prefer FE over igniting another item.
            // This is one heat source per advancing tick, never a double charge.
            if(plant.burnTime>0)plant.burnTime--;
            else if(plant.energy.getEnergyStored()>=FE_PER_TICK)plant.energy.consume(FE_PER_TICK);
            else {plant.consumeFuel();plant.burnTime--;}
        }
        if(++plant.progress>=plant.duration){
            if(!plant.pendingFluid.isEmpty())plant.output=FluidBlending.blend(plant.output,plant.pendingFluid);
            plant.store(SOLID_OUTPUT,plant.pendingSolid);
            plant.pendingFluid=FluidStack.EMPTY;plant.pendingSolid=ItemStack.EMPTY;
            plant.processing=false;plant.progress=0;plant.duration=0;plant.sync();
        }else if(level.getGameTime()%5==0)plant.sync();else plant.setChanged();
    }
    public boolean usable(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public Component getDisplayName(){return Component.translatable("block.vinum.sugar_plant");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new SugarPlantMenu(id,inv,this);}
    public final ContainerData data=new ContainerData(){
        public int get(int index){return switch(index){
            case 0->mode;case 1->progress;case 2->duration;case 3->burnTime;case 4->burnTotal;
            case 5->input.getAmount();case 6->output.getAmount();case 7->status();
            case 8->WineCatalog.color(input)&65535;case 9->WineCatalog.color(input)>>>16;
            case 10->WineCatalog.color(output)&65535;case 11->WineCatalog.color(output)>>>16;case 12->automatic?1:0;case 13->energy.getEnergyStored();default->0;};}
        public void set(int index,int value){}
        public int getCount(){return DATA_COUNT;}
    };
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putInt("Energy",energy.getEnergyStored());tag.put("Inventory",inventory.serializeNBT(registries));
        if(!input.isEmpty())tag.put("Input",input.save(registries));if(!output.isEmpty())tag.put("Output",output.save(registries));
        if(!pendingFluid.isEmpty())tag.put("PendingFluid",pendingFluid.save(registries));
        if(!pendingSolid.isEmpty())tag.put("PendingSolid",pendingSolid.save(registries));
        tag.putInt("Mode",mode);tag.putInt("Progress",progress);tag.putInt("Duration",duration);
        tag.putInt("BurnTime",burnTime);tag.putInt("BurnTotal",burnTotal);tag.putBoolean("Processing",processing);tag.putBoolean("HeatedBatch",heatedBatch);tag.putBoolean("Automatic",automatic);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);energy.load(tag.getInt("Energy"));
        var savedInventory=tag.getCompound("Inventory").copy();savedInventory.putInt("Size",INVENTORY_SIZE);
        inventory.deserializeNBT(registries,savedInventory);
        input=FluidStack.parseOptional(registries,tag.getCompound("Input"));output=FluidStack.parseOptional(registries,tag.getCompound("Output"));
        pendingFluid=FluidStack.parseOptional(registries,tag.getCompound("PendingFluid"));pendingSolid=ItemStack.parseOptional(registries,tag.getCompound("PendingSolid"));
        mode=Math.clamp(tag.getInt("Mode"),0,MODE_COUNT-1);duration=Math.max(0,tag.getInt("Duration"));progress=Math.clamp(tag.getInt("Progress"),0,duration);
        burnTime=Math.max(0,tag.getInt("BurnTime"));burnTotal=Math.max(burnTime,tag.getInt("BurnTotal"));
        processing=tag.getBoolean("Processing")&&duration>0&&(!pendingFluid.isEmpty()||!pendingSolid.isEmpty());heatedBatch=tag.getBoolean("HeatedBatch");automatic=tag.getBoolean("Automatic");
    }
    @Override public void saveToItem(ItemStack stack,HolderLookup.Provider registries){BlockItem.setBlockEntityData(stack,getType(),saveCustomOnly(registries));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
