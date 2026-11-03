package io.vinum.sugar;

import io.vinum.beverage.FluidBlending;
import io.vinum.production.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/** Single batch kettle. A process locks transfers; its liquid and solid outputs commit together. */
public final class SugarKettleBlockEntity extends BlockEntity {
    public static final int CAPACITY=4000;
    private FluidStack fluid=FluidStack.EMPTY;
    private ItemStack solid=ItemStack.EMPTY;
    private boolean insert;
    private int mode, progress;
    private boolean processing;
    public SugarKettleBlockEntity(BlockPos pos,BlockState state){super(SugarContent.ENTITY.get(),pos,state);}
    public FluidStack contents(){return fluid.copy();}
    public ItemStack solidOutput(){return solid.copy();}
    public boolean hasInsert(){return insert;}
    public boolean processing(){return processing;}
    public int progress(){return progress;}
    public SugarProcess recipe(){return SugarProcess.recipes().get(mode);}
    /** Atomically replaces a complete cold water batch; active heat recipes cannot be modified. */
    public boolean addHoney(){
        if(level==null||level.isClientSide||processing||!solid.isEmpty())return false;
        var next=HoneyMixing.prepare(fluid);
        if(next.isEmpty()||next.getAmount()>CAPACITY)return false;
        fluid=next;sync();return true;
    }
    public boolean installInsert(){if(insert||processing)return false;insert=true;sync();return true;}
    public void cycleMode(){if(!processing){mode=(mode+1)%SugarProcess.recipes().size();sync();}}
    public boolean start(){
        var r=recipe();var output=r.solids(fluid);
        if(processing||!solid.isEmpty()||r.batches(fluid)==0||r.insert()&&!insert||output.getCount()>64||r.liquid(fluid).getAmount()>CAPACITY)return false;
        processing=true;progress=0;sync();return true;
    }
    public ItemStack takeOutput(){if(processing)return ItemStack.EMPTY;var result=solid;solid=ItemStack.EMPTY;sync();return result;}
    public Component status(){return Component.translatable("message.vinum.kettle_status",
        Component.translatable("process.vinum."+recipe().id()),fluid.isEmpty()?Component.translatable("message.vinum.kettle_empty"):fluid.getHoverName(),
        fluid.getAmount(),String.format(java.util.Locale.ROOT,"%.0f",BatchProfile.of(fluid).sugarGramsPerLitre()),
        solid.isEmpty()?Component.translatable("message.vinum.kettle_no_output"):Component.literal(solid.getCount()+" × ").append(solid.getHoverName()),
        Component.translatable(insert?"message.vinum.kettle_insert_yes":"message.vinum.kettle_insert_no"),processing?progress:0,recipe().ticks());}
    public static void tick(Level level,BlockPos pos,BlockState state,SugarKettleBlockEntity kettle){
        if(level.isClientSide||!kettle.processing||!SugarKettleBlock.heated(level,pos))return;
        if(++kettle.progress>=kettle.recipe().ticks()) {
            var next=kettle.recipe().liquid(kettle.fluid);var output=kettle.recipe().solids(kettle.fluid);
            if(kettle.recipe().batches(kettle.fluid)>0&&kettle.solid.isEmpty()) {kettle.fluid=next;kettle.solid=output;}
            kettle.processing=false;kettle.progress=0;kettle.sync();
        } else kettle.setChanged();
    }
    /** Dissolves one provenance-bearing sugar/panela item, preserving its sugar mass. */
    public boolean dissolve(ItemStack item){
        if(processing||fluid.isEmpty()||fluid.getFluid().isSame(io.vinum.fluid.ModFluids.MEAD_MUST.source().get())||!accepted(fluid)||BatchProfile.of(fluid).alcoholFraction()!=0)return false;
        if(!item.is(Items.SUGAR)&&!item.is(SugarContent.PANELA.get()))return false;
        BatchProfile ingredient=item.get(ProductionDataComponents.BATCH_PROFILE.get());
        if(ingredient==null&&item.is(SugarContent.PANELA.get())) ingredient=new BatchProfile(200,0,0,0,Map.of(ProductionInputs.id("panela"),1.0),Optional.empty(),List.of());
        if(ingredient==null||ingredient.ingredientProportions().isEmpty())return false;
        var original=BatchProfile.of(fluid);
        double mass=original.sugarGramsPerLitre()*fluid.getAmount()/1000.0;
        double added=ingredient.sugarGramsPerLitre();
        if(added<=0||(mass+added)*1000/fluid.getAmount()>10000)return false;
        var sources=new LinkedHashMap<net.minecraft.resources.ResourceLocation,Double>();
        if(mass>0)original.ingredientProportions().forEach((key,value)->sources.merge(key,value*mass/(mass+added),Double::sum));
        ingredient.ingredientProportions().forEach((key,value)->sources.merge(key,value*added/(mass+added),Double::sum));
        double total=sources.values().stream().mapToDouble(Double::doubleValue).sum();
        if(total<=0||sources.size()>BatchProfile.MAX_INGREDIENTS)return false;
        sources.replaceAll((key,value)->value/total);
        var next=new FluidStack(BuiltInRegistries.FLUID.get(ProductionInputs.id("cane_wash")),fluid.getAmount());
        next.applyComponents(fluid.getComponentsPatch());
        new BatchProfile((mass+added)*1000/fluid.getAmount(),0,original.solidsGramsPerLitre()+ingredient.solidsGramsPerLitre()*1000/fluid.getAmount(),0,sources,Optional.empty(),original.history()).addHistory(ProductionInputs.id("dissolving")).applyTo(next);
        fluid=next;sync();return true;
    }
    private static boolean accepted(FluidStack value){
        var id=BuiltInRegistries.FLUID.getKey(value.getFluid());
        return value.getFluid()==Fluids.WATER||id.getNamespace().equals("vinum")&&Set.of("cane_juice","cane_syrup","molasses","cane_wash","mead_must").contains(id.getPath());
    }
    private final IFluidHandler handler=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int tank){return tank==0?fluid.copy():FluidStack.EMPTY;}
        public int getTankCapacity(int tank){return tank==0?CAPACITY:0;}
        public boolean isFluidValid(int tank,FluidStack resource){return tank==0&&accepted(resource);}
        public int fill(FluidStack resource,FluidAction action){
            if(processing||resource.isEmpty()||!accepted(resource)||!FluidBlending.canBlend(fluid,resource))return 0;
            int amount=Math.min(CAPACITY-fluid.getAmount(),resource.getAmount());
            if(amount>0&&action.execute()){fluid=FluidBlending.blend(fluid,resource.copyWithAmount(amount));sync();}
            return amount;
        }
        public FluidStack drain(FluidStack resource,FluidAction action){return FluidStack.isSameFluidSameComponents(resource,fluid)?drain(resource.getAmount(),action):FluidStack.EMPTY;}
        public FluidStack drain(int amount,FluidAction action){
            if(processing||amount<=0||fluid.isEmpty())return FluidStack.EMPTY;
            var result=fluid.copyWithAmount(Math.min(amount,fluid.getAmount()));
            if(action.execute()){fluid.shrink(result.getAmount());sync();}return result;
        }
    };
    public IFluidHandler fluids(){return handler;}
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);if(!fluid.isEmpty())tag.put("Fluid",fluid.save(registries));if(!solid.isEmpty())tag.put("Solid",solid.save(registries));
        tag.putBoolean("Insert",insert);tag.putInt("Mode",mode);tag.putInt("Progress",progress);tag.putBoolean("Processing",processing);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);fluid=FluidStack.parseOptional(registries,tag.getCompound("Fluid"));solid=ItemStack.parseOptional(registries,tag.getCompound("Solid"));
        if(fluid.getAmount()>CAPACITY)fluid=FluidStack.EMPTY;
        insert=tag.getBoolean("Insert");mode=Math.clamp(tag.getInt("Mode"),0,SugarProcess.recipes().size()-1);progress=Math.clamp(tag.getInt("Progress"),0,recipe().ticks());
        processing=tag.getBoolean("Processing")&&recipe().batches(fluid)>0&&(!recipe().insert()||insert)&&solid.isEmpty();
    }
    @Override public void saveToItem(ItemStack stack,HolderLookup.Provider registries){BlockItem.setBlockEntityData(stack,getType(),saveCustomOnly(registries));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveCustomOnly(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
