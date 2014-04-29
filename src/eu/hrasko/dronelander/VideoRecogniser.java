package eu.hrasko.dronelander;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.text.Utilities;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.*;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.highgui.*;
import org.opencv.features2d.*;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.DroneVideoListener;
import com.codeminders.ardrone.ARDrone.State;
import com.codeminders.ardrone.ARDrone.VideoChannel;
import com.codeminders.controltower.VideoPanel;

import eu.hrasko.dronelander.DroneLander;

public class VideoRecogniser {

	DroneLander droneLander;
	ARDrone drone;
	Mat heliport;
	boolean preserveAspect = true;
	BlobDetector bdetector;

	public static final int cameraXres = 176;
	public static final int cameraYres = 144;
	public static final int cameraXangle = 45;
	public static final int cameraYangle = 37;
	public static final double cameraPixelAngleRatio = cameraXres / (double) cameraXangle;

	public boolean freezeSwitch = false;

	// public boolean estimateTargetLocation = false;
	public long estimatedTargetMaxTime = 60; // IN CYCLES
	private long estimatedTargetTime = 0;
	Coordinate lastTargetPosition = null;

	HSVparams firstHSV;
	HSVparams secondHSV;

	Mat firstImage = null;
	Mat secondImage = null;
	Mat thirdImage = null;
	Mat thirdImageBackup = null;

	public VideoRecogniser(DroneLander droneLander) {

		firstHSV = HSVparams.load("leftHSV");
		secondHSV = HSVparams.load("rightHSV");

		this.droneLander = droneLander;
		this.drone = droneLander.drone;
		bdetector = new BlobDetector();
	}

	/**
	 * detects the landing point position
	 */
	public Coordinate findTargetByBlobDetection(BufferedImage bimg) {

		if (!freezeSwitch) {
			thirdImage = ImageProcess.bufferedImage2Mat(bimg);
			thirdImageBackup = thirdImage.clone();
		} else {
			thirdImage = thirdImageBackup.clone();
		}

		Mat hsv = ImageProcess.rgb2hsv(thirdImage);
		Mat first = ImageProcess.hsvExtract(hsv, firstHSV);
		Mat second = ImageProcess.hsvExtract(hsv, secondHSV);
		// Imgproc.erode(first, first, new Mat(), new Point(-1, -1), 3);
		// Imgproc.erode(second, second, new Mat(), new Point(-1, -1), 3);
		Imgproc.dilate(first, first, new Mat(), new Point(-1, -1), 3);
		Imgproc.dilate(second, second, new Mat(), new Point(-1, -1), 3);

		firstImage = first;
		secondImage = second;

		List<Coordinate> detectedFirst = bdetector.detect(first);
		List<Coordinate> detectedSecond = bdetector.detect(second);

		CoordinatePair bestPair = findNearestTarget(CoordinatePair.getAllPairs(detectedFirst, detectedSecond), lastTargetPosition);
		// findSimillarBlobs(detectedFirst, detectedSecond);

		if (bestPair == null && droneLander.controller.estimateTargetSwitch && drone.isFlying() && estimatedTargetMaxTime > estimatedTargetTime) {
			estimatedTargetTime++;
			if (lastTargetPosition == null) {
				return null;
			}
			lastTargetPosition = getEstimatedTargetLocation(lastTargetPosition);
			lastTargetPosition.isEstimated = true;
			return lastTargetPosition.clone();
		}

		if (bestPair != null /* && drone.isFlying() */) {

			estimatedTargetTime = 0;

			Coordinate c1 = bestPair.c1;
			Core.circle(firstImage, c1.getPoint(), 3, new Scalar(255, 0, 0));

			Coordinate c2 = bestPair.c2;
			Core.circle(secondImage, c2.getPoint(), 3, new Scalar(255, 0, 0));

			Coordinate m = c1.getMidpoint(c2);

			Core.line(firstImage, c1.getPoint(), m.getPoint(), new Scalar(255, 0, 0));
			Core.line(secondImage, c2.getPoint(), m.getPoint(), new Scalar(255, 0, 0));

			if (droneLander.controller.isWorking(4))
				Core.circle(thirdImage, m.getPoint(), 3, new Scalar(0, 255, 0));
			else
				Core.circle(thirdImage, m.getPoint(), 3, new Scalar(255, 0, 0));

			m.x -= cameraXres / 2;
			m.y -= cameraYres / 2;

			m.angle = new Coordinate(c2.x - c1.x, c2.y - c1.y).getAngleFromZero();

			lastTargetPosition = m;
			lastTargetPosition.isEstimated = false;
			return m.clone();
		}
		if (bestPair != null) {
			System.out.println("This should not happen!");
		}

		return null;

	}

	/**
	 * finds the nearest target from the last target
	 */
	public static CoordinatePair findNearestTarget(List<CoordinatePair> pairs, Coordinate target) {
		if (pairs.isEmpty()) {
			return null;
		}
		if (target == null) {
			return findSimillarBlobs(pairs);
		}

		CoordinatePair nearest = pairs.get(0);
		double minDist = target.distanceTo(nearest.getMidPoint());

		for (CoordinatePair cp : pairs) {
			double dist = target.distanceTo(cp.getMidPoint());
			if (minDist > dist) {
				nearest = cp;
				minDist = dist;
			}
		}

		return nearest;
	}

	/**
	 * returns the most matching blobs
	 */
	public static CoordinatePair findSimillarBlobs(List<CoordinatePair> pairs) {

		// List<CoordinatePair> list = CoordinatePair.getAllPairs(list1, list2);

		Collections.sort(pairs, CoordinatePair.getSizeDifferenceComparator());
		return pairs.isEmpty() ? null : pairs.get(0);

	}

	/**
	 * returns the accelerometer estimated location if target is not visible
	 */
	public Coordinate getEstimatedTargetLocation(Coordinate last) {
		// estimate by odometer
		double xDistance = droneLander.tachoMeter.getDistance('X', 1000 / droneLander.videoFrequency);
		double yDistance = droneLander.tachoMeter.getDistance('Y', 1000 / droneLander.videoFrequency);
		double angle = last.angle;
		// estimate by drone rotation
		List<Double> yawh = droneLander.tachoMeter.getHistory('w').getLastSet(1000 / droneLander.videoFrequency);
		if (!yawh.isEmpty()) {
			double angleDiff = yawh.get(yawh.size() - 1) - yawh.get(0);

			angle += angleDiff;

			xDistance = (xDistance * Math.cos(angleDiff)) - (yDistance * Math.sin(angleDiff));
			yDistance = (xDistance * Math.sin(angleDiff)) + (yDistance + Math.cos(angleDiff));
		}

		Coordinate c = last.clone();
		// needed in meters
		c.x += xDistance / 1000;
		c.y += yDistance / 1000;
		c.angle = angle;

		return c;
	}

}
