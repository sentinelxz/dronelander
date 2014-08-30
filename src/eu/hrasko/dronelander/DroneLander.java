package eu.hrasko.dronelander;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
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
	Object imageMutex = new Object();
	long imageCounter = Long.MIN_VALUE;

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

	Runnable computationRunnable = new Runnable() {

		@Override
		public void run() {
			long lastImageCounter = Long.MIN_VALUE;

			while (true) {
				synchronized (imageMutex) {
					if (lastImageCounter == imageCounter) {
						try {
							imageMutex.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				lastImageCounter = imageCounter;
				Coordinate target = videoRecogniser.findTargetByBlobDetection(image);
				controller.recompute(target);
			}
		}
	};
	Thread computationThread = null;

	/**
	 * main update loop when new data arrives
	 */
	public void update() {

		if (computationThread == null) {
			computationThread = new Thread(computationRunnable);
			computationThread.setName("Computer Vision and Controlling Thread");
			computationThread.start();
			return;
		}

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

		computationThread.interrupt();
		computationThread = null;

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

	@Override
	public void frameRecieved(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {
		long now = System.currentTimeMillis();

		lastTick = now;

		image = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
		image.setRGB(startX, startY, w, h, rgbArray, offset, scansize);

		update();

	}

	@Override
	public void frameRecieved(BufferedImage im) {
		long now = System.currentTimeMillis();
		lastTick = now;
		image = toBufferedImage(im.getScaledInstance(176, 144, Image.SCALE_FAST));
		synchronized (imageMutex) {
			imageCounter++;
			imageMutex.notifyAll();
		}
		update();

	}

	/**
	 * Converts a given Image into a BufferedImage
	 * 
	 * @param img
	 *            The Image to be converted
	 * @return The converted BufferedImage
	 */
	public static BufferedImage toBufferedImage(Image img) {
		if (img instanceof BufferedImage) {
			return (BufferedImage) img;
		}

		// Create a buffered image with transparency
		BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

		// Draw the image on to the buffered image
		Graphics2D bGr = bimage.createGraphics();
		bGr.drawImage(img, 0, 0, null);
		bGr.dispose();

		// Return the buffered image
		return bimage;
	}

}
