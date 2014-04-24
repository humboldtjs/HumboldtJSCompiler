package com.humboldtjs;

import java.io.File;
import java.util.ArrayList;

import com.humboldtjs.parser.HumboldtJSCompiler;

import com.yahoo.platform.yui.compressor.YUICompressor;

public class HumboldtJS
{
	public static String VERSION = "0.9.8";
	
	public static ArrayList<String> includePaths;
	public static ArrayList<String> libraryPaths;
	public static boolean isDebug = false;
	public static boolean isOptimize = false;
	public static boolean isCompress = true;
	public static int logLevel = 1;
	public static String baseDir = "./";
	public static String mainFile = "";
	public static String outputDir = "./";
	public static Boolean clean = false;
	public static Boolean noDependencies = false;
	public static String preInitFile = "";
	public static String languageFeaturesFile = "languagefeatures.js";
	
	public static long lastTime = System.nanoTime();

	public static void showError(String message, boolean showHelp)
	{
		System.err.println(message);
		if (showHelp) {
			System.out.println("\nUse 'humboldtjs -help' for information about the command line.");
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
	
	public static void main(String [] args)
	{
		System.out.println("HumboldtJS Compiler");
		System.out.println("Version " + VERSION);
		System.out.println("Copyright (c) 2014 - 2419studios\n");
		
        if (includePaths == null) {
        	includePaths = new ArrayList<String>();
        }
        if (libraryPaths == null) {
        	libraryPaths = new ArrayList<String>();
        }

		String thePreInit = "";
		String theRequires = "";
		
		for (int i = 0; i < args.length; i++) {
			if (args[i].substring(0, 1).equals("-")) {
				if (args[i].equals("-debug")) {
					isDebug = true;
					showLog("Enabling debug mode - writing to separate files", 3);
				} else if (args[i].substring(0, 2).equals("-v")) {
					logLevel = args[i].length();
					showLog("Setting logging mode to LOUD", 3);
				} else if (args[i].equals("-quiet")) {
					logLevel = 0;
				} else if (args[i].equals("-nocompress")) {
					isCompress = false;
				} else if (args[i].equals("-optimize")) {
					isOptimize = true;
					showLog("Enabling package and class name length optimization", 3);
				} else if (args[i].equals("-clean")) {
					clean = true;
					showLog("Generating clean output; recompiling everything", 3);
				} else if (args[i].equals("-nodep")) {
					noDependencies = true;
					showLog("Generating clean output; recompiling everything", 3);
				} else if (args[i].equals("-help")) {
					showLog("Help\n----\n", 1);
					showLog("humboldtjs [options] <applicationFile>\n", 1);
					showLog("-basedir  specifies the base directory to search for the main application classes", 1);
					showLog("-debug    if this flag is specified the compiler will run in debug mode causing it", 1);
					showLog("          to output each class as a separate file", 1);
					showLog("-optimize optimizes the filesize by renaming private members and by using a lookup", 1);
					showLog("          table to reduce size needed by class identifiers", 1);
					showLog("-preinit  specifies an additional JS file to prepend the final generated application", 1);
					showLog("          with, which can be used to provide initialization setup for the running", 1);
					showLog("          environment", 1);
					showLog("-clean    regerenate every output JS file regardless of whether it is already up to date", 1);
					showLog("-nocompress disables built-in compression when generating a non-debug version",1);
					showLog("-I        specifies an additional include path to search for classes", 1);
					showLog("-o        set the output directory", 1);
					showLog("-v        switches to verbose mode - adding extra 'v' characters increases verbosity",1);
					showLog("-quiet    switches off all logging (but not errors)", 1);
					showLog("-nodep    does not compile any dependencies", 1);
					showLog("\n-help     shows this help", 1);
					System.exit(0);
				} else if (args[i].equals("-I") || args[i].equals("-basedir") || args[i].equals("-o") || args[i].equals("-L")) {
					theRequires = args[i];
				} else if (args[i].equals("-preinit")) {
					thePreInit = args[i];
				} else {
					showError("Error: Unknown compiler flag \"" + args[i] + "\"", true);
				}
			} else {
				if (!theRequires.equals("")) {
					if (theRequires.equals("-I")) {
						includePaths.add(args[i]);
					} else if (theRequires.equals("-L")) {
						libraryPaths.add(args[i]);
					} else if (theRequires.equals("-basedir")) {
						baseDir = args[i];
					} else if (theRequires.equals("-o")) {
						outputDir = args[i];
					}
					theRequires = "";
				} else if (thePreInit.equals("-preinit")) {
					preInitFile = args[i];
					thePreInit = "";
				} else if (mainFile.equals("")) {
					mainFile = args[i];
				} else {
					showError("Error: Cannot define more than one main class to compile", true);
				}
			}
		}
		
		if (!theRequires.equals("")) {
			showError("Error: The flag " + theRequires + " requires an additional parameter", true);
		}
		
		if (mainFile.equals("")) {
			showError("Error: a target file must be specified", true);
		}
		
		if (!baseDir.substring(baseDir.length() - 1).equals("/")) baseDir += "/";
		if (!outputDir.substring(outputDir.length() - 1).equals("/")) outputDir += "/";
		for (int i = 0; i < includePaths.size(); i++) {
			String includePath = includePaths.get(i);
			if (!includePath.substring(includePath.length() - 1).equals("/")) includePath += "/";
			includePaths.set(i, includePath);
		}
		
		// Do some actual compiling
		HumboldtJSCompiler.mProcessedFiles = new ArrayList<String>();
		HumboldtJSCompiler.mApplicationFile = "";
		
		new HumboldtJSCompiler(mainFile, true);
		
		if (!isDebug && isCompress) {
			String tmpFileName = HumboldtJS.outputDir + mainFile.substring(0, mainFile.length() - 3) + ".tmp";
			String outFileName = HumboldtJS.outputDir + mainFile.substring(0, mainFile.length() - 3) + ".js";
			HumboldtJS.showLog("Compressing: " + outFileName, 1);
			HumboldtJS.showLog("Using YUICompressor with parameters --type js --nomunge parameters", 2);
			
			String[] args2;
			args2 = new String[6];
			args2[0] = "--type";
			args2[1] = "js";
			args2[2] = "--nomunge";
			args2[3] = "-o";
			args2[4] = outFileName;
			args2[5] = tmpFileName;
			                
			try {
				YUICompressor.main(args2);
				
				File tmp = new File(tmpFileName);
				tmp.delete();
			} catch (Exception e) {
				throw new Error(e.getLocalizedMessage());
			}
		}
}
}