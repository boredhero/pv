package io.vinum.item;

import javax.annotation.Nullable;

import io.vinum.gui.screen.EditModSignScreen;
import io.vinum.tileentity.ModSignTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.WallOrFloorItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class ModSignItem extends WallOrFloorItem {
	
	public ModSignItem(Properties propertiesIn, Block floorBlockIn, Block wallBlockIn) {
		super(floorBlockIn, wallBlockIn, propertiesIn);
		
	}
	
	@Override
	protected boolean onBlockPlaced(BlockPos pos, World worldIn, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		
		boolean flag = super.onBlockPlaced(pos, worldIn, player, stack, state);
		
		if (worldIn.isRemote && !flag && player != null) {
			
			((ModSignTileEntity) worldIn.getTileEntity(pos)).setPlayer(player);
			Minecraft.getInstance().displayGuiScreen(new EditModSignScreen((ModSignTileEntity) worldIn.getTileEntity(pos)));
			
		}
		
		return flag;
		
	}
	
}