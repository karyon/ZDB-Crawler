package main;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import Processing.ProcessingMain;


public class Main {
	
	private static JFrame frame;
	private final static JLabel infoLabel = new JLabel();
	
	private static int stepsDone = 0;
	private static int totalSteps = 0;
	
	public static boolean twentyfourInOne = true;
	
	
	public static void main(String[] args) {
//		 try {
//			UIManager.setLookAndFeel(
//				        UIManager.getSystemLookAndFeelClassName());
//		} catch (ClassNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (InstantiationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IllegalAccessException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (UnsupportedLookAndFeelException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		buildAndShowGui();
	}
	
	
	/**
	 * 
	 */
	private static void buildAndShowGui() {
		String windowTitle = "ZDB-Programm";
		if (twentyfourInOne)
			windowTitle += " 24in1-Version";
		frame = new JFrame(windowTitle);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		final JTextArea textArea = new JTextArea("", 1,28);
		textArea.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
//		textArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
		final Document doc = textArea.getDocument();
		
		doc.addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				
				final String input = textArea.getText();
				textArea.setEnabled(false);
				frame.repaint();
				
				Thread t = new Thread() {
					@Override
					public void run() {
						try {
//							throw new RuntimeException();
							SimpleTable result = ProcessingMain.processIDList(input);
							if (result == null) {
								showErrorAndRestart("Ein kritischer Fehler aus unbekanntem Grund trat auf.");
								restart();
								return;
							}
							writeToClipboard(HtmlTable.simpleTableToString(result));
							showEndDialog();
						} 
						catch (ParseException e) {
							showErrorAndRestart(e.getMessage());
						}
//						catch(Exception e) {
//							System.err.println("Should not have happened: " + e.getClass().getName() + ": " + e.getMessage());
//							System.out.println("\n\n");
//							e.printStackTrace();
//							//TODO errorhandling...?
//							showErrorAndRestart("Ein unvorhergeseher Fehler trat auf.\n" +
//									"Dagegen können Sie leider nichts machen.");
//							System.exit(-1);
//						}
						//Abgeschlossen
						restart();
					}
				};
				t.start();
			}
			public void removeUpdate(DocumentEvent e) {}
			public void changedUpdate(DocumentEvent e) {}
		});
		
		
//		JLabel manualLabel = new JLabel("<html> Hello<br>test<br>test2", JLabel.CENTER);
//		JTabbedPane tabbedPane = new JTabbedPane();

//		tabbedPane.add("Eingabe", textArea);
//		tabbedPane.add("Test", manualLabel);
//		tabbedPane.add("Test", new JPanel());
//		tabbedPane.setPreferredSize(new Dimension(200, 200));
		
//		tabbedPane.setBackground(null);
		JPanel textPanel = new JPanel();
		textPanel.add(textArea, BorderLayout.CENTER);
		//textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		frame.add(textPanel, BorderLayout.WEST);
//		frame.add(tabbedPane, BorderLayout.CENTER);
		frame.add(infoLabel, BorderLayout.SOUTH);
		
//		BorderLayout bl = new BorderLayout();
//		bl.
		JButton aboutButton = new JButton ("About") {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension (50, 20);
			}
		};
//		JPanel southPanel = new JPanel();
//		southPanel.add(infoLabel);
//		southPanel.add(aboutButton);
//		aboutButton.setPreferredSize(new Dimension (40, 5));
		aboutButton.setMargin(new Insets(0, 0, 0, 0));

//		frame.add(aboutButton, BorderLayout.EAST);
		
		setInfoText("ZDB-IDs oder ISSNs oder beides eingeben");
		frame.pack();
		
		//Place window at the center of the screen
		Dimension windowSize = frame.getSize();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - windowSize.width) / 2;
        int y = (screenSize.height - windowSize.height) / 2;
        frame.setLocation(x, y);
        
		frame.setVisible(true);
	}
	
	
	/**
	 * Cleans html-code from html-tags, removes excessive whitespace like multiple
	 * empty lines, and replaces some html-encoded special characters with the actual character.
	 * @param code
	 * @return
	 */
	public static String cleanCode(String code) {
		String result = code.replaceAll("<[^>]+>", " "); //html-tags
		result = result.replaceAll("[[\n][\\s&&[^\n]]+]+[\n][\\s]+", "\n");//whitespace
		result = result.replaceAll("&lt;", "<");
		result = result.replaceAll("&gt;", ">");
		result = result.replaceAll("&uuml;", "ü");
		result = result.replaceAll("&nbsp;", " ");
		return result.trim();
	}
	
	
	
	/**
	 * Returns the sourcecode of the webpage at the specified URL.
	 * @param link
	 * @return
	 */
	public static String getSourceCode(String link) {
        try {
        	InputStreamReader in = new InputStreamReader(new URL(link).openStream());
        	BufferedReader br = new BufferedReader(in);
        	StringBuffer buffer = new StringBuffer();
            String c;
            while((c = br.readLine()) != null){
            	buffer.append(c + "\n");
            }
            in.close();
            String result = buffer.toString();
            return result;
        } 
        catch(IOException ie) {
        	showErrorAndRestart("Es gab einen Fehler bei einem Seitenaufruf. Funktioniert die Internetverbindung und die Seite der ZDB und SFX?");
        	System.exit(-1);
        }

        return null;
	}
	
	
	/**
	 * Writes the specified String into the clipboard.
	 */
	private static void writeToClipboard(String input) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
				new StringSelection(input), null
		);
		return;
		
		/*JFileChooser fc = new JFileChooser();
		fc.setSelectedFile(new File(System.getProperty("user.home") + "/Desktop"+"/tabelle.html"));
		int returnVal = fc.showSaveDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			//now write
		}*/
	}
	
	
	private static void showManual() {
		System.out.println("sup");
		InputStream is = Main.class.getResourceAsStream("/resources/AnleitungZDB.odt");
		try {
			File temp = File.createTempFile("anleitung", ".odt");
			temp.deleteOnExit();

			OutputStream out = new FileOutputStream(temp);
			byte buf[] = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0)
				out.write(buf, 0, len);
			out.close();
			is.close();
			Desktop.getDesktop().open(temp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Shows the specified text in the information bar.
	 * @param text
	 */
	public static void setInfoText(String text) {
		infoLabel.setText(text);
		infoLabel.repaint();
	}
	
	
	/**
	 * Shows an error popup with the specified error message.
	 * @param message
	 */
	public static void showErrorAndRestart(String message) {
		JOptionPane.showMessageDialog(frame, message, "Fehler", JOptionPane.ERROR_MESSAGE);
	}
	
	
	private static void showEndDialog() {
		String message = "Fertig. Jetzt nur noch die erste Zelle des Bereiches anklicken, " +
				"in den die Ergebnisse sollen,\n und Strg+V drücken, um die Ergebnisse einzufügen.";
		JOptionPane.showMessageDialog(frame, message, "Fertig", JOptionPane.INFORMATION_MESSAGE);
	}
	
	
	/**
	 * Resets all internal variables.
	 */
	public static void restart() {
		JFrame oldFrame = frame;
		buildAndShowGui();
		oldFrame.dispose();
		ProcessingMain.clear();
		stepsDone = 0;
		totalSteps = 0;
		setInfoText("Neue Abfrage kann gestartet werden");
	}
	
	
	/**
	 * Set the total number of steps, used for progress information.
	 * @param totalSteps
	 */
	public static void setTotalSteps(int totalSteps) {
		Main.totalSteps = totalSteps;
	}
	
	
	/**
	 * Updates the progress information.
	 */
	public static void stepDone() {
		stepsDone++;
		setInfoText("Bearbeite... " + 100*stepsDone/totalSteps + "% fertig");
	}
}
