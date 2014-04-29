package eu.hrasko.dronelander;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

/**
 * class used for storing parameters for simple blob detector from OpenCV
 */
public class SimpleBlobParams {

	double thresholdStep;
	double minThreshold;
	double maxThreshold;
	int minRepeatability;
	double minDistBetweenBlobs;

	int filterByColor;
	int blobColor;

	int filterByArea;
	double minArea, maxArea;

	int filterByCircularity;
	double minCircularity, maxCircularity;

	int filterByInertia;
	double minInertiaRatio, maxInertiaRatio;

	int filterByConvexity;
	double minConvexity, maxConvexity;

	public SimpleBlobParams(String sourceFile) {

		InputStream input;
		try {
			input = new FileInputStream(new File(sourceFile));
		} catch (FileNotFoundException e) {
			return;
		}
		Yaml yaml = new Yaml();

		Map map = (Map) yaml.load(input);

		for (Field f : this.getClass().getDeclaredFields()) {

			try {
				f.set(this, map.get(f.getName()));
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

}
