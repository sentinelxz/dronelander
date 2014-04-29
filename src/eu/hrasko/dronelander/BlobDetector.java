package eu.hrasko.dronelander;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.highgui.Highgui;

public class BlobDetector {

	private FeatureDetector blobDetector;

	/**
	 * Class used for detecting blobs in images. Loads settings from file with
	 * name "blobparams".
	 */
	public BlobDetector() {
		blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
		blobDetector.read("blobparams");
	}

	/**
	 * Detects blobs in the image
	 * 
	 * @param image
	 *            image used for detecting blobs
	 * @return List of Coordinates of found blobs
	 */
	public List<Coordinate> detect(Mat image) {
		MatOfKeyPoint keypoints = new MatOfKeyPoint();

		blobDetector.detect(image, keypoints);

		List<Coordinate> coor = new ArrayList<Coordinate>();
		for (KeyPoint kp : keypoints.toArray()) {
			Coordinate c = new Coordinate(kp);
			coor.add(c);
		}

		return coor;
	}

}
