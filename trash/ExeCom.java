package eu.hrasko.dronelander;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.*;
import static com.googlecode.javacv.cpp.opencv_imgproc.*;
import static com.googlecode.javacv.cpp.opencv_highgui.*;
import static com.googlecode.javacv.cpp.opencv_calib3d.*;
import static com.googlecode.javacv.cpp.opencv_features2d.*;
import static com.googlecode.javacv.cpp.opencv_flann.*;

import eu.hrasko.dronelander.thriftcom.*;

public class ExeCom implements AutoCloseable {

	private TTransport transport;
	private TProtocol protocol;
	ShowImage.Client ShowImageClient;

	// constructor of communication interface
	public ExeCom() {
		try {
			transport = new TSocket("localhost", 9090);
			transport.open();

			protocol = new TBinaryProtocol(transport);
			ShowImageClient = new ShowImage.Client(protocol);

			// client.Show(list);

		} catch (TTransportException e) {
			e.printStackTrace();
		} catch (TException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws Exception {
		transport.close();
	}

	public static void main(String[] args) {

		ExeCom com = new ExeCom();

		try {

			IplImage img = cvLoadImage("shot.png", CV_LOAD_IMAGE_GRAYSCALE);
			ByteBuffer bb = img.getByteBuffer();
			byte[] b = new byte[bb.remaining()];
			bb.get(b);
			List<Byte> list = new ArrayList<>(b.length);

			for (Byte byte1 : b) {
				list.add(byte1);
			}

			com.ShowImageClient.Show(list);

			System.out.println("img sent");

		} catch (TException e) {
			e.printStackTrace();
		}
	}
}
