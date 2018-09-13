package hu.qgears.xtextdoc.util;

import java.util.Iterator;

import org.eclipse.emf.ecore.EObject;

public class UtilIterable implements Iterable<EObject>{
	EObject root;
	
	
	public UtilIterable(EObject root) {
		super();
		this.root=root;
	}


	@Override
	public Iterator<EObject> iterator() {
		return root.eAllContents();
	}

}
