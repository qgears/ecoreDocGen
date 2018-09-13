package hu.qgears.xtextdoc.editor;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.documentation.IEObjectDocumentationProvider;

import hu.qgears.xtextdoc.util.UtilDoc;

/**
 * This class is for adding own documentations to mouse hover and content assist.
 * 
 * Bind with [LanguageName]UiModule.bindIEObjectDocumentationProvider().
 * 
 * @author glaseradam
 *
 */
public class MyEObjectDocumentationProvider implements IEObjectDocumentationProvider {

	@Override
	public String getDocumentation(EObject o) {
		if (o instanceof EObjectWrapper) {
			EObjectWrapper eObjectWrapper = (EObjectWrapper) o;
			EObject instance=eObjectWrapper.getObj();
			String instancedoc= UtilDoc.getLastComment(instance, true);
			EClass eClass = eObjectWrapper.geteClass();
			EStructuralFeature eFeature = eObjectWrapper.geteFeature();
			EClass geteClassFeatureType = eObjectWrapper.geteClassFeatureType();
			StringBuilder sb = new StringBuilder();
			if(instancedoc!=null)
			{
				sb.append(instancedoc);
				sb.append("<hr/>");
			}
			UtilDoc.getEMFDocumentation(sb, eClass, eFeature, geteClassFeatureType);
			return sb.toString();
			
		} 
		StringBuilder sb = new StringBuilder();
		EClass eClass = o.eClass();
		sb.append(UtilDoc.getLastComment(o, true));
		UtilDoc.getEMFDocumentation(sb, eClass, null, null);
		return sb.toString();
	}

}
