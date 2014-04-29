/*
 * ControlTower.java
 *
 * Created on 17.05.2011, 13:41:27
 */
package com.codeminders.controltower;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.LogManager;
import java.util.prefs.Preferences;
import javax.swing.*;

import javax.swing.ImageIcon;
import javax.swing.LayoutStyle;
import javax.swing.border.*;

import org.apache.log4j.Logger;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.codeminders.ardrone.ARDrone;
import com.codeminders.ardrone.ARDrone.VideoChannel;
import com.codeminders.ardrone.DroneStatusChangeListener;
import com.codeminders.ardrone.NavData;
import com.codeminders.ardrone.NavDataListener;
import com.codeminders.ardrone.controllers.AfterGlowController;
import com.codeminders.ardrone.controllers.KeyboardController;
import com.codeminders.ardrone.controllers.PS3Controller;
import com.codeminders.ardrone.controllers.PS3ControllerState;
import com.codeminders.ardrone.controllers.PS3ControllerStateChange;
import com.codeminders.ardrone.controllers.SonyPS3Controller;
import com.codeminders.controltower.config.AssignableControl.ControllerButton;
import com.codeminders.controltower.config.ControlMap;
import com.codeminders.hidapi.ClassPathLibraryLoader;
import com.codeminders.hidapi.HIDDeviceInfo;
import com.codeminders.hidapi.HIDDeviceNotFoundException;
import com.codeminders.hidapi.HIDManager;

import eu.hrasko.dronelander.DroneLander;

/**
 * The central class that represents the main window and also manages the drone
 * update loop.
 * 
 * @author normenhansen
 */
@SuppressWarnings("serial")
public class ControlTower2 extends javax.swing.JFrame implements DroneStatusChangeListener, NavDataListener {

	ControlTower2 THISTOWER = this;
	private static final long READ_UPDATE_DELAY_MS = 5L;
	private static final long CONNECT_TIMEOUT = 8000L;
	private static float CONTROL_THRESHOLD = 0.5f;
	private final ImageIcon droneOn = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/drone_on.gif"));
	private final ImageIcon droneOff = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/drone_off.gif"));
	private final ImageIcon controllerOn = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/controller_on.png"));
	private final ImageIcon keyboradControllerOn = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/keyboard_on.png"));
	private final ImageIcon controllerOff = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/controller_off.png"));
	private final ImageIcon keyboradControllerOff = new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/keyboard_off.png"));
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicBoolean flying = new AtomicBoolean(false);
	private final AtomicBoolean flipSticks = new AtomicBoolean(false);
	private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	public ARDrone drone;
	private final AtomicReference<PS3Controller> dev = new AtomicReference<PS3Controller>();
	public VideoPanel video = new VideoPanel();
	private final DroneConfig droneConfigWindow;
	private final ControlConfig controlConfigWindow;
	private final KeyboardControlConfig keyboardControlConfigWindow;
	private final BottomGaugePanel gauges = new BottomGaugePanel();
	private final ControlMap controlMap = new ControlMap();

	private static boolean isHIDLibLoaded = false;

	static {
		isHIDLibLoaded = ClassPathLibraryLoader.loadNativeHIDLibrary();
		SLF4JBridgeHandler.install();
	}

	/**
	 * Creates new form ControlTower
	 */
	public ControlTower2() {
		setAlwaysOnTop(true);
		initComponents();
		droneConfigWindow = new DroneConfig(this, true);
		keyboardControlConfigWindow = new KeyboardControlConfig(this, true);
		controlConfigWindow = new ControlConfig(this, true, controlMap);
		videoPanel.add(video);
		jPanel2.add(gauges);
		initController();
		initDrone();
		flipSticks.set(prefs.getBoolean("FLIP_STICKS", false));
		flipSticksCheckbox.setSelected(flipSticks.get());
	}

	private void initDrone() {
		try {
			drone = new ARDrone();
		} catch (UnknownHostException ex) {
			Logger.getLogger(ControlTower2.class.getName()).error("Error creating drone object!", ex);
			return;
		}
		droneConfigWindow.setDrone(drone);
		gauges.setDrone(drone);
		video.setDrone(drone);
		drone.addStatusChangeListener(this);
		drone.addNavDataListener(this);
	}

	/**
	 * Tries to find PS3 controller, else creates keyboard controller
	 */
	private void initController() {
		PS3Controller current = dev.get();
		if (current != null) {
			try {
				current.close();
			} catch (IOException ex) {
				Logger.getLogger(ControlTower2.class.getName()).error("", ex);
			}
		}
		try {
			dev.set(findController());
		} catch (IOException ex) {
			Logger.getLogger(ControlTower2.class.getName()).error("{0}", ex);
		}
		if (dev.get() == null) {
			System.err.println("No suitable controller found! Using keyboard");
			dev.set(new KeyboardController(this));
			updateControllerStatus(false);
		} else {
			System.err.println("Gamepad controller found");
			updateControllerStatus(true);
		}
	}

	private static PS3Controller findController() throws IOException {
		if (!isHIDLibLoaded)
			return null;

		HIDDeviceInfo[] devs = HIDManager.getInstance().listDevices();
		if (null != devs) {
			for (int i = 0; i < devs.length; i++) {
				System.out.println("" + devs[i]);
				if (AfterGlowController.isA(devs[i])) {
					return new AfterGlowController(devs[i]);
				}
				if (SonyPS3Controller.isA(devs[i])) {
					return new SonyPS3Controller(devs[i]);
				}
			}
		}
		return null;
	}

	private void updateLoop() {
		if (running.get()) {
			return;
		}
		running.set(true);
		resetStatus();
		try {

			drone.addStatusChangeListener(new DroneStatusChangeListener() {

				@Override
				public void ready() {
					try {
						Logger.getLogger(getClass().getName()).debug("updateLoop::ready()");
						System.err.println("Configure");
						droneConfigWindow.updateDrone();
						drone.selectVideoChannel(VideoChannel.HORIZONTAL_ONLY);
						drone.setCombinedYawMode(true);
						drone.trim();
					} catch (IOException e) {
						drone.changeToErrorState(e);
					}
				}
			});

			System.err.println("Connecting to the drone");
			drone.connect();
			drone.waitForReady(CONNECT_TIMEOUT);
			drone.clearEmergencySignal();
			System.err.println("Connected to the drone");
			try {
				PS3ControllerState oldpad = null;
				while (running.get()) {
					PS3ControllerState pad = dev.get().read();
					PS3ControllerStateChange pad_change = new PS3ControllerStateChange(oldpad, pad);
					oldpad = pad;

					if (pad_change.isStartChanged() && pad_change.isStart()) {
						controlMap.sendCommand(drone, ControllerButton.START);
					}
					if (pad_change.isSelectChanged() && pad_change.isSelect()) {
						controlMap.sendCommand(drone, ControllerButton.SELECT);
					}
					if (pad_change.isPSChanged() && pad_change.isPS()) {
						controlMap.sendCommand(drone, ControllerButton.PS);
					}
					if (pad_change.isTriangleChanged() && pad_change.isTriangle()) {
						controlMap.sendCommand(drone, ControllerButton.TRIANGLE);
					}
					if (pad_change.isCrossChanged() && pad_change.isCross()) {
						controlMap.sendCommand(drone, ControllerButton.CROSS);
					}
					if (pad_change.isSquareChanged() && pad_change.isSquare()) {
						controlMap.sendCommand(drone, ControllerButton.SQUARE);
					}
					if (pad_change.isCircleChanged() && pad_change.isCircle()) {
						controlMap.sendCommand(drone, ControllerButton.CIRCLE);
					}
					if (pad_change.isL1Changed() && pad_change.isL1()) {
						controlMap.sendCommand(drone, ControllerButton.L1);
					}
					if (pad_change.isR1Changed() && pad_change.isR1()) {
						controlMap.sendCommand(drone, ControllerButton.R1);
					}
					if (pad_change.isL2Changed() && pad_change.isL2()) {
						controlMap.sendCommand(drone, ControllerButton.L2);
					}
					if (pad_change.isR2Changed() && pad_change.isR2()) {
						controlMap.sendCommand(drone, ControllerButton.R2);
					}
					if (flying.get()) {
						// Detecting if we need to move the drone

						int leftX = pad.getLeftJoystickX();
						int leftY = pad.getLeftJoystickY();

						int rightX = pad.getRightJoystickX();
						int rightY = pad.getRightJoystickY();

						float left_right_tilt = 0f;
						float front_back_tilt = 0f;
						float vertical_speed = 0f;
						float angular_speed = 0f;

						if (Math.abs(((float) leftX) / 128f) > CONTROL_THRESHOLD) {
							left_right_tilt = ((float) leftX) / 128f;
						}

						if (Math.abs(((float) leftY) / 128f) > CONTROL_THRESHOLD) {
							front_back_tilt = ((float) leftY) / 128f;
						}

						if (Math.abs(((float) rightX) / 128f) > CONTROL_THRESHOLD) {
							angular_speed = ((float) rightX) / 128f;
						}

						if (Math.abs(-1 * ((float) rightY) / 128f) > CONTROL_THRESHOLD) {
							vertical_speed = -1 * ((float) rightY) / 128f;
						}

						if (left_right_tilt != 0 || front_back_tilt != 0 || vertical_speed != 0 || angular_speed != 0) {
							if (flipSticks.get()) {
								drone.move(angular_speed, -1 * vertical_speed, -1 * front_back_tilt, left_right_tilt);
							} else {
								drone.move(left_right_tilt, front_back_tilt, vertical_speed, angular_speed);

							}
						} else if (drone.controller.isWorking(4)) {
							drone.move(drone.controller.roll, drone.controller.pitch, vertical_speed, angular_speed);
						} else {
							drone.hover();
						}
					}

					try {
						Thread.sleep(READ_UPDATE_DELAY_MS);
					} catch (InterruptedException e) {
						// Ignore
					}
				}
			} finally {
				drone.disconnect();
			}
		} catch (HIDDeviceNotFoundException hex) {
			hex.printStackTrace();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		resetStatus();
		running.set(false);
	}

	private void startUpdateLoop() {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				updateLoop();
			}
		});
		thread.setName("ARDrone Control Loop");
		thread.start();
	}

	/**
	 * Updates the drone status in the UI, queues command to AWT event dispatch
	 * thread
	 * 
	 * @param available
	 */
	private void updateDroneStatus(final boolean available) {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (!available) {
					droneStatus.setForeground(Color.RED);
					droneStatus.setIcon(droneOff);
				} else {
					droneStatus.setForeground(Color.GREEN);
					droneStatus.setIcon(droneOn);
				}
			}
		});

	}

	/**
	 * Updates the controller status in the UI, queues command to AWT event
	 * dispatch thread
	 * 
	 * @param available
	 */
	private void updateControllerStatus(final boolean available) {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (!available) {
					joystickControllerStatus.setForeground(Color.RED);
					joystickControllerStatus.setIcon(controllerOff);
					keyboardControllerStatus.setForeground(Color.GREEN);
					keyboardControllerStatus.setIcon(keyboradControllerOn);
				} else {
					joystickControllerStatus.setForeground(Color.GREEN);
					joystickControllerStatus.setIcon(controllerOn);
					keyboardControllerStatus.setForeground(Color.RED);
					keyboardControllerStatus.setIcon(keyboradControllerOff);
				}
			}
		});

	}

	/**
	 * Updates the battery status in the UI, queues command to AWT event
	 * dispatch thread
	 * 
	 * @param value
	 */
	private void updateBatteryStatus(final int value) {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				batteryStatus.setText(value + "%");
				if (value < 15) {
					batteryStatus.setForeground(Color.RED);
				} else if (value < 50) {
					batteryStatus.setForeground(Color.ORANGE);
				} else {
					batteryStatus.setForeground(Color.GREEN);
				}
			}
		});
	}

	/**
	 * Resets the UI, queues command to AWT event dispatch thread
	 * 
	 */
	private void resetStatus() {
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				droneStatus.setForeground(Color.RED);
				droneStatus.setIcon(droneOff);
				batteryStatus.setForeground(Color.RED);
				batteryStatus.setText("0%");
			}
		});

	}

	@Override
	public void ready() {
		Logger.getLogger(getClass().getName()).debug("ready()");
		updateDroneStatus(true);
	}

	@Override
	public void navDataReceived(NavData nd) {
		// Logger.getLogger(getClass().getName()).debug("navDataReceived()");
		updateBatteryStatus(nd.getBattery());
		this.flying.set(nd.isFlying());
	}

	public void setControlThreshold(float sens) {
		CONTROL_THRESHOLD = sens;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed"
	// desc="Generated Code">//GEN-BEGIN:initComponents
	// Generated using JFormDesigner Evaluation license - andy hrach
	private void initComponents() {
		jToolBar1 = new JToolBar();
		droneStatus = new JLabel();
		batteryStatus = new JLabel();
		joystickControllerStatus = new JLabel();
		keyboardControllerStatus = new JLabel();
		flipSticksCheckbox = new JCheckBox();
		mappingButton = new JButton();
		jPanel1 = new JPanel();
		instrumentButton1 = new JButton();
		instrumentButton = new JButton();
		configureButton = new JButton();
		videoPanel = new JPanel();
		jPanel2 = new JPanel();

		// ======== this ========
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("Control Tower");
		Container contentPane = getContentPane();

		// ======== jToolBar1 ========
		{
			jToolBar1.setBorder(new BevelBorder(BevelBorder.RAISED));
			jToolBar1.setFloatable(false);
			jToolBar1.setRollover(true);
			jToolBar1.addSeparator();

			// ---- droneStatus ----
			droneStatus.setForeground(Color.red);
			droneStatus.setIcon(new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/drone_off.gif")));
			droneStatus.setToolTipText("drone status (lit = connected)");
			droneStatus.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					droneStatusMouseReleased(e);
				}
			});
			jToolBar1.add(droneStatus);

			// ---- batteryStatus ----
			batteryStatus.setFont(new Font("Lucida Grande", Font.BOLD, 10));
			batteryStatus.setForeground(Color.red);
			batteryStatus.setText("0%");
			jToolBar1.add(batteryStatus);
			jToolBar1.addSeparator();

			// ---- joystickControllerStatus ----
			joystickControllerStatus.setForeground(Color.red);
			joystickControllerStatus.setIcon(new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/controller_off.png")));
			joystickControllerStatus.setToolTipText("controller status (green = available)");
			joystickControllerStatus.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					joystickControllerStatusMouseReleased(e);
				}
			});
			jToolBar1.add(joystickControllerStatus);

			// ---- keyboardControllerStatus ----
			keyboardControllerStatus.setForeground(Color.red);
			keyboardControllerStatus.setIcon(new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/keyboard_off.png")));
			keyboardControllerStatus.setToolTipText("keyboard status (green = available)");
			keyboardControllerStatus.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseReleased(MouseEvent e) {
					keyboardControllerStatusMouseReleased(e);
				}
			});
			jToolBar1.add(keyboardControllerStatus);

			// ---- flipSticksCheckbox ----
			flipSticksCheckbox.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
			flipSticksCheckbox.setText("flip sticks");
			flipSticksCheckbox.setFocusable(false);
			flipSticksCheckbox.setVerticalTextPosition(SwingConstants.BOTTOM);
			flipSticksCheckbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					flipSticksCheckboxActionPerformed(e);
				}
			});
			jToolBar1.add(flipSticksCheckbox);

			// ---- mappingButton ----
			mappingButton.setText("mapping");
			mappingButton.setToolTipText("map controller buttons");
			mappingButton.setFocusable(false);
			mappingButton.setHorizontalTextPosition(SwingConstants.CENTER);
			mappingButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			mappingButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					mappingButtonActionPerformed(e);
				}
			});
			jToolBar1.add(mappingButton);
			jToolBar1.addSeparator();

			// ======== jPanel1 ========
			{

				// JFormDesigner evaluation mark
				jPanel1.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.TitledBorder(new javax.swing.border.EmptyBorder(0, 0, 0, 0),
						"JFormDesigner Evaluation", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.BOTTOM, new java.awt.Font("Dialog",
								java.awt.Font.BOLD, 12), java.awt.Color.red), jPanel1.getBorder()));
				jPanel1.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
					public void propertyChange(java.beans.PropertyChangeEvent e) {
						if ("border".equals(e.getPropertyName()))
							throw new RuntimeException();
					}
				});

				// ---- instrumentButton1 ----
				instrumentButton1.setText("DRONE LANDER");
				instrumentButton1.setToolTipText("Launch Drone Lander");
				instrumentButton1.setActionCommand("lander");
				instrumentButton1.setFocusable(false);
				instrumentButton1.setHorizontalTextPosition(SwingConstants.CENTER);
				instrumentButton1.setVerticalTextPosition(SwingConstants.BOTTOM);
				instrumentButton1.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(new DroneLander(drone)).start();
					}
				});

				GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
				jPanel1.setLayout(jPanel1Layout);
				jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup().addComponent(instrumentButton1, GroupLayout.Alignment.TRAILING,
						GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE));
				jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup().addGroup(
						jPanel1Layout.createSequentialGroup().addComponent(instrumentButton1, GroupLayout.PREFERRED_SIZE, 21, GroupLayout.PREFERRED_SIZE)
								.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
			}
			jToolBar1.add(jPanel1);
			jToolBar1.addSeparator();

			// ---- instrumentButton ----
			instrumentButton.setIcon(new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/instruments.gif")));
			instrumentButton.setText("instruments");
			instrumentButton.setToolTipText("toggle instruments");
			instrumentButton.setFocusable(false);
			instrumentButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			instrumentButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					instrumentButtonActionPerformed(e);
				}
			});
			jToolBar1.add(instrumentButton);

			// ---- configureButton ----
			configureButton.setIcon(new ImageIcon(getClass().getResource("/com/codeminders/controltower/images/objects_039.gif")));
			configureButton.setText("tuning");
			configureButton.setToolTipText("show drone tuning settings");
			configureButton.setFocusable(false);
			configureButton.setHorizontalTextPosition(SwingConstants.RIGHT);
			configureButton.setVerticalTextPosition(SwingConstants.BOTTOM);
			configureButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					configureButtonActionPerformed(e);
				}
			});
			jToolBar1.add(configureButton);
			jToolBar1.addSeparator();
		}

		// ======== videoPanel ========
		{
			videoPanel.setBackground(new Color(102, 102, 102));
			videoPanel.setPreferredSize(new Dimension(320, 240));
			videoPanel.setLayout(new BoxLayout(videoPanel, BoxLayout.X_AXIS));
		}

		// ======== jPanel2 ========
		{
			jPanel2.setLayout(new BoxLayout(jPanel2, BoxLayout.X_AXIS));
		}

		GroupLayout contentPaneLayout = new GroupLayout(contentPane);
		contentPane.setLayout(contentPaneLayout);
		contentPaneLayout
				.setHorizontalGroup(contentPaneLayout.createParallelGroup().addComponent(jToolBar1, GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
						.addComponent(videoPanel, GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE)
						.addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, 859, Short.MAX_VALUE));
		contentPaneLayout.setVerticalGroup(contentPaneLayout.createParallelGroup().addGroup(
				GroupLayout.Alignment.TRAILING,
				contentPaneLayout.createSequentialGroup().addComponent(videoPanel, GroupLayout.DEFAULT_SIZE, 318, Short.MAX_VALUE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 209, GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jToolBar1, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)));
		pack();
		setLocationRelativeTo(getOwner());
	}// </editor-fold>//GEN-END:initComponents

	private void configureButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_configureButtonActionPerformed
		droneConfigWindow.setLocationRelativeTo(this);
		droneConfigWindow.setVisible(true);
	}// GEN-LAST:event_configureButtonActionPerformed

	private void droneStatusMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_droneStatusMouseReleased
		startUpdateLoop();
	}// GEN-LAST:event_droneStatusMouseReleased

	private void controllerStatusMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_controllerStatusMouseReleased
		initController();
	}// GEN-LAST:event_controllerStatusMouseReleased

	private void instrumentButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_instrumentButtonActionPerformed
		Dimension dim = jPanel2.getSize();
		if (dim.getHeight() > 0) {
			dim.setSize(dim.getWidth(), 0);
			jPanel2.setPreferredSize(dim);
			jPanel2.setSize(dim);
			jPanel2.setVisible(false);
		} else {
			dim.setSize(dim.getWidth(), 210);
			jPanel2.setPreferredSize(dim);
			jPanel2.setSize(dim);
			jPanel2.setVisible(true);
		}
	}// GEN-LAST:event_instrumentButtonActionPerformed

	private void mappingButtonActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_mappingButtonActionPerformed
		if (keyboardControllerStatus.getForeground() != Color.GREEN) {
			controlConfigWindow.setLocationRelativeTo(this);
			controlConfigWindow.setVisible(true);
		} else {
			keyboardControlConfigWindow.setLocationRelativeTo(this);
			keyboardControlConfigWindow.setVisible(true);
		}
	}// GEN-LAST:event_mappingButtonActionPerformed

	private void flipSticksCheckboxActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_flipSticksCheckboxActionPerformed
		flipSticks.set(flipSticksCheckbox.isSelected());
		prefs.putBoolean("FLIP_STICKS", flipSticks.get());
	}// GEN-LAST:event_flipSticksCheckboxActionPerformed

	private void keyboardControllerStatusMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_keyboardControllerStatusMouseReleased
		initController();
	}// GEN-LAST:event_keyboardControllerStatusMouseReleased

	private void joystickControllerStatusMouseReleased(java.awt.event.MouseEvent evt) {// GEN-FIRST:event_joystickControllerStatusMouseReleased
		initController();
	}// GEN-LAST:event_joystickControllerStatusMouseReleased

	/**
	 * @param args
	 *            the command line arguments
	 */
	public static void main(String args[]) {
		final ControlTower2 tower = new ControlTower2();
		java.awt.EventQueue.invokeLater(new Runnable() {

			@Override
			public void run() {
				tower.setLocationRelativeTo(null);
				tower.setVisible(true);
			}
		});
		tower.startUpdateLoop();
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// Generated using JFormDesigner Evaluation license - andy hrach
	private JToolBar jToolBar1;
	private JLabel droneStatus;
	private JLabel batteryStatus;
	private JLabel joystickControllerStatus;
	private JLabel keyboardControllerStatus;
	private JCheckBox flipSticksCheckbox;
	private JButton mappingButton;
	private JPanel jPanel1;
	private JButton instrumentButton1;
	private JButton instrumentButton;
	private JButton configureButton;
	private JPanel videoPanel;
	private JPanel jPanel2;
	// End of variables declaration//GEN-END:variables
}
