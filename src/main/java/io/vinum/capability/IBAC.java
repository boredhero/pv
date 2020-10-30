/*
    Project Vinum - IBAC.java
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