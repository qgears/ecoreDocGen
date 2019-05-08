package hu.qgears.xtextdoc.util;

import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.EnumRule;

public class KeyInfoEnum implements KeyInfo {

	private EnumRule directParent;
	private EnumLiteralDeclaration container;

	public KeyInfoEnum(EnumRule directParent, EnumLiteralDeclaration container) {
		this.directParent = directParent;
		this.container = container;
	}

	public EnumLiteralDeclaration getLiteral() {
		return container;
	}
	
	public EnumRule getParentRule() {
		return directParent;
	}

}
