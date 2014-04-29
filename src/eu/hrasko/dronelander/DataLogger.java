package eu.hrasko.dronelander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataLogger<T> {

	private List<T> history;
	T defalutT = null;

	public DataLogger() {
		history = new ArrayList<T>();
	}

	/**
	 * returns sublist of data containing howMany items which lays at the end of
	 * main list
	 */
	public List<T> getLastSet(long howMany) {
		List<T> result;
		synchronized (history) {

			howMany = Math.min(howMany, history.size());
			if (howMany == 0) {
				return new ArrayList<T>();
			}

			List<T> view = history.subList((int) (history.size() - howMany), (int) history.size());
			result = new ArrayList<T>(view.size());
			for (int i = 0; i < view.size(); i++) {
				result.add(view.get(i));
			}

		}

		return result;
	}

	public void add(T data) {
		synchronized (history) {
			history.add(data);
		}

	}

	public boolean isEmpty() {
		synchronized (history) {
			return history.isEmpty();
		}

	}

	public int size() {
		synchronized (history) {
			return history.size();
		}

	}

	public T get(int index) {
		synchronized (history) {
			return history.get(index);
		}

	}

	public T getLast() {
		synchronized (history) {
			if (history.isEmpty()) {
				return defalutT;
			}
			return history.get(history.size() - 1);
		}

	}

}
