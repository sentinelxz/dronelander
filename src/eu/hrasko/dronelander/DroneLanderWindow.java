package eu.hrasko.dronelander;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import com.codeminders.controltower.VideoPanel;
import com.google.gson.Gson;

@SuppressWarnings("serial")
public class DroneLanderWindow extends JFrame implements Runnable {

	public final JPanel landerButtonsPanel = new JPanel();

	// VideoPanel leftVideoPanel;
	// VideoPanel middleVideoPanel;
	// VideoPanel rightVideoPanel;

	DroneLander droneLander;
	static DroneLanderWindow window;

	public VideoPanel rightVideoPanel;
	public VideoPanel middleVideoPanel;
	public VideoPanel leftVideoPanel;

	public JSlider angleSlider;
	private JPanel leftPanel;
	private JPanel middlePanel;
	private JPanel rightPanel;

	private RangeSlider left_H_slider;
	private RangeSlider left_S_slider;
	private RangeSlider left_V_slider;
	private RangeSlider right_H_slider;
	private RangeSlider right_S_slider;
	private RangeSlider right_V_slider;

	private HSVparams leftHsvParams;
	private HSVparams rightHsvParams;

	public JCheckBox chckbxShowDrone = new JCheckBox("show drone");

	public JLabel displayedDroneStatus;

	public JLabel landingTime;

	public JCheckBox rotationCheckbox;

	public JCheckBox chckbxEstimateTarget;

	// ControlTower controlTowerRef;

	private void HSVactualize() {
		try {

			leftHsvParams.hLower = left_H_slider.getValue();
			leftHsvParams.hUpper = left_H_slider.getUpperValue();
			leftHsvParams.sLower = left_S_slider.getValue();
			leftHsvParams.sUpper = left_S_slider.getUpperValue();
			leftHsvParams.vLower = left_V_slider.getValue();
			leftHsvParams.vUpper = left_V_slider.getUpperValue();

			rightHsvParams.hLower = right_H_slider.getValue();
			rightHsvParams.hUpper = right_H_slider.getUpperValue();
			rightHsvParams.sLower = right_S_slider.getValue();
			rightHsvParams.sUpper = right_S_slider.getUpperValue();
			rightHsvParams.vLower = right_V_slider.getValue();
			rightHsvParams.vUpper = right_V_slider.getUpperValue();

		} catch (Exception e) {
		}
	}

	private class Config {
		int angle;
		boolean rotate, estimate, showDrone;

		public void load(String file, DroneLanderWindow dlw) {
			Config p = new Config();
			if (!new File(file).exists())
				return;

			Gson gson = new Gson();

			try {
				System.out.println("Loading Config");
				p = gson.fromJson(new InputStreamReader(new FileInputStream(file)), Config.class);
			} catch (Exception e) {
				System.err.println("Error reading Config");
				return;
			}

			dlw.angleSlider.setValue(p.angle);
			dlw.chckbxShowDrone.setSelected(p.showDrone);
			dlw.rotationCheckbox.setSelected(p.rotate);
			dlw.chckbxEstimateTarget.setSelected(p.estimate);

			return;
		}

		public void save(String file, DroneLanderWindow dlw) {

			angle = dlw.angleSlider.getValue();
			showDrone = dlw.chckbxShowDrone.isSelected();
			rotate = dlw.rotationCheckbox.isSelected();
			estimate = dlw.chckbxEstimateTarget.isSelected();

			Gson gson = new Gson();
			String s = gson.toJson(this);

			OutputStreamWriter osw = null;
			try {
				osw = new OutputStreamWriter(new FileOutputStream(file));
			} catch (FileNotFoundException e1) {
				return;
			}
			try {
				System.out.println("Saving Config");
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

	private void HSVreloadGUI() {

		System.out.println("Refreshing HSV sliders");

		try {

			left_H_slider.setValue(leftHsvParams.hLower);
			left_H_slider.setUpperValue(leftHsvParams.hUpper);
			left_S_slider.setValue(leftHsvParams.sLower);
			left_S_slider.setUpperValue(leftHsvParams.sUpper);
			left_V_slider.setValue(leftHsvParams.vLower);
			left_V_slider.setUpperValue(leftHsvParams.vUpper);

			left_H_slider.updateUI();
			left_S_slider.updateUI();
			left_V_slider.updateUI();

			right_H_slider.setValue(rightHsvParams.hLower);
			right_H_slider.setUpperValue(rightHsvParams.hUpper);
			right_S_slider.setValue(rightHsvParams.sLower);
			right_S_slider.setUpperValue(rightHsvParams.sUpper);
			right_V_slider.setValue(rightHsvParams.vLower);
			right_V_slider.setUpperValue(rightHsvParams.vUpper);

		} catch (Exception e) {
			System.err.println("Error actualizing HSV sliders");
		}
	}

	public DroneLanderWindow(DroneLander lander) {
		this();

		if (window != null) {
			return;
		}

		this.droneLander = lander;

		// -HSV PARAMS LOAD
		leftHsvParams = droneLander.videoRecogniser.firstHSV;
		rightHsvParams = droneLander.videoRecogniser.secondHSV;
		HSVreloadGUI();

		// keyboardController = lander.keyboardController;
		// this.addKeyListener(keyboardController);

		leftVideoPanel = new VideoPanel();
		middleVideoPanel = new VideoPanel();
		rightVideoPanel = new VideoPanel();

		leftPanel.setBackground(new Color(102, 102, 102));
		// leftPanel.setPreferredSize(new Dimension(320, 240));
		leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
		leftPanel.add(leftVideoPanel);

		middlePanel.setBackground(new Color(102, 102, 102));
		// middlePanel.setPreferredSize(new Dimension(320, 240));
		middlePanel.setLayout(new BoxLayout(middlePanel, BoxLayout.X_AXIS));
		middlePanel.add(middleVideoPanel);

		rightPanel.setBackground(new Color(102, 102, 102));
		// rightPanel.setPreferredSize(new Dimension(320, 240));
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
		rightPanel.add(rightVideoPanel);

		angleSlider.setValue((int) ((droneLander.landingAngle / 3.6) + 50));

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (e.getID() == KeyEvent.KEY_PRESSED) {
					window.droneLander.keyboardController.keyPressed(e);
				} else if (e.getID() == KeyEvent.KEY_RELEASED) {
					window.droneLander.keyboardController.keyReleased(e);
				} else if (e.getID() == KeyEvent.KEY_TYPED) {
					window.droneLander.keyboardController.keyTyped(e);
				}
				return false;
			}
		});

		this.setFocusable(true);

		right_H_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				HSVactualize();
			}
		});
		right_S_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				HSVactualize();
			}
		});
		right_V_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				HSVactualize();
			}
		});
		left_H_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				HSVactualize();
			}
		});
		left_S_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				HSVactualize();
			}
		});
		left_V_slider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				HSVactualize();
			}
		});

		window = this;
		new Config().load("config_dl", window);

	}

	/**
	 * Create the frame.
	 */
	public DroneLanderWindow() {

		if (window != null) {
			return;
		}

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				droneLander.close();
				leftHsvParams.save("leftHSV");
				rightHsvParams.save("rightHSV");

				new Config().save("config_dl", window);
				window = null;
			}

			@Override
			public void windowClosed(WindowEvent arg0) {

			}
		});

		setTitle("Drone Lander");
		setResizable(false);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 1159, 398);
		getContentPane().setLayout(new GridLayout(1, 0, 0, 0));

		JSplitPane splitPane = new JSplitPane();
		splitPane.setDividerSize(0);
		getContentPane().add(splitPane);

		splitPane.setLeftComponent(landerButtonsPanel);
		landerButtonsPanel.setLayout(new MigLayout("", "[grow]", "[][][][][][][][][][][][grow][100px]"));

		JButton btnLaunch = new JButton("launch");
		btnLaunch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					droneLander.drone.takeOff();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		landerButtonsPanel.add(btnLaunch, "cell 0 0,grow");

		JButton btnLand = new JButton("land on target");
		btnLand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				droneLander.controller.land(true);
				droneLander.controller.altLandingStart = droneLander.tachoMeter.altitude();
			}
		});

		JButton btnLockOnTarget = new JButton("lock on target");
		btnLockOnTarget.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				droneLander.controller.lock(true);
			}
		});
		landerButtonsPanel.add(btnLockOnTarget, "cell 0 2,grow");
		landerButtonsPanel.add(btnLand, "cell 0 3,grow");

		JButton btnEmergency = new JButton("EMERGENCY");
		btnEmergency.setBackground(UIManager.getColor("Button.background"));
		btnEmergency.setForeground(Color.RED);
		btnEmergency.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					droneLander.drone.sendEmergencySignal();
				} catch (IOException e1) {
					System.out.println("EMERGENCY SIGNAL SENT");
				}
			}
		});

		JButton btnLandNow = new JButton("land now");
		btnLandNow.setAutoscrolls(true);
		landerButtonsPanel.add(btnLandNow, "cell 0 4,grow");

		chckbxEstimateTarget = new JCheckBox("estimate target");
		landerButtonsPanel.add(chckbxEstimateTarget, "cell 0 5");

		landingTime = new JLabel("New label");
		landingTime.setHorizontalAlignment(SwingConstants.CENTER);
		landerButtonsPanel.add(landingTime, "cell 0 7,grow");

		displayedDroneStatus = new JLabel("LANDED");
		displayedDroneStatus.setHorizontalTextPosition(SwingConstants.CENTER);
		displayedDroneStatus.setHorizontalAlignment(SwingConstants.CENTER);
		landerButtonsPanel.add(displayedDroneStatus, "cell 0 9,grow");
		landerButtonsPanel.add(btnEmergency, "cell 0 12,grow");

		JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setDividerSize(1);
		splitPane_1.setResizeWeight(0.99);
		splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane.setRightComponent(splitPane_1);

		JSplitPane splitPane_2 = new JSplitPane();
		splitPane_2.setResizeWeight(0.33333);
		splitPane_2.setDividerSize(1);
		splitPane_2.setBorder(null);
		splitPane_1.setLeftComponent(splitPane_2);

		leftPanel = new JPanel();
		splitPane_2.setLeftComponent(leftPanel);
		leftPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JPanel panel_1 = new JPanel();
		splitPane_2.setRightComponent(panel_1);
		panel_1.setLayout(new GridLayout(0, 1, 0, 0));

		JSplitPane splitPane_3 = new JSplitPane();
		splitPane_3.setResizeWeight(0.5);
		splitPane_3.setDividerSize(1);
		splitPane_3.setBorder(null);
		panel_1.add(splitPane_3);

		middlePanel = new JPanel();
		splitPane_3.setLeftComponent(middlePanel);

		rightPanel = new VideoPanel();
		splitPane_3.setRightComponent(rightPanel);

		JSplitPane splitPane_4 = new JSplitPane();
		splitPane_4.setResizeWeight(0.31);
		splitPane_4.setDividerSize(1);
		splitPane_4.setBorder(null);
		splitPane_1.setRightComponent(splitPane_4);

		JPanel panel_4 = new JPanel();
		panel_4.setMaximumSize(new Dimension(100, 32767));
		splitPane_4.setLeftComponent(panel_4);
		panel_4.setLayout(new MigLayout("", "[][grow,fill]", "[grow,fill][grow,fill][grow,fill]"));

		JLabel lblH = new JLabel("H");
		panel_4.add(lblH, "cell 0 0");

		left_H_slider = new RangeSlider(0, 255);
		left_H_slider.setPreferredSize(new Dimension(240, left_H_slider.getPreferredSize().height));
		panel_4.add(left_H_slider, "cell 1 0");
		// left_H_slider.setMinimum(0);
		// left_H_slider.setMaximum(255);

		JLabel lblS = new JLabel("S");
		panel_4.add(lblS, "cell 0 1");

		left_S_slider = new RangeSlider(0, 255);

		left_S_slider.setPreferredSize(new Dimension(240, 16));
		panel_4.add(left_S_slider, "cell 1 1");
		// left_S_slider.setMinimum(0);
		// left_S_slider.setMaximum(255);

		repaint();

		JLabel lblV = new JLabel("V");
		panel_4.add(lblV, "cell 0 2");

		left_V_slider = new RangeSlider(0, 255);
		panel_4.add(left_V_slider, "cell 1 2");
		left_V_slider.setPreferredSize(new Dimension(240, 16));
		// left_V_slider.setMinimum(0);
		// left_V_slider.setMaximum(255);

		repaint();

		JPanel panel_5 = new JPanel();
		splitPane_4.setRightComponent(panel_5);
		panel_5.setLayout(new GridLayout(0, 1, 0, 0));

		JSplitPane splitPane_5 = new JSplitPane();
		splitPane_5.setResizeWeight(0.45);
		splitPane_5.setDividerSize(1);
		splitPane_5.setBorder(null);
		panel_5.add(splitPane_5);

		JPanel panel_6 = new JPanel();
		splitPane_5.setLeftComponent(panel_6);
		panel_6.setLayout(new MigLayout("", "[][grow,fill]", "[grow,fill][grow,fill][grow,fill]"));

		JLabel lblH_1 = new JLabel("H");
		panel_6.add(lblH_1, "cell 0 0");

		right_H_slider = new RangeSlider(0, 255);
		panel_6.add(right_H_slider, "cell 1 0");
		right_H_slider.setPreferredSize(new Dimension(240, 16));
		// right_H_slider.setMaximum(255);
		// right_H_slider.setMinimum(0);

		JLabel lblS_1 = new JLabel("S");
		panel_6.add(lblS_1, "cell 0 1");

		right_S_slider = new RangeSlider(0, 255);
		panel_6.add(right_S_slider, "cell 1 1");
		right_S_slider.setPreferredSize(new Dimension(240, 16));
		// right_S_slider.setMinimum(0);
		// right_S_slider.setMaximum(255);

		JLabel lblV_1 = new JLabel("V");
		panel_6.add(lblV_1, "cell 0 2");

		right_V_slider = new RangeSlider(0, 255);
		panel_6.add(right_V_slider, "cell 1 2");
		right_V_slider.setPreferredSize(new Dimension(240, 16));
		// right_V_slider.setMinimum(0);
		// right_V_slider.setMaximum(255);

		JPanel panel_7 = new JPanel();
		splitPane_5.setRightComponent(panel_7);
		panel_7.setLayout(new MigLayout("", "[][grow,fill][fill]", "[grow,fill][grow,fill][grow,fill]"));

		chckbxShowDrone.setSelected(true);
		panel_7.add(chckbxShowDrone, "cell 2 0");

		JButton btnFreezeVideo = new JButton("freeze video");
		btnFreezeVideo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (droneLander.videoRecogniser.freezeSwitch) {
					droneLander.videoRecogniser.freezeSwitch = false;
					JButton btn = (JButton) arg0.getSource();
					btn.setText("  freeze video  ");
				} else {
					droneLander.videoRecogniser.freezeSwitch = true;
					JButton btn = (JButton) arg0.getSource();
					btn.setText("UNfreeze video  ");
				}
			}
		});
		panel_7.add(btnFreezeVideo, "cell 0 0 2 2,grow");

		rotationCheckbox = new JCheckBox("use rotation angle");
		rotationCheckbox.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				droneLander.controller.rotation(rotationCheckbox.isSelected());
			}
		});

		panel_7.add(rotationCheckbox, "cell 2 1");
		JLabel lblAngle = new JLabel("angle");
		panel_7.add(lblAngle, "cell 0 2,aligny top");

		angleSlider = new JSlider();

		angleSlider.setValue(0);
		angleSlider.setMaximum(360);
		angleSlider.setMinimum(0);
		angleSlider.setPaintTicks(true);
		angleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				droneLander.landingAngle = (long) (angleSlider.getValue());
				System.out.println(droneLander.landingAngle);
			}
		});

		panel_7.add(angleSlider, "cell 1 2 2 1,growx,aligny bottom");

		// videoPanel.add();

	}

	@Override
	public void run() {
		// DroneLanderWindow frame = new DroneLanderWindow();
		this.repaint();
		this.setVisible(true);
		HSVreloadGUI();

	}

}
