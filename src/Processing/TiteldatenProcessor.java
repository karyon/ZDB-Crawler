package Processing;
import main.Main;
import main.SimpleTable;


public class TiteldatenProcessor extends AbstractProcessor{
	
	
	
	@Override
	protected void processOne(String zdbid) {
		if(zdbid.equals("")) {
			return;
		}
		
		String code = Main.getSourceCode("http://dispatch.opac.d-nb.de/CMD?ACT=SRCHM&IKT0=8506&TRM0="+zdbid);
		
		if (code.contains("Leider keine Treffer")) {
			table.newCell("Diese ZDB-ID wurde nicht in der ZDB gefunden.");
			return;
		}
		
		code = code.substring(code.indexOf("Titel:"));

		getOnlineLinkAndID(code);
		getDDC(code);
	}
	
	
	private void getDDC(String code) {
		try {
			String temp = code.substring(code.indexOf("DDC")).trim();
			String DDC = temp.substring(temp.indexOf(":")+2, temp.indexOf("</div>"));
			table.newCell(DDC);
		} //gibt keine DDC
		catch (IndexOutOfBoundsException e) {
			table.newCell("");
		}
	}
	
	
	
	private void getOnlineLinkAndID(String code) {
		int onlineIndex = code.indexOf("Online-Ausg");
		if (onlineIndex == -1) {
			table.newCell("");
			table.newCell("");
			table.newCell("");
			return;
		}
		
		String temp = code.substring(onlineIndex);
		int linkBegin = temp.indexOf("a href=\"") + "a href=\"".length();
		if (linkBegin == 7) { //temp.indexOf("a href=\"") was -1, no link found, happens rarely
			table.newCell("");
			table.newCell("");
			table.newCell("");
			return;
		}
		int linkEnd = temp.indexOf("\">", linkBegin);
		String link = "http://dispatch.opac.d-nb.de/" + temp.substring(linkBegin, linkEnd);

		String code2 = Main.getSourceCode(link);
		table.newCell("");
		if (code2.contains("URL:")) {
			code2 = code2.substring(code2.indexOf("URL:"));
			code2 = code2.substring(0, code2.indexOf("\n"));
			code2 = code2.replaceAll("<IMG.+?Kostenfrei>", "kostenfrei");
			while(code2.contains("<a href")) {
				code2 = code2.substring(code2.indexOf("<a href"));

				String URLResult = code2.substring(code2.indexOf("<a href"), code2.indexOf("</div>")) + "\n";

				String target = URLResult.substring(9, URLResult.indexOf("\"", 10));
				String text = URLResult.substring(URLResult.indexOf(">")+1, URLResult.indexOf("<", 10));
				String zusatz = URLResult.substring(URLResult.indexOf("</a>")+4);
				table.addToCurrentCell("\n");
				table.addToCurrentCell(SimpleTable.toLink(target, text));
				table.addToCurrentCell(" " + zusatz.trim());

				code2 = code2.substring(code2.indexOf("</div>"));
			}
		}

		code2 = code2.substring(code2.lastIndexOf("ZDB-ID: ")+"ZDB-ID: ".length());
		String zdbid = Main.cleanCode(code2);
		table.newCell(zdbid);
		table.newCell(SimpleTable.toLink("http://dispatch.opac.d-nb.de/CMD?ACT=SRCHM&IKT0=8506&TRM0=" + zdbid, "ZDB: Onlineausgabe"));
	}

}
