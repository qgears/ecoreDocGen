package hu.qgears.xtextdoc.keywords;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EStructuralFeature;

public abstract class FeatureAssignment {
	public EClassifier hostType;
	public EStructuralFeature feat;
	public List<EClassifier> usedOnTypes=new ArrayList<>();
	public Set<EClassifier> createsType=new HashSet<>();
	public FeatureAssignment(EClassifier hosttype,EStructuralFeature feat) {
		this.hostType=hosttype;
		this.feat=feat;
		
	}
	public FeatureAssignment(EStructuralFeature feat) {
		this (feat.getEContainingClass(),feat);
		//System.out.println("Feature assignment host: "+feat.getName()+" "+hostType);
	}
	public boolean semiEquals(FeatureAssignment obj) {
		return hostType==obj.hostType && feat==obj.feat;
	}
}
