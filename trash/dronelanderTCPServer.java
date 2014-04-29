package eu.hrasko.dronelander;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;

import org.apache.log4j.net.SocketServer;

public class dronelanderTCPServer {

	public String LocalIP = "127.0.0.1";
	public int LocalPort = 10000;
	public String RemoteIP = "127.0.0.1";
	public int RemotePort = 10001;

	private SocketServer serverSocket;
	private Socket socket;
	PrintWriter writer;
	BufferedReader reader;

	public int SocketConnect() {

		try {
			socket = new Socket(RemoteIP, RemotePort);
		} catch (Exception e) {
			return 1;
		}

		System.err.println("Initialized connection to: " + RemoteIP + ":" + socket.getPort());
		try {
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.err.println("Connected.");

		writer.println("HI");

		writer.println("HI2");
		while (true) {
			String str = "";
			try {
				str = reader.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(str);
			if (str.equals("end")) {
				break;
			}
		}

		try {
			if (!reader.readLine().equals("FIVE"))
				return 1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return 0;
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		dronelanderTCPServer server = new dronelanderTCPServer();
		server.SocketConnect();

		server.writer.write("loooool");
		System.err.println(server.reader.readLine());

	}
}
