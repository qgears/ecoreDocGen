package hu.bme.mit.documentation.generator.ecore;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PropertyResourceBundle;

import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

/**
 * Utility class for generating documentation.
 * 
 * @author adam
 *
 */
public class UtilDocGenerator {

	/**
	 * Generate documentation for the specified EMF packages to the
	 * given output file, using the given filter file, and {@link IDocGenerator}
	 * 
	 * 
	 * @param rootPackage
	 * @param outputFile
	 * @param filterFile
	 * @param docGen
	 * @see #getRootEPackages(ResourceSet)
	 */
	public static void generateDocForEPackage(List<EPackage> rootPackage, File outputFile, File filterFile,
			IDocGenerator docGen) {

		StringBuilder sb = new StringBuilder();

		InputStream fis = null;
		FileOutputStream fos = null;
		try {
			ArrayList<String> filter = new ArrayList<String>();
			if (filterFile != null && filterFile.exists()) {
				// Reading the configuration file
				PropertyResourceBundle bundle = null;
				fis = new FileInputStream(filterFile);
				bundle = new PropertyResourceBundle(fis);

				// Additional filters
				String[] filterArray = bundle.getString("filters").split("\\|");
				for (int i = 0; i < filterArray.length; i++) {
					String filterEntry = filterArray[i];
					if (filterEntry != null && !filterEntry.isEmpty()) {
						filter.add(filterEntry);
					}
				}
			}
			filter.add("http://www.eclipse.org/emf/2002/Ecore");
			new DocGenerationInstance().doGenerateAllSubpackages(docGen, sb, rootPackage, filter);
			if (!outputFile.getParentFile().exists()){
				if (!outputFile.getParentFile().mkdirs()){
					throw new IOException("Cannot create folder "+outputFile.getParent());
				}
			}
			
			fos = new FileOutputStream(outputFile, false);
			fos.write(sb.toString().getBytes());
		} catch (IOException e) {
			Logger.getLogger(UtilDocGenerator.class).error("Exception occurred when generating ecore doc", e);
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				Logger.getLogger(UtilDocGenerator.class).error("Exception occurred when closing streams", e);
			}
		}
	}

	/**
	 * Finds all {@link EPackage} instance that is a direct child content of one
	 * of the Resources within specified ResourceSet.
	 * <p>
	 * The packages are returned ordered by {@link EPackage#getNsURI()}
	 * 
	 * @param set
	 * @return
	 */
	public static List<EPackage> getRootEPackages(ResourceSet set) {
		List<EPackage> pcks = new ArrayList<EPackage>();
		for (Resource r : new ArrayList<Resource>(set.getResources())) {
			for (EObject pCand : r.getContents()) {
				if (pCand instanceof EPackage) {
					pcks.add((EPackage) pCand);
				}
			}
		}
		Collections.sort(pcks, new Comparator<EPackage>() {
			@Override
			public int compare(EPackage o1, EPackage o2) {
				String s1 = "" + o1.getNsURI();
				String s2 = "" + o2.getNsURI();
				return s1.compareTo(s2);
			}
		});
		return pcks;
	}

	/**
	 * Collects root {@link EPackage}s in specified Resourceset (see
	 * {@link #getRootEPackages(ResourceSet)}) and calls documentation
	 * generator.
	 * 
	 * @param set
	 * @param output
	 * @param filter
	 * @param docGen
	 */
	public static void generateDocForResourceSet(ResourceSet set, File output, File filter, IDocGenerator docGen) {
		List<EPackage> pcks = getRootEPackages(set);
		generateDocForEPackage(pcks, output, filter, docGen);
	}

	/**
	 * Creates an empty ResourceSet, that can be used to load ecore and xcore
	 * models. The URI converter of the resource set is initialized to properly
	 * resolve references to types defined in Ecore.genmodel (e.f EStrint, EInt
	 * ...).
	 * 
	 * @return
	 */
	public static ResourceSet newResourceSet() {
		EcorePackage.eINSTANCE.getClass();
        ResourceSet set = new ResourceSetImpl();
        set.getURIConverter().getURIMap().putAll(
        EcorePlugin.computePlatformURIMap(false));
        return set;
	}
}
