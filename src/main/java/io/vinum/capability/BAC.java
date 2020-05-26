package io.vinum.capability;

public class BAC implements IBAC {
	
	private int BACLevel;
	private int BACTicks;
	
	public BAC() {
		
		BACLevel = 0;
		BACTicks = 0;
		
	}
	
	@Override
	public int getBACLevel() {
		
		return BACLevel;
		
	}
	
	@Override
	public void setBACLevel(int BACLevel) {
		
		this.BACLevel = BACLevel;
		
	}
	
	@Override
	public void addBACLevel(int BACLevel) {
		
		this.BACLevel += BACLevel;
		
	}
	
	@Override
	public void removeBACLevel(int BACLevel) {
		
		this.BACLevel -= BACLevel;
		
	}
	
	@Override
	public int getBACTicks() {
		
		return BACTicks;
		
	}
	
	@Override
	public void setBACTicks(int BACTicks) {
		
		this.BACTicks = BACTicks;
		
	}
	
	@Override
	public void addBACTicks(int BACTicks) {
		
		this.BACTicks += BACTicks;
		
	}
	
	@Override
	public void removeBACTicks(int BACTicks) {
		
		this.BACTicks -= BACTicks;
		
	}
	
}