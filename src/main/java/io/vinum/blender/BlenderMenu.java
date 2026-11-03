package io.vinum.blender;

import io.vinum.inventory.container.ModContainers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.items.*;

public final class BlenderMenu extends AbstractContainerMenu {
    private final BlenderBlockEntity blender;
    private final ContainerData data;
    public BlenderMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer){
        this(id,inventory,inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof BlenderBlockEntity b?b:null,
            new ItemStackHandler(BlenderBlockEntity.INVENTORY_SIZE),new SimpleContainerData(BlenderBlockEntity.DATA_COUNT));
    }
    public BlenderMenu(int id,Inventory inventory,BlenderBlockEntity blender){this(id,inventory,blender,blender.inventory,blender.data);}
    private BlenderMenu(int id,Inventory inventory,BlenderBlockEntity blender,IItemHandler items,ContainerData data){
        super(ModContainers.BLENDER.get(),id);this.blender=blender;this.data=data;
        int[][] positions={{48,57},{48,83},{140,57},{140,83},{274,57},{274,83},{18,158},{54,158}};
        for(int i=0;i<positions.length;i++){
            int slot=i;
            addSlot(new SlotItemHandler(items,i,positions[i][0],positions[i][1]){
                @Override public boolean mayPlace(ItemStack stack){return (slot==0||slot==2||slot==4||slot==6)&&super.mayPlace(stack);}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,89+col*18,153+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,89+col*18,211));
        addDataSlots(data);
    }
    public int mode(){return data.get(0);}public int ratio(){return data.get(1);}public boolean automatic(){return data.get(2)!=0;}
    public int progress(){return data.get(3);}public int required(){return data.get(4);}
    public int aAmount(){return data.get(5);}public int bAmount(){return data.get(6);}public int outputAmount(){return data.get(7);}
    public int status(){return data.get(8);}public int targetAbv(){return data.get(15);}
    public int color(int tank){int offset=9+tank*2;return (data.get(offset)&65535)|((data.get(offset+1)&65535)<<16);}
    public FluidStack fluid(int tank){return blender==null?FluidStack.EMPTY:tank==0?blender.a():tank==1?blender.b():blender.output();}
    @Override public boolean stillValid(Player player){return blender!=null&&blender.usable(player);}
    @Override public boolean clickMenuButton(Player player,int action){
        if(blender==null||player.level().isClientSide||!blender.usable(player)||!player.mayBuild())return false;
        if(action>=0&&action<=3)blender.selectMode(action);
        else if(action==100)blender.start();
        else if(action==103)blender.toggleAutomatic();
        else if(action==104||action==105)blender.recoverInput(action==105);
        else if(action>=1000&&action<=1100)blender.setRatio(action-1000);
        else if(action>=2001&&action<=2950)blender.setTargetAbv(action-2000);
        else return false;
        broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size()||!slots.get(index).hasItem())return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var original=stack.copy();boolean moved=false;
        if(index<8)moved=moveItemStackTo(stack,8,44,true);
        else{
            boolean empty=FluidUtil.getFluidHandler(stack).map(h->h.getFluidInTank(0).isEmpty()).orElse(false);
            for(int target:empty?new int[]{6,4}:new int[]{6,0,2})if(slots.get(target).mayPlace(stack)){
                moved=moveItemStackTo(stack,target,target+1,false);if(moved)break;
            }
            if(!moved)moved=index<35?moveItemStackTo(stack,35,44,false):moveItemStackTo(stack,8,35,false);
        }
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
}
