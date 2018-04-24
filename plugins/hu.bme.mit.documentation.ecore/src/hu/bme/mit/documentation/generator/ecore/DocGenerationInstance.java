package hu.bme.mit.documentation.generator.ecore;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;

/**
 * Class that represents the process of a single documentation generation.
 * 
 * @author adam
 *
 */
public class DocGenerationInstance{
	
	private boolean genHeader = true;
	
	public void doGenerateAllSubpackages(IDocGenerator docGen, StringBuilder sb, List<EPackage> pckg, ArrayList<String> filter) {
		for (EPackage p : pckg) {
			generateAll(docGen, sb, p, filter);
		}
		docGen.generateTail();
	}
	private void generateAll(IDocGenerator docGen, StringBuilder sb, EPackage pckg, ArrayList<String> filter){
		if(!pckg.getEClassifiers().isEmpty()){
			if (filter == null || !filter.contains(pckg.getNsURI())) {
				docGen.documentEPackage(sb, pckg, filter,genHeader);
				//the first, non-empty package is found, no need to generate headers for the others
				genHeader = false;
			}
		}
		for (EPackage subpck : pckg.getESubpackages()) {
			generateAll(docGen, sb, subpck, filter);
		}
	}
	
}