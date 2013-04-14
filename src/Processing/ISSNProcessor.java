package Processing;

import main.Main;
import main.SimpleTable;

public class ISSNProcessor extends AbstractProcessor{
	
	
	@Override
	protected void processOne(String ISSN) {
		if (ISSN.equals("")) {
			return;
		}
		
		String sfxlink = "http://sfx.kobv.de/sfx_fub?ctx_id=10_1&ctx_ver=Z39.88-2004&rfr_id=info%3Asid%2Fsfxit.com%3Acitation&rft.issn=" + ISSN.replace("*", "");
		String text = "SFX-Link";
		String code = Main.getSourceCode(sfxlink);
		if (code.contains("Kein Volltext verf"))
			text += " (Kein Volltext verfügbar)";
		else if (code.contains("Volltext verf"))
			text += " (Volltext verfügbar)";
		table.newCell(SimpleTable.toLink(sfxlink, text));
	}

}
