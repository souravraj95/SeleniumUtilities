package utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import pojo.DesiredException;

public class SupportUtil {

	MyLogger logger;

	public SupportUtil() {
		this.logger = new MyLogger(
				Thread.currentThread().getStackTrace()[2].getClassName() + " : " + this.getClass().getSimpleName());
	}

	public String partialLookUp(String[] lookupParam, int[] refCol, int lookupCol, String[][] lookupTable) {
		ArrayList<String[]> list = new ArrayList<>();
		for (String[] valueSet : lookupTable)
			list.add(valueSet);

		String[] filterList = list.stream().filter(
				item -> item[refCol[0] - 1].equals(lookupParam[0]) && item[refCol[1] - 1].contains(lookupParam[1]))
				.findFirst().orElse(null);

		return filterList != null ? filterList[lookupCol - 1] : "No_Data";
	}

	public String partialLookUp(String lookupParam, int refCol, int lookupCol, String[][] lookupTable) {
		String temp = "", lookupValue = "";
		int i = -1;
		while (++i < lookupTable.length && !temp.equals("No_Data")) {
			temp = lookupTable[i][refCol - 1];
			if (temp.contains(lookupParam)) {
				// System.out.println("Debug>> " + lookupParam + " --> " +
				// lookupTable[i][lookupCol-1]);
				return lookupTable[i][lookupCol - 1];
			}
		}
		return lookupValue;
	}

	public String softLookUp(String lookupParam, int refCol, int lookupCol, String[][] lookupTable) {
		String temp = "";
		String regex = "\\$|\\+|-|,|:|\\s+|/";
		int i = -1;
		while (++i < lookupTable.length && !temp.equals("No_Data")) {
			lookupParam = lookupParam.replaceAll(regex, "");
			temp = lookupTable[i][refCol - 1];
			temp = temp.replaceAll(regex, "");

			if (lookupParam.equalsIgnoreCase(temp)) {
				// System.out.println("Debug>> " + lookupParam + " --> " +
				// lookupTable[i][lookupCol-1]);
				return lookupTable[i][lookupCol - 1];
			}
		}
		return "No_Data";
	}

	// Look up based on multiple lookup parameters
	public String lookUp(String[] lookupParam, int[] refCol, int lookupCol, String[][] lookupTable) {

		if (lookupParam.length != refCol.length)
			return "Error!";
		String mapLookup = "";
		for (String param : lookupParam)
			mapLookup = mapLookup + param;
		// Convert to Collection
		HashMap<String, String> map = new HashMap<>();
		for (String[] valueSet : lookupTable) {
			String concatinatedValue = "";
			for (int i = 0; i < refCol.length; i++)
				concatinatedValue = concatinatedValue + valueSet[refCol[i] - 1];
			map.put(concatinatedValue, valueSet[lookupCol - 1]);
		}

		String lookupValue = map.get(mapLookup);
		return lookupValue != null ? lookupValue : "No_Data";
	}

	// Lookup by single lookup parameter & column number
	public String lookUp(String lookupParam, int refCol, int lookupCol, String[][] lookupTable) {
		String temp = "";
		int i = -1;
		while (++i < lookupTable.length && !temp.equals("No_Data")) {
			temp = lookupTable[i][refCol - 1];
			if (lookupParam.equalsIgnoreCase(temp)) {
				// System.out.println("Debug>> " + lookupParam + " --> " +
				// lookupTable[i][lookupCol-1]);
				return lookupTable[i][lookupCol - 1];
			}
		}
		return "No_Data";
	}

	// Lookup by single lookup parameter & column header name
	public String lookUp(String lookupParam, String refColName, String lookupColName, String[][] lookupTable) {
		String[] header = lookupTable[0];
		int refCol = Arrays.asList(header).indexOf(refColName);
		int lookupCol = Arrays.asList(header).indexOf(lookupColName);
		return lookUp(lookupParam, refCol, lookupCol, lookupTable);
	}

	// Get Machine IP
	public String machineIP() {
		String thismachineIP = null;
		try {
			thismachineIP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			thismachineIP = "Unknown";
		}
		return thismachineIP;
	}

	// Addition and Subtraction on dates
	public String dateAddition(String referenceDate, int days) {
		Date date;
		String newDate = null;
		try {
			DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
			date = formatter.parse(referenceDate);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, days);
			date = cal.getTime();
			newDate = formatter.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return newDate;
	}

	// Check if given text contains any of the text from given array
	public boolean ifTextContains(String textValue, String[] textArray) {
		boolean flag = false;
		for (String text : textArray)
			flag |= textValue.contains(text);
		return flag;
	}

	// Check if given text contains any of the text from given array
	public boolean ifTextEquals(String textValue, String[] textArray) {
		boolean flag = false;
		for (String text : textArray)
			flag |= textValue.equals(text);
		return flag;
	}

	public boolean killProcess(String serviceName) {
		String KILL = "taskkill /F /IM ";
		try {
			Runtime.getRuntime().exec(KILL + serviceName);
			logger.info("Process killed: " + serviceName);
			return true;
		} catch (IOException e) {
			logger.warning("Unable to Kill: " + serviceName);
			return false;
		}
	}

	public Object cleanValue(String inp) {
		inp = inp.replaceAll("\\$|\\+|\\'|,|%", "");
		inp = inp.trim();
		try {
			Date date = new SimpleDateFormat("MM/dd/yyyy").parse(inp);
			return date;
		} catch (Exception e) {
		}

		try {
			return Double.parseDouble(inp);
		} catch (Exception e) {
		}

		return inp;
	}

	// Rename the file name in file system, Full file-folder path is required
	public void renameFile(String oldFileFullPath, String newFileFullPath) throws Exception {

		// File (or directory) with old name
		File file = new File(oldFileFullPath);

		// File (or directory) with new name
		File file2 = new File(newFileFullPath);

		if (file2.exists())
			throw new DesiredException("file exists");

		// Rename file (or directory)
		boolean success = file.renameTo(file2);

		if (!success) {
			throw new DesiredException("Renaming Failed");
		}
	}

	// Read PDF/Text File and Return the Extracted Text as Array of lines
	public String[] fileHandler(String filePath) {
		String[] lines = null;
		try {
			if (filePath.toLowerCase().endsWith(".pdf")) {
				PDDocument document = PDDocument.load(new File(filePath));
				String text = new PDFTextStripper().getText(document);
				lines = text.split(System.getProperty("line.separator"));
				document.close();
				logger.success("PDF File Loaded Successfully", false);
			} else if (filePath.toLowerCase().endsWith(".txt")) {
				String text = IOUtils.toString(new FileInputStream(filePath));
				lines = text.split(System.getProperty("line.separator"));
				logger.success("TXT File Loaded Successfully", false);
			} else {
				logger.error("File Type not handled for parsing", false);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	// Validate date format
	public boolean isValidFormat(String format, String value) {
		LocalDateTime ldt = null;
		DateTimeFormatter fomatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH);

		try {
			ldt = LocalDateTime.parse(value, fomatter);
			String result = ldt.format(fomatter);
			return result.equals(value);
		} catch (DateTimeParseException e) {
			try {
				LocalDate ld = LocalDate.parse(value, fomatter);
				String result = ld.format(fomatter);
				return result.equals(value);
			} catch (DateTimeParseException exp) {
				try {
					LocalTime lt = LocalTime.parse(value, fomatter);
					String result = lt.format(fomatter);
					return result.equals(value);
				} catch (DateTimeParseException e2) {
				}
			}
		}

		return false;
	}
	
	public String getTimestamp() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
	}
}
