// Main.java

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.io.*;
import javax.imageio.ImageIO;

import javax.sound.sampled.*;

import java.util.logging.*;

public class Main
{
	private static final Logger LOGGER = Logger.getLogger (Main.class.getName ());
	private static Clip ringtone;

	static
	{
		try
		{
			LOGGER.setUseParentHandlers (false);
			ConsoleHandler handler = new ConsoleHandler ();
			handler.setFormatter (new SimpleFormatter ());
			LOGGER.addHandler (handler);
		}
		catch (Exception e)
		{
			System.err.println ("Logger setup failed: " + e.getMessage ());
		}
	}

	private static void playAlarmSound ()
	{
		try (InputStream input = Main.class.getResourceAsStream ("/Alarm.wav"))
		{
			if (input == null)
			{
				LOGGER.severe ("Sound file not found in JAR: alarm.wav");
				return;
			}

			byte[] audioData = input.readAllBytes ();
			try (ByteArrayInputStream byteStream = new ByteArrayInputStream (audioData);
			     AudioInputStream audioStream = AudioSystem.getAudioInputStream (byteStream))
			{

				ringtone = AudioSystem.getClip ();
				ringtone.open (audioStream);
				ringtone.loop (Clip.LOOP_CONTINUOUSLY);
			}
		}
		catch (IOException | UnsupportedAudioFileException | LineUnavailableException e)
		{
			LOGGER.log (Level.SEVERE, "Error loading ringtone", e);
		}
	}

	private static void stopAlarmSound ()
	{
		if (ringtone != null && ringtone.isRunning ())
		{
			ringtone.stop ();
			ringtone.close ();
		}
	}

	public static void main (String[] args)
	{
		try
		{
			UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (Exception e)
		{
			LOGGER.log (Level.SEVERE, "Look and feel setup error", e);
		}

		SwingUtilities.invokeLater (() -> {
			MainWindow window = new MainWindow ();
			window.setVisible (true);
		});
	}

	static class MainWindow extends JFrame
	{
		private int workTimeSeconds;
		private int shortBreakSeconds;
		private int longBreakSeconds;
		private int cyclesBeforeLongBreak;

		private int currentSeconds;
		private boolean isWorkTime = true;
		private int workSessionsCount;

		private final Timer timer;
		private JLabel timeLabel;
		private JButton startButton;
		private JButton pauseButton;
		private JLabel tomatoLabel;

		public MainWindow ()
		{
			setTitle ("Tomato");
			setSize (380, 420);
			setDefaultCloseOperation (EXIT_ON_CLOSE);
			setLayout (null);
			setLocationRelativeTo (null);

			loadSettings ();
			currentSeconds = workTimeSeconds;
			timer = new Timer (1000, _ -> updateTimer ());

			initUI ();
		}

		private void initUI ()
		{
			JLabel titleLabel = new JLabel ("Tomato");
			titleLabel.setFont (new Font ("Avenir Next", Font.BOLD, 30));
			titleLabel.setBounds (10, 10, 270, 40);
			add (titleLabel);

			timeLabel = new JLabel (formatTime (currentSeconds));
			timeLabel.setFont (new Font ("Avenir Next", Font.PLAIN, 20));
			timeLabel.setBounds (160, 210, 100, 30);
			add (timeLabel);

			tomatoLabel = new JLabel ();
			tomatoLabel.setBounds (70, 70, 241, 241);
			tomatoLabel.setHorizontalAlignment (SwingConstants.CENTER);
			loadTomatoImage ();
			add (tomatoLabel);

			startButton = createButton ("Start", 80, new Color (82, 190, 128), _ -> startTimer ());
			pauseButton = createButton ("Pause", 80, new Color (247, 220, 111), _ -> pauseTimer ());
			pauseButton.setVisible (false);

			createButton ("Reset", 200, new Color (205, 97, 85), _ -> resetTimer ());
			createSettingsButton ();
		}

		private JButton createButton (String text, int x, Color color, ActionListener listener)
		{
			JButton button = new JButton (text);
			button.setFont (new Font ("Avenir Next", Font.PLAIN, 14));
			button.setBounds (x, 350, 100, 32);
			button.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
			button.setBackground (color);
			button.setFocusPainted (false);
			button.addActionListener (listener);
			add (button);
			return button;
		}

		private void createSettingsButton ()
		{
			JButton button = new JButton ("⚙");
			button.setBounds (345, 10, 31, 31);
			button.setCursor (Cursor.getPredefinedCursor (Cursor.HAND_CURSOR));
			button.setFocusPainted (false);
			button.setBorderPainted (false);
			button.setContentAreaFilled (false);
			button.addActionListener (_ -> openSettings ());
			add (button);
		}

		private String formatTime (int totalSeconds)
		{
			int minutes = totalSeconds / 60;
			int seconds = totalSeconds % 60;
			return String.format ("%02d:%02d", minutes, seconds);
		}

		private void updateTimer ()
		{
			if (currentSeconds <= 0)
			{
				timer.stop ();
				switchCycle ();
			}
			else
			{
				currentSeconds--;
				timeLabel.setText (formatTime (currentSeconds));
			}
		}

		private void switchCycle ()
		{
			if (isWorkTime)
			{
				workSessionsCount++;
				if (workSessionsCount >= cyclesBeforeLongBreak)
				{
					showNotification ("Long break time");
					currentSeconds = longBreakSeconds;
					workSessionsCount = 0;
					loadImage ("/Green tomato.png");
				}
				else
				{
					showNotification ("Break time");
					currentSeconds = shortBreakSeconds;
					loadImage ("/Green tomato.png");
				}
				isWorkTime = false;
			}
			else
			{
				showNotification ("Work time");
				currentSeconds = workTimeSeconds;
				isWorkTime = true;
				loadImage ("/Tomato.png");
			}
			startButton.setText ("Start");
			timeLabel.setText (formatTime (currentSeconds));
		}

		private void loadTomatoImage ()
		{
			loadImage ("/Tomato.png");
		}

		private void loadImage (String path)
		{
			try (InputStream input = Main.class.getResourceAsStream (path))
			{
				if (input != null)
				{
					ImageIcon icon = new ImageIcon (ImageIO.read (input));
					Image scaled = icon.getImage ().getScaledInstance (
					  tomatoLabel.getWidth (),
					  tomatoLabel.getHeight (),
					  Image.SCALE_SMOOTH
					                                                  );
					tomatoLabel.setIcon (new ImageIcon (scaled));
				}
				else
				{
					LOGGER.warning ("Image not found in JAR: " + path);
				}
			}
			catch (IOException e)
			{
				LOGGER.log (Level.SEVERE, "Error loading image", e);
			}
		}

		private void startTimer ()
		{
			stopAlarmSound ();
			startButton.setVisible (false);
			pauseButton.setVisible (true);

			if (currentSeconds <= 0)
			{
				currentSeconds = isWorkTime ? workTimeSeconds :
				                 (workSessionsCount >= cyclesBeforeLongBreak ? longBreakSeconds : shortBreakSeconds);
			}
			timer.start ();
		}

		private void pauseTimer ()
		{
			timer.stop ();
			pauseButton.setVisible (false);
			startButton.setVisible (true);
		}

		private void resetTimer ()
		{
			stopAlarmSound ();
			timer.stop ();
			currentSeconds = workTimeSeconds;
			workSessionsCount = 0;
			isWorkTime = true;
			timeLabel.setText (formatTime (currentSeconds));
			pauseButton.setVisible (false);
			startButton.setVisible (true);
			startButton.setText ("Start");
			loadImage ("/Tomato.png");
		}

		private void openSettings ()
		{
			SettingsWindow settings = new SettingsWindow (this);
			settings.setLocationRelativeTo (this);
			settings.setVisible (true);
		}

		private void showNotification (String message)
		{
			String script = String.format (
			  "display dialog \"%s\" with title \"Tomato\" buttons {\"OK\", \"Cancel\"} default button \"OK\"",
			  message.replace ("\"", "\\\"")
			                              );

			try
			{
				playAlarmSound ();
				Process process = Runtime.getRuntime ().exec (new String[] {"osascript", "-e", script});

				BufferedReader reader = new BufferedReader (
				  new InputStreamReader (process.getInputStream ())
				);
				String result = reader.readLine ();
				process.waitFor ();
				stopAlarmSound ();

				if (result != null && result.contains ("OK"))
				{
					startTimer ();
				}
				else
				{
					pauseButton.setVisible (false);
					startButton.setVisible (true);
				}
			}
			catch (IOException | InterruptedException e)
			{
				LOGGER.log (Level.SEVERE, "Notification error", e);
				stopAlarmSound ();
			}
		}

		private void loadSettings ()
		{
			String home = System.getProperty ("user.home");
			File configDir = new File (home, "Library/Application Support/Tomato");
			File configFile = new File (configDir, "Settings.cfg");

			int work = 25;
			int shortBreak = 5;
			int longBreak = 15;
			int cycles = 4;
			boolean debug = false;
			boolean configValid = false;

			if (configFile.exists ())
			{
				try (BufferedReader reader = new BufferedReader (new FileReader (configFile)))
				{
					work = Integer.parseInt (reader.readLine ());
					shortBreak = Integer.parseInt (reader.readLine ());
					longBreak = Integer.parseInt (reader.readLine ());
					cycles = Integer.parseInt (reader.readLine ());
					debug = Boolean.parseBoolean (reader.readLine ());

					if (validateSettings (work, shortBreak, longBreak, cycles))
					{
						configValid = true;
					}
				}
				catch (Exception e)
				{
					LOGGER.log (Level.SEVERE, "Error reading settings", e);
				}
			}

			if (!configValid)
			{
				if (!configDir.exists () && !configDir.mkdirs ())
				{
					LOGGER.severe ("Failed to create config directory: " + configDir);
				}
				try (PrintWriter writer = new PrintWriter (configFile))
				{
					writer.println (work);
					writer.println (shortBreak);
					writer.println (longBreak);
					writer.println (cycles);
					writer.println (debug);
				}
				catch (IOException e)
				{
					LOGGER.log (Level.SEVERE, "Error saving settings", e);
				}
			}

			workTimeSeconds = debug ? work : work * 60;
			shortBreakSeconds = debug ? shortBreak : shortBreak * 60;
			longBreakSeconds = debug ? longBreak : longBreak * 60;
			cyclesBeforeLongBreak = cycles;
		}

		private boolean validateSettings (int work, int sb, int lb, int cycles)
		{
			return work > 0 && work < 60 &&
			       sb > 0 && sb < 60 &&
			       lb > 0 && lb < 60 &&
			       cycles > 0 && cycles < 10;
		}

		public void updateSettings (int work, int sb, int lb, int cycles, boolean debug)
		{
			workTimeSeconds = debug ? work : work * 60;
			shortBreakSeconds = debug ? sb : sb * 60;
			longBreakSeconds = debug ? lb : lb * 60;
			cyclesBeforeLongBreak = cycles;
			resetTimer ();
		}
	}

	static class SettingsWindow extends JDialog
	{
		private final JTextField workField = new JTextField ();
		private final JTextField shortBreakField = new JTextField ();
		private final JTextField longBreakField = new JTextField ();
		private final JTextField cyclesField = new JTextField ();
		private final MainWindow mainWindow;
		private boolean debugMode = false;

		public SettingsWindow (MainWindow owner)
		{
			super (owner, "Settings", true);
			this.mainWindow = owner;
			setSize (400, 275);
			setLayout (null);
			initUI ();
			loadSettings ();
			setupDebugHotkey ();
		}

		private void initUI ()
		{
			JLabel title = new JLabel ("Settings");
			title.setFont (new Font ("Avenir Next", Font.BOLD, 30));
			title.setBounds (10, 10, 300, 40);
			add (title);

			addField ("Work time (min):", 70, workField);
			addField ("Short break (min):", 100, shortBreakField);
			addField ("Long break (min):", 130, longBreakField);
			addField ("Cycles:", 160, cyclesField);

			createButton ("Save", 290, _ -> saveSettings ()).setBackground (new Color (84, 153, 199));
			createButton ("Close", 185, _ -> dispose ());
		}

		private void addField (String label, int y, JTextField field)
		{
			JLabel title = new JLabel (label);
			title.setBounds (10, y, 250, 20);
			add (title);
			field.setBounds (260, y, 130, 25);
			add (field);
		}

		private JButton createButton (String text, int x, ActionListener action)
		{
			JButton button = new JButton (text);
			button.setBounds (x, 210, 100, 30);
			button.addActionListener (action);
			add (button);
			return button;
		}

		private void loadSettings ()
		{
			String home = System.getProperty ("user.home");
			File configFile = new File (home, "Library/Application Support/Tomato/Settings.cfg");

			if (configFile.exists ())
			{
				try (BufferedReader reader = new BufferedReader (new FileReader (configFile)))
				{
					workField.setText (reader.readLine ());
					shortBreakField.setText (reader.readLine ());
					longBreakField.setText (reader.readLine ());
					cyclesField.setText (reader.readLine ());
					String debugLine = reader.readLine ();
					debugMode = Boolean.parseBoolean (debugLine);
				}
				catch (Exception e)
				{
					LOGGER.log (Level.SEVERE, "Error loading settings", e);
				}
			}
		}

		private void saveSettings ()
		{
			try
			{
				int work = Integer.parseInt (workField.getText ());
				int sb = Integer.parseInt (shortBreakField.getText ());
				int lb = Integer.parseInt (longBreakField.getText ());
				int cycles = Integer.parseInt (cyclesField.getText ());

				if (!mainWindow.validateSettings (work, sb, lb, cycles))
				{
					JOptionPane.showMessageDialog (this,
					                               "Values must be between 1-59 for times and 1-9 for cycles",
					                               "Invalid Settings",
					                               JOptionPane.ERROR_MESSAGE
					                              );
					return;
				}

				String home = System.getProperty ("user.home");
				File configDir = new File (home, "Library/Application Support/Tomato");
				if (!configDir.exists () && !configDir.mkdirs ())
				{
					LOGGER.severe ("Failed to create config directory: " + configDir);
				}

				try (PrintWriter writer = new PrintWriter (new File (configDir, "Settings.cfg")))
				{
					writer.println (work);
					writer.println (sb);
					writer.println (lb);
					writer.println (cycles);
					writer.println (debugMode);
				}

				mainWindow.updateSettings (work, sb, lb, cycles, debugMode);
				dispose ();
			}
			catch (NumberFormatException e)
			{
				JOptionPane.showMessageDialog (this,
				                               "Please enter valid numbers",
				                               "Input Error",
				                               JOptionPane.ERROR_MESSAGE
				                              );
			}
			catch (IOException e)
			{
				LOGGER.log (Level.SEVERE, "Error saving settings", e);
			}
		}

		private void setupDebugHotkey ()
		{
			KeyStroke debugKeyStroke = KeyStroke.getKeyStroke (KeyEvent.VK_D, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMaskEx () | InputEvent.ALT_DOWN_MASK);

			InputMap inputMap = getRootPane ().getInputMap (JComponent.WHEN_IN_FOCUSED_WINDOW);
			ActionMap actionMap = getRootPane ().getActionMap ();

			inputMap.put (debugKeyStroke, "toggleDebug");
			actionMap.put ("toggleDebug", new AbstractAction ()
			{
				@Override
				public void actionPerformed (ActionEvent e)
				{
					debugMode = !debugMode;
					saveDebugToFile ();
					JOptionPane.showMessageDialog (SettingsWindow.this,
					                               "Debug mode is now " + (debugMode ? "ON" : "OFF"),
					                               "Debug Mode",
					                               debugMode ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE
					                              );
				}
			});
		}

		private void saveDebugToFile ()
		{
			String home = System.getProperty ("user.home");
			File configFile = new File (home, "Library/Application Support/Tomato/Settings.cfg");

			try
			{
				String[] lines = new String[5];
				if (configFile.exists ())
				{
					try (BufferedReader reader = new BufferedReader (new FileReader (configFile)))
					{
						for (int i = 0; i < 4; i++)
						{
							lines[i] = reader.readLine ();
						}
						reader.readLine ();
					}
				}
				else
				{
					lines[0] = "25";
					lines[1] = "5";
					lines[2] = "15";
					lines[3] = "4";
				}
				lines[4] = Boolean.toString (debugMode);

				try (PrintWriter writer = new PrintWriter (configFile))
				{
					for (String line : lines)
					{
						writer.println (line != null ? line : "");
					}
				}
			}
			catch (IOException ex)
			{
				LOGGER.log (Level.SEVERE, "Error saving debug mode", ex);
			}
		}
	}
}