package eu.hrasko.dronelander;

import java.awt.image.BufferedImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
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

public class dronelanderEXE {

	enum CPPfunction {
		SHOW(0);

		private int id_;

		private CPPfunction(int i) {
			id_ = i;
		}

		public int id() {
			return id_;
		}
	}

	public static byte[] load() {

		byte readBuf[] = new byte[512 * 1024];
		byte[] byteArray = null;
		try {
			FileInputStream fin = new FileInputStream(new File("heliport.jpg"));
			ByteArrayOutputStream bout = new ByteArrayOutputStream();

			int readCnt = fin.read(readBuf);
			while (0 < readCnt) {
				bout.write(readBuf, 0, readCnt);
				readCnt = fin.read(readBuf);
			}

			fin.close();

			byteArray = bout.toByteArray();
			// for (int i = 0; i < byteArray.length; i++) {
			// System.out.print((char) byteArray[i]);
			// }

		} catch (Exception e) {
			e.printStackTrace();
		}
		return byteArray;

	}

	public class Data {
		int i = 123456;
		byte[] bytearray = { 1, 2, 3, 120, 127, 1, 2, 3, 120, 127, 1, 2, 3, 120, 127, 1, 2, 3, 120, 127, 1, 2, 3, 120, 127, 1, 2, 3, 120, 127, 1, 2, 3, 120,
				127, 1, 2, 3, 120, 127, 1, 2, 3, 120, 127 };
		String str = "ahoj";

		public Data() {
		}

	}

	public static void main(String[] args) throws Exception {

		TCP tcp = new TCP();

		// tcp.writer.writeInt(CPPfunction.SHOW.id());
		// tcp.writer.flush();

		// IplImage i = VideoRecogniser.loadImage("heliport.jpg");
		// ByteBuffer bb = i.getByteBuffer();

		// byte[] ba = load();

		Gson g = new Gson();

		Data data = new dronelanderEXE().new Data();
		String s = g.toJson(data);

		tcp.writer.println(s);
		tcp.writer.flush();

		// tcp.writer.writeInt(ba.length);

		// tcp.writer.flush();

		// tcp.writer.write(ba, 0, ba.length);

		// for (int i = 0; i < ba.length; i++) {
		// tcp.writer.print(ba[i]);
		// tcp.writer.flush();
		// }
		tcp.writer.flush();

	}
}
