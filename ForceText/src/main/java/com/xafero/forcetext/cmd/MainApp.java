package com.xafero.forcetext.cmd;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class MainApp {

	private static final String utf8 = "UTF8";

	public static void main(String[] args) throws Exception {
		// Get globals
		Date now = new Date();
		// Read configuration
		Properties rawCfg = new Properties();
		rawCfg.loadFromXML(new FileInputStream("config.xml"));
		Map<Object, Object> cfg = new TreeMap<Object, Object>(rawCfg);
		// Build settings
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (Entry<Object, Object> e : cfg.entrySet()) {
			String value = e.getValue() + "";
			// Update with other keys
			for (Object k : cfg.keySet()) {
				String key = "${" + k + "}";
				if (!value.contains(key))
					continue;
				value = value.replace(key, cfg.get(k) + "");
			}
			// Change if date formatted
			if (value.contains(",date,"))
				value = MessageFormat.format(value, now);
			// Set the finished value
			map.put(e.getKey() + "", value);
		}
		// Log it
		for (Entry<String, String> e : map.entrySet())
			System.out.println(" * " + e.getKey() + " := " + e.getValue().trim());
		// Get root folder
		File root = new File(cfg.get("root") + "");
		System.out.println("Looking for '" + root + "'...");
		// Loop for files
		for (File file : FileUtils.listFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)) {
			// Skip if directory or not readable
			if (!file.isFile() || !file.canRead())
				continue;
			// Go for file filters
			String key = "name=" + file.getName().toLowerCase();
			if (!map.containsKey(key))
				continue;
			// Get replacement value
			String value = map.get(key);
			// Read all lines
			List<String> lines = FileUtils.readLines(file, utf8);
			List<String> newLines = new LinkedList<String>();
			// Split for multiple replaces
			String[] ubers = value.trim().split('\n' + "");
			// Go for every replace
			Map<String, String> rplMap = new HashMap<String, String>();
			for (String uber : ubers) {
				String[] parts = uber.trim().split(Pattern.quote("|"));
				// Split arguments
				String searchWord = parts[0].trim();
				String replaceWord = parts[1].trim();
				// Set in map
				rplMap.put(searchWord, replaceWord);
			}
			// Loop for all lines
			for (String line : lines) {
				String searchWord;
				while ((searchWord = find(line, rplMap.keySet())) != null) {
					String replaceWord = rplMap.get(searchWord);
					// Log and replace some text
					int l = newLines.size() + 1;
					System.out.println("Found '" + searchWord + "' in '" + file + "' at line " + l + "...");
					line = line.replace(searchWord, replaceWord);
				}
				newLines.add(line);
			}
			// Rewrite the lines
			FileUtils.writeLines(file, utf8, newLines);
		}
		// Log end
		System.out.println("Done.");
	}

	private static String find(String line, Iterable<String> keys) {
		for (String key : keys)
			if (line.contains(key))
				return key;
		return null;
	}
}