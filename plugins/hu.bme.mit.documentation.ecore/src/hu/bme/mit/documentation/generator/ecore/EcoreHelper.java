package hu.bme.mit.documentation.generator.ecore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class EcoreHelper {

	
	
	public static BackRef getBackReferences (EClass clz){
		BackRef br = new BackRef(); 
		List<EClass> subTypes = br.getKnownSubtypes();
		Notifier topResource = getTopResource(clz);
		if (topResource != null){
			TreeIterator<Object> content = EcoreUtil.getAllContents(Collections.singleton(topResource));
			while (content.hasNext()){
				Object o = content.next();
				if (o instanceof EClass){
					EClass eClass = (EClass) o;
					if (!subTypes.contains(eClass)) {
						if (eClass.getEAllSuperTypes().contains(clz)) {
							subTypes.add(eClass);
						}
					}
					for ( EReference r : eClass.getEReferences()) {
						if (clz.equals(r.getEType()) || clz.getEAllSuperTypes().contains(r.getEType())) {
							br.getUsedByReferences().add(r);
						}
					}
				}
			}
		}
		return br;
	}

	private static Notifier getTopResource(EClass clz) {
		Notifier topResource = null;
		if (clz.getEPackage() != null) {
			topResource = clz.getEPackage();
			if (clz.eResource() != null){
				topResource = clz.eResource();
				if (clz.eResource().getResourceSet() != null) {
					topResource = clz.eResource().getResourceSet();
				}
			}
		}
		return topResource;
	}
	
}
