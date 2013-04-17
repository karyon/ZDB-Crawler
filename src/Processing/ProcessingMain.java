package Processing;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;

import main.Main;
import main.SimpleTable;





public class ProcessingMain {
	private static boolean SFXLINKS = false;
	private static boolean TITELDATEN = false;
	private static boolean BESTANDSDATEN = false;
	private static String errorMessages = "";
	
	
	
	public static SimpleTable processIDList(String inputString) throws ParseException {
		String[] lines = handleNewLinesInCells(inputString);
		String[][] allColumns = formatInput(lines);
		String[][] columns = checkAndSortInput(allColumns);
		
		String[] ZDBIDs = columns[0];
		String[] ISSNs = columns[1];
		
		int numberOfSteps = 0;
		if (SFXLINKS)
			numberOfSteps += 1;
		if (TITELDATEN)
				numberOfSteps += 1;
		if (BESTANDSDATEN)
			numberOfSteps += 1;
		Main.setTotalSteps(numberOfSteps * ((ZDBIDs == null) ? ISSNs.length : ZDBIDs.length));
		
		ArrayList<SimpleTable> resultTables = new ArrayList<SimpleTable>();
		if (SFXLINKS) {
			ISSNProcessor issnProcessor = new ISSNProcessor();
			resultTables.add(issnProcessor.process(ISSNs));
		}
		if (TITELDATEN) {
			TiteldatenProcessor titeldatenProcessor = new TiteldatenProcessor();
			resultTables.add(titeldatenProcessor.process(ZDBIDs));
		}
		if (BESTANDSDATEN) {
			BestandsdatenProcessor bestandsdatenProcessor = new BestandsdatenProcessor();
			resultTables.add(bestandsdatenProcessor.process(ZDBIDs));
		}
		//concatenate all resultTables to the first resultTable
		for(int i = 1; i < resultTables.size(); i++) {
			resultTables.get(0).concat(resultTables.get(i));
		}
		
		BestandsdatenProcessor.output();
		return resultTables.get(0);
	}
	
	
	/**
	 * In the input, \n could mean a new line in the table OR a new line in a cell.
	 * Each String in the returned array represents one line of the table.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	private static String[] handleNewLinesInCells(String input) throws ParseException {

		String[] lines = input.replace("*", "").split("\n");
		
		int maxTabs = 0;
		for (String line: lines) {
			maxTabs = Math.max(maxTabs, countTabs(line));
		}
		
		int newLineIndex = 0;
		for (int oldLineIndex = 0; oldLineIndex < lines.length; newLineIndex++, oldLineIndex++) {
			lines[newLineIndex] = lines[oldLineIndex];
			
			//everything from here does not get executed when line length is ok
			while(countTabs(lines[newLineIndex]) < maxTabs) {
				try{
					String nextLine = lines[++oldLineIndex];
					lines[newLineIndex] += "\n" + nextLine;
				}
				catch (ArrayIndexOutOfBoundsException e) {
					break;
				}
			}
			//line is too long, error out
			if (countTabs(lines[newLineIndex]) != maxTabs) {
				int longestLineIndex = 0;
				while(countTabs(lines[longestLineIndex]) < maxTabs)
					longestLineIndex++;
				throw new ParseException("Fehler bei der Behandlung von Zeilenumbrüchen. \n" + 
						"In der Eingabe wurden (z.B. in Zeile " + (longestLineIndex+1) + ") " + (maxTabs+1) + " Spalten erkannt, "+
						" in Zeile " + (newLineIndex+1) + " scheint die Spaltenanzahl anders zu sein. \n" +
						"1. Wurde die Eingabe wirklich aus Excel herauskopiert? \n" +
						"2. Falls die erkannte Anzahl der Spaltennicht stimmt, überprüfen Sie  Zeile " + (longestLineIndex+1) + " auf Unregelmäßigkeiten.\n" +
						"3. Als letztes können Sie Zeile " + (newLineIndex+1) + " auf Unregelmäßigkeiten überprüfen.", newLineIndex+1);
			}
		}
		lines = Arrays.copyOf(lines, newLineIndex);
		return lines;
	}
	
	
	
	/**
	 * Returns the input differently formatted. First index of returned array is
	 * the column index, second is the line index.
	 * @param input
	 * @return
	 */
	private static String[][] formatInput(String[] lines) {
		int columncount = 1 + countTabs(lines[0]);
		String[][] columns = new String[columncount][lines.length];
		
		for (int i = 0; i < lines.length; i++) {
			String[] linesplit = lines[i].split("\t");
			for (int j = 0; j < columncount; j++) {
				if (j < linesplit.length)
					columns[j][i] = linesplit[j].trim();
				else
					columns[j][i] = "";
			}
		}
		return columns;
	}
	
	
	/** 
	 * Returns the number of occurrences of the '\t' character in the specified String
	 */
	private static int countTabs(String text) {
		return (text.length() - text.replace("\t", "").length());
	}
	
	
	
	/**
	 * Checks the input for correctness and returns an array containing two arrays,
	 * one for ZDBIDs and one for ISSNs.
	 * @param input
	 * @return
	 * @throws ParseException 	if any problems while parsing the input array occurred
	 */
	private static String[][] checkAndSortInput(String[][] input) throws ParseException {
		String[] ZDBIDs = null;
		String[] ISSNs = null;
		
		for (int i = 0; i < input.length; i++) {
			double[] counts = getZdbidIssnJunkWhitespacePercentages(input[i]);
			if (counts[0] > 0.75)
				ZDBIDs = input[i];
			else if (counts[1] > 0.75)
				ISSNs = input[i];
		}
		

		if (ZDBIDs == null && ISSNs == null) {
			throw new ParseException("Es wurde weder eine ZDBID- noch eine ISSN-Spalte gefunden.\n" +
					"Bitte überprüfen Sie Ihre Eingabe.", -1);
		}
			
		if (ZDBIDs != null) {
			TITELDATEN = true;
			BESTANDSDATEN = true;
			isPureZDBID(ZDBIDs);
			System.out.println("Input ZDBIDs: \n" + Arrays.toString(ZDBIDs));
		}
		
		if (ISSNs != null) {
			SFXLINKS = true;
			cleanISSNs(ISSNs);
			System.out.println("Input ISSNs: \n" + Arrays.toString(ISSNs));
		}
		
		if (!errorMessages.equals("")) {
			throw new ParseException("Die Eingabe war fehlerhaft: " + errorMessages + "\nBitte korrigieren Sie die angegebenen Stellen in Ihrer Tabelle.", -1);
		}
		
		return new String[][]{ZDBIDs, ISSNs};
	}
	
	
	private static void isPureZDBID(String[] ZDBIDs) {
		for (int i = 0; i < ZDBIDs.length; i++) {
			if (!ZDBIDs[i].equals("") && !isZDBID(ZDBIDs[i])) {
				//do not tolerate any non-ZDBID
				errorMessages += "\nZeile " + (i+1) + ": \"" + ZDBIDs[i] + "\" wurde nicht als ZDB-ID erkannt.";
			}
		}
	}
	
	
	private static void cleanISSNs(String[] ISSNs) {
		for (int i = 0; i < ISSNs.length; i++) {
			if (!ISSNs[i].equals("") && !isISSN(ISSNs[i])) {
				ISSNs[i] = "";
			}
		}
	}
	
	private static double[] getZdbidIssnJunkWhitespacePercentages(String[] temp) {
		double[] result = {0, 0, 0, 0};
		for (String s: temp) {
			if(isZDBID(s))
				result[0]++;
			else if(isISSN(s))
				result[1]++;
			else if(s.equals(""))
				result[3]++;
			else
				result[2]++;
		}
		double nonWhitespaceCount = result[0] + result[1] + result[2];
		for (int i = 0; i < result.length-1; i++)
			result[i] /= nonWhitespaceCount;
		result[3] /= temp.length;
		return result;
	}
	
	
	
	/**
	 * Checks whether a String could be an ISSN.
	 * A String is considered to be an ISSN if it has the form 4 digits, hypen, 3 digits, any character.
	 * @param s
	 * @return
	 */
	private static boolean isISSN(String s) {
		return s.matches("\\d{4}-\\d{3}.");
	}
	
	
	
	/**
	 * Checks whether a String could be an ZDBID.
	 * A String is considered to be an ZDB-ID if it has the form 2 or more digits, hypen, any character.
	 * @param s
	 * @return
	 */
	private static boolean isZDBID(String s) {
		return s.matches("\\d{2,}-.");
	}
	
	
	
	public static void clear() {
		SFXLINKS = false;
		TITELDATEN = false;
		BESTANDSDATEN = false;
		errorMessages = "";
	}
}
