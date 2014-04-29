package eu.hrasko.dronelander;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JSplitPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import net.miginfocom.swing.MigLayout;
import javax.swing.BoxLayout;
import javax.swing.SpringLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JLabel;
import java.awt.Insets;
import java.awt.CardLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HSVsetter extends JFrame {

	private JPanel contentPane;
	private JSlider lowHslider;
	private JSlider lowSslider;
	private JSlider lowVslider;
	private JSlider upHslider;
	private JSlider upSslider;
	private JSlider upVslider;

	HSVparams params;
	String fileName;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					HSVsetter frame = new HSVsetter(new HSVparams(10, 15, 25, 35, 45, 55), "test");
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public HSVsetter(HSVparams hsv, String hsvName) {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				params.save(fileName);
			}
		});
		setTitle("HSV Parameters Setter");

		params = hsv;
		fileName = hsvName;

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 525, 574);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] { 499, 0 };
		gbl_contentPane.rowHeights = new int[] { 0, 526, 0 };
		gbl_contentPane.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_contentPane.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		contentPane.setLayout(gbl_contentPane);

		JLabel lblNewLabel = new JLabel("Lower H              Lower S               Lower V                Upper H              Upper S             Upper V");
		GridBagConstraints gbc_lblNewLabel = new GridBagConstraints();
		gbc_lblNewLabel.insets = new Insets(0, 0, 5, 0);
		gbc_lblNewLabel.gridx = 0;
		gbc_lblNewLabel.gridy = 0;
		contentPane.add(lblNewLabel, gbc_lblNewLabel);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.5);
		GridBagConstraints gbc_splitPane = new GridBagConstraints();
		gbc_splitPane.fill = GridBagConstraints.BOTH;
		gbc_splitPane.gridx = 0;
		gbc_splitPane.gridy = 1;
		contentPane.add(splitPane, gbc_splitPane);

		JPanel panel = new JPanel();
		splitPane.setLeftComponent(panel);

		lowHslider = new JSlider();
		lowHslider.setValue((int) (params.hLower / 2.55));
		lowHslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.hLower = (int) (lowHslider.getValue() * 2.55);
			}
		});
		lowHslider.setPaintLabels(true);
		lowHslider.setPaintTicks(true);
		lowHslider.setOrientation(SwingConstants.VERTICAL);

		lowSslider = new JSlider();
		lowSslider.setValue((int) (params.sLower / 2.55));
		lowSslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.sLower = (int) (lowSslider.getValue() * 2.55);
			}
		});
		lowSslider.setPaintTicks(true);
		lowSslider.setOrientation(SwingConstants.VERTICAL);

		lowVslider = new JSlider();
		lowVslider.setValue((int) (params.vLower / 2.55));
		lowVslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.vLower = (int) (lowVslider.getValue() * 2.55);
			}
		});
		lowVslider.setPaintTicks(true);
		lowVslider.setOrientation(SwingConstants.VERTICAL);
		panel.setLayout(new GridLayout(0, 3, 0, 0));
		panel.add(lowHslider);
		panel.add(lowSslider);
		panel.add(lowVslider);

		JPanel panel_1 = new JPanel();
		splitPane.setRightComponent(panel_1);
		panel_1.setLayout(new GridLayout(0, 3, 0, 0));

		upHslider = new JSlider();
		upHslider.setValue((int) (params.hUpper / 2.55));
		upHslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.hUpper = (int) (upHslider.getValue() * 2.55);
				// System.out.println("param changed" + (byte)
				// (upHslider.getValue() * 2.55));
			}
		});
		upHslider.setPaintTicks(true);
		upHslider.setOrientation(SwingConstants.VERTICAL);
		panel_1.add(upHslider);

		upSslider = new JSlider();
		upSslider.setValue((int) (params.sUpper / 2.55));
		upSslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.sUpper = (int) (upSslider.getValue() * 2.55);
			}
		});
		upSslider.setPaintTicks(true);
		upSslider.setOrientation(SwingConstants.VERTICAL);
		panel_1.add(upSslider);

		upVslider = new JSlider();
		upVslider.setValue((int) (params.vUpper / 2.55));
		upVslider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent arg0) {
				params.vUpper = (int) (upVslider.getValue() * 2.55);
			}
		});
		upVslider.setPaintTicks(true);
		upVslider.setOrientation(SwingConstants.VERTICAL);
		panel_1.add(upVslider);

	}

}
