package hu.bme.mit.documentation.generator.ecore;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

public class BackRef {
	private List<EClass> knownSubtypes = new ArrayList<EClass>();
	private List<EReference> usedByReferences = new ArrayList<EReference>();

	public List<EClass> getKnownSubtypes() {
		return knownSubtypes;
	}

	public List<EReference> getUsedByReferences() {
		return usedByReferences;
	}

}