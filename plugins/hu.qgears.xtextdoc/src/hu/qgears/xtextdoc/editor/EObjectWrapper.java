package hu.qgears.xtextdoc.editor;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EObjectImpl;

/**
 * Wrapper class used in hover/content assist documentations. 
 * Holds more information about the object: the container's class type, the feature, the feature's class type.
 * Null value means the information has no usage.
 * 
 * @author glaseradam
 *
 */
public class EObjectWrapper extends EObjectImpl {
	
	private EClass eClass;
	private EStructuralFeature eFeature;
	private EClass eClassFeatureType;
	private EObject obj;
	
	public EObjectWrapper(EObject obj, EClass eClass, EStructuralFeature eFeature, EClass eClassFeatureType) {
		this.obj=obj;
		this.eClass = eClass;
		this.eFeature = eFeature;
		this.eClassFeatureType = eClassFeatureType;
	}
	
	public EStructuralFeature geteFeature() {
		return eFeature;
	}
	
	public EClass geteClass() {
		return eClass;
	}
	
	public EClass geteClassFeatureType() {
		return eClassFeatureType;
	}
	public EObject getObj() {
		return obj;
	}
}
