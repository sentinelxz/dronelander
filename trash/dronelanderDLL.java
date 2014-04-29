package eu.hrasko.dronelander;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.androidIncludepath;
import static com.googlecode.javacv.cpp.opencv_core.androidLinkpath;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_core.genericIncludepath;
import static com.googlecode.javacv.cpp.opencv_core.genericLinkpath;
import static com.googlecode.javacv.cpp.opencv_core.windowsIncludepath;
import static com.googlecode.javacv.cpp.opencv_core.windowsx64Linkpath;
import static com.googlecode.javacv.cpp.opencv_core.windowsx64Preloadpath;
import static com.googlecode.javacv.cpp.opencv_core.windowsx86Linkpath;
import static com.googlecode.javacv.cpp.opencv_core.windowsx86Preloadpath;
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

import com.googlecode.javacpp.FunctionPointer;
import com.googlecode.javacpp.Pointer;
import com.googlecode.javacpp.PointerPointer;
import com.googlecode.javacpp.annotation.ByPtrPtr;
import com.googlecode.javacpp.annotation.ByVal;
import com.googlecode.javacpp.annotation.Cast;
import com.googlecode.javacpp.annotation.Opaque;
import com.googlecode.javacpp.annotation.Platform;
import com.googlecode.javacpp.annotation.Properties;
import static com.googlecode.javacpp.Loader.*;

@Platform(include = "dronelanderDLL.h")
@Namespace("std")
public class dronelanderDLL {

	// public static native IplImage matchTemplate(IplImage image, IplImage
	// template);

	public static class recogniser {
		static {
			Loader.load();
		}

		public recogniser() {
			allocate();
		}

		private native void allocate();

		public static native int getWtf();
	}

	// native public static int test(Pointer<CvArr> image);

	public static void main(String[] args) {
		System.out.println("init");
		// dronelanderDLL lander = new dronelanderDLL();
		// System.out.println();
		recogniser dll = new recogniser();
		System.out.println(recogniser.getWtf());

		// IplImage image = cvLoadImage("heliport.jpg");
		// cvShowImage("java", image);
		// cvWaitKey();
		//
		//

		// System.out.println(image.position());

	}

}
