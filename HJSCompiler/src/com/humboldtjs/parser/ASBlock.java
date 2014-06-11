package com.humboldtjs.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.humboldtjs.HumboldtJS;
import com.humboldtjs.parser.stringhelper.HumboldtJSLang;
import com.humboldtjs.parser.stringhelper.HumboldtJSString;
import com.humboldtjs.parser.stringhelper.MD5;

public class ASBlock
{
	public static int mUIDLength = 4;
	
//	private static Integer mCurrentIndex = 0;
//	private static Integer mTotalLength = 0;
	public static ArrayList<String> mLanguageObjects = null;
	private ASBlock mRoot = null;
	private ASBlock mClassNode;
	private static HashMap<String, String> mUIDMap = new HashMap<String, String>();
	
	private ArrayList<Object> mSubNodes = null;
//	private ArrayList<Integer> mSubNodeIndex = null;
	private ArrayList<String> mGlobalStatics = null;
	public ArrayList<String> imports = null;
	public ArrayList<String> privates = null;
	public ArrayList<String> privateReplacements = null;
	public ArrayList<String> vectorClasses = null;

	private String mPackage;
	private String mClass;
	private String mExtends;
	private String mClassParameters;
	private String mBaseDir;
	private String mMemberInit = "";
	
	private boolean mIsClass = false;
	private boolean mIsApplication = false;
	
	public String mAppArgs = "";

	/**
	 * Parse a code block into a code tree and parse any AS-specific code into
	 * JS compatible code.
	 * 
	 * @param aCode The input code
	 * @param aBaseDir The baseDir to use for lookups of classes in the same package
	 */
	public ASBlock(String aCode, String aBaseDir)
	{
		mBaseDir = aBaseDir;
		mRoot = this;

		mPackage = "";
		mClass = "";
		mExtends = "";
		mMemberInit = "";
		mClassParameters = "()";

		mGlobalStatics = new ArrayList<String>();
		imports = new ArrayList<String>();
		privates = new ArrayList<String>();
		privateReplacements = new ArrayList<String>();
		vectorClasses = new ArrayList<String>();
		if (mLanguageObjects == null) {
			mLanguageObjects = new ArrayList<String>();
			mLanguageObjects.add("Number");
			mLanguageObjects.add("int");
			mLanguageObjects.add("uint");
			mLanguageObjects.add("Array");
			mLanguageObjects.add("Boolean");
			mLanguageObjects.add("Function");
			mLanguageObjects.add("String");
			mLanguageObjects.add("Date");
		}

		initialize(aCode, this, false);

		parse();

		for (int i = 0; i < vectorClasses.size(); i++) {
			replaceClassPackages(vectorClasses.get(i));
		}
		
		addImplicitImports();
	}

	/**
	 * Returns the converted JS as a string.
	 */
	public String toString()
	{
		return toString("");
	}
	
	/**
	 * Get the class name (without package) which the parsed class represents
	 */
	public String getClassName()
	{
		return mRoot.mClass;
	}
	
	/**
	 * Generate code to retrieve a class prototype definition based on a UID (which is generated from a classname)
	 */
	public String getUID(String aClassName, Boolean usePrototype)
	{
		if (HumboldtJS.isOptimize) {
			return (usePrototype ? "__" : "_$") + genUID(aClassName, usePrototype);
		} else {
			return genUID(aClassName, usePrototype);
		}
	}
	
	public String getUID(String aClassName)
	{
		return getUID(aClassName, true);
	}
	
	/**
	 * Generate a short UID based on a class name
	 */
	public String genUID(String aClassName, Boolean usePrototype)
	{
		if (HumboldtJS.isOptimize) {
			String theBaseUID = MD5.crypt(aClassName).substring(0, mUIDLength);
			String theUID = theBaseUID;
			int i = 0;
			
			while (mUIDMap.containsKey(theUID) && !mUIDMap.get(theUID).equals(aClassName)) {
				i++;
				theUID = theBaseUID + i;
			}

			if (!mUIDMap.containsKey(theUID)) mUIDMap.put(theUID, aClassName);
			
			return theUID; 
		} else {
			return aClassName +  (usePrototype ? ".prototype" : "");
		}
	}
	
	/**
	 * Get the full class name (including package) which the parsed class represents
	 */
	public String getFullClass()
	{
		return HumboldtJSString.getFullClass(mRoot.mPackage, mRoot.mClass); 
	}

	/**
	 * Returns whether or not the parsed class is the application starting point
	 */
	public boolean isApplication()
	{
		return mRoot.mIsApplication;
	}
	
	/**
	 * Constructor used during parsing when creating subnodes of the main AS tree
	 * 
	 * @param aInput The input code
	 * @param aRoot The root node for references to global items such as the package, imports and statics
	 * @param aIsClass Whether this block is the class-definition level. This causes vars and functions defined in this section to be defined on the class prototype.
	 */
	protected ASBlock(String aCode, ASBlock aRoot, boolean aIsClass)
	{
		super();
		initialize(aCode, aRoot, aIsClass);
	}
	
	/**
	 * Initialize the ASBlock object. This exists reduce duplication of code in the two
	 * constructors.
	 * 
	 * @param aCode The input code to parse
	 * @param aRoot The root node for references to global items such as the package, imports and statics
	 * @param aIsClass Whether this block is the class-definition level. This causes vars and functions defined in this section to be defined on the class prototype.
	 */
	protected void initialize(String aCode, ASBlock aRoot, boolean aIsClass)
	{
		mRoot = aRoot;
		mIsClass = aIsClass;

		mSubNodes = new ArrayList<Object>();
//		mSubNodeIndex = new ArrayList<Integer>();
		
//		parseMap(aCode);
		parseBlocks(aCode);
	}
	
/*	protected void parseMap(String aCode)
	{
		for (int i = 1; i < aCode.length(); i++) {
			char theChar = aCode.charAt(i);
			
			if (theChar == '\n') {
				mSubNodeIndex.add(arg0)
				mCurrentIndex++;
			}
			if (theChar == aBlockEnder) theDepth--;
			
			theNewString += theOutput;

			if (theDepth == 0) return theNewString;
		}	}
		*/
	
	protected void parseBlocks(String aCode)
	{
		if (aCode == "") return;
		String theCode = HumboldtJSString.normalizeWhitespace(aCode);
		
		HumboldtJS.showLog("Converting Vectors to Arrays", 3);
		int pos = -2;
		while ((HumboldtJSString.hasToken(theCode, "Vector", pos))) {
			String theVar = HumboldtJSString.getBeforeToken(theCode, "Vector");
			String theRest = HumboldtJSString.getAfterToken(theCode, "Vector");
			String theVectorClass = "";
			if (theRest.substring(0, 2).equals(".<")) {
				theRest = theRest.substring(2);
				theVectorClass = theRest.substring(0, theRest.indexOf('>'));
				vectorClasses.add(theVectorClass);
				theRest = theRest.substring(theRest.indexOf('>') + 1);
			}
			
			theCode = theVar + "Array" + theRest;
			pos = theVar.length() + 5;
		}
		
		HumboldtJS.showLog("Parsing command block", 4);

		while (theCode.length() > 0) {
			boolean theIsClass = false;
			boolean addSemiColon = false;
			
			String theCommand = theCode;
			theCommand = HumboldtJSString.getBeforeToken(theCommand, "{", false);
			theCommand = HumboldtJSString.getBeforeToken(theCommand, ";", false, true);
			
			if (HumboldtJSString.hasToken(theCommand, "case")) {
				theCommand = HumboldtJSString.getBeforeToken(theCommand, ":", false, true);
			}
			
			if ((HumboldtJSString.hasToken(theCommand, "var") || HumboldtJSString.hasToken(theCommand, "const")) && mIsClass && HumboldtJSLang.isStatic(theCommand)) {
				String theVar = "";
				
				if (HumboldtJSString.hasToken(theCommand, "var")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "var").trim();
				} else if (HumboldtJSString.hasToken(theCommand, "const")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "const").trim();
				}

				theVar = HumboldtJSString.getBeforeToken(theVar, ";", false);
				theVar = HumboldtJSString.getBeforeToken(theVar, "=", false);
				theVar = HumboldtJSString.getBeforeToken(theVar, ":", false).trim();

				HumboldtJS.showLog("Registering static var " + theVar, 3);
				mRoot.mGlobalStatics.add(theVar);
			}
			if ((HumboldtJSString.hasToken(theCommand, "var") || HumboldtJSString.hasToken(theCommand, "const")) && mIsClass && HumboldtJSLang.isPrivate(theCommand)) {
				String theVar = "";
				
				if (HumboldtJSString.hasToken(theCommand, "var")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "var").trim();
				} else if (HumboldtJSString.hasToken(theCommand, "const")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "const").trim();
				}

				theVar = HumboldtJSString.getBeforeToken(theVar, ";", false);
				theVar = HumboldtJSString.getBeforeToken(theVar, "=", false);
				theVar = HumboldtJSString.getBeforeToken(theVar, ":", false).trim();

				HumboldtJS.showLog("Registering private var " + theVar, 3);

				String theRenamedVar = "$$" + MD5.crypt(getFullClass() + ":" + theVar).substring(0, mUIDLength);
				mRoot.privates.add(theVar);
				mRoot.privateReplacements.add(theRenamedVar);
			}
			if (HumboldtJSString.hasToken(theCommand, "var") ||
				HumboldtJSString.hasToken(theCommand, "const") && mIsClass && !HumboldtJSLang.isStatic(theCommand)) {
				HumboldtJS.showLog("Parsing member initialization [" + theCommand + "]", 4);
				String theVar = "";
				if (HumboldtJSString.hasToken(theCommand, "var")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "var");
				} else if (HumboldtJSString.hasToken(theCommand, "const")) {
					theVar = HumboldtJSString.getAfterToken(theCommand, "const");
				}
				String theValue = theVar;
				theVar = HumboldtJSString.getBeforeToken(theVar, "=", false);
				theVar = HumboldtJSString.getBeforeToken(theVar, ":", false).trim();
				if (HumboldtJSString.hasToken(theValue, "=", false)) {
					theValue = HumboldtJSString.getAfterToken(theValue, "=", false).trim();
				} else {
					theValue = "null;";
				}

				if (mIsClass && !HumboldtJSLang.isStatic(theCommand) && !theValue.trim().equals("null;") && !theValue.trim().equals("null")) {
					mRoot.mMemberInit += "\t\t\t" + theVar + " = (" + theVar + " !== null) ? " + theVar + " : " + replaceClassPackages(theValue) + "\n";
				}
			}
			
			if (HumboldtJSString.hasToken(theCommand, "function") && mIsClass && HumboldtJSLang.isStatic(theCommand)) {
				String theFunction = HumboldtJSString.getAfterToken(theCommand, "function");
				theFunction = HumboldtJSString.getBeforeToken(theFunction, "(", false).trim();
				
				HumboldtJS.showLog("Registering static function " + theFunction, 3);

				mRoot.mGlobalStatics.add(theFunction);
				
				addSemiColon = true;
			}
			if (HumboldtJSString.hasToken(theCommand, "import")) {
				String theImport = HumboldtJSString.getAfterToken(theCommand, "import");
				theImport = HumboldtJSString.getBeforeToken(theImport, ";", false).trim();
				
				if (theImport.substring(0, 6).equals("flash.")) {
					throw new Error("You may not use objects in the \"flash\" package. These objects and methods are not available in HumboldtJS.");
				}
				
				if (!isImport(theImport, false)) {
					HumboldtJS.showLog("Adding import " + theImport, 3);

					mRoot.imports.add(theImport);
				}
			}
			if (HumboldtJSString.hasToken(theCommand, "package")) {
				mRoot.mPackage = HumboldtJSString.getAfterToken(theCommand, "package").trim();
				importPackages();
				
				addSemiColon = true;
			}
			
			if (HumboldtJSString.hasToken(theCommand, "class")) {
				String theClass = HumboldtJSString.getAfterToken(theCommand, "class").trim();
				String theExtends = "";

				theClass = HumboldtJSString.getBeforeToken(theClass, "implements").trim();
				
				if (HumboldtJSString.hasToken(theClass, "extends")) {
					theExtends = HumboldtJSString.getAfterToken(theClass, "extends").trim();
					theClass = HumboldtJSString.getBeforeToken(theClass, "extends").trim();
				}
				
				theIsClass = true;
				
				mRoot.mClass = theClass;
				mRoot.mExtends = theExtends;
				
				HumboldtJS.showLog("This file contains class " + theClass + (theExtends.equals("") ? "" : " which extends " + theExtends), 3);
				
				addSemiColon = true;
			}
			
			if (HumboldtJSString.hasToken(theCommand, "interface")) {
				String theClass = HumboldtJSString.getAfterToken(theCommand, "interface").trim();
				String theExtends = "";

				if (HumboldtJSString.hasToken(theClass, "extends")) {
					theExtends = HumboldtJSString.getAfterToken(theClass, "extends").trim();
					theClass = HumboldtJSString.getBeforeToken(theClass, "extends").trim();
				}
				
				mRoot.mClass = theClass;
				mRoot.mExtends = theExtends;
				
				HumboldtJS.showLog("This file contains interface " + theClass + (theExtends.equals("") ? "" : " which extends " + theExtends), 3);
				
				addSemiColon = true;
			}
			
			if (HumboldtJSString.hasToken(theCommand, "[Application")) {
				mRoot.mIsApplication = true;

				String theStr = HumboldtJSString.getAfterToken(theCommand, "[Application");
				if (theStr.substring(0, 1).equals("(")) {
					theStr = theStr.substring(1);
					theStr = theStr.substring(0, theStr.indexOf(")]"));
					mRoot.mAppArgs = theStr;
				}
				
				HumboldtJS.showLog("Defined [Application] metatag with arguments '" + mRoot.mAppArgs + "'", 2);
			}
			
			if (!theCommand.equals(""))
				mSubNodes.add(theCommand.trim());
			
			theCode = theCode.substring(theCommand.length());
			
			if (theCode.length() > 0 && theCode.charAt(0) == '{') {
				String theCodeBlock = HumboldtJSLang.getCodeBlock(theCode, '{', '}');
				ASBlock theASBlock = new ASBlock(theCodeBlock.substring(1, theCodeBlock.length() - 1), mRoot, theIsClass);
				mSubNodes.add(theASBlock);
				
				if (theIsClass) {
					mRoot.mClassNode = theASBlock;
				}

				theCode = theCode.substring(theCodeBlock.length());
			}
			
			if (addSemiColon) {
				if (!(theCode.length() > 0 && theCode.charAt(0) == ';')) 
					mSubNodes.add(";");
			}
		}
	}
	
	protected void parse()
	{
		if (mSubNodes.size() == 0) return;

		for (int i = 0; i < mSubNodes.size(); i++) {
			Object theObject = mSubNodes.get(i);
			if (theObject instanceof ASBlock) {
				ASBlock theASBlock = (ASBlock) theObject;
				theASBlock.parse();
			}						
		}

		int theIgnoreCount = 0;
		for (int i = 0; i < mSubNodes.size(); i++) {
			Object theObject = mSubNodes.get(i);
			if (theObject instanceof String) {
				if (theIgnoreCount == 0) {
					String theCommand = HumboldtJSString.normalizeWhitespace((String) theObject);
					boolean theSearchCasts = true;
					
					if (HumboldtJSString.hasToken(theCommand, "package")) {
						HumboldtJS.showLog("Parsing package [" + theCommand + "]", 4);
						
						String thePackage = HumboldtJSString.getAfterToken(theCommand, "package").trim();
						theCommand = "__hjs.pkg(\"" + thePackage +"\");\nwith(__hjs)";

						theSearchCasts = false;
					} else
					if (HumboldtJSString.hasToken(theCommand, "import")) {
						HumboldtJS.showLog("Parsing import [" + theCommand + "]", 4);
						String theImport = HumboldtJSString.getAfterToken(theCommand, "import");
						theImport = HumboldtJSString.getBeforeToken(theImport, ";", false).trim();
						theCommand = theImport.substring(0, 4).equals("dom.") || !HumboldtJS.isDebug ? "" : "include(\"" + theImport +"\"" + (HumboldtJS.isDebug ? ", true" : "") + ");";

						theSearchCasts = false;
					} else
					if (HumboldtJSString.hasToken(theCommand, "interface")) {
						HumboldtJS.showLog("Parsing interface [" + theCommand + "]", 4);
						String theInterface = HumboldtJSString.getAfterToken(theCommand, "interface").trim();
						String theExtends = "";
						
						if (HumboldtJSString.hasToken(theInterface, "extends")) {
							theExtends = HumboldtJSString.getAfterToken(theInterface, "extends").trim();
							theInterface = HumboldtJSString.getBeforeToken(theInterface, "extends").trim();
						}
						theInterface = HumboldtJSString.getFullClass(mRoot.mPackage, theInterface);
						theCommand = theInterface + " = ";

						String theConstructCode = "\"interface\":\"" + theInterface + "\",\"extends\":[";
						if (!theExtends.equals("")) {
							String[] theExtendeds = theExtends.split(",");
							for (int k = 0; k < theExtendeds.length; k++) {
								String theExtended = theExtendeds[k];
								Boolean wasOptimize = HumboldtJS.isOptimize;
								HumboldtJS.isOptimize = false;
								theConstructCode += ((k != 0) ? "," : "") + "\"" + replaceClassPackages(" " + theExtended + " ").trim() + "\"";
								HumboldtJS.isOptimize = wasOptimize;
							}
						}
						theConstructCode += "]";
						
						ASBlock theASBlock = new ASBlock(theConstructCode, mRoot, false);
						mSubNodes.add(i+1, theASBlock);
						mSubNodes.remove(i+2);

						if (HumboldtJS.isOptimize) {
							HumboldtJS.showLog("Registering interface UID for " + getFullClass() + " as __" + genUID(getFullClass(), false), 3);
							mSubNodes.add(i+2, "regUid(\"" + genUID(getFullClass(), false) + "\", " + getFullClass() + ");");
							theIgnoreCount++;
						}

						theSearchCasts = false;
					} else
					if (HumboldtJSString.hasToken(theCommand, "class")) {
						HumboldtJS.showLog("Parsing class [" + theCommand + "]", 4);
						String theClass = HumboldtJSString.getAfterToken(theCommand, "class").trim();
						String theExtends = "";
						String theImplements = "";
						
						if (HumboldtJSString.hasToken(theClass, "implements")) {
							theImplements = HumboldtJSString.getAfterToken(theClass, "implements").trim();
							theClass = HumboldtJSString.getBeforeToken(theClass, "implements").trim();
						}
						
						if (HumboldtJSString.hasToken(theClass, "extends")) {
							theExtends = HumboldtJSString.getAfterToken(theClass, "extends").trim();
							theClass = HumboldtJSString.getBeforeToken(theClass, "extends").trim();
						}
						
						mRoot.mClass = theClass;
						String theParameters = mRoot.mClassParameters;
						String theStrippedParams = theParameters.substring(0, theParameters.length() - 1).substring(1);
						
						theCommand = "if (" + getFullClass() + " == undefined) {\n" +
									 getFullClass() + " = function" + theParameters;
						
						HumboldtJS.showLog("Generating constructor code for when run while inheriting other class", 3);
						String theConstructCode = "";
						theConstructCode += "if (__hjs._inheriting) return;\n";
						if (!mRoot.mMemberInit.equals("")) {
							theConstructCode += "with(this) {\n\t\t\t" + mRoot.mMemberInit + "\t\t};\n";
						}
						theConstructCode += "__hjs.bindMethods(this, " + getFullClass() + ");\n";
						theConstructCode += "var __c = "+getUID(getFullClass())+".__constructor;\n";
						theConstructCode += "if(__c)__c.apply(this" + (theStrippedParams.equals("") ? "" : ", [" + theStrippedParams + "]") + ");\n";

						ASBlock theASBlock = new ASBlock(theConstructCode, mRoot, false);
						mSubNodes.add(i+1, theASBlock);
						mSubNodes.add(i+3, "}");
						
						HumboldtJS.showLog("Generating inheritance chain wrapper function", 3);
							
						mSubNodes.add(i+2, getUID(getFullClass()) + ".__chain = function ()");
	
						theASBlock = mRoot.mClassNode;
						theASBlock.mSubNodes.add(0, "__hjs.cls(__p);\n");

						if (theExtends.equals("")) {
							theExtends = "null";
						}
						theASBlock.mSubNodes.add(0, "if (!__hjs.inherit(" + getUID(getFullClass(), false) + ", " + replaceClassPackages(" " + theExtends + " ").trim() + (HumboldtJS.isOptimize ? ", \"" + genUID(getFullClass(), false) + "\"" : "") + ")) return;\nvar __p = " + getUID(getFullClass()) + ";\n");

						theIgnoreCount = 1;
		
						if (!theImplements.equals("")) {
							String[] theImplementations = theImplements.split(",");

							theConstructCode = "if (__p.__implements === undefined) " + getUID(getFullClass()) + ".__implements = [];\n";
							theConstructCode += "__p.__implements = __p.__implements.concat([";
							Boolean wasOptimize = HumboldtJS.isOptimize;
							HumboldtJS.isOptimize = false;
							for (int k = 0; k < theImplementations.length; k++) {
								theConstructCode += ((k != 0) ? "," : "") + "\"" + replaceClassPackages(" " + theImplementations[k] + " ").trim() + "\"";
							}
							HumboldtJS.isOptimize = wasOptimize;
							theConstructCode += "]);";
							theASBlock.mSubNodes.add(theExtends.equals("") ? 0 : 1, theConstructCode);
						}

						if (HumboldtJS.isOptimize) {
							HumboldtJS.showLog("Registering class UID for " + getFullClass() + " as __" + genUID(getFullClass(), false), 3);
							mSubNodes.add(i+2, "regUid(\"" + genUID(getFullClass(), false) + "\", " + getFullClass() + ");");
							theIgnoreCount++;
						}

						theSearchCasts = false;
					} else
					if (HumboldtJSString.hasToken(theCommand, "catch")) {
						HumboldtJS.showLog("Parsing catch [" + theCommand + "]", 4);
						String theFirstPart = HumboldtJSString.getBeforeToken(theCommand, "(", false);
						String theLastPart = HumboldtJSString.getAfterToken(theCommand, ")", false);
						String theVar = HumboldtJSString.getAfterToken(theCommand, "(", false);
						theVar = HumboldtJSString.getBeforeToken(theVar, ")", false);
						theVar = HumboldtJSString.getBeforeToken(theVar, ":", false);
						
						theCommand = theFirstPart + "(" + theVar + ")" + theLastPart;
					} else
					if (HumboldtJSString.hasToken(theCommand, "var") ||
						HumboldtJSString.hasToken(theCommand, "const")) {
						HumboldtJS.showLog("Parsing var [" + theCommand + "]", 4);
						String theVar = "";
						String theFirstPart = "";
						String theAccessor = "";
						String theInLoop = "";
						Boolean isForInLoop = false;
						if (HumboldtJSString.hasToken(theCommand, "in"))
							isForInLoop = true;
						if (HumboldtJSString.hasToken(theCommand, "var")) {
							theVar = HumboldtJSString.getAfterToken(theCommand, "var");
						} else if (HumboldtJSString.hasToken(theCommand, "const")) {
							theVar = HumboldtJSString.getAfterToken(theCommand, "const");
						}
						theFirstPart = theCommand.substring(0, theCommand.length() - theVar.length() - 3).trim();
						theAccessor = theFirstPart;
						if (theFirstPart.length() > 0 && theFirstPart.charAt(theFirstPart.length()-1) != '(') {
							theFirstPart = "";
						} else {
							theAccessor = "public";
							if (HumboldtJSString.hasToken(theFirstPart, "for each"))
								System.out.println("WARNING: For Each not supported in all browsers. Your code will only work in browsers that implement JavaScript 1.6 or up!");
						}
						if (theAccessor == null) {}
						
						String theValue = theVar;
						String theType = "*";
						theVar = HumboldtJSString.getBeforeToken(theVar, "=", false);
						if (HumboldtJSString.hasToken(theVar, ":", false)) {
							theType = HumboldtJSString.getAfterToken(theVar, ":", false).trim();
							if (isForInLoop) {
								theInLoop = " in " + HumboldtJSString.getAfterToken(theType, "in").trim();
								theType = HumboldtJSString.getBeforeToken(theType, "in").trim();
							}
							replaceClassPackages(theType);
						}
						theVar = HumboldtJSString.getBeforeToken(theVar, ":", false).trim();
						if (HumboldtJSString.hasToken(theValue, "=", false)) {
							theValue = HumboldtJSString.getAfterToken(theValue, "=", false).trim();
						} else {
							theValue = "null;";
						}

						if (mIsClass && !HumboldtJSLang.isStatic(theCommand) && !theValue.trim().equals("null;")) {
							theValue = "null;";
						}
						theCommand = theFirstPart + (mIsClass ? "__p." : "var ") + theVar + (isForInLoop ? theInLoop : " = " + theValue);

						if (mIsClass) theSearchCasts = false;
					} else
					if (HumboldtJSString.hasToken(theCommand, "function")) {
						HumboldtJS.showLog("Parsing function [" + theCommand + "]", 4);
						Boolean autoScope = true;
						if (HumboldtJSString.hasToken(theCommand, "[AutoScope=false]")) autoScope = false;
						String theFunction = HumboldtJSString.getAfterToken(theCommand, "function");
						String theParameters = theFunction;
						Boolean isStatic = HumboldtJSLang.isStatic(theCommand);
						theFunction = HumboldtJSString.getBeforeToken(theFunction, "(", false).trim();
						theParameters = HumboldtJSString.getAfterToken(theParameters, "(", false);
						theParameters = HumboldtJSString.getBeforeToken(theParameters, ")", false).trim();
						
						String theNewParameters = "(";
						if (!theParameters.equals("")) { 
							String theParameterParts[] = theParameters.split(",");
							for (int k = 0; k < theParameterParts.length; k++) {
								String theVar = theParameterParts[k];
								String theValue = theVar;
								String theType = "*";
								
								theVar = HumboldtJSString.getBeforeToken(theVar, "=", false).trim();
								if (HumboldtJSString.hasToken(theVar, ":", false)) {
									theType = HumboldtJSString.getAfterToken(theVar, ":", false).trim();
									theVar = HumboldtJSString.getBeforeToken(theVar, ":", false).trim();
								}
								if (HumboldtJSString.hasToken(theValue, "=", false)) {
									theValue = HumboldtJSString.getAfterToken(theValue, "=", false).trim();
								} else {
									theValue = "";
								}
								
								ASBlock theASBlock = null;
								if (mSubNodes.size() > i+1) {
									theObject = mSubNodes.get(i+1);
									if (theObject instanceof ASBlock) {
										theASBlock = (ASBlock) theObject;
									}
								}
								
								if (theType.equals("Event") && theASBlock != null) {
									theASBlock.mSubNodes.add(0, theVar + " = __hjs.event(" + theVar + ");");
								}

								replaceClassPackages(theType);
								theValue = replaceClassPackages(theValue);

								if (!theValue.equals("") && theASBlock != null && !theValue.trim().equals("null;") && !theValue.trim().equals("null")) {
									theASBlock.mSubNodes.add(0, theVar + " = (typeof(" + theVar + ") != \"undefined\") ? " + theVar + " :  " + theValue + ";");
								}
								theNewParameters += ((k != 0) ? "," : "") + theVar;
							}
						}
						theNewParameters += ")";
	
						if (autoScope && mSubNodes.size() > i+1) {
							theObject = mSubNodes.get(i+1);
							if (theObject instanceof ASBlock) {
								ASBlock theASBlock = (ASBlock) theObject;

								if (isStatic) {
									theASBlock.mSubNodes.add(0, "with (" + getUID(getFullClass()) + ") {");
								} else {
									theASBlock.mSubNodes.add(0, "with (this) {");
								}
								theASBlock.mSubNodes.add("}");
							}
						}

						if (theFunction.equals(mRoot.mClass)) {
							HumboldtJS.showLog("Found the constructor: " + theFunction, 3);
							theFunction = "__constructor";
							mRoot.mClassParameters = theNewParameters;

							/* ASBlock theASBlock = null;
							if (mSubNodes.size() > i+1) {
								theObject = mSubNodes.get(i+1);
								if (theObject instanceof ASBlock) {
									theASBlock = (ASBlock) theObject;
								}
							} */
						}
						if (theFunction.indexOf(' ') != -1 && mIsClass) {
							
							String[] theFunctionParts = theFunction.split(" ");
							String theAccessor = theFunctionParts[0].trim();
							theFunction = theFunctionParts[1].trim();

							if (theAccessor.equals("get")) theAccessor = "__defineGetter__";
							if (theAccessor.equals("set")) theAccessor = "__defineSetter__";
							theCommand = "__p." + theAccessor + "(\"" + theFunction +"\", function" + theNewParameters;
							mSubNodes.add(i+2, ")");

							HumboldtJS.showLog("WARNING: Getter/setter not supported in Internet Explorer. Your code will only work in FF/Safari/Opera!", 1);
						} else {
							if (HumboldtJSLang.isStatic(theCommand) && mIsClass) {
								theCommand = "__p." + theFunction + " = __hjs.bind(__p, function" + theNewParameters;
								mSubNodes.add(i+2, ");");
							} else {
								theCommand = (mIsClass ? "__hjs.regm(__p, \"" : "function ") + theFunction + (mIsClass ? "\", function" + (HumboldtJS.isDebug ? " " + theFunction : "") : "") + theNewParameters;
								if (mIsClass) {
									mSubNodes.add(i+2, ");");
								}
							}
						}

						if (mIsClass) theSearchCasts = false;
					}
	
					HumboldtJS.showLog("Replacing class packages at this level", 5);
					theCommand = replaceClassPackages(theCommand);
					if (!mIsClass) {
						HumboldtJS.showLog("Replacing global statics at this level", 5);
						theCommand = replaceGlobalStatics(theCommand);
						HumboldtJS.showLog("Replacing super calls at this level", 5);
						theCommand = replaceSupers(theCommand);
					}
					if (HumboldtJS.isOptimize) {
						//HumboldtJS.showLog("Replacing privates", 5);
						//theCommand = replacePrivates(theCommand);
					}

					HumboldtJS.showLog("Fixing 'casts' at this level", 5);
					while ((HumboldtJSString.hasToken(theCommand, "as") || 
						   HumboldtJSString.hasToken(theCommand, "is")) && theSearchCasts) {
						int thePos = -1;
						String operator = "";
						String preChars = " +-=/*<>!&|([,";
						String postChars = " +-=/*<>!&|;)],";

						if (HumboldtJSString.hasToken(theCommand, "as")) {
							thePos = HumboldtJSString.indexOfToken(theCommand, "as");
							operator = "__hjs.castAs";
						} else 
						if (HumboldtJSString.hasToken(theCommand, "is")) {
							thePos = HumboldtJSString.indexOfToken(theCommand, "is");
							operator = "__hjs.isOfType";
						}

						String theFirstPart = theCommand.substring(0, thePos).trim();
						String theSecondPart = "";
						String theThirdPart = "";
						String theFourthPart = theCommand.substring(thePos + 2).trim();;
						
						boolean hasFinished = theFourthPart.length() == 0;
						int theDepth1 = 0;
						int theDepth2 = 0;
						while (!hasFinished) {
							char theChar = theFourthPart.charAt(0);
							if ((postChars.indexOf(theChar) != -1 && theDepth1 <= 0 && theDepth2 <= 0) || theDepth1 < 0 || theDepth2 < 0) hasFinished = true;
							if (theChar == '(') theDepth1++;
							if (theChar == ')') theDepth1--;
							if (theChar == '[') theDepth2++;
							if (theChar == ']') theDepth2--;

							if (!hasFinished) {
								theFourthPart = theFourthPart.substring(1);
								theThirdPart += theChar;
							}
							if (theFourthPart.length() == 0) hasFinished = true;
						}
						
						hasFinished = theFirstPart.length() == 0;
						theDepth1 = 0;
						theDepth2 = 0;
						while (!hasFinished) {
							char theChar = theFirstPart.charAt(theFirstPart.length()-1);
							if ((preChars.indexOf(theChar) != -1 && theDepth1 <= 0 && theDepth2 <= 0) || theDepth1 < 0 || theDepth2 < 0) hasFinished = true;
							if (theChar == '(') theDepth1--;
							if (theChar == ')') theDepth1++;
							if (theChar == '[') theDepth2--;
							if (theChar == ']') theDepth2++;

							if (!hasFinished) {
								theFirstPart = theFirstPart.substring(0, theFirstPart.length()-1);
								theSecondPart = theChar + theSecondPart;
							}
							if (theFirstPart.length() == 0) hasFinished = true;
						}

						theFirstPart = theFirstPart + operator + "(";
						theFourthPart = ")" + theFourthPart;
						theCommand = theFirstPart + theSecondPart + ", " + theThirdPart + theFourthPart;
					}

					mSubNodes.set(i, theCommand.trim());
				} else {
					theIgnoreCount--;
				}
			}
		}
	}
	
	protected String replaceSupers(String aCode)
	{

		// constructors
		int pos = -2;
		while (pos != -1) {
			if (pos != -2) {
				HumboldtJS.showLog("Rewriting call to constructor super", 3);
				String theFirstPart = aCode.substring(0, pos);
				String theSecondPart = aCode.substring(pos + 5);
				if (mRoot.mExtends.equals("")) {
					aCode = theFirstPart + "/* super(); */";
					pos = aCode.length();
				} else {
					aCode = theFirstPart + "__hjs.sup(this, " + replaceClassPackages(mRoot.mExtends) + ")";
					pos = aCode.length();
					aCode += theSecondPart;
				}
			}
			
			pos = HumboldtJSString.indexOfTokenWithPrePost(aCode, "super", " (.{", " (", pos, false, true);
		}

		// super methods
		pos = -2;
		while (pos != -1) {
			if (pos != -2) {
				String theFirstPart = aCode.substring(0, pos);
				String theSecondPart = aCode.substring(pos + 5);
				String theProperty = "";

				boolean hasFinished = theSecondPart.length() == 0;
				String chars = " +-=/*<>!&|;)(,";
				while (!hasFinished) {
					char theChar = theSecondPart.charAt(0);
					if (chars.indexOf(theChar) != -1) hasFinished = true;

					if (!hasFinished) {
						theSecondPart = theSecondPart.substring(1);
						theProperty += theChar;
					}
					if (theSecondPart.length() == 0) hasFinished = true;
				}
				
				HumboldtJS.showLog("Rewriting call to super" + theProperty, 3);
				aCode = theFirstPart + "__hjs.sup(this, " + replaceClassPackages(mRoot.mExtends + theProperty) + ")";

				pos = aCode.length();
				aCode += theSecondPart;
			}
			pos = HumboldtJSString.indexOfTokenWithPrePost(aCode, "super", " (.{", ".", pos, false, true);
		}
		
		return aCode;
	}
	
	protected String replacePrivates(String aCode)
	{
		for (int i = 0; i < mRoot.privates.size(); i++) {
			String theVar = mRoot.privates.get(i);
			String theRenamedVar = mRoot.privateReplacements.get(i);
			
			System.out.println("Trying to replace " + theVar + " with " + theRenamedVar);
			
			int pos = -2;
			while (pos != -1) {
				if (pos != -2) {
					System.out.println(aCode);
					aCode = aCode.substring(0, pos) + theRenamedVar + aCode.substring(pos + theVar.length());
					System.out.println(aCode);
					pos = pos + theRenamedVar.length();
				}
				pos = HumboldtJSString.indexOfTokenWithPrePost(aCode + " ", theVar, ": (+-/=*!<>&|{,", "(+-/=*!<>&|.;, )}", pos, false, true);
			}
		}
		return aCode;
	}
	
	protected String replaceGlobalStatics(String aCode)
	{
		for (int i = 0; i < mRoot.mGlobalStatics.size(); i++) {
			String theVar = mRoot.mGlobalStatics.get(i);
			if (theVar.charAt(0) == '_') {
				theVar = theVar.substring(1);
			}
			
			int pos = -2;
			while (pos != -1) {
				if (pos != -2) {
					aCode = aCode.substring(0, pos) + "__class." + theVar + aCode.substring(pos + theVar.length());
					HumboldtJS.showLog("Rewriting call to " + theVar, 3);
				}
				pos = HumboldtJSString.indexOfTokenWithPrePost(aCode, theVar, ": (+-/=*!<>&|{,", "(+-/=*!<>&|.;, )}", pos, false, true);
			}
		}
		return aCode;
	}
		
	protected String replaceClassPackages(String aCode)
	{
		for (int i = 0; i < mRoot.imports.size(); i++) {
			String thePackage = mRoot.imports.get(i);
			aCode = replaceClassPackage(aCode, thePackage, i);
		}
		
		for (int i = 0; i < mLanguageObjects.size(); i++) {
			String theLangObject = mLanguageObjects.get(i);
			aCode = replaceClassPackage(aCode, theLangObject, -1);
		}
		return aCode;
	}
	
	protected String replaceClassPackage(String aCode, String aPackage, int aImportNumber)
	{
		boolean isImplicitImport = false;
		boolean isUsed = false;
		if (aPackage.charAt(0) == '$' || aPackage.charAt(0) == '*') {
			isImplicitImport = true;
			aPackage = aPackage.substring(1);
		}
		
		boolean isDomClass = false;
		if (aPackage.length() > 4 && aPackage.substring(0,4).equals("dom.")) isDomClass = true;
		if (aPackage.equals("dom")) isDomClass = true;
		
		String theClass = aPackage;
		while (theClass.indexOf(".") != -1) {
			theClass = theClass.substring(theClass.indexOf(".") + 1);
		}
		
		if (aPackage.length() > theClass.length()) {
			aPackage = aPackage.substring(0, aPackage.length() - theClass.length() - 1);
		} else {
			aPackage = "";
		}
		
		int pos = -2;
		while (pos != -1) {
			if (pos != -2) {
				isUsed = true;
				char theNextChar = ' ';
				if (aCode.length() > pos + theClass.length()) theNextChar = aCode.charAt(pos + theClass.length());

				String replacement = "";
				if (isDomClass) {
					replacement = HumboldtJSLang.findClassSubstitute(theClass);
				} else {
					Boolean wasOptimize = HumboldtJS.isOptimize;
					if (aImportNumber == -1) HumboldtJS.isOptimize = false;
					replacement = getUID(HumboldtJSString.getFullClass(aPackage, theClass), theNextChar == '.');
					HumboldtJS.isOptimize = wasOptimize;
				}
				aCode = aCode.substring(0, pos) + replacement + aCode.substring(pos + theClass.length());
				HumboldtJS.showLog("Rewriting call to " + theClass, 3);

				pos = pos + replacement.length();
			}
			pos = HumboldtJSString.indexOfTokenWithPrePost(aCode, theClass, ": ([+-=/*<>&|!,", " ,([.+-=/*<>&|!;])", pos, false, true);

		}
		if (isImplicitImport && isUsed && (aImportNumber != -1)) {
			mRoot.imports.set(aImportNumber, "$" + HumboldtJSString.getFullClass(aPackage, theClass));
		}
		return aCode;
	}
	
	protected String toString(String aIndent)
	{
		String theOutput = "";
		int theControlDepth = 0;
		
		for (int i = 0; i < mSubNodes.size(); i++) {
			Object theObject = mSubNodes.get(i);
			if (theObject instanceof String) {
				String theLine = (String)theObject;
				if (theLine.equals(";")) {
					theOutput = theOutput.substring(0, theOutput.length() - 1);
					theOutput += ";\n";
				} else 
				if (!theLine.equals("")) {
					int thePrevControlDepth = theControlDepth;
					for (int j = 0; j < theLine.length(); j++) {
						char theChar = theLine.charAt(j);
						if (theChar == '(') theControlDepth++;
						if (theChar == ')') theControlDepth--;
					}
					
					theOutput += ((thePrevControlDepth == 0) ? aIndent : " ") + theLine.trim() + ((theControlDepth ==  0) ? "\n" : "");
				}
			} else
			if (theObject instanceof ASBlock) {
				theOutput += aIndent + "{\n" + ((ASBlock) theObject).toString("\t" + aIndent) + aIndent + "}\n";
			}
		}
		
		return theOutput;
	}
	
	protected void addImplicitImports()
	{
		if (mSubNodes.size() <= 2) return;
		
		Object theObject = mSubNodes.get(1);
		if (theObject instanceof ASBlock) {
			ASBlock theASBlock = (ASBlock) theObject;
			
			for (int i = 0; i < mRoot.imports.size(); i++) {
				String theImport = mRoot.imports.get(i);
				if (theImport.charAt(0) == '*' || theImport.charAt(0) == '$') {
					theImport = theImport.substring(1);
					if (isImport(theImport, true) && !getFullClass().equals(theImport)) {
						String theImportCommand = theImport.substring(0, 4).equals("dom.") || !HumboldtJS.isDebug ? "" : "include(\"" + theImport +"\"" + (HumboldtJS.isDebug ? ", true" : "") + ");";
						theASBlock.mSubNodes.add(0,theImportCommand);
					}
				}
			}
		}
	}
	
	public boolean isImport(String aImport, boolean aUsed)
	{
		for (int i = 0; i < mRoot.imports.size(); i++) {
			String theImport = mRoot.imports.get(i);
			if (theImport.equals(aImport)) return true;
			if (!aUsed && theImport.charAt(0) == '*' && theImport.substring(1).equals(aImport)) return true;
			if (theImport.charAt(0) == '$' && theImport.substring(1).equals(aImport)) return true;
		}
		return false;
	}
	
	protected void importPackages()
	{
		File theFile = new File(mRoot.mBaseDir);
		if (theFile.isFile()) { // then it must be a library
			String theFolderName = convertPackageToFolder(mRoot.mPackage, "");
			try {
				ZipFile zf = new ZipFile(theFile.getAbsolutePath());
				for (Enumeration<? extends ZipEntry> e = zf.entries() ; e.hasMoreElements() ;) {
					ZipEntry entry = e.nextElement();
					if (entry.getName().startsWith(theFolderName)) {
						String theFileName = entry.getName().substring(theFolderName.length());
						if (theFileName.endsWith(".as") && theFileName.indexOf('/') == -1) {
							mRoot.imports.add("*" + HumboldtJSString.getFullClass(mRoot.mPackage, theFileName.substring(0, theFileName.length()-3)));
						}
					}
				}
		     } catch (IOException e) {}			
		} else {
			String theFolderName = convertPackageToFolder(mRoot.mPackage, mRoot.mBaseDir);
			File theFolder = new File(theFolderName);
			File[] theFiles = theFolder.listFiles();
			
			HumboldtJS.showLog("Automatically adding imports from class package " + mRoot.mPackage, 3);
	
			for (int i = 0; i < theFiles.length; i++) {
				if (theFiles[i].isFile()) {
					String theFileName = theFiles[i].getName();
					if (theFileName.substring(theFileName.length()-3).equals(".as")) {
						mRoot.imports.add("*" + HumboldtJSString.getFullClass(mRoot.mPackage, theFileName.substring(0, theFileName.length()-3)));
					}
				}
			}
		}
	}
	
	protected String convertPackageToFolder(String aPackage, String aBaseDir)
	{
		String[] thePackageParts = aPackage.split("\\.");
		String theFolder = aBaseDir;
		if (!aPackage.equals(""))
		for (int i = 0; i < thePackageParts.length; i++) {
			String thePackagePart = thePackageParts[i];
			if (i != 0) theFolder += "/";
			theFolder += thePackagePart;
		}
		if (theFolder.charAt(theFolder.length()-1) != '/') theFolder += "/";
		return theFolder;
	}

	
	public String convertClassWithPackageToFile(String aClass)
	{
		String[] thePackageParts = aClass.split("\\.");
		String theFile = "";
		if (!aClass.equals(""))
		for (int i = 0; i < thePackageParts.length; i++) {
			String thePackagePart = thePackageParts[i];
			if (i != 0) theFile += "/";
			theFile += thePackagePart;
		}
		return theFile + ".as";
	}
}