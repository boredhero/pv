package io.vinum.capability;

public interface IBAC {
	
	public double getBACLevel();
	public void setBACLevel(double BACLevel);
	public void addBACLevel(double BACLevel);
	public void removeBACLevel(double BACLevel);
	public int getBACTicks();
	public void setBACTicks(int BACTicks);
	public void addBACTicks(int BACTicks);
	public void removeBACTicks(int BACTicks);
	
}