package hu.qgears.xtextdoc.keywords;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xcore.XcorePackage;
import org.eclipse.emf.ecore.xcore.XcoreStandaloneSetup;
import org.eclipse.emf.mwe.utils.StandaloneSetup;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.XtextPackage;
import org.eclipse.xtext.XtextStandaloneSetup;
import org.eclipse.xtext.resource.XtextResourceSet;

import com.google.inject.Injector;

import hu.qgears.xtextdoc.examples.ExamplesParser;

public class KeywordsHtml {
	public class Args {
		public File[] xcore;
		public File[] xtext;
		public ExamplesParser.Args examples=new ExamplesParser.Args();
		public File output;
		public String[] skippedKeys=new String[]
				{
					"{", "}",
					"[", "\\", "]", "^", "_",
					"0", "9,", "\n", " ", "/*", "*/", ",", "$", "=", "(", ")", ".","\"","`","'",
					"*", "+", "-", "/", "//", "9", "A"
				};
		
		public Args() {}

		@Override
		public String toString() {
			return "Args [xcore=" + Arrays.toString(xcore) + ", xtext=" + Arrays.toString(xtext) + ", examples="
					+ examples + ", output=" + output + "]";
		}
		
	}
	private Args createArgs() {
		Args a=new Args();
		File sourceFolder=new File (""); //FIXME parameter
		a.xcore=new File[]{
				new File(sourceFolder, ""),
				new File(sourceFolder, ""),}; //FIXME parameter
		a.xtext=new File[]{
				new File(sourceFolder, ""),
				new File(sourceFolder, ""), //FIXME parameter
		};
		a.output=new File("/tmp/doc");
		a.examples.examplesFolders=new File[]{
				new File(sourceFolder, "") //FIXME parameter
		};
		a.examples.exampleExtensions=new String[]{""}; //FIXME parameter
		return a;
	}
	protected Set<String> skippedKeys=new HashSet<>();

	public static void main(String[] args) throws Exception {
		KeywordsHtml keywordsHtml = new KeywordsHtml();
		Args a = keywordsHtml.createArgs();
		keywordsHtml.run(a);
	}
	
	protected ExamplesParser examples;
	
	public KeywordsHtml() {
	}
	protected Args a;
	
	public void run(Args a) throws Exception {
		this.a = a;
		System.out.println("Args: " + a);
		
		for(String s: a.skippedKeys)
		{
			this.skippedKeys.add(s);
		}
		examples=new ExamplesParser(a.examples);
		examples.run();
		StandaloneSetup standaloneSetup = new StandaloneSetup();
		standaloneSetup.addRegisterGeneratedEPackage(XtextPackage.class.getName());
		standaloneSetup.addRegisterGeneratedEPackage(XcorePackage.class.getName());

		XcoreStandaloneSetup xcoreSupport=new XcoreStandaloneSetup();
		XtextStandaloneSetup xtextSupport=new XtextStandaloneSetup();
		xcoreSupport.createInjectorAndDoEMFRegistration();
		Injector injector = xtextSupport.createInjectorAndDoEMFRegistration();
		XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);
		for(File f: a.xcore)
		{
			Resource r = resourceSet.createResource(URI.createFileURI(f.getCanonicalPath()));
			r.load(resourceSet.getLoadOptions());
		}
		for(File f: a.xtext)
		{
			Resource r = resourceSet.createResource(URI.createFileURI(f.getCanonicalPath()));
			r.load(resourceSet.getLoadOptions());
		}
		EcoreUtil.resolveAll(resourceSet);
		for(Resource r: resourceSet.getResources())
		{
			if(r.getURI().toString().endsWith(".xtext"))
			{
				new ProcessGrammar(this).process(r);
			}
		}
	}
	List<EObject> unfold(ParserRule root)
	{
		List<EObject> ret=new ArrayList<EObject>();
		TreeIterator<EObject> iter=root.eAllContents();
		while(iter.hasNext())
		{
			EObject o= iter.next();
			ret.add(o);
		}
		return ret;
	}
}
