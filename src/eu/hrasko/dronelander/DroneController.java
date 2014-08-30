package eu.hrasko.dronelander;

import java.io.IOException;

import org.apache.commons.lang3.time.StopWatch;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.ARDrone.State;
import com.codeminders.ardrone.DroneStatusChangeListener;

public class DroneController implements DroneStatusChangeListener {

	public PIDAxisController xController;
	public PIDAxisController yController;
	public PIDAxisController yawController;
	public PIDAxisController altController;

	ARDrone drone;
	DroneLander droneLander;

	private boolean lockOnTargetSwtich = false;
	public double landAltThreshold = 0.40;
	private boolean landingSwitch = false;
	private boolean rotateSwitch = false;
	public boolean goingBack = false;
	public boolean estimateTargetSwitch = false;
	public double altLandingStart = 0;

	// 2 * tan (pi/8)
	public static final double altitudeWeight = 2 * (Math.sqrt(2) - 1);

	public volatile float pitch, roll, yaw, alt;

	public boolean land(boolean b) {
		System.out.println("Landing " + b);
		if (b) {
			lockOnTargetSwtich = false;
		}

		// if landing is initiated
		if (b && !landingSwitch) {
			startStopWatch();
		}

		// if landing was canceled
		if (!b) {
			landingStopWatch.stop();
		}

		landingSwitch = b;
		return true;
	}

	public boolean isLanding() {
		return landingSwitch;
	}

	public boolean lock(boolean b) {
		System.out.println("Locked " + b);
		if (b) {
			landingSwitch = false;
		}

		lockOnTargetSwtich = b;

		return true;
	}

	public void rotation(boolean b) {
		System.out.println("Rotation " + b);
		rotateSwitch = b;
	}

	public boolean isRotating() {
		return rotateSwitch;
	}

	public boolean isLocked() {
		return lockOnTargetSwtich;
	}

	@Override
	public void ready() {

		if (droneLander == null || droneLander.window != null || drone.getState() == null) {
			return;
		}

		droneLander.window.displayedDroneStatus.setText(drone.getState().toString());
		if (drone.getState() == State.BOOTSTRAP) {
			landingStopWatch.stop();
		}
	}

	public StopWatch landingStopWatch = new StopWatch();

	public void startStopWatch() {
		landingStopWatch.reset();
		landingStopWatch.start();
	}

	/**
	 * r class containg controllers for moving drone in each axis
	 */
	public DroneController(DroneLander droneLander) {
		this.drone = droneLander.drone;
		drone.addStatusChangeListener(this);
		this.droneLander = droneLander;
		xController = new PIDAxisController('X', drone);
		yController = new PIDAxisController('Y', drone);
		xController.setPID(0.35f, 0f, 4f);
		yController.setPID(0.35f, 0f, 4f);
		yawController = new PIDAxisController('w', drone);
		yawController.setPID(0.005f, 0f, 0f);
		altController = new PIDAxisController('a', drone);
		altController.setPID(5f, 0f, 0f);
	}

	int continousLock = 0;
	int continousUnlock = 0;

	/**
	 * recomputes new regulating values used for moving drone
	 */
	public void recompute(Coordinate target) {

		if (!droneLander.drone.isFlying()) {
			altLandingStart = 0;
		}

		if ((target == null || target.isEstimated) && altLandingStart > 0 && droneLander.tachoMeter.altitude() < altLandingStart - 0.1) {
			goingBack = true;
			alt = 0;// -altController.recompute(altLandingStart, true);
			pitch = 0;
			roll = 0;
			yaw = 0;
		} else
			goingBack = false;

		if (target == null) {
			continousUnlock++;
			if (continousUnlock > 20) { // LEAVE LOCKED FOR A WHILE
				continousLock = 0;
				xController.reset();
				yController.reset();
				pitch = 0;
				roll = 0;
				yaw = 0;
				return;
			} else {
				continousLock++;
				return;
			}
		} else {
			continousUnlock = 0;
			continousLock++;

			if (continousLock < 3) {
				return;
			}
		}

		double altitude = droneLander.tachoMeter.altitude();

		alt = -altController.recompute(
				altitude
						* 10
						/ (Math.abs(target.x) * 6 + Math.abs(target.y) * 6 + 5 * Math.abs(Coordinate.getAngleBetweenAngles(target.angle,
								droneLander.landingAngle))), target.isEstimated);

		// System.out.println(alt);
		target = computeRealPosition(target);

		roll = xController.recompute(target.x, target.isEstimated);
		pitch = yController.recompute(target.y, target.isEstimated);
		yaw = yawController.recompute(Coordinate.getAngleBetweenAngles(target.angle, droneLander.landingAngle), target.isEstimated);

		yaw = (float) (yaw / (Math.abs(target.x) * 6 + Math.abs(target.y) * 6));
		// double yasd = Coordinate.getAngleBetweenAngles(`target.angle,
		// droneLander.landingAngle);
		// System.out.println(yasd);

		roll = Math.min(roll, 1f);
		roll = Math.max(roll, -1f);
		pitch = Math.min(pitch, 1f);
		pitch = Math.max(pitch, -1f);
		if (isRotating()) {
			yaw = Math.min(yaw, 1f);
			yaw = Math.max(yaw, -1f);
		} else
			yaw = 0;
		alt = Math.min(alt, 0.3f);
		alt = Math.max(alt, -0.3f);

		if (altitude > 1.8) {

		}

		// do not descend so quickly if rotating too fast
		if (Math.abs(yaw) > 0.2) {
			alt /= 5;
		}

		if (isLocked()) {
			alt = 0;
		}

		if (drone.isFlying() && altitude < landAltThreshold && isWorking(1)) {
			try {
				System.out.println("** Sending automated land signal. **");
				drone.land();
				land(false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// alt = 0;

		String x = ">";
		String y = "^";
		if (pitch > 0)
			y = "v";
		if (roll < 0)
			x = "<";

	}

	/**
	 * returns wether controller is working for minimal minContinousLock frames
	 */
	public boolean isWorking(int minContinousLock) {
		if (drone.isFlying() && (goingBack || ((isLanding() || isLocked()) && this.continousLock >= minContinousLock))) {
			return true;
		}
		return false;
	}

	/**
	 * returns real position in meters given the seen angle, angle of drone and
	 * the altitude
	 */
	Coordinate computeRealPosition(Coordinate c) {
		double alt = droneLander.tachoMeter.altitude();
		double pitch = droneLander.tachoMeter.pitch();
		double roll = droneLander.tachoMeter.roll();

		double anglePixelRatio = VideoRecogniser.cameraPixelAngleRatio;

		double seenAngleX = c.x / anglePixelRatio;
		double seenAngleY = c.y / anglePixelRatio;

		double realAngleX = seenAngleX + roll;
		double realAngleY = seenAngleY - pitch;

		double realPosX = Math.tan(Math.toRadians(realAngleX)) * alt;
		double realPosY = Math.tan(Math.toRadians(realAngleY)) * alt;

		Coordinate res = c.clone();

		res.x = realPosX;
		res.y = realPosY;

		return res;

	}

}
