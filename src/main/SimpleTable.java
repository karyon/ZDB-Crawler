package main;
import java.util.ArrayList;


public class SimpleTable {
	
	private ArrayList<ArrayList<String>> tableList = new ArrayList<ArrayList<String>> ();
	private ArrayList<String> currRow = null;
	
	
	
	/**
	 * Begins a new cell in the current row and writes the specified String into it.
	 */
	public void newCell(String text) {
		currRow.add(text);
	}
	
	
	
	/**
	 * Begins a new row. Afterwards, newColumn(String) must be called before calling addToCurrentCell.
	 */
	public void newRow() {
		currRow = new ArrayList<String>();
		tableList.add(currRow);
	}
	
	
	/**
	 * Adds the specified text to the current cell.
	 * @param text
	 */
	public void addToCurrentCell(String text) {
		String oldContent = currRow.remove(currRow.size()-1);
		String newContent = oldContent + text;
		
		currRow.add(newContent);
	}
	
	
	/**
	 * Appends the specified table to the right of this table.
	 * Pseudocode: for all x, this.getRow(x) += another.getRow(x)
	 * In case soem rows in this table are shorter than others, there are filled
	 * with empty cells so that all rows have the same length.
	 * @param another
	 */
	public void concat(SimpleTable another) {
		//fill rows so that all rows have the same length
		int maxElements = 0;
		for (ArrayList<String> row: tableList) {
			maxElements = Math.max(maxElements, row.size());
		}
		for (ArrayList<String> row: tableList) {
			while(row.size() != maxElements)
				row.add("");
		}
		
		
		for (ArrayList<String> row: tableList) {
			if (row.size() != tableList.get(0).size())
				throw new RuntimeException("This table has different row lengths");
		}
		
		if (this.tableList.size() != another.tableList.size())
			throw new RuntimeException("Different sizes of tables");
		
		for (int i = 0; i < this.tableList.size(); i++) {
			this.tableList.get(i).addAll(another.tableList.get(i));
		}
	}
	
	
	/**
	 * Builds a String which represents a hyperlink with the given text and target.
	 * @param target
	 * @param text
	 * @return
	 */
	public static String toLink(String target, String text) {
		return "[URL_AND_TEXT]" + target + "[/URL]" + text + "[/TEXT]";
	}
	
	
	/**
	 * Builds a String which represents the specified text in bold letters.
	 * @param text
	 * @return
	 */
	public static String toBold(String text) {
		return "[fett]" + text + "[/fett]";
	}
	
	
	/**
	 * Returns true if the specified text contains any bold letters, false otherwise.
	 * @param text
	 * @return
	 */
	public static boolean containsBold(String text) {
		return text.contains("[fett]");
	}
	
	
	
	public ArrayList<ArrayList<String>> getList() {
		return tableList;
	}
}
