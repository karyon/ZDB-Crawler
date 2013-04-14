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
		System.out.println("Eingabe:\n" + inputString);
		
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
		
		if (input.length == 1) { //one column
			int result = isPureISSNOrZDBID(input[0]);
			
			if (!errorMessages.equals("")) {
				throw new ParseException("Die Eingabe war fehlerhaft:" + errorMessages + "\nBitte korrigieren Sie die angegebenen Stellen in Ihrer Tabelle.", -1);
			}
			
			if (result == 2) {//pure ISSN
				ISSNs = input[0];
				SFXLINKS = true;
			}
			else if (result == 1) { //pure ZDBID
				ZDBIDs = input[0];
				TITELDATEN = true;
				BESTANDSDATEN = true;
			}
		} else { //multiple columns
			SFXLINKS = true;
			TITELDATEN = true;
			BESTANDSDATEN = true;
			String[] firstColumn = input[0];
			String[] lastColumn = input[input.length-1];
			
			
			int result1 = isPureISSNOrZDBID(firstColumn);
			errorMessages = errorMessages.replace("Der erste Wert (", "Der erste Wert der ersten Spalte (");
			int result2 = isPureISSNOrZDBID(lastColumn);
			errorMessages = errorMessages.replace("Der erste Wert (", "Der erste Wert der letzten Spalte (");
			
			if (!errorMessages.equals("")) {
				throw new ParseException("Die Eingabe war fehlerhaft:" + errorMessages + "\nBitte korrigieren Sie die angegebenen Stellen in Ihrer Tabelle.", -1);
			}
			
			if (result1 == result2) {
				throw new ParseException("Die erste und letzte Spalte der Eingabe scheinen die gleiche Nummernart zu enthalten, war das beabsichtigt?", -1);
			}
			
			ZDBIDs = (result1 == 1) ? firstColumn : lastColumn;
			ISSNs = (result1 == 2) ? firstColumn : lastColumn;
		}
		return new String[][]{ZDBIDs, ISSNs};
	}
	
	
	
	/**
	 * Checks whether the specified array contains solely ZDBIDs or ISSNs.
	 * @param temp
	 * @return	1, if the array contains exclusively ZDBIDs (empty Strings are allowed, too)
	 * 			2, if the array contains exclusively ISSNs (empty Strings are allowed, too)
	 * 			-1 otherwise
	 */
	private static int isPureISSNOrZDBID(String[] temp) {
		int i = 0;
		//gehe zum ersten nichtleeren String
		while (i < temp.length && temp[i].equals(""))
			i++;
		if (i == temp.length) {
			errorMessages += "Fehler: Leere Spalte"; 
			return -1;
		}
		boolean isISSN = isISSN(temp[i]);
		boolean isZDBID = isZDBID(temp[i]);
		if (!isISSN && !isZDBID) {
			errorMessages += "\nDer erste Wert (\"" + temp[i] + "\", Zeile " + (i+1) + ") wurde weder als ZDB-ID noch als ISSN erkannt.";
		}
		i++;
		for (;i < temp.length; i++){
			String line = temp[i];
			if (line.equals(""))
					continue;
			if (isISSN && !isISSN(line)) {
				errorMessages += "\nZeile " + (i+1) + ": \"" + line + "\" wurde nicht als ISSN erkannt.";
			}
			if (isZDBID && !isZDBID(line)) {
				errorMessages += "\nZeile " + (i+1) + ": \"" + line + "\" wurde nicht als ZDB-ID erkannt.";
			}
		}
		return (isZDBID) ? 1 : 2;
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
