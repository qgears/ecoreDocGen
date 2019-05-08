package hu.qgears.xtextdoc.keywords;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

/**
 * 
 * boolFiled?='boolkeyword'
 */
public class FeatureAssignmentBoolean extends FeatureAssignment
{
	public FeatureAssignmentBoolean(EClassifier hostType, EStructuralFeature feat) {
		super(hostType, feat);
	}
	public FeatureAssignmentBoolean(EStructuralFeature feat) {
		super(feat);
	}
	@Override
	public int hashCode() {
		return feat.hashCode()^hostType.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FeatureAssignmentBoolean)
		{
			FeatureAssignmentBoolean other=(FeatureAssignmentBoolean) obj;
			return super.semiEquals(other);
		}
		return super.equals(obj);
	}
}
