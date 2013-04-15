package main;
import java.util.ArrayList;


class HtmlTable {
	
	private final static boolean multipleLinks = false;
	
	private static final String excelBegin = 
		"<html xmlns:v=\"urn:schemas-microsoft-com:vml\"\n"
		+"xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n"
		+"xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n"
		+"xmlns=\"http://www.w3.org/TR/REC-html40\">\n"
		+""
		+"<head>"
		+"<meta http-equiv=Content-Type content=\"text/html; charset=utf-8\">\n"
		+"<meta name=ProgId content=Excel.Sheet>\n"
		+"<meta name=Generator content=\"Microsoft Excel 14\">\n"
		+"<style>\n"
		+"<!--table\n"
		+"@page\n"
		+"	{margin:.98in .79in .98in .79in;\n"
		+"	mso-header-margin:.5in;\n"
		+"	mso-footer-margin:.5in;}\n"
		+"tr\n"
		+"	{mso-height-source:auto;}\n"
		+"col\n"
		+"	{mso-width-source:auto;}\n"
		+"br\n"
		+"	{mso-data-placement:same-cell;}\n"
		+"td\n"
		+"	{padding-top:1px;\n"
		+"	padding-right:1px;\n"
		+"	padding-left:1px;\n"
		+"	mso-ignore:padding;\n"
		+"	color:windowtext;\n"
		+"	font-size:10.0pt;\n"
		+"	font-weight:400;\n"
		+"	font-style:normal;\n"
		+"	text-decoration:none;\n"
		+"	font-family:Arial;\n"
		+"	mso-generic-font-family:auto;\n"
		+"	mso-font-charset:0;\n"
		+"	mso-number-format:General;\n"
		+"	text-align:general;\n"
		+"	vertical-align:bottom;\n"
//		+"	border:none;\n"
		+"	mso-background-source:auto;\n"
		+"	mso-pattern:auto;\n"
		+"	mso-protection:locked visible;\n"
		+"	mso-rotate:0;}\n"
		+".xl65\n"
		+"	{color:blue;\n"
		+"	text-decoration:underline;\n"
		+"	text-underline-style:single;\n"
		+"	border:.5pt solid windowtext;\n"
		+"	white-space:normal;}\n"
		+".xl66\n"
		+"	{text-align:left;\n"
		+"	vertical-align:top;\n"
		+"	border:.5pt solid windowtext;\n"
		+"	white-space:normal;}\n"
		+"-->\n"
		+"</style>\n"
		+"</head>\n"
		+"\n"
		+"<body link=blue vlink=purple>\n"
		+"<table border=1 cellpadding=0 cellspacing=0 style='border-collapse: collapse;width:1184pt'>\n";
	
	
	
	/**
	 * Returns a String, representing the specified SimpleTable and intended for the clipboard.
	 * After copying a String returned by this method to the clipboard and pressing
	 * ctrl-v in a Microsoft Excel table, Excel will correctly correctly paste the specified SimpleTable.
	 * @param table
	 * @return
	 */
	public static String simpleTableToString(SimpleTable table) {
		StringBuffer result = new StringBuffer();
		result.append(excelBegin);

		for (ArrayList<String> row: table.getList()) {
			result.append("<TR>");
			for(String cell: row) {
				result.append("<TD class=xl66>" + convert(cell) + "</TD>" + "\n");
			}
			result.append("</TR>");
		}
		result.append("</TBODY></TABLE></BODY>");
		return result.toString();
	}
	
	
	
	private static String convert(String input) {
		String newContent = input.replaceAll("<", "&lt;");
		
		newContent = newContent.replace("[fett]", "<b>");
		newContent = newContent.replace("[/fett]", "</b>");
		
		if (multipleLinks) {
			newContent = newContent.replace("[URL_AND_TEXT]", "<a href=\"");
			newContent = newContent.replace("[/URL]", "\">");
			newContent = newContent.replace("[/TEXT]", "</a>");
		}
		else {
			newContent = newContent.replaceFirst("\\[URL_AND_TEXT\\]", "<a href=\"");
			newContent = newContent.replaceFirst("\\[/URL\\]", "\">");
			newContent = newContent.replaceFirst("\\[/TEXT\\]", "</a>");
			newContent = newContent.replaceAll("\\[URL_AND_TEXT\\]", "");
			newContent = newContent.replaceAll("\\[/URL\\].*?\\[/TEXT\\]", "");
		}
		newContent = newContent.replaceAll("\n", "<BR>");
		return newContent;
	}
}