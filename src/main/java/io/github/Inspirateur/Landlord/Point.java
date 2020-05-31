package io.github.Inspirateur.Landlord;

import java.io.Serializable;

public class Point implements Serializable {
	public int x, y, z;

	public Point(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Point min(Point p1, Point p2) {
		return new Point(Math.min(p1.x, p2.x), Math.min(p1.y, p2.y), Math.min(p1.z, p2.z));
	}

	public static Point max(Point p1, Point p2) {
		return new Point(Math.max(p1.x, p2.x), Math.max(p1.y, p2.y), Math.max(p1.z, p2.z));
	}

	public static boolean leq(Point p1, Point p2) {
		return p1.x <= p2.x && p1.y <= p2.y && p1.z <= p2.z;
	}

	public static boolean geq(Point p1, Point p2) {
		return p1.x >= p2.x && p1.y >= p2.y && p1.z >= p2.z;
	}
}
