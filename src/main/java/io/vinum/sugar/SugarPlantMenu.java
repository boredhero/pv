package io.vinum.sugar;

import io.vinum.inventory.container.ModContainers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.*;

public final class SugarPlantMenu extends AbstractContainerMenu {
    private final SugarPlantBlockEntity plant;
    private final ContainerData data;
    public SugarPlantMenu(int id,Inventory inventory,RegistryFriendlyByteBuf buffer) {
        this(id,inventory,inventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof SugarPlantBlockEntity p?p:null,
            new ItemStackHandler(7),new SimpleContainerData(12));
    }
    public SugarPlantMenu(int id,Inventory inventory,SugarPlantBlockEntity plant) {
        this(id,inventory,plant,plant.inventory,plant.data);
    }
    private SugarPlantMenu(int id,Inventory inventory,SugarPlantBlockEntity plant,IItemHandler items,ContainerData data) {
        super(ModContainers.SUGAR_PLANT.get(),id);this.plant=plant;this.data=data;
        int[][] positions={{48,54},{48,86},{274,54},{274,86},{18,147},{18,184},{302,147}};
        for(int i=0;i<positions.length;i++) {
            int slot=i;
            addSlot(new SlotItemHandler(items,i,positions[i][0],positions[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return slot!=1&&slot!=3&&slot!=6&&super.mayPlace(stack);
                }
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inventory,9+row*9+col,89+col*18,139+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,89+col*18,197));
        addDataSlots(data);
    }
    public int mode(){return data.get(0);}
    public int progress(){return data.get(1);}
    public int required(){return data.get(2);}
    public int burn(){return data.get(3);}
    public int burnTotal(){return data.get(4);}
    public int inputAmount(){return data.get(5);}
    public int outputAmount(){return data.get(6);}
    public int status(){return data.get(7);}
    public int inputColor(){return (data.get(8)&65535)|((data.get(9)&65535)<<16);}
    public int outputColor(){return (data.get(10)&65535)|((data.get(11)&65535)<<16);}
    public net.neoforged.neoforge.fluids.FluidStack input(){return plant==null?net.neoforged.neoforge.fluids.FluidStack.EMPTY:plant.input();}
    public net.neoforged.neoforge.fluids.FluidStack output(){return plant==null?net.neoforged.neoforge.fluids.FluidStack.EMPTY:plant.output();}
    @Override public boolean stillValid(Player player){return plant!=null&&plant.usable(player);}
    @Override public boolean clickMenuButton(Player player,int button) {
        if(plant==null||player.level().isClientSide||!plant.usable(player))return false;
        if(button>=0&&button<5)plant.selectMode(button);
        else if(button==100)plant.start();
        else if(button==101)plant.recycleOutput();
        else if(button==102)plant.recoverInput();
        else return false;
        broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0||index>=slots.size()||!slots.get(index).hasItem())return ItemStack.EMPTY;
        Slot slot=slots.get(index);ItemStack stack=slot.getItem(),original=stack.copy();
        boolean moved=false;
        if(index<7)moved=moveItemStackTo(stack,7,43,true);
        else {
            boolean emptyContainer=net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack)
                .map(handler->handler.getFluidInTank(0).isEmpty()).orElse(false);
            for(int target:emptyContainer?new int[]{5,4,2}:new int[]{5,4,0})if(slots.get(target).mayPlace(stack)) {
                moved=moveItemStackTo(stack,target,target+1,false);if(moved)break;
            }
            if(!moved)moved=index<34?moveItemStackTo(stack,34,43,false):moveItemStackTo(stack,7,34,false);
        }
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return original;
    }
}
