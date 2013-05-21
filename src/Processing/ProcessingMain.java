package Processing;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.com.bytecode.opencsv.CSVReader;

import main.Main;
import main.SimpleTable;



public class ProcessingMain {
	private static boolean SFXLINKS = false;
	private static boolean TITELDATEN = false;
	private static boolean BESTANDSDATEN = false;
	private static String errorMessages = "";
	
	
	
	public static SimpleTable processIDList(String inputString) throws ParseException {
		String[][] columns = parseInput(inputString);
		
		String[] ZDBIDs = columns[0];
		String[] ISSNs = columns[1];
		
		initNumberOfSteps(columns[0].length);
		
		SimpleTable resultTable = process(ZDBIDs, ISSNs);
		
		return resultTable;
	}


	/** Parses the input and returns 
	 * @param inputString
	 * @return
	 * @throws ParseException
	 */
	private static String[][] parseInput(String inputString) throws ParseException {
		String[][] lines = parseCells(inputString);
		cleanCells(lines);
		String[][] allColumns = flip2DArray(lines);
		String[][] columns = checkAndSortInput(allColumns);
		return columns;
	}


	/**
	 * Tell Main how many steps will be done, used for progressbar
	 * @param numberOfLines
	 */
	private static void initNumberOfSteps(int numberOfLines) {
		int numberOfSteps = 0;
		if (SFXLINKS)
			numberOfSteps += 1;
		if (TITELDATEN)
				numberOfSteps += 1;
		if (BESTANDSDATEN)
			numberOfSteps += 1;
		Main.setTotalSteps(numberOfSteps * numberOfLines);
	}
	
	
	/**
	 * Parses the input string into a twodimensional list representing lines 
	 * and cells of the input.
	 * @param input
	 * @return
	 * @throws ParseException
	 */
	private static String[][] parseCells(String input) throws ParseException {
		@SuppressWarnings("resource") //no need to close a string reader
		CSVReader reader = new CSVReader(new StringReader(input), '\t');

	    List<String[]> parsedCsv = null;
		try {
			parsedCsv = reader.readAll();
		} catch (IOException e) {
			throw new ParseException(e.getMessage(), -1);
		}
	    
	    String[][] parsedCsvArray = parsedCsv.toArray(new String[][]{});
		
		return parsedCsvArray;
	}


	/**
	 * Central method, starts processing of everything.
	 * @param ZDBIDs
	 * @param ISSNs
	 * @return
	 */
	private static SimpleTable process(String[] ZDBIDs, String[] ISSNs) {
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
		for(int i = 1; i < resultTables.size(); i++)
			resultTables.get(0).concat(resultTables.get(i));
		
		BestandsdatenProcessor.output();
		return resultTables.get(0);
	}
	
	
	/**
	 * removes asterisks and whitespace from the beginning and end of each cell.
	 * @param lines
	 */
	private static void cleanCells(String[][] lines) {
		for (String[] line: lines) 
			for (int i = 0; i < line.length; i++) 
				line[i] = line[i].replace("*", "").trim();
	}
	
	
	/**
	 * Flips the input array, i.e. mirrors it at its 
	 * upperleft-lowerright-diagonal.
	 * @param input
	 * @return
	 */
	private static String[][] flip2DArray(String[][] lines) {
		int numColumns = lines[0].length;
		String[][] columns = new String[numColumns][lines.length];
		for (int i = 0; i < lines.length; i++)
			for (int j = 0; j < numColumns; j++)
				columns[j][i] = lines[i][j];
		
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
