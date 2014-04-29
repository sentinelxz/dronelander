package eu.hrasko.dronelander;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.LiveGraph.dataFile.write.DataStreamWriter;
import org.LiveGraph.dataFile.write.DataStreamWriterFactory;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.NavData;
import com.codeminders.ardrone.NavDataListener;

public class Tachometer implements NavDataListener {

	/**
	 * Class storing and computing flight data of the drone
	 */
	public Tachometer(DroneLander droneLander) {
		droneLander.drone.addNavDataListener(this);
		this.drone = droneLander.drone;
		this.droneLander = droneLander;
		lastTick = System.nanoTime();
	}

	ARDrone drone;
	DroneLander droneLander;
	public long navDataFrequency = 30;
	public boolean write_data = true;
	public String data_dir = System.getProperty("user.dir");
	private DataStreamWriter log_writer;
	private long startMillis;

	// most MEASUREMENTS ARE IN milimeters per second
	private DoubleLogger xHistory = new DoubleLogger();
	private DoubleLogger yHistory = new DoubleLogger();
	private DoubleLogger altitudeHistory = new DoubleLogger();
	private DoubleLogger yawHistory = new DoubleLogger();
	private DoubleLogger pitchHistory = new DoubleLogger();
	private DoubleLogger rollHistory = new DoubleLogger();

	private DataLogger<Integer> batteryHistory = new DataLogger<Integer>();
	private DataLogger<String> modeHistory = new DataLogger<String>();
	private DataLogger<String> stateHistory = new DataLogger<String>();

	private long lastTick = 0;

	@Override
	public void navDataReceived(NavData nd) {

		long now = System.nanoTime();
		long tickWasBefore = now - lastTick;
		navDataFrequency = 1000000 / tickWasBefore;
		lastTick = now;

		xHistory.add(nd.getVx());
		yHistory.add(nd.getLongitude());
		altitudeHistory.add(nd.getAltitude()); // in meters
		yawHistory.add(nd.getYaw());
		pitchHistory.add(nd.getPitch());
		rollHistory.add(nd.getRoll());
		batteryHistory.add(nd.getBattery());
		modeHistory.add(nd.getMode().toString());
		stateHistory.add(nd.getFlyingState().toString());

		pushToLog(nd);

	}

	/**
	 * returns travelled distance for last forTimeInMs milliseconds
	 */
	public double getDistance(char axis, long forTimeInMs) {

		long howmany = (long) (forTimeInMs * navDataFrequency / 1000f);
		if (howmany == 0)
			howmany = 1;

		double avg = getHistory(axis).getArithmeticAverage(howmany);

		avg = avg * howmany;

		return avg;
	}

	/**
	 * Class implementing functios for measuring travelled distance
	 */
	public class OdoMeter implements NavDataListener {

		private double distance;
		private int measureEvery;
		private int zeroToMeasure;
		private ARDrone drone;
		private char axis;
		private int ms;

		public OdoMeter(char axis, ARDrone drone, int averageMeasureEveryMS) {
			ms = averageMeasureEveryMS;
			measureEvery = averageMeasureEveryMS / 5;
			zeroToMeasure = measureEvery;
		}

		public void start() {
			drone.addNavDataListener(this);
		}

		@Override
		public void navDataReceived(NavData nd) {

			zeroToMeasure--;

			if (zeroToMeasure == 0) {
				zeroToMeasure = measureEvery;

				distance += droneLander.tachoMeter.getDistance(axis, ms);
			}
		}

		public double getDistance() {
			return distance;
		}

		public void stop() {
			drone.removeNavDataListener(this);
		}

	}

	/**
	 * returns log of required history
	 */
	public DoubleLogger getHistory(char axis) {
		DoubleLogger history;

		switch (axis) {
		case 'X':
			history = xHistory;
			break;
		case 'Y':
			history = yHistory;
			break;
		case 'a':
			history = altitudeHistory;
			break;
		case 'w':
			history = yawHistory;
			break;
		case 'y':
			System.err.println("CHYBA. Y tachometer neexistuje");
			return null;
		case 'p':
			history = pitchHistory;
			break;
		case 'r':
			history = rollHistory;
			break;
		default:
			return null;
		}

		return history;
	}

	/**
	 * returns last status of the drone
	 */
	public Coordinate getLastPosition(int averageN) {
		Coordinate c = new Coordinate();
		c.pich = getHistory('p').getMedianAverage(averageN);
		c.roll = getHistory('r').getMedianAverage(averageN);
		c.yaw = getHistory('w').getMedianAverage(averageN);
		c.altitude = getHistory('a').getMedianAverage(averageN);

		return c;
	}

	/**
	 * returns actual pitch of the drone
	 */
	public double pitch() {
		return getHistory('p').getMedianAverage(3);
	}

	/**
	 * returns roll pitch of the drone
	 */
	public double roll() {
		return getHistory('r').getMedianAverage(3);
	}

	/**
	 * returns actual yaw of the drone
	 */
	public double yaw() {
		return getHistory('w').getMedianAverage(1);
	}

	/**
	 * returns altitude of the drone in meters
	 */
	public double altitude() {
		return getHistory('a').getMedianAverage(9);
	}

	public void pushToLog(NavData navdata) {
		if (log_writer == null) {
			createGraphWriterSetting();
		}

		// Set-up the data values:
		log_writer.setDataValue(System.currentTimeMillis() - startMillis);
		log_writer.setDataValue(getHistory('X').getMedianAverage(3));
		log_writer.setDataValue(getHistory('Y').getMedianAverage(3));

		log_writer.setDataValue(getDistance('X', (System.currentTimeMillis() - startMillis) / 5));
		log_writer.setDataValue(getDistance('Y', (System.currentTimeMillis() - startMillis) / 5));

		log_writer.setDataValue(navdata.getAltitude());
		log_writer.setDataValue(navdata.getPitch());
		log_writer.setDataValue(navdata.getRoll());
		log_writer.setDataValue(navdata.getYaw());

		log_writer.setDataValue(navdata.getBattery());

		// Write dataset to disk:
		log_writer.writeDataSet();

		// Check for IOErrors:
		if (log_writer.hadIOException()) {
			log_writer.getIOException().printStackTrace();
			log_writer.resetIOException();
		}
	}

	// GRAPH WRITER
	void createGraphWriterSetting() {

		File f = new File("Speedometer.lgdat");
		if (f.exists()) {
			f.delete();
		}

		// Print a welcome message:
		System.out.println("Welcome to the LiveLog demo.");

		startMillis = System.currentTimeMillis();
		// Setup a data writer object:
		log_writer = DataStreamWriterFactory.createDataWriter(data_dir, "Speedometer");

		// Set a values separator:
		log_writer.setSeparator(";");

		// Add a file description line:
		log_writer.writeFileInfo("AR.Drone speeds log");

		// Set-up the data series:
		log_writer.addDataSeries("Time");
		log_writer.addDataSeries("X speed");
		log_writer.addDataSeries("Y speed");

		log_writer.addDataSeries("X travelled");
		log_writer.addDataSeries("Y travelled");

		log_writer.addDataSeries("Altitude");
		log_writer.addDataSeries("Pitch");
		log_writer.addDataSeries("Roll");
		log_writer.addDataSeries("Yaw");
		log_writer.addDataSeries("Battery");

	}
}
