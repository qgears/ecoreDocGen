package hu.qgears.xtextdoc.util;

import java.util.Collection;

import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.ParserRule;

public class KeyInfoTypeCreation implements KeyInfo {

	
	private Collection<Assignment> assignments;
	private ParserRule directParent;
	
	public KeyInfoTypeCreation(ParserRule directParent, Collection<Assignment> collection) {
		this.directParent = directParent;
		this.assignments = collection;

	}
	
	public ParserRule getParentRule() {
		return directParent;
	}

	public Collection<Assignment> getAssignments() {
		return assignments;
	}
}
