package hu.qgears.xtextdoc.util;

import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.ParserRule;

public class KeyInfoFeatureSet implements KeyInfo {

	private Assignment assignment;
	private ParserRule directParent;
	
	public KeyInfoFeatureSet(ParserRule directParent, Assignment assignment) {
		this.directParent = directParent;
		this.assignment = assignment;
	}

	
	public Assignment getAssignment() {
		return assignment;
	}

	public ParserRule getParentRule(){
		return directParent;
	}
	
	
}
