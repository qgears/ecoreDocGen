package hu.qgears.xtextdoc.generator;

import java.util.Comparator;

import org.eclipse.xtext.ParserRule;

public class NameComparator implements Comparator<ParserRule> {

	@Override
	public int compare(ParserRule o1, ParserRule o2) {
		return o1.getName().compareTo(o2.getName());
	}

}
