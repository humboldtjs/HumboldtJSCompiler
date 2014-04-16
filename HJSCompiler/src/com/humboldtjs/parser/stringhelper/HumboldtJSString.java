package com.humboldtjs.parser.stringhelper;

import com.humboldtjs.parser.ASBlock;

public class HumboldtJSString
{
	private static String lastNonCode = "";
	private static String lastInputCode = "";
	
	/**
	 * Strip a piece of code of anything after an occurence of a token
	 * 
	 * @param aCode The code to strip
	 * @param aToken The token to look for
	 * @param aIncludeToken Whether or not to include the token in the result
	 * @return Returns the code with the end stripped starting from the token.
	 */
	public static String getBeforeToken(String aCode, String aToken, boolean aUseTokenDelimiters, boolean aIncludeToken)
	{
		if (hasToken(aCode, aToken, aUseTokenDelimiters)) {
			return aCode.substring(0, indexOfToken(aCode, aToken, aUseTokenDelimiters) + (aIncludeToken ? aToken.length() : 0));
		}
		return aCode;
	}
	public static String getBeforeToken(String aCode, String aToken, boolean aUseTokenDelimiters)
	{
		return getBeforeToken(aCode, aToken, aUseTokenDelimiters, false);
	}
	public static String getBeforeToken(String aCode, String aToken)
	{
		return getBeforeToken(aCode, aToken, true, false);
	}

	/**
	 * Strip a piece of code of anything after an occurence of a token
	 * 
	 * @param aCode The code to strip
	 * @param aToken The token to look for
	 * @param aIncludeToken Whether or not to include the token in the result
	 * @return Returns the code with the end stripped starting from the token.
	 */
	public static String getAfterToken(String aCode, String aToken, boolean aUseTokenDelimiters, boolean aIncludeToken)
	{
		if (hasToken(aCode, aToken, aUseTokenDelimiters)) {
			return aCode.substring(indexOfToken(aCode, aToken, aUseTokenDelimiters) + (aIncludeToken ? 0 : aToken.length()));
		}
		return aCode;
	}
	public static String getAfterToken(String aCode, String aToken, boolean aUseTokenDelimiters)
	{
		return getAfterToken(aCode, aToken, aUseTokenDelimiters, false);
	}
	public static String getAfterToken(String aCode, String aToken)
	{
		return getAfterToken(aCode, aToken, true, false);
	}

	/**
	 * Converts non-code parts of AS code to $ so the parser won't try to interpret
	 * AS commands, variables, classes, etc in comments and Strings.
	 * 
	 * @param aCode The ASCode to convert
	 * @return A cleaned String with all comments and Strings replaced with $
	 */
	public static String convertNonCode(String aCode)
	{
		if (aCode == null) return null;
		if (lastInputCode.indexOf(aCode) != -1 && !lastNonCode.substring(lastInputCode.indexOf(aCode)).substring(0, 1).equals("$")) return lastNonCode.substring(lastInputCode.indexOf(aCode)).substring(0, aCode.length());
		
		boolean isDoubleQuotes = false;
		boolean isSingleQuotes = false;
		boolean isNewDoubleQuotes = false;
		boolean isNewSingleQuotes = false;
		
		boolean isDeepComment = false;
		boolean isComment = false;
		boolean isNewDeepComment = false;
		boolean isNewComment = false;
		
		char thePreviousChar = ' ';
		String theNewString = "";
		
		for (int i = 0; i < aCode.length(); i++) {
			char theChar = aCode.charAt(i);
			if (theChar == '\\' && (isSingleQuotes || isDoubleQuotes)) {
				theNewString += "$";
				i++;
			} else {
				if (theChar == '"' && !isDeepComment && !isComment && !isSingleQuotes) {
					isNewDoubleQuotes = !isDoubleQuotes;
				}
				if (theChar == '\'' && !isDeepComment && !isComment && !isDoubleQuotes) {
					isNewSingleQuotes = !isSingleQuotes;
				}
				if (theChar == '/' && thePreviousChar == '/' && !isDeepComment && !isSingleQuotes && !isDoubleQuotes) {
					isNewComment = true;
				}
				if (isComment && theChar == '\n') {
					isNewComment = false;
				}
				if (theChar =='*' && thePreviousChar == '/' && !isComment && !isSingleQuotes && !isDoubleQuotes) {
					isNewDeepComment = true;
				}
				if (isDeepComment && theChar == '/' && thePreviousChar == '*') {
					isNewDeepComment = false;
					isDeepComment = false;
				}
			}
			
			if (isDoubleQuotes || isNewDoubleQuotes || isSingleQuotes || isNewSingleQuotes || isDeepComment || isComment) {
				theNewString += "$";
			} else {
				theNewString += aCode.substring(i, i+1);
			}
			
			isDoubleQuotes = isNewDoubleQuotes;
			isSingleQuotes = isNewSingleQuotes;
			isDeepComment = isNewDeepComment;
			isComment = isNewComment;
			thePreviousChar = theChar;
		}
		
		lastNonCode = theNewString;
		lastInputCode = aCode;
		
		return theNewString;
	}

	/**
	 * Search a piece of AS code for a token that occurs within the code and
	 * return whether it exists. This first converts the non-code to $ before 
	 * processing so no instances of the token are found in comments and Strings.
	 * 
	 * @param aCode The AS code to search for the token
	 * @param aToken The token to search for
	 * @return Whether or not the code contains the token
	 */
	public static boolean hasToken(String aCode, String aToken, boolean aUseTokenDelimiters)
	{
		return indexOfToken(aCode, aToken, aUseTokenDelimiters) != -1;
	}
	public static boolean hasToken(String aCode, String aToken)
	{
		return hasToken(aCode, aToken, true);
	}
	public static boolean hasToken(String aCode, String aToken, int aAfter)
	{
		if (aAfter < 0) aAfter = 0;
		return hasToken(aCode.substring(aAfter), aToken, true);
	}
	
	/**
	 * Search a piece of AS code for a token that occurs within the code and
	 * return the token's position. This first converts the non-code to $ before 
	 * processing so no instances of the token are found in comments and Strings.
	 * 
	 * @param aCode The AS code to search for the token
	 * @param aToken The token to search for
	 * @return The position of the token, or -1 if not found
	 */
	public static int indexOfToken(String aCode, String aToken, boolean aUseTokenDelimiters)
	{
		if (aUseTokenDelimiters) {
			return indexOfTokenWithPrePost(aCode, aToken, "(+-/=*!<>&|.;:, ){[]\t\n\r", "(+-/=*!<>&|.;:, )}[]\t\n\r", false);
		} else {
			return convertNonCode(aCode).indexOf(aToken);
		}
	}
	public static int indexOfToken(String aCode, String aToken)
	{
		return indexOfToken(aCode, aToken, true);
	}
	
	/**
	 * Search a piece of AS code for a token that occurs within the code and
	 * return the token's position. This first converts the non-code to $ before 
	 * processing so no instances of the token are found in comments and Strings.
	 * 
	 * @param aCode The AS code to search for the token
	 * @param aToken The token to search for
	 * @return The position of the token, or -1 if not found
	 */
	public static int lastIndexOfToken(String aCode, String aToken, boolean aUseTokenDelimiters)
	{
		if (aUseTokenDelimiters) {
			return lastIndexOfTokenWithPrePost(aCode, aToken, "(+-/=*!<>&|.;, ){[]", "(+-/=*!<>&|.;, )}[]", false);
		} else {
			return convertNonCode(aCode).lastIndexOf(aToken);
		}
	}
	public static int lastIndexOfToken(String aCode, String aToken)
	{
		return lastIndexOfToken(aCode, aToken, false);
	}

	/**
	 * Converts any whitespace in a block of AS code to a space. This makes searching in
	 * code much easier because there is no need to check many different variations of
	 * whitespace.
	 * 
	 * @param aCode The code to normalize the whitespace in
	 * @return Code with all tabs, CRs & LFs replaced by spaces
	 */
	public static String normalizeWhitespace(String aCode)
	{
		if (aCode == null) return null;
		
		String theInput = convertNonCode(aCode);
		String theNewString = " ";
		for (int i = 0; i < aCode.length(); i++) {
			char theChar = theInput.charAt(i);
			char theOutputChar = aCode.charAt(i);
			if (theChar == '\t') theOutputChar = ' ';
			if (theChar == '\n') theOutputChar = ' ';
			if (theChar == '\r') theOutputChar = ' ';
			
			theNewString += theOutputChar;
		}
		theNewString += ' ';
		
		return theNewString;
	}
	
	/**
	 * Return a full class identifier for use in the compiled JS code. If it is
	 * a top-level class "window." will automatically be prepended.
	 * 
	 * @param aPackage The package in which the class lives
	 * @param aClass The classname
	 * @return An identifier which points to the JS object containing the class
	 */
	public static String getFullClass(String aPackage, String aClass)
	{
		if (aPackage.equals(""))
		for (int i = 0; i < ASBlock.mLanguageObjects.size(); i++) {
			if (ASBlock.mLanguageObjects.get(i).equals(aClass)) return aClass;
		}
		return aPackage + (aPackage.equals("") ? "window." : ".") + aClass;
	}
	
	/**
	 * Search a string for a token by checking if any combination of 
	 * preChar + token + postChar exists in the string.
	 * 
	 * @param aCode The AS code to search for the token
	 * @param aToken The token to search for
	 * @param aPreChars A list of characters to try in front of the token
	 * @param aPostChars A list of characters to try behind the token
	 * @param aAfter The number of characters to skip in the token-search
	 * @return The position of the found token, or -1 if not found
	 */
	public static int indexOfTokenWithPrePost(String aCode, String aToken, String aPreChars, String aPostChars, int aAfter, boolean aWithoutMode, boolean aForwardDirection)
	{
		if (aAfter < 0) aAfter = 0;
		
		String theStrippedCode = aCode.substring(0, aAfter) + convertNonCode(aCode.substring(aAfter));
		
		int pos = -2;
		while (pos != -1) {
			if (pos != -2) {
				char thePreChar = ' ';
				char thePostChar = ' ';
				boolean isValid = false;

				if (pos > 0) thePreChar = theStrippedCode.charAt(pos - 1);
				if (pos + aToken.length() < theStrippedCode.length() - 1) thePostChar = theStrippedCode.charAt(pos + aToken.length());
				if (aWithoutMode) {
					if (aPreChars.indexOf(thePreChar) == -1 && aPostChars.indexOf(thePostChar) == -1) isValid = true;
				} else {
					if (aPreChars.indexOf(thePreChar) != -1 && aPostChars.indexOf(thePostChar) != -1) isValid = true;
				}
				if (isValid) {
					return pos;
				}
				
				aAfter = pos + aToken.length();
			}
			if (aForwardDirection) {
				pos = theStrippedCode.indexOf(aToken, aAfter);
			} else {
				pos = theStrippedCode.lastIndexOf(aToken, aAfter);
			}
		}
		return -1;
	}
	public static int indexOfTokenWithPrePost(String aCode, String aToken, String aPreChars, String aPostChars, boolean aWithoutMode)
	{
		return indexOfTokenWithPrePost(aCode, aToken, aPreChars, aPostChars, 0, aWithoutMode, true);
	}
	public static int lastIndexOfTokenWithPrePost(String aCode, String aToken, String aPreChars, String aPostChars, boolean aWithoutMode)
	{
		return indexOfTokenWithPrePost(aCode, aToken, aPreChars, aPostChars, aCode.length(), aWithoutMode, false);
	}


	private static String hex(char ch)
	{
		return Integer.toHexString(ch).toUpperCase();
	}
	
	public static String escapeJavaStyleString(String str, boolean escapeSingleQuote)
	{
		String theReturn = "";
		for (int i = 0; i < str.length(); i++) {
			char ch = str.charAt(i);
	
			if (ch > '࿿') {
				theReturn += "\\u" + hex(ch);
			} else if (ch > 'ÿ') {
				theReturn += "\\u0" + hex(ch);
			} else if (ch > '') {
				theReturn += "\\u00" + hex(ch);
			} else if (ch < ' ') {
				switch (ch) {
				case '\b':
					theReturn += (char) 92;
					theReturn += (char) 98;
					break;
				case '\n':
					theReturn += (char) 92;
					theReturn += (char) 110;
					break;
				case '\t':
					theReturn += (char) 92;
					theReturn += (char) 116;
					break;
				case '\f':
					theReturn += (char) 92;
					theReturn += (char) 102;
					break;
				case '\r':
					theReturn += (char) 92;
					theReturn += (char) 114;
					break;
				case '\013':
				default:
					if (ch > '\017') {
						theReturn += "\\u00" + hex(ch);
						continue;
					}
					theReturn += "\\u000" + hex(ch);
					break;
				}
			} else {
				switch (ch) {
				case '\'':
					if (escapeSingleQuote) {
						theReturn += (char) 92;
					}
					theReturn += (char) 39;
					break;
				case '"':
					theReturn += (char) 92;
					theReturn += (char) 34;
					break;
				case '\\':
					theReturn += (char) 92;
					theReturn += (char) 92;
					break;
				default:
					theReturn += ch;
					break;
				}
			}
		}
		
		return theReturn;
	}	
}