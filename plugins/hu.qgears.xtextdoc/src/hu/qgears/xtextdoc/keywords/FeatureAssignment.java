package hu.qgears.xtextdoc.keywords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

public abstract class FeatureAssignment {
	public EClass hostType;
	public EStructuralFeature feat;
	public List<EClassifier> usedOnTypes=new ArrayList<>();
	public Set<EClassifier> createsType=new HashSet<>();
	public FeatureAssignment(EStructuralFeature feat) {
		hostType=(EClass)feat.eContainer();
		this.feat=feat;
		System.out.println("Feature assignment host: "+feat.getName()+" "+hostType);
	}
	public boolean semiEquals(FeatureAssignment obj) {
		return hostType==obj.hostType && feat==obj.feat;
	}
}
