package hu.bme.mit.documentation.ecore.ui.handlers;

import hu.bme.mit.documentation.generator.ecore.EPackageDocGenEclipseHelp;
import hu.bme.mit.documentation.generator.ecore.IDocGenerator;

/**
 * 
 * Generates HTML documentation from the supplied .ecore file. 
 * 
 * @author adam
 *
 */
public class GenerateEcoreDocEclipseHelp extends AbstractGenerateEcoreDoc {

	@Override
	protected IDocGenerator getCodeGenerator() {
		return new EPackageDocGenEclipseHelp();
	}

    @Override
    protected String getFileExtension() {
        return "eclipsehelp";
    }

}
