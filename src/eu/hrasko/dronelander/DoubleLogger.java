package eu.hrasko.dronelander;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DoubleLogger extends DataLogger<Double> {

	public DoubleLogger() {
		super();
		defalutT = 0d;
	}

	/**
	 * returns arithmetic average of howMany last numbers
	 */
	public Double getArithmeticAverage(long howMany) {
		List<Double> h = getLastSet(howMany);

		if (h.size() == 0) {
			return 0d;
		}

		double tmp = 0;
		for (Double d : h) {
			tmp += d;
		}
		tmp /= h.size();

		return tmp;
	}

	public void add(float f) {
		super.add((double) f);
	}

	/**
	 * returns median average of howMany last numbers
	 */
	public Double getMedianAverage(long howMany) {
		List<Double> h = getLastSet(howMany);

		if (h.size() == 0) {
			return 0d;
		}

		List<Double> sorted = new ArrayList<Double>(h);
		Collections.sort(sorted);

		return sorted.get((int) (sorted.size() / 2));
	}

	/**
	 * returns the sum of howMany last numbers
	 */
	public Double getSum(long howMany) {

		List<Double> h = getLastSet(howMany);
		if (h.size() == 0)
			return 0d;

		howMany = Math.min(h.size(), howMany);

		double tmp = 0;
		for (Double d : h) {
			tmp += d;
		}

		return tmp;

	}

}
