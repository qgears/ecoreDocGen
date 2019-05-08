package hu.qgears.xtextdoc.util;

public class KeyInfoSubkeyword implements KeyInfo {

	private MultiKey multikey;

	public KeyInfoSubkeyword(MultiKey multikey) {
		this.multikey = multikey;
	}

	public MultiKey getMultikey() {
		return multikey;
	}
}
