package Processing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.Main;
import main.SimpleTable;



public class BestandsdatenProcessor extends AbstractProcessor{
	
	private final static ArrayList<String> siegel = new ArrayList<String>(Arrays.asList(initSiegel()));
	
	
	private final static ArrayList<String> RVKs = new ArrayList<String>(Arrays.asList(new String[]{
		"AA","AB","AC","AD","AE","AF","AK","AL","AM","AN","AP","AR","AW","AX","AZ",
		"BA","CA","CL","DA",
		"EA","EG","EK","EL","EP","EQ",
		"FA","GA","HC","IA","KA","LA","LD","MA","MN","MX","NA",
		"PA","PB","PD","PE","PF","PG","PH","PI","PM","PO","PP","PQ","PR","PS","PT","PU","PV","PW","PX","PY","PZ",
		"QA","RA","SA","SQ","TA","TD","TE","UA","VA","XA","ZA","ZX",
		"ZG","ZH","ZI","ZK","ZL","ZM","ZN","ZO","ZP","ZQ","ZS"}));
	
	static ArrayList<String> removed = new ArrayList<String>();
	static ArrayList<String> kept = new ArrayList<String>();
	
	
	private static String[] initSiegel() {
		if (Main.twentyfourInOne)
			return new String[]{ "188/25", "188/726", "188/802", "188/812", "188/814", 
				"188/816", "188/822", "188/823", "188/827", "188/839", "188/867", "188/869", 
				"188/870JA", "188/870KO", "188/870SI", "188/871", "188/877", "188/879", 
				"188/885", "188/896", "188/898", "188/908", "188/912" };
		return new String[]{"1", "1a", "11", "83", "109", "578/M"};
	}
	
	
	
	
	@Override
	protected void processOne(String zdbid) {
		if (zdbid.equals("")) {
			return;
		}
		table.newCell(SimpleTable.toLink("http://dispatch.opac.d-nb.de/PRS=HOL/CMD?ACT=SRCHM&IKT0=8506&TRM0="+zdbid+"&HLIB=255&PRSHOLDINGOPTION=BER", "Berlin-Bestand"));
		
		
		String code = Main.getSourceCode("http://dispatch.opac.d-nb.de/PRS=HOL/CMD?ACT=SRCHM&IKT0=8506&TRM0="+zdbid+"&HLIB=255&PRSHOLDINGOPTION=BER&PRSHOLDINGOPTION=BAY");
		
		
		Pattern pattern = Pattern.compile(".*<TABLE>.*?Bibliothek: B.*?</TABLE>");
		Matcher matcher = pattern.matcher(code);
		while(matcher.find()) {
			
			
		}
		
		code = code.substring(code.indexOf("Bibliothek: "));
		code = code.substring(0, code.indexOf("</TABLE>")); //make sure we dont include crap after the table
		code = Main.cleanCode(code);
		code = code.substring(16); //remove the first "Bibliothek: BER" so split works as intended
		
		code = code.replaceAll(":[[\n][\\s]+]", ": ");// : \n whitespace -> :_
		code = code.replaceAll("\nFernleihe:[^\n]+", "");

		String RVK = getRVK(code);
		code = code.replaceAll("(?s)Bibliothek: BAY.+?Bibliothek: BER", "Bibliothek: BER");//Bayern raus, am ende bleibt was übrig
		
		code = code.replaceAll("\nGrundsignatur:[^\n]+", "");
		code = code.replaceAll("\nStandort:[^\n]+", "");
		
		code = code.replaceAll(">.+?\nBestand", ">");
		code = code.replace("\n", "");
		String[] ar = code.split("Bibliothek: \\p{javaUpperCase}{3} ");

		String siegel = getSiegel(ar);
		table.newCell(siegel);
		table.newCell(RVK);
	}
	
	
	private static String getSiegel(String[] ar) {
		
		String result = "";
		for (String s: ar) {
			int begin = s.indexOf('<');
			int end = s.indexOf('>');
			String currSiegel = s.substring(begin+1, end);
			

			s = removeBrackets(s);
			if (!Main.twentyfourInOne && currSiegel.contains("188")) {
				result += SimpleTable.toBold(s.substring(begin, end+1)) + s.substring(end+1) + "\n";
			}
			else if (Main.twentyfourInOne && siegel.contains(currSiegel)) {
				result += s + "\n";
			}
		}
		return result.trim();
	}
	
	
	private static String getRVK(String code) {
		ArrayList<String> RVK = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\\p{javaUpperCase}\\p{javaUpperCase} \\d{3,}");
		Matcher matcher = pattern.matcher(code);
		while(matcher.find()) {
			StringBuffer buffer = new StringBuffer(code).reverse();
			int siegelIndex = code.length() - buffer.indexOf("<", code.length() - matcher.start());
			String siegel = code.substring(siegelIndex-1, code.indexOf(">", siegelIndex)+1);

			if (siegel.equals("<109>")) //109 hat keine RVKs
				continue;

			String currRVK = matcher.group();
			int start = matcher.start();
			int end = matcher.end();
			int nextNewLine = code.indexOf("\n", end);
			if(nextNewLine == -1) //if this is the last entry
				nextNewLine = code.length();
			String test = code.substring(start, nextNewLine);
			if (test.matches("\\S\\S [\\d]+ \\p{javaUpperCase}[^\n]+"))
				currRVK = test;
			
			if (!RVKs.contains(currRVK.substring(0, 2))) {
				removed.add(currRVK);
				continue;
			}
			kept.add(currRVK);
			if(siegel.equals("<188>") && currRVK.startsWith("ZI")) {
				//188 mit diesen Buchstaben sind keine RVKs
				continue;
			}
			
			
			boolean contains = false;
			for (int i = 0; i < RVK.size(); i++) {
				String curr = RVK.get(i);
				if (curr.startsWith(currRVK)) {
					if(!curr.contains(siegel)) {
						RVK.set(i, curr + ", " + ((siegel.equals("<188>") || siegel.equals("<355>")) ? SimpleTable.toBold(siegel) : siegel)); //fett wenn UB oder regensburg
					}
					contains = true;
					break;
				}
			}
			if (!contains) {
				RVK.add(currRVK + ": " + ((siegel.equals("<188>") || siegel.equals("<355>")) ? SimpleTable.toBold(siegel) : siegel));
			}
		}
		
		Collections.sort(RVK);
		String result = "";
		for (String s: RVK) {
			//highlight the RVK if a highlighted library assigned that RVK
			if (SimpleTable.containsBold(s)) {
				s = SimpleTable.toBold(s.substring(0, s.indexOf(":"))) + s.substring(s.indexOf(":"));
			}
			result += s + "\n";
		}
		return result.trim();
	}
	
	
	private static String removeBrackets(String s) {
		String result = s;
		Pattern pattern = Pattern.compile("\\[[^\\]]+\\]");
		Matcher matcher = pattern.matcher(result);
		while (matcher.find()) {
			if (matcher.group().startsWith("[Standort:") ||
					matcher.group().contains("Lesesaal") ||
					matcher.group().contains("auslage") ||
					matcher.group().contains("prüfen Sie die V") ||
					matcher.group().contains("Bestand geprüft") ||
					matcher.group().contains("Bezug abgebr")) {
				result = result.replace(matcher.group(), "");
				matcher.reset(result);
			}
		}
		result = result.replaceAll("\n?Bestandslücken:", "");
		return result;
	}
	
	
	static void output() {
		System.out.println("\n\n\n\n\nremoved");
		Collections.sort(removed);
		Collections.sort(kept);
		for (String s: removed)
			System.out.println(s);
		System.out.println("\n\n\n\n\nkept");
		for(String s: kept)
			System.out.println(s);
	}
}
