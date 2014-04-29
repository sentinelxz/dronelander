package eu.hrasko.dronelander;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Comparator;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.opencv.core.Point;
import org.opencv.features2d.KeyPoint;

public class Coordinate implements Serializable, Cloneable {

	public double x, y, z; // z is often as altitude / height
	public double size;
	public double angle;
	public double speed;

	public double altitude;
	public double roll, pich, yaw;

	public boolean isEstimated = false;

	public static final Coordinate verticalOneVector = new Coordinate(0, 1);

	public Coordinate() {

	}

	public Coordinate(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public Coordinate(double x, double y, double z) {
		this(x, y);
		this.z = z;
	}

	public Coordinate(KeyPoint kp) {
		x = kp.pt.x;
		y = kp.pt.y;
		size = kp.size;
		angle = kp.angle;
	}

	public static Comparator<Coordinate> getSizeComparator() {
		return new Comparator<Coordinate>() {
			@Override
			public int compare(Coordinate o1, Coordinate o2) {
				return new CompareToBuilder().append(o1.size, o2.size).toComparison();
			}
		};
	}

	/**
	 * Returns distance of coordinate to second coordinate
	 */
	public double distanceTo(Coordinate second) {
		double xd = second.x - x;
		double yd = second.y - y;
		double zd = second.z - z;

		double dist = Math.sqrt(xd * xd + yd * yd + zd * zd);

		return dist;
	}

	/**
	 * returs a coordinate which is in the midle of the path to second
	 * coordinate
	 */
	public Coordinate getMidpoint(Coordinate second) {
		Coordinate c = new Coordinate();

		c.x = (x + second.x) / 2;
		c.y = (y + second.y) / 2;
		c.z = (z + second.z) / 2;

		return c;
	}

	/**
	 * returns angle of two vectors given by two coordinates
	 */
	public double getAngleTo(Coordinate second) {
		double thisAngleToZero = this.getAngleFromZero();
		double secondAngleToZero = second.getAngleFromZero();
		return getAngleBetweenAngles(thisAngleToZero, secondAngleToZero);
	}

	/**
	 * returns angle of vector and vertical axe
	 */
	public double getAngleToVertical() {
		return getAngleTo(verticalOneVector);
	}

	public double getAngleFromZero() {
		double fromZero = Math.atan2(x, y);
		fromZero = (fromZero > 0 ? fromZero : (2 * Math.PI + fromZero)) * 360 / (2 * Math.PI);
		fromZero = getAngleBetweenAngles(0, fromZero);
		return fromZero;
	}

	/**
	 * returns angle given by vector and angle in degrees
	 */
	public double getAngleToAngle(double secondAngle) {
		double thisAngleFromZero = this.getAngleFromZero();

		return getAngleBetweenAngles(thisAngleFromZero, secondAngle);
	}

	public static double getAngleBetweenAngles(double firstAngle, double secondAngle) {
		firstAngle = (firstAngle + 360) % 360;
		secondAngle = (secondAngle + 360) % 360;
		double angle = firstAngle - secondAngle;
		angle = (angle + 360) % 360;

		if (angle > 180) {
			angle -= 360;
		}

		return -angle;
	}

	public Point getPoint() {
		return new Point(x, y);
	}

	@Override
	public String toString() {

		String s = "";
		for (Field f : Coordinate.class.getFields()) {
			try {
				s += f.getName() + ": " + f.get(this) + "; ";
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return s;
	}

	/**
	 * substracs dimensions by second coodrinate
	 */
	public Coordinate substract(Coordinate second) {
		Coordinate c = this.clone();

		c.x = x - second.x;
		c.y = y - second.y;
		c.z = z - second.z;

		return c;
	}

	@Override
	public Coordinate clone() {
		return (Coordinate) SerializationUtils.clone(this);
	}

}
