package com.humboldtjs.parser;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.humboldtjs.HumboldtJS;
import com.humboldtjs.parser.stringhelper.HumboldtJSString;

public class HumboldtJSCompiler
{
	private String mInput;
	private String mOutput;
	private ASBlock mCode;
	private String activeFile;
	private String mBaseDir;
	public static String mApplicationFile;
	private boolean mIsApplication = false;
	public static ArrayList<String> mProcessedFiles;
	public static String mFullClass;
	
	public HumboldtJSCompiler(String fileName, boolean generateDependencies)
	{
		activeFile = fileName;

		File inFile = getFile(activeFile);
		File outFile;
//		File mapFile;

		File outDir = new File(HumboldtJS.outputDir);
		if (!outDir.exists()) outDir.mkdirs();

		outFile = new File(HumboldtJS.outputDir + activeFile.substring(0, activeFile.length()-3) + ".js");
//		mapFile = new File(HumboldtJS.outputDir + activeFile.substring(0, activeFile.length()-3) + ".js.map");
		
		if (!HumboldtJS.isDebug || !outFile.exists() || HumboldtJS.clean || inFile.lastModified() > outFile.lastModified()) {
			
			if (mProcessedFiles.size() == 0) {
				HumboldtJS.showLog("Compiling\n---------\nApplication: " + convertToClassName(activeFile) + "*", 1);
				mApplicationFile = activeFile;
			} else {
				HumboldtJS.showLog("Dependency:  " + convertToClassName(activeFile) + "*", 1);
			}
			mProcessedFiles.add(activeFile);
			
			HumboldtJS.showLog("File has changed... recompiling", 2);
			
			loadASFile();
			parseASFile();
			saveJSFile();
			
		} else {
			if (mProcessedFiles.size() == 0) {
				HumboldtJS.showLog("Compiling\n---------\nApplication: " + convertToClassName(activeFile), 1);
				mApplicationFile = activeFile;
			} else {
				HumboldtJS.showLog("Dependency:  " + convertToClassName(activeFile), 1);
			}
			mProcessedFiles.add(activeFile);
			
			HumboldtJS.showLog("File is unchanged... generating dependencies from compiled JS file", 2);

			generateDependenciesFromJSFile();
		}
		
		if (generateDependencies)
			processDependencies();
		
		if (!HumboldtJS.isDebug && activeFile.equals(mApplicationFile)) {
			outFile = new File(HumboldtJS.outputDir + mApplicationFile.substring(0, mApplicationFile.length() - 3) + (HumboldtJS.isCompress ? ".tmp" : ".js"));
			HumboldtJS.showLog("Writing extra footer code to JS file" + outFile.getPath(), 3);
			
			try {
				outDir = outFile.getParentFile();
				if (!outDir.exists()) outDir.mkdirs();
				if (!outFile.exists()) outFile.createNewFile();
				
				Writer output = new BufferedWriter(new FileWriter(outFile, true));
				output.write("\n__hjs.ready(\"" + mFullClass + "\");");
				output.close();
			} catch(Exception e) {}
		}
	}
	
	protected void generateDependenciesFromJSFile()
	{
		mCode = new ASBlock("", mBaseDir);

		if (HumboldtJS.noDependencies) return;

		// We do this to set the baseDir
		getFile(activeFile);

		File outFile = new File(HumboldtJS.outputDir + activeFile.substring(0, activeFile.length()-3) + ".js");
		if (!outFile.exists()) {
			HumboldtJS.showError("Cannot find file: " + outFile.getAbsoluteFile());
		}
		
		byte[] buffer = new byte[(int) outFile.length()];
		try {
			BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(outFile));
			inputStream.read(buffer);
		} catch (Exception e) {
			HumboldtJS.showError("Could not access file: " + outFile.getAbsoluteFile());
		}

		mInput = new String(buffer);

		if (HumboldtJSString.hasToken(mInput, "if (__hjs._inheriting) return;")) {
			mInput = HumboldtJSString.getBeforeToken(mInput, "if (__hjs._inheriting) return;");
		}
		if (HumboldtJSString.hasToken(mInput, "__hjs.isOfType = function")) {
			mInput = HumboldtJSString.getAfterToken(mInput, "__hjs.isOfType = function");
		}
		while (HumboldtJSString.hasToken(mInput, "include")) {
			mInput = HumboldtJSString.getAfterToken(mInput, "include");
			String theClass = "";
			theClass = mInput.substring(mInput.indexOf('\"') + 1);
			theClass = theClass.substring(0, theClass.indexOf('\"'));
			mCode.imports.add(theClass);
			HumboldtJS.showLog("Found dependency on " + theClass, 3);
		}
		
	}
	
	protected String convertToClassName(String aFileName)
	{
		String[] thePackageParts = aFileName.substring(0, aFileName.length() - 3).split("/");
		String theClass = "";
		for (int i = 0; i < thePackageParts.length; i++) {
			String thePackagePart = thePackageParts[i];
			if (i != 0) theClass += ".";
			theClass += thePackagePart;
		}
		return theClass;
	}
	
	protected void processDependencies()
	{
		for (int i = 0; i < mCode.imports.size(); i++) {
			String theImport = mCode.imports.get(i);
			
			if (theImport.charAt(0) == '*' || theImport.charAt(0) == '$') {
				theImport = theImport.substring(1);
			}
			if (!theImport.substring(0, 4).equals("dom.") && mCode.isImport(theImport, true)) {
				String theFile = mCode.convertClassWithPackageToFile(theImport);
				
				if (mProcessedFiles.indexOf(theFile) == -1) {
					new HumboldtJSCompiler(theFile, true);
				}
			} else {
				HumboldtJS.showLog("Ignoring " + theImport + " because " + (theImport.substring(0, 4).equals("dom.") ? "it's a dom class" : "it was already imported"), 3);
			}
		}
	}
	
	protected File getFile(String fileName)
	{
		mBaseDir = "./";
		File theFile = new File(fileName);
		if (theFile.exists()) return theFile;

		mBaseDir = HumboldtJS.baseDir;
		theFile = new File(HumboldtJS.baseDir + fileName);
		if (theFile.exists()) return theFile;
		
		for (int i = 0; i < HumboldtJS.includePaths.size(); i++) {
			String theIncludePath = HumboldtJS.includePaths.get(i);
			mBaseDir = theIncludePath;
			theFile = new File(theIncludePath + fileName);
			if (theFile.exists()) return theFile;
		}
		
		for (int i = 0; i < HumboldtJS.libraryPaths.size(); i++) {
			String theLibraryPath = HumboldtJS.libraryPaths.get(i);
			mBaseDir = theLibraryPath;

			theFile = new File(theLibraryPath);
			if (theFile.exists()) {
				try {
					ZipFile zf = new ZipFile(theFile.getAbsolutePath());
					ZipEntry entry = zf.getEntry(fileName);
				    
					if (entry != null)
						return theFile;
				} catch (IOException e) {}
			}
		}
		
		return null;
	}
	
	protected void loadASFile()
	{
		File inFile = getFile(activeFile);
		if (inFile == null || !inFile.exists()) {
			HumboldtJS.showError("Cannot find file: " + activeFile);
			return;
		}
		
		byte[] buffer = loadFile(inFile, activeFile);

		mInput = new String(buffer);
	}
	
	protected byte[] loadFile(File inFile, String fileName)
	{
		if (fileName.endsWith(inFile.getName())) {
			byte[] buffer = new byte[(int) inFile.length()];
			try {
				BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(inFile));
				inputStream.read(buffer);
				
				return buffer;
			} catch (Exception e) {
				HumboldtJS.showError("Could not access file: " + fileName);
			}
		} else {
			try {
				ZipFile zf = new ZipFile(inFile.getAbsolutePath());
				ZipEntry entry = zf.getEntry(fileName);
			    
				try {
					BufferedInputStream inputStream = new BufferedInputStream(zf.getInputStream(entry));

					byte[] buffer = new byte[(int) entry.getSize()];
					inputStream.read(buffer);
					
					return buffer;
				} finally {
					zf.close();
				}
			} catch(IOException e) {}
		}

		return null;
	}
	
	protected void saveJSFile()
	{
		File outDir = new File(HumboldtJS.outputDir);
		if (!outDir.exists()) outDir.mkdirs();
		
		File outFile;
		File mapFile = null;
		boolean append = false;
		
		if (!HumboldtJS.isDebug) {
			outFile = new File(HumboldtJS.outputDir + mApplicationFile.substring(0, mApplicationFile.length() - 3) + (HumboldtJS.isCompress ? ".tmp" : ".js"));
			if (!activeFile.equals(mApplicationFile)) append = true;
		} else {
			outFile = new File(HumboldtJS.outputDir + activeFile.substring(0, activeFile.length()-3) + ".js");
			// mapFile = new File(HumboldtJS.outputDir + activeFile.substring(0, activeFile.length()-3) + ".js.map");
		}
		
		HumboldtJS.showLog("Writing compiled code to JS file" + outFile.getPath(), 3);
		
		try {
			outDir = outFile.getParentFile();
			if (!outDir.exists()) outDir.mkdirs();
			if (!outFile.exists()) outFile.createNewFile();
			
			Writer output = new BufferedWriter(new FileWriter(outFile, append));
			output.write(mOutput);
			output.close();
			
/*			if (mapFile != null) {
				HumboldtJS.showLog("Writing source map to JS.map file" + outFile.getPath(), 3);

				if (!mapFile.exists()) mapFile.createNewFile();
				
				Writer mapOutput = new BufferedWriter(new FileWriter(mapFile, append));
				mapOutput.write(mOutput);
				mapOutput.close();
			}*/
		} catch(Exception e) {}
	}

	protected void parseASFile()
	{
		parseToASObject();
		addHeader();
		addFooter();
		pureJSHandler();
	}
	
	protected void parseToASObject()
	{
		mCode = new ASBlock(mInput, mBaseDir);
		mIsApplication = mCode.isApplication();
		mOutput = mCode.toString();
	}
	
	protected void addHeader()
	{
		String theHeader = "";
		if (HumboldtJS.preInitFile != "") {
			File inFile = getFile(HumboldtJS.preInitFile);
			if (inFile == null) {
				HumboldtJS.showError("Cannot find file: " + HumboldtJS.preInitFile);
			}
			
			byte[] buffer = loadFile(inFile, HumboldtJS.preInitFile);

			theHeader += new String(buffer) + "\n\n";
			
			HumboldtJS.preInitFile = "";
			
		}
		if (mIsApplication) {
			HumboldtJS.showLog("Generating JS language features header", 2);

			if (HumboldtJS.languageFeaturesFile != "") {
				InputStream theFile = HumboldtJS.class.getResourceAsStream(HumboldtJS.languageFeaturesFile);
				
				if (theFile == null) {
					HumboldtJS.showError("Cannot find file: " + HumboldtJS.languageFeaturesFile);
				}
				
				try {
					byte[] buffer = new byte[(int) theFile.available()];
					
					try {
						BufferedInputStream inputStream = new BufferedInputStream(theFile);
						inputStream.read(buffer);
						
						theHeader += new String(buffer) + "\n\n";
					} catch (Exception e) {
						HumboldtJS.showError("Could not access file: " + HumboldtJS.languageFeaturesFile);
					}

				} catch (Exception e) {
					HumboldtJS.showError("Could not create buffer for file: " + HumboldtJS.languageFeaturesFile);
				}
			}

			if (!mCode.mAppArgs.equals("noinit")) {
				HumboldtJS.showLog("Application will auto-initialize", 2);
				theHeader += "__hjs.autoInit(\"" + mCode.getFullClass() + "\");\n";
			}

		}

		if (mIsApplication) {
			theHeader += "__hjs.app(\"" + mCode.getFullClass() + "\");\n";
			mFullClass = mCode.getFullClass();
		}
		
		mOutput = theHeader + mOutput;
	}
	
	protected void addFooter()
	{
		String theFooter = "";
		if (HumboldtJS.isDebug)
			theFooter += "\n\n__hjs.ready(\"" + mCode.getFullClass() + "\");\n";
		
		mOutput = mOutput + theFooter;
	}
	
	protected void pureJSHandler()
	{
		mOutput = mOutput.replace("/*--{", "").replace("}--*/", "");
	}
}