package hu.bme.mit.documentation.generator.ecore;

import java.io.File;
import java.util.List;

import org.eclipse.emf.ecore.EPackage;

/**
 * Interface for documentation generators
 * 
 * @author Adam Horvath
 *
 */
public interface IDocGenerator {
	public static enum OutputType {
		/** The output is be generated as a single file. */
		SINGLE_FILE,
		/** The output is be generated as a directory structure. */
		DIRECTORY
	}
	/**
	 * Generate all documentation of the supplied {@link EPackage} to the
	 * supplied {@link StringBuilder}. Hyperlinks will not be generated for
	 * elements, that are in one of the packages in the nameRefFilter list. Only
	 * generate a header comment for the item if genHeader is <code>true</code>
	 * 
	 * @param sb
	 * @param pckg
	 * @param nameRefFilter
	 * @param genHeader
	 */
	void documentEPackage(final StringBuilder sb, final EPackage pckg, final List<String> nameRefFilter, final boolean genHeader, String tocFolder);
	
	void documentEPackage(final StringBuilder sb, final EPackage pckg, final List<String> nameRefFilter, final boolean genHeader);

	/**
	 * Generate tail of the document.
	 */
	void generateTail();
	
	/**
	 * Sets the output file or directory.
	 * @param outputFile the output file or directory
	 */
	void setOutputFile(final File outputFile);
	
	/**
	 * 
	 * @return the type of output this generator produces
	 */
	OutputType getOutputType();
}
