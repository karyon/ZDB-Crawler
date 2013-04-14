package Processing;

import main.Main;
import main.SimpleTable;

public abstract class AbstractProcessor {
	protected SimpleTable table;
	
	public SimpleTable process(String[] IDs) {
		table = new SimpleTable();
		
		for (String ID: IDs) {
			table.newRow();
			processOne(ID);
			Main.stepDone();
		}
		return table;
	}
	
	
	protected abstract void processOne(String ID);

}
