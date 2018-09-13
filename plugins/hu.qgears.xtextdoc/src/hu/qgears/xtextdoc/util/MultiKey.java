package hu.qgears.xtextdoc.util;

import java.util.Arrays;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ParserRule;

public class MultiKey {
	private final Keyword keys[];
	private String value;
	private AbstractElement element;
	private int elementsLength;
	public MultiKey(Keyword kv) {
		keys=new Keyword[]{kv};
		value=kv.getValue();
		element=kv;
		elementsLength=1;
		detectOrKey();
	}
	public MultiKey(List<AbstractElement> kvs) {
		keys=kvs.toArray(new Keyword[]{});
		StringBuilder ret=new StringBuilder();
		UtilComma c=new UtilComma(" ");
		for(Keyword k: keys)
		{
			ret.append(c.getSeparator());
			ret.append(k.getValue());
		}
		value=ret.toString();
		element=keys[0];
		elementsLength=keys.length;
		detectOrKey();
	}
	private void detectOrKey()
	{
		if(element.eContainer() instanceof Alternatives)
		{
			Alternatives a=(Alternatives) element.eContainer();
			element=a;
			elementsLength=1;
		}
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
	public boolean isFirstKeyword(ParserRule localRoot) {
		{
			{
				AbstractElement ae=(AbstractElement)localRoot.getAlternatives();
				while(ae instanceof Group)
				{
					Group g=(Group) ae;
					ae=g.getElements().get(0);
					if(ae instanceof Alternatives)
					{
						for(AbstractElement ae2: ((Alternatives) ae).getElements())
						{
							if(ae2 instanceof Group)
							{
								if(((Group) ae2).getElements().get(0)==keys[0])
								{
									return true;
								}
							}
						}
					}
				}
			}
		}
		{
			AbstractElement ae= localRoot.getAlternatives();
			findFirstKeyword(localRoot);
			if(ae instanceof Group)
			{
				Group g=(Group) ae;
				if(g.getElements().get(0) == keys[0])
				{
					return true;
				}
			}
		}
		return false;
	}
	private void findFirstKeyword(ParserRule localRoot) {
	}
	public AbstractElement getElement()
	{
		return element;
	}
	public int getElementsLength() {
		return elementsLength;
	}
}
