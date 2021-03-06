package hu.qgears.xtextdoc.keywords;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.AbstractRule;

/**
 *
 *
 * containmentRef=ContentType
 * containmentRef+=ContentType*
 *
 */
public class FeatureAssignmentRule extends FeatureAssignment
{
	public AbstractRule rule;

	public FeatureAssignmentRule(EClassifier hostType, EStructuralFeature feat, AbstractRule rule) {
		super(hostType, feat);
		this.rule = rule;
	}
	public FeatureAssignmentRule(EStructuralFeature feat, AbstractRule rule) {
		super(feat);
		this.rule = rule;
	}
	
	@Override
	public int hashCode() {
		return feat.hashCode()^hostType.hashCode()^rule.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FeatureAssignmentRule)
		{
			FeatureAssignmentRule other=(FeatureAssignmentRule) obj;
			return other.rule==rule && super.semiEquals(other);
		}
		return super.equals(obj);
	}

}
