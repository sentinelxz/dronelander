package eu.hrasko.dronelander;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class CoordinatePair {
	Coordinate c1;
	Coordinate c2;
	double sizeDifference;
	private Coordinate midPoint;

	public CoordinatePair(Coordinate c1, Coordinate c2) {
		this.c1 = c1;
		this.c2 = c2;

		sizeDifference = Math.abs(c1.size - c2.size);
		midPoint = c1.getMidpoint(c2);
	}

	/**
	 * returns distance beteen coordinates
	 */
	public double getDistance() {
		return c1.distanceTo(c2);
	}

	public static Comparator<CoordinatePair> getSizeDifferenceComparator() {
		return new Comparator<CoordinatePair>() {
			@Override
			public int compare(CoordinatePair o1, CoordinatePair o2) {
				return o1.sizeDifference < o2.sizeDifference ? -1 : o1.sizeDifference > o2.sizeDifference ? 1 : 0;
			}
		};
	}

	/**
	 * Makes all combinations of coodrinates given in lists
	 */
	public static List<CoordinatePair> getAllPairs(List<Coordinate> list1, List<Coordinate> list2) {
		List<CoordinatePair> list = new ArrayList<CoordinatePair>();
		for (Coordinate c1 : list1) {
			for (Coordinate c2 : list2) {
				list.add(new CoordinatePair(c1, c2));
			}
		}
		return list;
	}

	/**
	 * returns a coordinate which lays right beteen coordinates
	 */
	public Coordinate getMidPoint() {
		return midPoint.clone();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		// @formatter:off
		CoordinatePair other = (CoordinatePair) obj;
		return ((new EqualsBuilder().append(c1, other.c1).isEquals() 
				&& new EqualsBuilder().append(c2, other.c1).isEquals()) 
				|| (new EqualsBuilder().append(c1, other.c2).isEquals() 
				&& new EqualsBuilder().append(c2, other.c1).isEquals()));
		// @formatter:on
	}
}
