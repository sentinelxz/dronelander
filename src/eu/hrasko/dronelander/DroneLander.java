package eu.hrasko.dronelander;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.video.Video;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.ARDrone.VideoChannel;
import com.codeminders.ardrone.DroneVideoListener;
import com.codeminders.ardrone.NavDataListener;
import com.codeminders.ardrone.controllers.Controller;
import com.codeminders.ardrone.controllers.KeyboardController;
import com.codeminders.controltower.VideoPanel;

public class DroneLander implements Runnable, DroneVideoListener {

	ARDrone drone;
	Status status;

	DroneLanderWindow window;
	VideoRecogniser videoRecogniser;
	BufferedImage image = null;

	public Tachometer tachoMeter;
	public DroneController controller;
	KeyboardController keyboardController;
	long videoFrequency = 20;
	private long lastTick = 0;

	public volatile long landingAngle = 90;

	public enum Status {
		DISCONNECTED, CONNECTING, BOOTSTRAP, DEMO, ERROR, TAKING_OFF, LANDING, LOCKED_ON_TARGET, LANDING_ON_TARGET;

		// public Status(ARDrone drone) {
		//
		// }

		// public void change(Status status)
		// {
		// if()
		// }
	}

	public DroneLander(ARDrone dr, KeyboardController kbc) {

		drone = dr;
		this.keyboardController = kbc;
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("Loaded OpenCV " + Core.VERSION);

		videoRecogniser = new VideoRecogniser(this);
		controller = new DroneController(this);

		tachoMeter = new Tachometer(this);

		try {
			drone.selectVideoChannel(VideoChannel.VERTICAL_ONLY);
		} catch (IOException e) {
			e.printStackTrace();
		}

		lastTick = System.currentTimeMillis();
	}

	@Override
	public void frameReceived(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {

		long now = System.currentTimeMillis();

		lastTick = now;

		image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		image.setRGB(startX, startY, w, h, rgbArray, offset, scansize);

		update();
	}

	/**
	 * main update loop when new data arrives
	 */
	public void update() {
		Coordinate target = videoRecogniser.findTargetByBlobDetection(image);
		controller.recompute(target);

		Mat left = videoRecogniser.firstImage;
		Mat middle = videoRecogniser.secondImage;
		Mat right = videoRecogniser.thirdImage;

		refreshVideoPanel(window.leftVideoPanel, left);
		refreshVideoPanel(window.middleVideoPanel, middle);
		refreshVideoPanel(window.rightVideoPanel, right);

		window.landingTime.setText(controller.landingStopWatch.getTime() + "");

	}

	void refreshVideoPanel(VideoPanel panel, Mat image) {
		refreshVideoPanel(panel, ImageProcess.mat2BufferedImage(image));
	}

	void refreshVideoPanel(VideoPanel panel, BufferedImage image) {
		if (image == null) {
			panel.image.set(this.image);
		} else {
			panel.image.set(image);
		}
		panel.repaint();
	}

	@Override
	public void run() {

		System.out.println("DRONE LANDER STARTED");
		// loadHeliportIfExists();
		window = new DroneLanderWindow(this);
		new Thread(window).start();
		drone.addImageListener(this);
	}

	/**
	 * disconnects listeners from drone
	 */
	public void close() {

		try {
			drone.selectVideoChannel(VideoChannel.HORIZONTAL_ONLY);
		} catch (IOException e) {
			e.printStackTrace();
		}
		drone.removeImageListener(this);

		window.rightVideoPanel.removeAll();
		window.middleVideoPanel.removeAll();
		window.leftVideoPanel.removeAll();

		controller = null;
	}

}
