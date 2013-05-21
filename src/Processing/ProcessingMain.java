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
		String[][] allColumns = handleNewLinesInCells(inputString);
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
	 * 
	 * In the input, \n could mean a new line in the table OR a new line in a cell.
	 * Each String in the returned array represents one line of the table.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	private static String[][] handleNewLinesInCells(String input) throws ParseException {
		//TODO move this somewhere else
		input = input.replace("*", "");
		
		input = helpSplitting(input);
		
		ArrayList<ArrayList<String>> lines = parseCells(input);
		checkCellNumberPerLine(lines);
		
		cleanCells(lines);

		String[][] everythingToArray = formatInputList(lines);

		return everythingToArray;
	}


	/**
	 * Removes doublequotes (these are quotes in a cell) 
	 * and inserts "" for empty cells.
	 * @param input
	 * @return
	 */
	private static String helpSplitting(String input) {
		//remove all in-cell-quotes
		input = input.replace("\"\"", "");
		//replace empty cells by '""'
		input = input.replace("\t\t", "\t\"\"\t");
		input = input.replace("\t\n", "\t\"\"\n");
		input = input.replace("\n\t", "\n\"\"\t");
		return input;
	}
	
	
	/**
	 * Parses the input string into a twodimensional list representing lines 
	 * and cells of the input.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	private static ArrayList<ArrayList<String>> parseCells(String input) throws ParseException {
		ArrayList<String> currentLine = new ArrayList<String>();
		ArrayList<ArrayList<String>> lines = new ArrayList<ArrayList<String>>();
		lines.add(currentLine);
		
		String[] cellarray = input.split("\t");
		for (int i = 0; i < cellarray.length; i++) {
			String cell = cellarray[i];
			
			String[] split;
			if (cell.startsWith("\"")) {
				int indexOfSecondQuote = cell.indexOf("\"", 1);
				String firstCell = cell.substring(0, indexOfSecondQuote+1);
				String rest = cell.substring(indexOfSecondQuote+1);
				if (rest.startsWith("\n"))
					split = new String[]{firstCell, rest.substring(1)};
				else
					split = new String[]{firstCell};
			}
			else
				split = cell.split("\n");
			
			currentLine.add(split[0]);
			//encountered a newline which actually means a new line in the table, so start a new line.
			if (split.length == 2 && i != cellarray.length-1) {
				currentLine = new ArrayList<String>();
				lines.add(currentLine);
				currentLine.add(split[1]);
			}
			else if (split.length > 2) //sanity check
				throw new ParseException("Fehler bei der Behandlung von Zeilenumbrüchen."
						+ "Bitte überprüfen Sie die Zeilen " + lines.size() + " und " + (lines.size()+1) + "auf Unregelmäßigkeiten.", -1);
		}
		return lines;
	}
	
	
	/**
	 * removes quotes and whitespace from the beginning and end of each cell.
	 * @param lines
	 */
	private static void cleanCells(ArrayList<ArrayList<String>> lines) {
		for (ArrayList<String> line: lines) 
			for (int i = 0; i < line.size(); i++) 
				line.set(i, line.get(i).replace("\"", "").trim());
	}
	
	
	/**
	 * Checks whether each list in the specified list has the same size.
	 * @param lines
	 * @throws ParseException
	 */
	private static void checkCellNumberPerLine(
			ArrayList<ArrayList<String>> lines) throws ParseException {
		int columnCount = lines.get(0).size();
		for (int i = 0; i < lines.size(); i++) {
			ArrayList<String> line = lines.get(i);
			if (line.size() != columnCount) {
				throw new ParseException("Fehler bei der Behandlung von Zeilenumbrüchen. \n" + 
						"In der ersten Zeile der Eingabe wurden" + (columnCount) + " Spalten, \n"+
						" in Zeile " + (i+1) + " aber " + line.size() + "Spalten erkannt.\n" +
						"1. Wurde die Eingabe wirklich aus Excel herauskopiert? \n" +
						"2. Überprüfen Sie die Zeile, deren genannte Spaltenanzahl nicht stimmt, auf Unregelmäßigkeiten.", -1);
			}
		}
	}
	
	
	/**
	 * Returns the input list as a flipped array. First index of returned array is
	 * the column index, second is the line index.
	 * @param input
	 * @return
	 */
	private static String[][] formatInputList(ArrayList<ArrayList<String>> lines) {
		int numColumns = lines.get(0).size();
		String[][] columns = new String[numColumns][lines.size()];
		for (int i = 0; i < lines.size(); i++)
			for (int j = 0; j < numColumns; j++)
				columns[j][i] = lines.get(i).get(j);
		
		return columns;
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
