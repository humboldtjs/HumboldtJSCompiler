package com.humboldtjs.parser.stringhelper;

public class HumboldtJSLang
{
	/**
	 * Detects if the contained var or function declaration is a static.
	 * 
	 * @param aCode The AS command to check
	 * @return Whether the AS command contains the keyword static.
	 */
	public static boolean isStatic(String aCode)
	{
		String theCommand = HumboldtJSString.normalizeWhitespace(aCode);
		boolean isStatic = false;
		
		if (HumboldtJSString.hasToken(theCommand, "static")) {
			isStatic = true;
		}
		
		return isStatic;
	}
	
	/**
	 * Detects if the contained var or function declaration is a private.
	 * 
	 * @param aCode The AS command to check
	 * @return Whether the AS command contains the keyword private.
	 */
	public static boolean isPrivate(String aCode)
	{
		String theCommand = HumboldtJSString.normalizeWhitespace(aCode);
		boolean isPrivate = false;
		
		if (HumboldtJSString.hasToken(theCommand, "private")) {
			isPrivate = true;
		}
		
		return isPrivate;
	}
	
	/**
	 * Detects if the contained var or function declaration is a public.
	 * 
	 * @param aCode The AS command to check
	 * @return Whether the AS command doesn't contains the keyword private or protected.
	 */
	public static boolean isPublic(String aCode)
	{
		String theCommand = HumboldtJSString.normalizeWhitespace(aCode);
		boolean isPublic = true;
		
		if (HumboldtJSString.hasToken(theCommand, "public")) {
			isPublic = true;
		}
		if (HumboldtJSString.hasToken(theCommand, "private")) {
			isPublic = false;
		}
		if (HumboldtJSString.hasToken(theCommand, "protected")) {
			isPublic = false;
		}
		
		return isPublic;
	}

	/**
	 * Strip accessor such as public private static override protected etc from
	 * function and var definitions.
	 * 
	 * @deprecated
	 * 
	 * @param aCode The Code to parse
	 * @return The code with these accessors stripped from the beginning of the line
	 */
	protected String stripAccessors(String aCode)
	{
		String theCommand = HumboldtJSString.normalizeWhitespace(aCode);

		theCommand = HumboldtJSString.getAfterToken(aCode, "public");
		theCommand = HumboldtJSString.getAfterToken(aCode, "protected");
		theCommand = HumboldtJSString.getAfterToken(aCode, "private");
		theCommand = HumboldtJSString.getAfterToken(aCode, "override");
		theCommand = HumboldtJSString.getAfterToken(aCode, "static");

		return theCommand;
	}

	/**
	 * Grab the next code block (between eg. brackets) enclosed with the blockstarter
	 * and blockender.
	 * 
	 * @param aCode The code to get the block from
	 * @param aBlockStarter A character which starts the code block (usually {)
	 * @param aBlockEnder A character which ends the code block (usually })
	 * @return The code block including blockstarter and blockender
	 */
	public static String getCodeBlock(String aCode, char aBlockStarter, char aBlockEnder)
	{
		if (aCode == null) return null;
		if (aCode.charAt(0) != aBlockStarter) return null;

		int theDepth = 1;
		String theNewString = "{";
		String theInput = HumboldtJSString.convertNonCode(aCode);
		for (int i = 1; i < aCode.length(); i++) {
			char theChar = theInput.charAt(i);
			char theOutput = aCode.charAt(i);
			
			if (theChar == aBlockStarter) theDepth++;
			if (theChar == aBlockEnder) theDepth--;
			
			theNewString += theOutput;

			if (theDepth == 0) return theNewString;
		}
		
		return theNewString;
	}
	
	/**
	 * For DOM objects instanceof always returns "object", therefore to do meaningful
	 * type conversions and checks we must check their nodeName property to see what
	 * element they are. All is and as operators will be replaced by isOfType(object, class)
	 * and castAs(object, class). These functions also accept a string, and in string
	 * mode it will check if the nodeName matches that string. So here for known DOM
	 * classes we find a suitable substitute string which can be used to check against
	 * nodeName.
	 * 
	 * @param aClass The class to find a substitute for
	 * @return The substitute or the original class depending on whether a substitute was found.
	 */
	public static String findClassSubstitute(String aClass)
	{
		if (aClass.equals("HTMLAnchorElement")) return "\"a\"";
		if (aClass.equals("HTMLAreaElement")) return "\"area\"";
		if (aClass.equals("HTMLBodyElement")) return "\"body\"";
		if (aClass.equals("HTMLButtonElement")) return "\"button\"";
		if (aClass.equals("HTMLElement")) return "HTMLElement";
		if (aClass.equals("HTMLFormElement")) return "\"form\"";
		if (aClass.equals("HTMLFrameElement")) return "\"frame\"";
		if (aClass.equals("HTMLIFrameElement")) return "\"iframe\"";
		if (aClass.equals("HTMLFramesetElement")) return "\"frameset\"";
		if (aClass.equals("HTMLImageElement")) return "\"img\"";
		if (aClass.equals("HTMLInputElement")) return "\"input\"";
		if (aClass.equals("HTMLLinkElement")) return "\"link\"";
		if (aClass.equals("HTMLMetaElement")) return "\"meta\"";
		if (aClass.equals("HTMLObjectElement")) return "\"object\"";
		if (aClass.equals("HTMLOptionElement")) return "\"option\"";
		if (aClass.equals("HTMLScriptElement")) return "\"script\"";
		if (aClass.equals("HTMLSelectElement")) return "\"select\"";
		if (aClass.equals("HTMLTableElement")) return "\"table\"";
		if (aClass.equals("HTMLTableCaptionElement")) return "\"caption\"";
		if (aClass.equals("HTMLTableRowElement")) return "\"tr\"";
		if (aClass.equals("HTMLTableTHeadElement")) return "\"thead\"";
		if (aClass.equals("HTMLTableTFootElement")) return "\"tfoot\"";
		if (aClass.equals("HTMLTextAreaElement")) return "\"textarea\"";

		return aClass;
	}
}