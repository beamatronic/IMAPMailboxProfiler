package com.beamatronic;

import java.util.Arrays;
import java.util.HashMap;

// Top 10 list of strings
// December 28, 2016

class StringCountEntry implements Comparable <StringCountEntry>  {

	public String stringKey;
	public int intValue;

	public StringCountEntry(String s, int i) {
		stringKey = s;
		intValue = i;
	}

	// This comparator does the REVERSE order
	public int compareTo(StringCountEntry e2) {
		return (e2.intValue - intValue);
	}

}

public class TopStrings {

	// private
	int _totalStringsProcessed = 0;
	int _maxStringWidth        = 0;
	HashMap<String,Integer> _stringCounterMap;

	public TopStrings() {
		_stringCounterMap = new HashMap<String,Integer>();
		_totalStringsProcessed = 0;
		_maxStringWidth = 0;
	}

	public int totalStringsProcessed() {
		return _totalStringsProcessed;
	}

	public int maxStringWidth() {
		return _maxStringWidth;
	}
	public void processString(String s) {

		if (s == null) {
			s = "Null String";
		}

		_totalStringsProcessed++;

		if (s.length() > _maxStringWidth) { _maxStringWidth = s.length(); };

		Integer currentValue = _stringCounterMap.get(s);

		if (currentValue == null) {
			_stringCounterMap.put(s, 1);  // First Entry
		} else {
			_stringCounterMap.put(s, currentValue + 1);
		}

	}

	public void displayTopStrings(int rowLimit) {

		String columnHeader1 = "Message";
		String columnHeader2 = "# Times Seen";

		int column1Width = ( columnHeader1.length() > this.maxStringWidth()) ? (columnHeader1.length() + 5): this.maxStringWidth();
		int column2Width = columnHeader2.length();

		String printfFormat  = "%-" + column1Width + "s %" + column2Width + "d\n";
		String printfFormat2 = "%-" + column1Width + "s %" + column2Width + "s\n";

		StringBuffer sb1 = new StringBuffer();
		for (int i = 0; i < column1Width; i++) { sb1.append("-"); }
		String dash1 = sb1.toString();

		StringBuffer sb2 = new StringBuffer();
		for (int i = 0; i < column2Width; i++) { sb2.append("-"); }
		String dash2 = sb2.toString();

		String headerLine = dash1 + " " + dash2;

		System.out.printf(printfFormat2, columnHeader1, columnHeader2);
		System.out.println(headerLine);

		StringCountEntry[] sceArray = new StringCountEntry[_stringCounterMap.size()];

		int i = 0;
		for (String eachKey : _stringCounterMap.keySet()) {
			int eachValue = _stringCounterMap.get(eachKey);
			// System.out.println("Key:" + eachKey + " Value: " + eachValue);
			StringCountEntry sce = new StringCountEntry(eachKey, eachValue);
			sceArray[i++] = sce;
		}

		Arrays.sort(sceArray);

		// Print the newly sorted array
		for (int x = 0; x < sceArray.length; x++) {
			if (x < rowLimit) {
				System.out.printf(printfFormat, sceArray[x].stringKey, sceArray[x].intValue);
			}
		}

	}

	// For unit testing
	public static void main(String[] args) {

		TopStrings ts = new TopStrings();

		ts.processString("Hello");
		ts.processString("Hello");
		ts.processString("World!");
		ts.processString("Hello");
		ts.processString("Hello");
		ts.processString("Hello");
		ts.processString("Hello");
		ts.processString("Hello");

		ts.displayTopStrings(10);

		System.out.println("Total strings processed: " + ts.totalStringsProcessed());
		System.out.println("Longest string length:   " + ts.maxStringWidth());
	}

}


// EOF