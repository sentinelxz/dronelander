package eu.hrasko.dronelander;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Queue;

import org.LiveGraph.dataFile.write.DataStreamWriter;
import org.LiveGraph.dataFile.write.DataStreamWriterFactory;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.NavData;
import com.codeminders.ardrone.NavDataListener;

public class PIDAxisController /* implements NavDataListener */{

	private ARDrone drone;
	private char axis;
	private long startMillis;
	public int freq = 200;
	private DoubleLogger errorHistory;
	private DataLogger<Boolean> isRealPositionHistory;
	public double derivativeThreshold = 0.2;

	public volatile Float moveBy = 0f;

	// LOGGER SETTINGS
	public String data_dir = System.getProperty("user.dir");
	private DataStreamWriter log_writer;
	public boolean writeToLog = true;

	// OUR PID CONSTANTS
	public float P = 0.25f; // Proportional gain
	public float I = 0;// .008f; // Integral gain
	public float D = 4.5f; // Derivative gain

	// CONSTRUCTOR
	/**
	 * Implements PID controller axis - name of the controlled axis (used only
	 * for logging)
	 */
	public PIDAxisController(char axis, ARDrone drone) {
		this.drone = drone;
		errorHistory = new DoubleLogger();
		isRealPositionHistory = new DataLogger<Boolean>();
		this.axis = axis;
		startMillis = System.currentTimeMillis();
		CreateGraphWriterSetting();
	}

	/**
	 * sets PID constants
	 */
	public void setPID(float P, float I, float D) {
		this.P = P;
		this.I = I;
		this.D = D;
	}

	/**
	 * computes and returns new regulating value for drone from new error data
	 */
	public float recompute(double target, boolean isEstimated) {
		// ZERO is our still setpoint
		errorHistory.add(target);
		isRealPositionHistory.add(!isEstimated);

		moveBy = (float) getControlOutput(); // GET ERROR WITH NEW
												// MEASUREMENTS

		if (isEstimated) {
			moveBy /= 2;
		}
		// moveBy = moveBy / 15; // ADITIONALY EDIT GAIN
		moveBy = Math.min(moveBy, 1f);
		moveBy = Math.max(moveBy, -1f);

		if (writeToLog) {
			log_writer.setDataValue(moveBy);
			log_writer.setDataValue(isEstimated);
			log_writer.writeDataSet();
		}

		return moveBy;

	}

	/**
	 * computes PID terms
	 */
	private double getControlOutput() {

		double lastError = errorHistory.getArithmeticAverage(1);

		// ZERO is our still setpoint
		double error = lastError;
		double drift = getSensorDrift();
		// errorHistory.add(error);

		if (writeToLog) {
			log_writer.setDataValue(System.currentTimeMillis() - startMillis);
			log_writer.setDataValue(error);
			log_writer.setDataValue(drift);
		}

		double pe = processPropotionalError(error);
		double ie = processIntegralError(error);
		double de = processDerivativeError(error);

		double output = pe + ie + de;

		if (writeToLog) {
			log_writer.setDataValue(pe);
			log_writer.setDataValue(ie);
			log_writer.setDataValue(de);
			log_writer.setDataValue(output);
		}

		return output;
	}

	/**
	 * resets controller
	 */
	public void reset() {
		errorHistory.add(0d);
		isRealPositionHistory.add(true);
		Isum = 0;
	}

	/**
	 * computes proportional error
	 */
	private double processPropotionalError(double error) {
		return error * P;
	}

	private double Isum = 0;

	/**
	 * computes integrative error
	 */
	private double processIntegralError(double newError) {

		Isum += newError;

		return Isum * I;
	}

	// DoubleLogger derivativeLog = new DoubleLogger();
	private boolean lastWasEstimated = false;
	private double lastDerivative = 0d;
	private double lastError = 0;

	/**
	 * computes derivative error
	 */
	private double processDerivativeError(double newError) {
		double k = 0;

		if (lastWasEstimated && isRealPositionHistory.getLast()) {
			k = lastDerivative;
			lastWasEstimated = false;
		} else {
			if (errorHistory.size() >= 2) {
				k = newError - lastError;
			}
			lastWasEstimated = !isRealPositionHistory.getLast();
		}

		lastError = newError;
		lastDerivative = k;

		if (Math.abs(k * D) >= derivativeThreshold) {
			k = 0;
		}

		return Math.min(0.2, Math.max(-0.2, k * D));

	}

	/**
	 * returns computed sensor drift
	 */
	private double getSensorDrift() {
		if (errorHistory.isEmpty()) {
			return 0;
		}
		return Isum / errorHistory.size();
	}

	// GRAPH WRITER
	void CreateGraphWriterSetting() {

		File f = new File(axis + "-PIDlog.lgdat");
		if (f.exists()) {
			boolean deleted = f.delete();
			System.out.println(f.getName() + " shoud be deleted: " + deleted);
		}

		// Print a welcome message:
		System.out.println(axis + "-PIDlogger started");

		startMillis = System.currentTimeMillis();
		// Setup a data writer object:
		log_writer = DataStreamWriterFactory.createDataWriter(data_dir, axis + "-PIDlog");

		// Set a values separator:
		log_writer.setSeparator(";");

		// Add a file description line:
		log_writer.writeFileInfo(axis + "-PID controller log");

		// Set-up the data series:
		log_writer.addDataSeries("Time");

		log_writer.addDataSeries("RawError");

		log_writer.addDataSeries("Sensor Drift");

		log_writer.addDataSeries("P error");
		log_writer.addDataSeries("I error");
		log_writer.addDataSeries("D error");
		log_writer.addDataSeries("Controller Output");
		log_writer.addDataSeries("Move Percentual");
		log_writer.addDataSeries("Is Estimated");

	}

}
