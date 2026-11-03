package io.vinum.blender;

import io.vinum.beverage.*;
import io.vinum.sugar.SugarContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.items.*;
import org.jetbrains.annotations.Nullable;

/** Independently loaded inputs, exact-ratio planning, and an isolated output reservoir. */
public final class BlenderBlockEntity extends BlockEntity implements MenuProvider {
    public static final int TANK_CAPACITY=8000,INVENTORY_SIZE=8,DATA_COUNT=16;
    public static final int A_CONTAINER=0,A_RETURN=1,B_CONTAINER=2,B_RETURN=3,OUTPUT_CONTAINER=4,FILLED_CONTAINER=5,INGREDIENT=6,INGREDIENT_RETURN=7;
    public static final int BLEND=0,HONEY=1,WASH=2,TARGET_ABV=3;
    public static final int STATUS_READY=0,STATUS_RUNNING=1,STATUS_INPUT=2,STATUS_OUTPUT=3,STATUS_RETURN=4,STATUS_INGREDIENT=5,STATUS_INCOMPATIBLE=6;
    private FluidStack a=FluidStack.EMPTY,b=FluidStack.EMPTY,output=FluidStack.EMPTY,pending=FluidStack.EMPTY;
    private int mode,ratio=50,targetTenths=400,progress,duration;
    private boolean automatic,processing;
    public final ItemStackHandler inventory=new ItemStackHandler(INVENTORY_SIZE){
        @Override public boolean isItemValid(int slot,ItemStack stack){return switch(slot){
            case A_CONTAINER,B_CONTAINER,OUTPUT_CONTAINER->FluidUtil.getFluidHandler(stack).isPresent();
            case INGREDIENT->stack.is(Items.HONEY_BOTTLE)||stack.is(Items.SUGAR)||stack.is(SugarContent.PANELA.get());
            default->false;};}
        @Override public int getSlotLimit(int slot){return slot==INGREDIENT||slot==INGREDIENT_RETURN?64:1;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public BlenderBlockEntity(BlockPos pos,BlockState state){super(BlenderContent.ENTITY.get(),pos,state);}
    public FluidStack a(){return a.copy();}public FluidStack b(){return b.copy();}public FluidStack output(){return output.copy();}
    public int targetAbv(){return targetTenths;}
    public boolean setTargetAbv(int value){if(processing||value<1||value>950)return false;targetTenths=value;sync();return true;}
    public int mode(){return mode;}public int ratio(){return ratio;}public boolean automatic(){return automatic;}
    public boolean processing(){return processing;}public int progress(){return progress;}public int requiredTicks(){return duration;}
    public boolean selectMode(int value){if(processing||value<BLEND||value>TARGET_ABV)return false;mode=value;sync();return true;}
    public boolean setRatio(int value){if(processing||value<0||value>100)return false;ratio=value;sync();return true;}
    public void toggleAutomatic(){automatic=!automatic;sync();}
    private final IFluidHandler portA=new Port(0),portB=new Port(1),portOutput=new Port(2);
    public IFluidHandler inputA(){return portA;}public IFluidHandler inputB(){return portB;}public IFluidHandler outputFluids(){return portOutput;}
    /** Facing-relative left accepts A, right accepts B; bottom drains output, other sides accept A. */
    public IFluidHandler fluids(@Nullable Direction side){
        if(side==Direction.DOWN)return portOutput;
        var facing=getBlockState().getValue(BlenderBlock.FACING);
        return side==facing.getClockWise()?portB:portA;
    }
    private final class Port implements IFluidHandler {
        private final int index;Port(int index){this.index=index;}
        private FluidStack stored(){return index==0?a:index==1?b:output;}
        public int getTanks(){return 1;}public FluidStack getFluidInTank(int tank){return tank==0?stored().copy():FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank==0?TANK_CAPACITY:0;}
        public boolean isFluidValid(int tank,FluidStack fluid){return tank==0&&index!=2&&!fluid.isEmpty();}
        public int fill(FluidStack fluid,FluidAction action){
            if(index==2||processing||fluid.isEmpty()||!stored().isEmpty()&&!stored().getFluid().isSame(fluid.getFluid())||!FluidBlending.canBlend(stored(),fluid))return 0;
            int accepted=Math.max(0,Math.min(fluid.getAmount(),TANK_CAPACITY-stored().getAmount()));
            if(accepted>0&&action.execute()){var merged=FluidBlending.blend(stored(),fluid.copyWithAmount(accepted));if(index==0)a=merged;else b=merged;sync();}return accepted;
        }
        public FluidStack drain(FluidStack fluid,FluidAction action){return FluidStack.isSameFluidSameComponents(stored(),fluid)?drain(fluid.getAmount(),action):FluidStack.EMPTY;}
        public FluidStack drain(int amount,FluidAction action){
            if(amount<=0||index!=2&&processing)return FluidStack.EMPTY;
            var result=stored().copyWithAmount(Math.min(amount,stored().getAmount()));
            if(action.execute()&&!result.isEmpty()){stored().shrink(result.getAmount());sync();}return result;
        }
    }
    public IItemHandler items(@Nullable Direction side){return new IItemHandler(){
        private final int[] slots=side==Direction.DOWN?new int[]{A_RETURN,B_RETURN,FILLED_CONTAINER,INGREDIENT_RETURN}:new int[]{A_CONTAINER,B_CONTAINER,OUTPUT_CONTAINER,INGREDIENT};
        public int getSlots(){return slots.length;}public ItemStack getStackInSlot(int s){return inventory.getStackInSlot(slots[s]);}
        public int getSlotLimit(int s){return inventory.getSlotLimit(slots[s]);}
        public boolean isItemValid(int s,ItemStack stack){return side!=Direction.DOWN&&inventory.isItemValid(slots[s],stack);}
        public ItemStack insertItem(int s,ItemStack stack,boolean simulate){return side==Direction.DOWN?stack:inventory.insertItem(slots[s],stack,simulate);}
        public ItemStack extractItem(int s,int count,boolean simulate){return side==Direction.DOWN?inventory.extractItem(slots[s],count,simulate):ItemStack.EMPTY;}
    };}
    private boolean fitsItem(int slot,ItemStack value){var old=inventory.getStackInSlot(slot);return value.isEmpty()||(old.isEmpty()||ItemStack.isSameItemSameComponents(old,value))&&old.getCount()+value.getCount()<=Math.min(inventory.getSlotLimit(slot),value.getMaxStackSize());}
    private void storeItem(int slot,ItemStack value){if(value.isEmpty())return;var old=inventory.getStackInSlot(slot);inventory.setStackInSlot(slot,old.isEmpty()?value.copy():old.copyWithCount(old.getCount()+value.getCount()));}
    private boolean fitsFluid(FluidStack value){return value.isEmpty()||output.getAmount()+value.getAmount()<=TANK_CAPACITY&&(output.isEmpty()||output.getFluid().isSame(value.getFluid()))&&FluidBlending.canBlend(output,value);}
    private BlenderProcess.Plan plan(){return BlenderProcess.plan(mode,ratio,targetTenths,a,b,inventory.getStackInSlot(INGREDIENT));}
    public int status(){
        if(processing)return fitsFluid(pending)?STATUS_RUNNING:STATUS_OUTPUT;
        if(mode==TARGET_ABV){
            if(a.isEmpty()||b.isEmpty())return STATUS_INPUT;
            double alcohol=io.vinum.production.BatchProfile.of(a).alcoholFraction();
            if(!BlenderProcess.plainWater(b)||alcohol<=targetTenths/1000D||!FluidBlending.canBlend(a,b))return STATUS_INCOMPATIBLE;
        }else if(mode==BLEND){
            if(ratio>0&&a.isEmpty()||ratio<100&&b.isEmpty())return STATUS_INPUT;
            if(ratio>0&&ratio<100&&!FluidBlending.canBlend(a,b))return STATUS_INCOMPATIBLE;
        }else{
            int count=BlenderProcess.ingredientsRequired(mode,inventory.getStackInSlot(INGREDIENT));
            if(count==0||inventory.getStackInSlot(INGREDIENT).getCount()<count)return STATUS_INGREDIENT;
            if(!BlenderProcess.plainWater(a)||a.getAmount()<1000)return STATUS_INPUT;
        }
        var plan=plan();if(plan==null)return mode==BLEND||mode==TARGET_ABV?STATUS_INPUT:STATUS_INGREDIENT;
        if(!fitsFluid(plan.output()))return STATUS_OUTPUT;
        if(!fitsItem(INGREDIENT_RETURN,plan.remainder()))return STATUS_RETURN;
        return STATUS_READY;
    }
    public boolean start(){
        if(level==null||level.isClientSide||processing||status()!=STATUS_READY)return false;
        var plan=plan();a.shrink(plan.amountA());b.shrink(plan.amountB());
        inventory.extractItem(INGREDIENT,plan.ingredients(),false);storeItem(INGREDIENT_RETURN,plan.remainder());
        pending=plan.output().copy();processing=true;duration=20;progress=0;sync();return true;
    }
    public boolean recoverInput(boolean second){
        var fluid=second?b:a;if(level==null||level.isClientSide||processing||fluid.isEmpty()||!fitsFluid(fluid))return false;
        output=FluidBlending.blend(output,fluid);if(second)b=FluidStack.EMPTY;else a=FluidStack.EMPTY;sync();return true;
    }
    private void loadContainer(int sourceSlot,int returnSlot,IFluidHandler target){
        if(processing)return;
        var source=inventory.getStackInSlot(sourceSlot);if(source.isEmpty())return;
        var handler=FluidUtil.getFluidHandler(source.copyWithCount(1)).orElse(null);if(handler==null)return;
        var sample=handler.drain(Integer.MAX_VALUE,IFluidHandler.FluidAction.SIMULATE);
        int amount=target.fill(sample,IFluidHandler.FluidAction.SIMULATE);if(amount<=0)return;
        var drained=handler.drain(amount,IFluidHandler.FluidAction.EXECUTE);
        if(drained.getAmount()!=amount||!fitsItem(returnSlot,handler.getContainer()))return;
        target.fill(drained,IFluidHandler.FluidAction.EXECUTE);inventory.extractItem(sourceSlot,1,false);storeItem(returnSlot,handler.getContainer());sync();
    }
    private void transferContainers(){
        loadContainer(A_CONTAINER,A_RETURN,portA);loadContainer(B_CONTAINER,B_RETURN,portB);
        var source=inventory.getStackInSlot(OUTPUT_CONTAINER);if(source.isEmpty()||output.isEmpty()||!inventory.getStackInSlot(FILLED_CONTAINER).isEmpty())return;
        var handler=FluidUtil.getFluidHandler(source.copyWithCount(1)).orElse(null);if(handler==null)return;
        int amount=handler.fill(output.copy(),IFluidHandler.FluidAction.SIMULATE);
        if(amount<=0||handler.fill(output.copyWithAmount(amount),IFluidHandler.FluidAction.EXECUTE)!=amount)return;
        output.shrink(amount);inventory.extractItem(OUTPUT_CONTAINER,1,false);inventory.setStackInSlot(FILLED_CONTAINER,handler.getContainer());sync();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,BlenderBlockEntity blender){
        if(level.isClientSide)return;blender.transferContainers();
        if(!blender.processing){if(blender.automatic)blender.start();return;}
        if(!blender.fitsFluid(blender.pending))return;
        if(++blender.progress>=blender.duration){
            blender.output=FluidBlending.blend(blender.output,blender.pending);blender.pending=FluidStack.EMPTY;
            blender.processing=false;blender.duration=0;blender.progress=0;blender.sync();
        }else if(level.getGameTime()%5==0)blender.sync();else blender.setChanged();
    }
    public boolean usable(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
    @Override public Component getDisplayName(){return Component.translatable("block.vinum.blender");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new BlenderMenu(id,inventory,this);}
    public final ContainerData data=new ContainerData(){
        public int getCount(){return DATA_COUNT;}public void set(int index,int value){}
        public int get(int index){return switch(index){case 0->mode;case 1->ratio;case 2->automatic?1:0;case 3->progress;case 4->duration;case 5->a.getAmount();case 6->b.getAmount();case 7->output.getAmount();case 8->status();case 9->WineCatalog.color(a)&65535;case 10->WineCatalog.color(a)>>>16;case 11->WineCatalog.color(b)&65535;case 12->WineCatalog.color(b)>>>16;case 13->WineCatalog.color(output)&65535;case 14->WineCatalog.color(output)>>>16;case 15->targetTenths;default->0;};}
    };
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.put("Inventory",inventory.serializeNBT(registries));
        if(!a.isEmpty())tag.put("A",a.save(registries));if(!b.isEmpty())tag.put("B",b.save(registries));if(!output.isEmpty())tag.put("Output",output.save(registries));if(!pending.isEmpty())tag.put("Pending",pending.save(registries));
        tag.putInt("TargetAbv",targetTenths);tag.putInt("Mode",mode);tag.putInt("Ratio",ratio);tag.putInt("Progress",progress);tag.putInt("Duration",duration);tag.putBoolean("Automatic",automatic);tag.putBoolean("Processing",processing);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);var inv=tag.getCompound("Inventory").copy();inv.putInt("Size",INVENTORY_SIZE);inventory.deserializeNBT(registries,inv);
        a=FluidStack.parseOptional(registries,tag.getCompound("A"));b=FluidStack.parseOptional(registries,tag.getCompound("B"));output=FluidStack.parseOptional(registries,tag.getCompound("Output"));pending=FluidStack.parseOptional(registries,tag.getCompound("Pending"));
        targetTenths=tag.contains("TargetAbv")?Math.clamp(tag.getInt("TargetAbv"),1,950):400;
        mode=Math.clamp(tag.getInt("Mode"),BLEND,TARGET_ABV);ratio=tag.contains("Ratio")?Math.clamp(tag.getInt("Ratio"),0,100):50;
        duration=Math.max(0,tag.getInt("Duration"));progress=Math.clamp(tag.getInt("Progress"),0,duration);automatic=tag.getBoolean("Automatic");processing=tag.getBoolean("Processing")&&duration>0&&!pending.isEmpty();
    }
    @Override public void saveToItem(ItemStack stack,HolderLookup.Provider registries){BlockItem.setBlockEntityData(stack,getType(),saveCustomOnly(registries));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
