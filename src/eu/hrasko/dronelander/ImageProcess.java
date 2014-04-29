package eu.hrasko.dronelander;

import java.awt.image.BufferedImage;
import java.nio.file.Files;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class ImageProcess {

	/**
	 * converts image from rgb to hsv color space
	 */
	public static Mat rgb2hsv(Mat rgb) {

		Mat hsv = new Mat(rgb.size(), CvType.CV_8U);
		Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_BGR2HSV, 3);

		return hsv;
	}

	/**
	 * converts image from hsv to rgb color space
	 */
	public static Mat hsv2rgb(Mat hsv) {
		Mat rgb = new Mat(hsv.size(), CvType.CV_8UC3);
		Imgproc.cvtColor(hsv, rgb, Imgproc.COLOR_HSV2BGR, 0);

		return rgb;
	}

	/**
	 * provides HSV filtration by given HSV parameters
	 */
	public static Mat hsvExtract(Mat hsvImg, HSVparams params) {
		Mat extracted = new Mat(hsvImg.size(), CvType.CV_8U);
		Core.inRange(hsvImg, new Scalar(params.hLower, params.sLower, params.vLower), new Scalar(params.hUpper, params.sUpper, params.vUpper), extracted);
		return extracted;
	}

	/**
	 * converts matrix to BufferedImage
	 */
	public static BufferedImage mat2BufferedImage(Mat img) {

		if (img == null) {
			return null;
		}

		BufferedImage bi = new BufferedImage(img.width(), img.height(), BufferedImage.TYPE_INT_BGR);

		int[] bgr = new int[img.width() * img.height()];

		int index = 0;
		for (int i = 0; i < img.rows(); i++) {
			for (int j = 0; j < img.cols(); j++) {
				byte[] data = new byte[3];

				img.get(i, j, data);

				int blue = (data[0] & 0xFF) << 16;
				int green = (data[1] & 0xFF) << 8;
				int red = (data[2] & 0xFF);

				bgr[index++] = blue | green | red;
			}
		}

		bi.setRGB(0, 0, img.width(), img.height(), bgr, 0, img.width());

		return bi;

	}

	/**
	 * loads image from disk to matrix
	 */
	public static Mat loadImage(String path) {
		Mat image = Highgui.imread(path, Highgui.CV_LOAD_IMAGE_COLOR);
		Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2BGR);
		if (image == null) {
			throw new IllegalArgumentException(path);
		}
		return image;
	}

	/**
	 * displays image stored matrix
	 */
	public static void showImage(Mat img) {
		ImageProcess.showImage(mat2BufferedImage(img));
	}

	/**
	 * displays image stored as BufferedImage
	 */
	public static void showImage(BufferedImage img) {
		ImageIcon icon = new ImageIcon(img);
		JLabel label = new JLabel(icon);
		JOptionPane.showMessageDialog(null, label);
	}

	/**
	 * converts BufferedImage to matrix
	 */
	public static Mat bufferedImage2Mat(BufferedImage b) {

		if (b == null) {
			return null;
		}

		Mat mat = new Mat(b.getHeight(), b.getWidth(), CvType.CV_8UC3);

		int[] rgb = new int[b.getWidth() * b.getHeight()];
		b.getRGB(0, 0, b.getWidth(), b.getHeight(), rgb, 0, b.getWidth());

		int index = 0;
		for (int i = 0; i < mat.rows(); i++) {
			for (int j = 0; j < mat.cols(); j++) {

				int ia = rgb[index++];
				byte[] data = new byte[3];

				// RGB to BGR
				data[0] = (byte) (ia >> 16); // R
				data[1] = (byte) (ia >> 8); // G
				data[2] = (byte) ia; // B

				mat.put(i, j, data);
			}
		}

		return mat;

	}

}
