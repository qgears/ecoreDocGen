package hu.qgears.xtextdoc.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;


/**
 * Abstract base class for HTML generating template classes.
 * See https://github.com/qgears/rtemplate for information on templates used in this project.
 * @author rizsi
 *
 */
abstract public class AbstractHTMLTemplate2 extends AbstractTemplate {
	public final static String hashTag="#";

	public AbstractHTMLTemplate2() {
		super();
	}
	protected void writeHtml(String str) throws IOException {
		EscapeString.escapeHtml(out, str);
	}
	protected List<EClassifier> getAllSuperClasses(EClassifier cla)
	{
		List<EClassifier> ret=new ArrayList<EClassifier>();
		if(cla instanceof EClass)
		{
			EClass c=(EClass) cla;
			recurse(ret, c);
		}else
		{
			ret.add(cla);
		}
		return ret;
	}

	private void recurse(List<EClassifier> ret, EClass c) {
		if(!ret.contains(c))
		{
			ret.add(c);
			List<EClass> sts=c.getESuperTypes();
			for(EClass st: sts)
			{
				recurse(ret, st);
			}
		}
	}
	protected String handleMissing(String documentation) {
		return documentation==null?"TODO missing documentation":documentation;
	}
}
