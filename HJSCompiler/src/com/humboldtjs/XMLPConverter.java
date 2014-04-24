package com.humboldtjs;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import java.text.StringCharacterIterator;

import com.humboldtjs.parser.stringhelper.HumboldtJSString;

public class XMLPConverter
{
	public static String VERSION = "0.9.8";
	
	public static int logLevel = 1;
	public static String mainFile = "";
	public static String outFile = "";
	public static String parseFunction = "";
	public static boolean isClean = false;
	public static boolean shouldProcess = true;
	public static String languageFeaturesFile = "languagefeatures.js";
	
	public static long lastTime = System.nanoTime();

	public static void showError(String message, boolean showHelp)
	{
		System.out.println(message);
		if (showHelp) {
			System.out.println("\nUse 'xmlpconverter -help' for information about the command line.");
		}
		System.exit(1);
	}
	public static void showError(String message)
	{
		showError(message, false);
	}
	
	public static void showLog(String message, int level)
	{
		if (level <= logLevel) {
			if (logLevel > 3) {
				long newTime = System.nanoTime();
				float timeDiff = (float)(newTime - lastTime) / 100000000;
				String disp = "" + ((float)Math.round(timeDiff) / 10);
				if (disp.equals("0")) disp = "0.0";
				while (disp.length() < 5) disp = " " + disp;
				System.out.print("[" + disp + "s]" + (timeDiff / 10 > 1 ? "!" : " ") + " ");
				lastTime = newTime;
			}
			System.out.println(message);
		} else {
			if (logLevel > 3) {
				lastTime = System.nanoTime();
			}
		}
	}
	
	public static String tabs(int indent)
	{
		String theTabs = "";
		for (int i = 0; i < indent; i++) {
			theTabs += "\t";
		}
		return theTabs;
	}
	
	public static void main(String [] args)
	{
		System.out.println("XMLP Converter");
		System.out.println("Version " + VERSION);
		System.out.println("Copyright (c) 2013 2419studios\n");
		
		mainFile = "";
		outFile = "";
		parseFunction = "parse";

		String theRequires = "";

		for (int i = 0; i < args.length; i++) {
			if (args[i].startsWith("-")) {
				if (args[i].startsWith("-v")) {
					logLevel = args[i].length();
					showLog("Setting loggine mode to LOUD", 3);
				} else if (args[i].equals("-quiet")) {
					logLevel = 0;
				} else if (args[i].equals("-clean")) {
					isClean = true;
				} else if (args[i].equals("-n")) {
					shouldProcess = false;
				} else if (args[i].equals("-help")) {
					showLog("Help\n----\n", 1);
					showLog("xmlpconverter [options] <xmlFile>\n", 1);
					showLog("-o        set the output file", 1);
					showLog("-n        no processing, just dump the input file into a parse method", 1);
					showLog("-clean    strip comments", 1);
					showLog("-f        set the name of the parse function that should be called", 1);
					showLog("-v        switches to verbose mode - adding extra 'v' characters increases verbosity",1);
					showLog("-quiet    switches off all logging (but not errors)", 1);
					showLog("\n-help     shows this help", 1);
					System.exit(0);
				} else if (args[i].equals("-f") || args[i].equals("-o")) {
					theRequires = args[i];
				} else {
					showError("Error: Unknown compiler flag \"" + args[i] + "\"", true);
				}
			} else {
				if (!theRequires.equals("")) {
					if (theRequires.equals("-o")) {
						outFile = args[i];
					} else if (theRequires.equals("-f")) {
						parseFunction = args[i];
					}
					theRequires = "";
				} else if (mainFile.equals("")) {
					mainFile = args[i];
				} else {
					showError("Error: Cannot define more than one file to convert", true);
				}
			}
		}
		
		if (!theRequires.equals("")) {
			showError("Error: The flag " + theRequires + " requires an additional parameter", true);
		}
		
		if (mainFile.equals("")) {
			showError("Error: a target file must be specified", true);
		}
		
		if (outFile.equals("")) {
			outFile = mainFile + "p";
			showError("Error: a target file must be specified", true);
		}
		
		showLog("Converting", 1);
		showLog("----------", 1);
		
		showLog("File: " + mainFile + " -> " + outFile, 1);
		
		File inFile = new File(mainFile);
		byte[] buffer = new byte[(int) inFile.length()];
		try {
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inFile));
			inputStream.read(buffer);
		} catch (Exception e) {
			XMLPConverter.showError("Could not access file: " + mainFile);
		}
		
		Writer output = null;
		try {
			output = new BufferedWriter(new FileWriter(outFile, false));
		} catch (Exception e) {
			XMLPConverter.showError("Could not write file: " + outFile);
		}
		
		String input = new String(buffer, Charset.forName("UTF8"));
		String outputStr = new String();
		showLog("Input size: " + input.length(), 3);
		
		long startTime = System.nanoTime();
		
		outputStr = parseFunction + "(";
		int indent = 0;
		if (shouldProcess) {
			outputStr += "[";
			while (input.length() > 0) {
				input = input.trim();
				if (input.startsWith("<")) {
					if (input.startsWith("<![CDATA[")) {
						String simpleContent = input.substring(0, input.indexOf("]]>")).substring(9);
						input = input.substring(input.indexOf("]]>") + 3).trim();
						outputStr += tabs(indent) + "\"" + HumboldtJSString.escapeJavaStyleString(simpleContent, true) + "\"";
						if (input.startsWith("</"))
							outputStr += ",";
						outputStr += "\n";
					} else
					if (input.startsWith("<?")) {
						input = input.substring(input.indexOf("?>") + 2);
					} else
					if (input.startsWith("</>")) {
						input = input.substring(3).trim();
						indent--;
						outputStr += tabs(indent) + "}";
						if (input.length() > 2 && !input.startsWith("</")) {
							outputStr += ",";
						}
						outputStr += "\n";
					} else
					if (input.startsWith("</")) {
						input = input.substring(input.indexOf(">") + 1).trim();
						indent--;
						outputStr += tabs(indent) + "]\n";
						indent--;
						outputStr += tabs(indent) + "}";
						if (input.length() > 2 && !input.startsWith("</")) {
							outputStr += ",";
						}
						if (input.trim().length() > 0)
							outputStr += "\n";
					} else
					if (input.startsWith("<!--")) {
						input = input.substring(4);
						String comment = input.substring(0, input.indexOf("-->")).trim();
						input = input.substring(input.indexOf("-->") + 3).trim();
						if (!isClean) {
							outputStr += tabs(indent) + "{\n";
							outputStr += tabs(indent + 1) + "comment:\"" + HumboldtJSString.escapeJavaStyleString(comment, true) + "\"\n";
							outputStr += tabs(indent) + "}";
							if (input.length() > 2 && !input.startsWith("</"))
								outputStr += ",";
							outputStr += "\n";
						}
					} else
					{
						String tag = input.substring(0, input.indexOf(">")).substring(1);
						input = input.substring(tag.length() + 2);
						outputStr += tabs(indent) + "{\n";
						
						indent++;
						
						Boolean isEmpty = tag.substring(tag.length() - 1).equals("/");
						if (isEmpty)
							tag = tag.substring(0, tag.length() - 1);
						tag += " ";
						
						String name = tag.substring(0, tag.indexOf(" ")).trim();
						tag = tag.substring(tag.indexOf(" ") + 1).trim();
						
						outputStr += tabs(indent) + "name:\"" + name + "\"";
						if (tag.length() > 1 || !isEmpty)
							outputStr += ",";
						outputStr += "\n";
						
						if (tag.length() > 0) {
							outputStr += tabs(indent) + "at:{\n";
							while (tag.length() > 0) {
								String attr = tag.substring(0, tag.indexOf("="));
								tag = tag.substring(tag.indexOf("=") + 1).trim().substring(1);
								String value = tag;
								value = value.substring(0, value.indexOf("\""));
								tag = tag.substring(tag.indexOf("\"") + 1).trim();
								outputStr += tabs(indent + 1) + "\"" + HumboldtJSString.escapeJavaStyleString(attr, true) + "\":\"" + HumboldtJSString.escapeJavaStyleString(value, true) + "\"";
								
								if (tag.length() > 0)
									outputStr += ",";
								outputStr += "\n";
							}
							outputStr += tabs(indent) + "}";
							if (!isEmpty) {
								outputStr += ",\n";
							} else {
								outputStr += "\n";
							}
						}
						if (!isEmpty) {
							outputStr += tabs(indent) + "value:[\n";
							indent++;
						}
						if (isEmpty) {
							input = "</>" + input.trim();
						}
					}
				} else {
					String simpleContent = input.substring(0, input.indexOf("<")).trim();
					input = input.substring(input.indexOf("<")).trim();
					outputStr += tabs(indent) + "\"" + HumboldtJSString.escapeJavaStyleString(simpleContent, true) + "\"";
					if (input.length() > 2 && !input.startsWith("</"))
						outputStr += ",";
					outputStr += "\n";
				}
				
				// Write out the current value to the output buffer.
				// Don't let the string get to big, or it will blow the parsing time.
				try {
					output.write(outputStr);
					outputStr = "";
				} catch (Exception e) {
					XMLPConverter.showError("Could not write file: " + outFile);
				}
			}
			outputStr += "]";
		} else {
			outputStr += "\"" + escapeForJSON(input) + "\"";
		}
		outputStr += ");";
		
		// Write out remainder of the output.
		try {
			output.write(outputStr);
			output.close();
			output = null;
		} catch (Exception e) {
			XMLPConverter.showError("Could not write file: " + outFile);
		}
		
		showLog("Elapsed time: " + ((System.nanoTime() - startTime) / 1000000000.0) + "s", 3);
	}
	
	public static String escapeForJSON(String aText){
	    final StringBuilder result = new StringBuilder();
	    StringCharacterIterator iterator = new StringCharacterIterator(aText);
	    char character = iterator.current();
	    while (character != StringCharacterIterator.DONE){
	      if( character == '\"' ){
	        result.append("\\\"");
	      }
	      else if(character == '\\'){
	        result.append("\\\\");
	      }
	      else if(character == '/'){
	        result.append("\\/");
	      }
	      else if(character == '\b'){
	        result.append("\\b");
	      }
	      else if(character == '\f'){
	        result.append("\\f");
	      }
	      else if(character == '\n'){
	        result.append("\\n");
	      }
	      else if(character == '\r'){
	        result.append("\\r");
	      }
	      else if(character == '\t'){
	        result.append("\\t");
	      }
	      else {
	        //the char is not a special one
	        //add it to the result as is
	        result.append(character);
	      }
	      character = iterator.next();
	    }
	    return result.toString();    
	  }	
}