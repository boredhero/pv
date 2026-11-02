package io.vinum.capability;

public interface IBAC {
	
	public int getBACLevel();
	public void setBACLevel(int BACLevel);
	public void addBACLevel(int BACLevel);
	public void removeBACLevel(int BACLevel);
	public int getBACTicks();
	public void setBACTicks(int BACTicks);
	public void addBACTicks(int BACTicks);
	public void removeBACTicks(int BACTicks);
	
}