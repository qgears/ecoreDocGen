package hu.qgears.xtextdoc.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ParserRule;

public class MultiKey {
	private final Keyword keys[];
	private String value;
	private int elementsLength;
	private KeyInfo keyInfo;

	public MultiKey(Keyword kv) {
		keys=new Keyword[]{kv};
		value=kv.getValue();
		elementsLength=1;
	}
	public MultiKey(List<Keyword> kvs) {
		keys=kvs.toArray(new Keyword[]{});
		StringBuilder ret=new StringBuilder();
		UtilComma c=new UtilComma(" ");
		for(Keyword k: keys)
		{
			ret.append(c.getSeparator());
			ret.append(k.getValue());
		}
		value=ret.toString();
		elementsLength=keys.length;
	}
	
	public Keyword[] getKeys() {
		return keys;
	}
	
	@Override
	public int hashCode() {
		int ret=113;
		for(Keyword k: keys)
		{
			ret^=k.hashCode();
		}
		return ret;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof MultiKey)
		{
			MultiKey other=(MultiKey) obj;
			return Arrays.equals(keys, other.keys);
		}else
		{
			return super.equals(obj);
		}
	}
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "'"+value+"'";
	}
	
	
	private boolean isFirstKeyWordInBranch(AbstractElement a) {
		if (a == getFirstKey()){
			return true;
		} else if (a instanceof Group) {
			for (AbstractElement g : ((Group) a).getElements()) {
				//only the first branch is accepted
				return isFirstKeyWordInBranch(g);
			}
		} else if (a instanceof Alternatives){
			for (AbstractElement branch : ((Alternatives) a).getElements()){
				if (isFirstKeyWordInBranch(branch)){
					//all alternative branches are accepted
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean isFirstKeyword(ParserRule localRoot) {
		
		
		return isFirstKeyWordInBranch(localRoot.getAlternatives());
		
	}

	public int getElementsLength() {
		return elementsLength;
	}
	
	public void setKeyInfo(KeyInfo keyInfo) {
		this.keyInfo = keyInfo;
	}
	
	public KeyInfo getKeyInfo() {
		return keyInfo;
	}
	public Keyword getFirstKey() {
		return keys[0];
	}
}
