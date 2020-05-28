package io.github.Inspirateur.Landlord;

import java.io.Serializable;

public class Zone implements Serializable {
	private int x1, y1, z1;
	private int x2, y2, z2;

	public void setCorner1(int x1, int y1, int z1) {
		this.x1 = x1;
		this.y1 = y1;
		this.z1 = z1;
	}

	public void setCorner2(int x2, int y2, int z2) {
		this.x2 = x2;
		this.y2 = y2;
		this.z2 = z2;
	}

	public int getVolume() {
		 return Math.abs(this.x1-this.x2)*Math.abs(this.y1-this.y2)*Math.abs(this.z1-this.z2);
	}
}
