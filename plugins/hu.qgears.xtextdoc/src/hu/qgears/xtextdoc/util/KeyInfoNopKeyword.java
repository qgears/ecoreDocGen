package hu.qgears.xtextdoc.util;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.xtext.ParserRule;

/**
 * Key info for keys that do nothing : they are not directly related to any
 * model element, attribute or reference.
 * 
 * This type of keywords are used for mainly for making the XTEXT file more user readable.
 * 
 * XXX: If you feel that a given keyword does not belong to this category, then it might be related 
 * to an uncovered use case, which cannot be identified by the generator.
 * 
 * @author agostoni
 *
 */
public class KeyInfoNopKeyword implements KeyInfo {

	private ParserRule parserRule;

	public KeyInfoNopKeyword(ParserRule parserRule) {
		this.parserRule = parserRule;
	}
	
	public EClassifier getHostType() {
		return parserRule.getType().getClassifier();
	}

	public ParserRule getParserRule() {
		return parserRule;
	}
}
