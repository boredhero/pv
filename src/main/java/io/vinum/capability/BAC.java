/*
    Project Vinum - BAC.java
    Copyright (C) 2020 Noah Martino and Tiller Eaton

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
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