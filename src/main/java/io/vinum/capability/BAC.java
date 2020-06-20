package io.vinum.capability;

public class BAC implements IBAC {
	
	private double BACLevel;
	private int BACTicks;
	
	public BAC() {
		
		BACLevel = 0;
		BACTicks = 0;
		
	}
	
	@Override
	public double getBACLevel() {
		
		if(this.BACLevel < 0){
			this.BACLevel = 0;
		}
		return BACLevel;
		
	}
	
	@Override
	public void setBACLevel(double BACLevel) {
		
		if(this.BACLevel < 0){
			this.BACLevel = 0;
		}
		this.BACLevel = BACLevel;
		
	}
	
	@Override
	public void addBACLevel(double BACLevel) {
		
		if(this.BACLevel < 0){
			this.BACLevel = 0;
		}
		this.BACLevel += BACLevel;
		
	}
	
	@Override
	public void removeBACLevel(double BACLevel) {
		
		this.BACLevel -= BACLevel;
		if(this.BACLevel < 0){
			this.BACLevel = 0;
		}
		
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