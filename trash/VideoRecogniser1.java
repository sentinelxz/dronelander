package eu.hrasko.dronelander;

import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_highgui.CV_LOAD_IMAGE_COLOR;
import static com.googlecode.javacv.cpp.opencv_highgui.cvLoadImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GRAY2BGR;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_CCORR_NORMED;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;
import com.googlecode.javacv.JavaCV;
import com.googlecode.javacv.ObjectFinder;
import com.googlecode.javacv.cpp.opencv_core.*;
import com.googlecode.javacv.cpp.*;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.*;
import com.googlecode.javacpp.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import com.googlecode.javacpp.Loader;
import static com.googlecode.javacpp.Loader.*;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_features2d.Feature2D;

import com.googlecode.javacpp.annotation.Adapter;
import com.googlecode.javacpp.annotation.ByPtrPtr;
import com.googlecode.javacpp.annotation.ByRef;
import com.googlecode.javacpp.annotation.ByVal;
import com.googlecode.javacpp.annotation.Cast;
import com.googlecode.javacpp.annotation.Const;
import com.googlecode.javacpp.annotation.Convention;
import com.googlecode.javacpp.annotation.Index;
import com.googlecode.javacpp.annotation.MemberGetter;
import com.googlecode.javacpp.annotation.MemberSetter;
import com.googlecode.javacpp.annotation.Name;
import com.googlecode.javacpp.annotation.Namespace;
import com.googlecode.javacpp.annotation.NoOffset;
import com.googlecode.javacpp.annotation.Opaque;
import com.googlecode.javacpp.annotation.Platform;
import com.googlecode.javacpp.annotation.Properties;
import com.googlecode.javacpp.annotation.ValueGetter;
import com.googlecode.javacpp.annotation.ValueSetter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import com.codeminders.ardrone.DroneVideoListener;
import com.codeminders.controltower.VideoPanel;
import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.CvSize;
import com.googlecode.javacv.cpp.opencv_core.CvSeq;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_features2d.KeyPoint;
import com.googlecode.javacv.cpp.opencv_legacy.CvSURFPoint;
import com.googlecode.javacv.cpp.opencv_nonfree.*;
import static com.googlecode.javacv.cpp.opencv_legacy.CvSURFParams;
//import com.googlecode.javacv.cpp.opencv_legacy.CvSURFParams;
import com.googlecode.javacv.cpp.opencv_nonfree.SIFT;
import com.googlecode.javacv.cpp.opencv_nonfree.SURF;
import com.googlecode.javacv.cpp.opencv_stitching.SurfFeaturesFinder;

import static com.googlecode.javacv.cpp.cvkernels.*;
import com.googlecode.javacv.*;

import eu.hansolo.steelseries.tools.BrushedMetalFilter;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_flann.*;






public class VideoRecogniser extends VideoPanel implements DroneVideoListener {

	BufferedImage bufferedImage;
	boolean preserveAspect = true;
	DroneLander caller;
	IplImage heliport, lastIplFrame = new IplImage();

	public VideoRecogniser(DroneLander caller) {
		this.caller = caller;

		// try {
		// caller.drone.addImageListener(this);
		// } catch (Exception e) {
		// System.err.println("Error while connecting to drone.");
		// }

		loadHeliportIfExists();
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int width = getWidth();
		int height = getHeight();
		drawDroneImage(g2d, width, height);
	}

	@Override
	public void frameReceived(int startX, int startY, int w, int h, int[] rgbArray, int offset, int scansize) {

		bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
		bufferedImage.setRGB(startX, startY, w, h, rgbArray, offset, scansize);
		// convertedImg.getGraphics().drawImage(bufferedImage, 0, 0, null);
		// bufferedImage = convertedImg;

		lastIplFrame = IplImage.createFrom(bufferedImage);
		// cvCvtColor(lastIplFrame, lastIplFrame, CV_RGB2BGR);
		// showImg(lastIplFrame, 10000, "name");

		// CvSize size = cvSize(w, h);
		// IplImage dest_img = IplImage.create(size, lastIplFrame.depth(), 1);

		IplImage load = cvLoadImage("shot.png", CV_LOAD_IMAGE_GRAYSCALE);

		featureDetection(load, heliport);
		// IplImage recognised = matchTemplate(lastIplFrame, heliport);
		// recognised = markRecognisedPlaces(recognised, lastIplFrame,
		// heliport);
		// bufferedImage = recognised.getBufferedImage();

		repaint();

	}

	private void drawDroneImage(Graphics2D g2d, int width, int height) {

		if (bufferedImage == null) {
			return;
		}
		int xPos = 0;
		int yPos = 0;
		if (preserveAspect) {
			g2d.setColor(Color.BLACK);
			g2d.fill3DRect(0, 0, width, height, false);
			float widthUnit = ((float) width / 4.0f);
			float heightAspect = (float) height / widthUnit;
			float heightUnit = ((float) height / 3.0f);
			float widthAspect = (float) width / heightUnit;

			if (widthAspect > 4) {
				xPos = (int) (width - (heightUnit * 4)) / 2;
				width = (int) (heightUnit * 4);
			} else if (heightAspect > 3) {
				yPos = (int) (height - (widthUnit * 3)) / 2;
				height = (int) (widthUnit * 3);
			}
		}
		if (bufferedImage != null) {
			g2d.drawImage(bufferedImage, xPos, yPos, width, height, null);
		}
	}

	public static IplImage matchTemplate(IplImage image, IplImage template) {

		// IplImage image = loadImage("test5.jpg");
		// template = loadImage("heliport.jpg");

		CvSize size = cvSize(image.width() - template.width() + 1, image.height() - template.height() + 1);

		IplImage dest_img = IplImage.create(size, IPL_DEPTH_32F, 1);

		cvMatchTemplate(image, template, dest_img, CV_TM_CCORR_NORMED);

		return dest_img;
	}

	public static IplImage markRecognisedPlaces(IplImage recognised, IplImage original, IplImage template) {

		double[] min_val = new double[2];
		double[] max_val = new double[2];

		CvPoint min_loc = new CvPoint();
		CvPoint max_loc = new CvPoint();

		cvMinMaxLoc(recognised, min_val, max_val, min_loc, max_loc, null);

		IplImage clone = original.clone();
		cvRectangle(clone, max_loc, cvPoint(max_loc.x() + template.width(), max_loc.y() + template.height()), CV_RGB(255, 0, 0), 3, 1, 0);

		// cvShowImage("test", clone);
		// cvWaitKey();

		return clone;
	}

	public static IplImage SiftFeatureDetection(IplImage image, IplImage object) {
		SIFT sift = new SIFT();

		KeyPoint image_keypoints = new KeyPoint();
		sift.detect(image, null, image_keypoints);
		IplImage result = image.clone();
		// drawKeypoints(image, image_keypoints, result, CvScalar.WHITE, 5);

		// cvShowImage("res", result);
		// cvWaitKey();

		KeyPoint object_keypoints = new KeyPoint();
		sift.detect(object, null, object_keypoints);
		result = object.clone();
		// drawKeypoints(object, object_keypoints, result, CvScalar.WHITE, 5);
		// cvShowImage("res", result);
		// cvWaitKey();

		DescriptorExtractor extractor = sift.getDescriptorExtractor();
		CvMat object_descriptors = new CvMat(null);
		CvMat image_descriptors = new CvMat(null);

		extractor.compute(image, image_keypoints, image_descriptors);
		extractor.compute(object, object_keypoints, object_descriptors);

		BFMatcher matcher = new BFMatcher(NORM_L1);
		DMatch matches = new DMatch();
		matcher.match(image_descriptors, object_descriptors, matches, null);

		double max_dist = 0;
		double min_dist = 100;

		for (int i = 0; i < object_descriptors.rows(); i++) {
			double dist = matches.position(i).distance();
			if (dist < min_dist)
				min_dist = dist;
			if (dist > max_dist)
				max_dist = dist;
			System.out.println(dist);
		}

		System.out.printf("-- Max dist : %f \n", max_dist);
		System.out.printf("-- Min dist : %f \n", min_dist);

		// for (int i = 0; i < 412; i++) {
		// System.out.println("-- Good Match " + i + " Keypoint 1:  " +
		// matches.position(i).queryIdx() + "  -- Keypoint 2: " +
		// matches.position(i).trainIdx()
		// + "  \n");
		// }

		// result = object.clone();
		// drawMatches(object, object_keypoints, image, image_keypoints,
		// matches, correspond, CvScalar.BLUE, CvScalar.RED, null,
		// DrawMatchesFlags.DEFAULT);

		cvShowImage("res", result);
		cvWaitKey();

		// CvMat image_descriptors = new CvMat(null);
		// detector.detect(image, image_keypoints, null);
		// System.out.println("Keypoints found: " + image_keypoints.capacity());
		// descriptor.compute(image, image_keypoints, image_descriptors);
		// System.out.println("Descriptors calculated: " +
		// image_descriptors.rows());

		return null;
	}

	public static IplImage featureDetection(IplImage image, IplImage object) {

		SURF surf = new SURF(500d);
		FeatureDetector detector = surf.getFeatureDetector();
		DescriptorExtractor descriptor = surf.getDescriptorExtractor();

		KeyPoint image_keypoints = new KeyPoint();
		CvMat image_descriptors = new CvMat(null);
		detector.detect(image, image_keypoints, null);
		System.out.println("Keypoints found: " + image_keypoints.capacity());
		descriptor.compute(image, image_keypoints, image_descriptors);
		System.out.println("Descriptors calculated: " + image_descriptors.rows());
		drawKeypoints(image, image_keypoints, image, CvScalar.WHITE, DrawMatchesFlags.DEFAULT);

		KeyPoint object_keypoints = new KeyPoint();
		CvMat object_descriptors = new CvMat(null);
		detector.detect(object, object_keypoints, null);
		System.out.println("Keypoints found: " + object_keypoints.capacity());
		descriptor.compute(object, object_keypoints, object_descriptors);
		System.out.println("Descriptors calculated: " + object_descriptors.rows());

		// -- Step 3: Matching descriptor vectors using FLANN matcher
		FlannBasedMatcher matcher = new FlannBasedMatcher();
		DMatch matches = new DMatch();
		matcher.match(image_descriptors, object_descriptors, matches, null);

		// -- Quick calculation of max and min distances between keypoints
		double max_dist = 0;
		double min_dist = 100;

		for (int i = 0; i < object_descriptors.rows(); i++) {
			double dist = matches.position(i).distance();
			if (dist < min_dist)
				min_dist = dist;
			if (dist > max_dist)
				max_dist = dist;
			System.out.println(dist);
		}

		System.out.printf("-- Max dist : %f \n", max_dist);
		System.out.printf("-- Min dist : %f \n", min_dist);

		IplImage result = image.clone();
		drawMatches(object, object_keypoints, image, image_keypoints, matches, result, CvScalar.BLUE, CvScalar.RED, null,
				opencv_features2d.DrawMatchesFlags.DEFAULT);

		cvShowImage("res", result);
		cvWaitKey();

		return null;

	}

	public static void showImg(IplImage img, int time, String name) {
		CanvasFrame canvas = new CanvasFrame(name);
		canvas.showImage(img);

		// sleep((long) time);
		try {
			Thread.sleep((long) time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		canvas.dispose();

	}

	void loadHeliportIfExists() {
		heliport = loadImage("heliport.jpg");
		// TODO: ask user to load heliport if not found
	}

	public static IplImage loadImage(String path) {
		IplImage image = cvLoadImage(path, CV_LOAD_IMAGE_GRAYSCALE);
		if (image == null) {
			throw new IllegalArgumentException(path);
		}
		return image;

	}
}
