package eu.hrasko.dronelander;

import java.util.Comparator;

import org.opencv.features2d.DMatch;

public class DMatchComparator implements Comparator<DMatch> {
	@Override
	public int compare(DMatch o1, DMatch o2) {
		return o1.distance < o2.distance ? -1 : o1.distance < o2.distance ? 1 : 0;
	}
}