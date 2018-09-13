package hu.qgears.xtextdoc.util;

import org.eclipse.xtext.ParserRule;

public class KeywordUsecase {
	public ParserRule root;
	public ParserRule localRule;
	public KeywordUsecase(ParserRule root, ParserRule localRule) {
		super();
		this.root = root;
		this.localRule = localRule;
	}
}
