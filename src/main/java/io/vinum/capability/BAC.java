package io.vinum.capability;

public class BAC implements IBAC {
	
	private int BACLevel;
	
	public BAC() {
		
		BACLevel = 0;
		
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
	
}