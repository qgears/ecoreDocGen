package hu.qgears.xtextdoc.keywords;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

public class FeatureAssignmentCrossReference extends FeatureAssignment
{
	public EClassifier classifier;

	public FeatureAssignmentCrossReference(EStructuralFeature feat, EClassifier classifier) {
		super(feat);
		this.classifier = classifier;
	}
	@Override
	public int hashCode() {
		return feat.hashCode()^hostType.hashCode()^classifier.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FeatureAssignmentCrossReference)
		{
			FeatureAssignmentCrossReference other=(FeatureAssignmentCrossReference) obj;
			return other.classifier==classifier && super.semiEquals(other);
		}
		return super.equals(obj);
	}
}
