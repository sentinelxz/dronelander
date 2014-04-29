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
import com.googlecode.javacv.ObjectFinder;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class MyObjectFinder extends ObjectFinder {

	public MyObjectFinder(IplImage objectImage) {
		super(objectImage);
	}

	public MyObjectFinder(Settings s) {
		super(s);
	}

	public static double[] getObjectCorners(IplImage object, IplImage image) {

		// IplImage object = cvLoadImage(objectFilename,
		// CV_LOAD_IMAGE_GRAYSCALE);
		// IplImage image = cvLoadImage(sceneFilename, CV_LOAD_IMAGE_GRAYSCALE);

		if (object == null || image == null) {
			System.err.println("Incorrect images!");
			System.exit(-1);
		}

		IplImage objectColor = IplImage.create(object.width(), object.height(), 8, 3);
		cvCvtColor(object, objectColor, CV_GRAY2BGR);

		IplImage correspond = IplImage.create(image.width(), object.height() + image.height(), 8, 1);
		cvSetImageROI(correspond, cvRect(0, 0, object.width(), object.height()));
		cvCopy(object, correspond);
		cvSetImageROI(correspond, cvRect(0, object.height(), correspond.width(), correspond.height()));
		cvCopy(image, correspond);
		cvResetImageROI(correspond);

		ObjectFinder.Settings settings = new ObjectFinder.Settings();
		settings.setObjectImage(object);
		settings.setUseFLANN(true);
		settings.setRansacReprojThreshold(500);
		ObjectFinder finder = new ObjectFinder(settings);

		long start = System.currentTimeMillis();
		double[] dst_corners = finder.find(image);
		System.out.println("Finding time = " + (System.currentTimeMillis() - start) + " ms");

		if (dst_corners != null) {
			for (int i = 0; i < 4; i++) {
				int j = (i + 1) % 4;
				int x1 = (int) Math.round(dst_corners[2 * i]);
				int y1 = (int) Math.round(dst_corners[2 * i + 1]);
				int x2 = (int) Math.round(dst_corners[2 * j]);
				int y2 = (int) Math.round(dst_corners[2 * j + 1]);
				cvLine(correspond, cvPoint(x1, y1 + object.height()), cvPoint(x2, y2 + object.height()), CvScalar.WHITE, 1, 8, 0);
			}
		}

		// for (int i = 0; i < finder.ptpairs.size(); i += 2) {
		// CvPoint2D32f pt1 =
		// finder.objectKeypoints[finder.ptpairs.get(i)].pt();
		// CvPoint2D32f pt2 =
		// finder.imageKeypoints[finder.ptpairs.get(i+1)].pt();
		// cvLine(correspond, cvPointFrom32f(pt1),
		// cvPoint(Math.round(pt2.x()), Math.round(pt2.y()+object.height())),
		// CvScalar.WHITE, 1, 8, 0);
		// }
		//
		CanvasFrame objectFrame = new CanvasFrame("Object");
		CanvasFrame correspondFrame = new CanvasFrame("Object Correspond");
		//
		// correspondFrame.showImage(correspond);
		// for (int i = 0; i < finder.objectKeypoints.length; i++ ) {
		// CvSURFPoint r = finder.objectKeypoints[i];
		// CvPoint center = cvPointFrom32f(r.pt());
		// int radius = Math.round(r.size()*1.2f/9*2);
		// cvCircle(objectColor, center, radius, CvScalar.RED, 1, 8, 0);
		// }
		objectFrame.showImage(correspond);

		try {
			objectFrame.waitKey();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		objectFrame.dispose();
		correspondFrame.dispose();
		return dst_corners;
	}

}
