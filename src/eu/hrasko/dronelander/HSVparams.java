package eu.hrasko.dronelander;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.yaml.snakeyaml.Yaml;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

/**
 * Class for storing and loading HSV parameters
 */
public class HSVparams {
	int hLower = 0;
	int hUpper = 255;

	int sLower = 0;
	int sUpper = 255;

	int vLower = 0;
	int vUpper = 255;

	public HSVparams(int i, int j, int k, int l, int m, int n) {
		this.hLower = i;
		this.hUpper = l;
		this.sLower = j;
		this.sUpper = m;
		this.vLower = k;
		this.vUpper = n;
	}

	public HSVparams() {
	}

	public static HSVparams load(String file) {
		HSVparams p = new HSVparams();
		if (!new File(file).exists())
			return p;

		Gson gson = new Gson();

		try {
			System.out.println("Loading HSV params");
			p = gson.fromJson(new InputStreamReader(new FileInputStream(file)), HSVparams.class);
		} catch (Exception e) {
			System.err.println("Error reading HSVparams");
			return p;
		}
		if (p == null)
			return new HSVparams();
		return p;
	}

	public void save(String file) {

		Gson gson = new Gson();
		String s = gson.toJson(this);

		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(new FileOutputStream(file));
		} catch (FileNotFoundException e1) {
			return;
		}
		try {
			System.out.println("Saving HSV params");
			osw.write(s);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				osw.close();
			} catch (Exception e) {

			}
		}

	}
}