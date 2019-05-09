package hu.qgears.xtextdoc.keywords;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.EnumRule;
import org.eclipse.xtext.Grammar;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import hu.bme.mit.documentation.generator.ecore.EPackageDocGenHtml;
import hu.qgears.commons.MultiMap;
import hu.qgears.commons.MultiMapHashToHashSetImpl;
import hu.qgears.commons.UtilComma;
import hu.qgears.commons.UtilFile;
import hu.qgears.documentation.DocumentationFieldUtils;
import hu.qgears.xtextdoc.examples.ERefType;
import hu.qgears.xtextdoc.examples.ExampleContent;
import hu.qgears.xtextdoc.util.AbstractHTMLTemplate2;
import hu.qgears.xtextdoc.util.KeyInfo;
import hu.qgears.xtextdoc.util.KeyInfoEnum;
import hu.qgears.xtextdoc.util.KeyInfoFeatureSet;
import hu.qgears.xtextdoc.util.KeyInfoNopKeyword;
import hu.qgears.xtextdoc.util.KeyInfoSubkeyword;
import hu.qgears.xtextdoc.util.KeyInfoTerminalRule;
import hu.qgears.xtextdoc.util.KeyInfoTypeCreation;
import hu.qgears.xtextdoc.util.KeyInfoUnknown;
import hu.qgears.xtextdoc.util.MultiKey;
import hu.qgears.xtextdoc.util.UtilIterable;

public class ProcessGrammar extends AbstractHTMLTemplate2{
	
	private static final Logger LOG = Logger.getLogger(ProcessGrammar.class);
	
	private MultiMap<String, MultiKey> keywords;
	private MultiMap<AbstractRule,Assignment> callHierarchy;
	private class AutoNumbering {
		
		private int h1;
		private int h2;
		private int h3;
		
		public String nextLevel1() {
			h2 = 0;
			h3 = 0;
			h1++;
			return String.format("%d", h1);
		}
		public String nextLevel2() {
			h3 = 0;
			if (h2 > 0) {
				rtout.write("<hr/>\n");
			}
			h2++;
			return  String.format("%d.%d", h1,h2);
		}
		public String nextLevel3() {
			h3++;
			return  String.format("%d.%d.%d", h1,h2,h3);
		}
	}
	
	private AutoNumbering autoNumbering = new AutoNumbering();
	
	private Comparator<ENamedElement> byNameEcore = new Comparator<ENamedElement>() {
		@Override
		public int compare(ENamedElement c1, ENamedElement c2) {
			return c1.getName().compareTo(c2.getName());
		}
	};
	private KeywordsHtml host;
	
	public ProcessGrammar(KeywordsHtml host) {
		super();
		this.host = host;
	}
	public void process(Resource r) throws Exception {
		if (LOG.isInfoEnabled()){
			LOG.info("Processing "+r.getURI());
		}
		keywords=new MultiMapHashToHashSetImpl<>();
		TreeIterator<EObject> iter=r.getAllContents();
		boolean first=true;
		while(iter.hasNext())
		{
			EObject o=iter.next();
			if(o instanceof Grammar)
			{
				if(first)
				{
					initCallHierarchy((Grammar) o);
					first = false;
				}
			}
			if (o instanceof AbstractElement) {
				
				AbstractElement abstractElement = (AbstractElement) o;
				Keyword keyword = getKeyWord(abstractElement);
				if (keyword != null) {
					String k = keyword.getValue();
					if (! isSkipped(k)) {
						MultiKey mk = new MultiKey(keyword);
						//collect single keywords first
						keywords.putSingle(k, mk);
						MultiKey checkmulti = findWidestMultikey(abstractElement);
						if (!checkmulti.equals(mk)) {
							//the single keyword is part of wider keyword
							mk.setKeyInfo(new KeyInfoSubkeyword(checkmulti));
							discoverKey(checkmulti);
						} else {
							discoverKey(mk);
						}
					}
				}
			}
		}
		
		String s=generate();
		String name=r.getURI().toString();
		host.a.output.mkdirs();
		UtilFile.saveAsFile(new File(host.a.output, name.substring(name.lastIndexOf("/")+1)+".html"), s);
	}
	
	/**
	 * 
	 * Discovers the category of the given keyword, and assigns a {@link KeyInfo} to it.
	 * 
	 * @param multik
	 */
	private void discoverKey(MultiKey multik) {
		Keyword k = multik.getFirstKey();
		
		EObject container=k.eContainer();
		AbstractRule directParent = findRoot(k);
		if(container instanceof EnumLiteralDeclaration)
		{
			multik.setKeyInfo(new KeyInfoEnum((EnumRule)directParent, (EnumLiteralDeclaration)container));
			//enum literal
		} else if (directParent instanceof ParserRule) {
			
			ParserRule parserRule = (ParserRule) directParent;
			if (multik.isFirstKeyword(parserRule)) {
				//type creation rule
				multik.setKeyInfo(new KeyInfoTypeCreation(parserRule,callHierarchy.get(parserRule)));
			} else {
				//assignment rule
				Assignment fa = getAssignmentOfKeyword(parserRule, multik);
				if (fa != null) {
					multik.setKeyInfo(new KeyInfoFeatureSet(parserRule,fa));
				} else {
					multik.setKeyInfo(new KeyInfoNopKeyword(parserRule));
				}
			}
		} else if (directParent instanceof TerminalRule){
			multik.setKeyInfo(new KeyInfoTerminalRule());
		} else {
			multik.setKeyInfo(new KeyInfoUnknown());
			LOG.warn("Uncategorizied keyword for keyword: "+k);
		}
	}

	private Keyword findFirstKeyword(ParserRule p) {
		//System.out.print(""+p.getType().getClassifier().getName()+": ");
		for(EObject c:new UtilIterable(p))
		{
			if(c instanceof Keyword)
			{
				//System.out.println(((Keyword) c).getValue());
				return (Keyword) c;
			}
		}
		//System.out.println(p.getType().getClassifier().getName()+": ERROR - no keyword");
		return null;
	}
	private MultiKey findWidestMultikey(AbstractElement toSearch) {
		MultiKey mk;
		Keyword main = getKeyWord(toSearch);
		if(toSearch.eContainer() instanceof Group)
		{
			List<Keyword> allKeys = new LinkedList<>();
			allKeys.add(main);
			Group g=(Group)toSearch.eContainer();
			int first=g.getElements().indexOf(toSearch);
			int last=g.getElements().indexOf(toSearch);
			
			while(first>0) {
				Keyword  e = getKeyWord( g.getElements().get(first-1));
				if (e != null && !isSkipped(e.getValue())){
					allKeys.add(0,e);
					first--;
				} else {
					break;
				}
			}
			while(last<g.getElements().size()-1 ) {
				Keyword  e = getKeyWord( g.getElements().get(last+1));
				if (e != null && !isSkipped(e.getValue())){
					allKeys.add(e);
					last++;
				} else {
					break;
				}
			}
			mk = new MultiKey(allKeys);
		}
		else
		{
			mk = new MultiKey(main);
		}
		if (keywords.containsKey(mk.getValue())) {
			for (MultiKey emk : keywords.get(mk.getValue())) {
				if (emk.equals(mk)) {
					return emk;
				}
			}
		}
		keywords.putSingle(mk.getValue(), mk);
		return mk;
	}
	
	private Keyword getKeyWord(AbstractElement e) {
		if (e instanceof Keyword) {
			return (Keyword) e;
		} else if (e instanceof Assignment) {
			Assignment assignment = (Assignment) e;
			if ("?=".equals(assignment.getOperator()) && assignment.getTerminal() instanceof Keyword) {
				return (Keyword) assignment.getTerminal();
			}
		}
		return null;
	}
	
	private void documentKey(String key) throws IOException {
		
		List<MultiKey> occurrences = new ArrayList<>(keywords.get(key));
		
		Collections.sort(occurrences, new Comparator<MultiKey>() {
			@Override
			public int compare(MultiKey o1, MultiKey o2) {
				ICompositeNode n1 = NodeModelUtils.findActualNodeFor(o1.getFirstKey());
				ICompositeNode n2 = NodeModelUtils.findActualNodeFor(o2.getFirstKey());
				
				return Integer.compare(n1 == null ? -1 : n1.getOffset(),n2 == null ? -1 : n2.getOffset());
			}
		});
		
		if (hasDocumentableKeyInfo(occurrences)) {
			newKey(key);
			List<String> parOfKeywords = new ArrayList<>();
			List<KeyInfoFeatureSet> featureSets = new ArrayList<>();
			List<KeyInfoTypeCreation> typeCreations = new ArrayList<>();
			for(MultiKey kv: occurrences)
			{
				KeyInfo kinfo = kv.getKeyInfo();
				if (kinfo instanceof KeyInfoSubkeyword) {
					KeyInfoSubkeyword keyInfoSubkeyword = (KeyInfoSubkeyword) kinfo;
					String sk = keyInfoSubkeyword.getMultikey().getValue();
					if (!parOfKeywords.contains(sk)) {
						parOfKeywords.add(sk);
					}
				} else if (kinfo instanceof KeyInfoEnum){
					KeyInfoEnum keyInfoEnum = (KeyInfoEnum) kinfo;
					documentEnumLiteral(kv, keyInfoEnum);
				} else if (kinfo instanceof KeyInfoFeatureSet){
					KeyInfoFeatureSet keyInfoFeatureSet = (KeyInfoFeatureSet) kinfo;
					featureSets.add(keyInfoFeatureSet);
//					documentFeatureAssignment(kv, keyInfoFeatureSet);
//					rtcout.write("<hr/>");
				} else if (kinfo instanceof KeyInfoTypeCreation){
					KeyInfoTypeCreation keyInfoTypeCreation = (KeyInfoTypeCreation) kinfo;
					typeCreations.add(keyInfoTypeCreation);
					documentTypeCreation(kv,keyInfoTypeCreation);
				} else if (kinfo instanceof KeyInfoNopKeyword) {
					KeyInfoNopKeyword keyInfoNopKeyword = (KeyInfoNopKeyword) kinfo;
					rtout.write("<p>Used on ");
					writeEClass(keyInfoNopKeyword.getHostType());
					rtout.write(" to improve the user-readablity of the model. Does not have any direct effect on model.</p>\n");
					ParserRule pr = keyInfoNopKeyword.getParserRule();
					
					for (ExampleContent e : host.examples.getExamples(key, ERefType.reference, null)) {
						newExample();
						rtout.write("<pre>\n");
						writeHtml(e.content);
						rtout.write("\n</pre>\n");
					}
					if (pr != null) {
						Keyword k = findFirstKeyword(pr);
						if (k != null && !isSkipped(k.getValue())) {
							MultiKey mk = findWidestMultikey(k);
							rtout.write("<p>See also <a href=\"");
							rtcout.write(hashTag);
							writeHtml(mk.getValue());
							rtout.write("\" >");
							writeHtml(mk.getValue());
							rtout.write("</a></p>\n");
						}
					}
					rtcout.write("<hr/>");
					
					
				} else {
					throw new RuntimeException("Unimplemented keyinfo: "+kinfo);
				}
				
			}
			if (!featureSets.isEmpty()) {
				documentFeatureAssignments(key,featureSets);
			}
			
			if (!parOfKeywords.isEmpty()) {
				newUseCase();
				rtout.write("<p> Sub keyword of");
				Collections.sort(parOfKeywords);
				UtilComma c = new UtilComma(",");
				for(String sk : parOfKeywords) {
					rtcout.write(c.getSeparator());
					rtout.write(" <a href=\"");
					rtcout.write(hashTag);
					writeHtml(sk);
					rtout.write("\" >");
					writeHtml(sk);
					rtout.write("</a>\n");
				}
			}
			rtout.write("</p>\n");
		}
	}
	private void newKey(String key) throws IOException {
		rtout.write("<h2>");
		rtcout.write(autoNumbering.nextLevel1());
		rtout.write(" Keyword <a href=\"");
		rtcout.write(hashTag);
		writeHtml(key);
		rtout.write("\" id=\"");
		writeHtml(key);
		rtout.write("\">");
		writeHtml(key);
		rtout.write("</a></h2>\n");
	}
	private void newUseCase() {
		rtout.write("<h3>");
		rtcout.write(autoNumbering.nextLevel2());
		rtout.write(" Use case</h3>\n");
	}
	private void newExample() {
		rtout.write("<h4>");
		rtcout.write(autoNumbering.nextLevel3());
		rtout.write(" Example</h4>\n");
	}
	private boolean hasDocumentableKeyInfo(List<MultiKey> occurrences) {
		boolean ret = false;
		for(MultiKey kv: occurrences)
		{
			KeyInfo kinfo = kv.getKeyInfo();
			if (kinfo instanceof KeyInfoUnknown){
				LOG.error("Keyword belongs to unknown category : '"+kv+ "'");
			} else if (kinfo instanceof KeyInfoTerminalRule){
				if (LOG.isInfoEnabled()){
					LOG.info("Skipping terminal rule from doc: '"+kv+"'");
				}
			} else {
				ret = true;
			}
		}
		return ret;
	}
	private void documentTypeCreation(MultiKey kv, KeyInfoTypeCreation keyInfoTypeCreation) throws IOException {
		newUseCase();
		ParserRule parent = keyInfoTypeCreation.getParentRule();
		
		EClassifier type = findActualClass(kv.getFirstKey());
		if (type == null) {
			type = parent.getType().getClassifier();
		}
		rtout.write("<p>Creates: <em>");
		writeEClass(type);
		rtout.write("</em>: ");
		writeHtml(getDocumentation(type));
		rtout.write("</p>\n");
		
		List<EReference> usages = new ArrayList<>();
		for (Assignment a :  keyInfoTypeCreation.getAssignments()) {
			EStructuralFeature f = getFeatureForAssignment(a);
			if (f instanceof EReference) {
				EReference eReference = (EReference) f;
				if (!usages.contains(eReference)) {
					if (eReference.isContainment()) {
						usages.add(eReference);
					}
				}
			}
		}
		if (!usages.isEmpty()) {
			Collections.sort(usages,byNameEcore);
			UtilComma c = new UtilComma(", ");
			rtout.write("<p>Adds a new element to: \n");
			for ( EReference r : usages) {
				rtcout.write(c.getSeparator());
				writeEFeature(r);
			}
			rtout.write("</p>\n");
		}
		
		
		for(ExampleContent s: host.examples.getExamples(kv.getValue(), ERefType.creator, type.getName()))
		{
			newExample();
			rtout.write("<pre>\n");
			writeHtml(s.content);
			rtout.write("\n</pre>\n");
		}
	}
	private void documentFeatureAssignments(String key,List< KeyInfoFeatureSet> keyInfoFeatureSets) throws IOException {
		
		MultiMap<EStructuralFeature,FeatureAssignment> groupByFeature = new MultiMapHashToHashSetImpl<>();

		for (KeyInfoFeatureSet ki : keyInfoFeatureSets) {
			Assignment a = ki.getAssignment();
			ParserRule localRoot = ki.getParentRule();
			FeatureAssignment fas=null;
			EClassifier type = findActualClass(a);
			if (type == null) {
				type = localRoot.getType().getClassifier();
			}
			if(a!=null)
			{
				AbstractElement value=a.getTerminal();
				// System.out.print("assign: "+ a.getFeature()+" "+a.getOperator());
				EStructuralFeature feat=findFeatureByName(type, a.getFeature());
				if(value instanceof RuleCall)
				{
					RuleCall rc=(RuleCall) value;
					fas=new FeatureAssignmentRule(type,feat, rc.getRule());
				} else if (value instanceof CrossReference)
				{
					CrossReference cr=(CrossReference) value;
					fas=new FeatureAssignmentCrossReference(type,feat, cr.getType().getClassifier());
				}
				else
				{
					if(feat!=null)
					{
						fas=new FeatureAssignmentBoolean(type,feat);
					}
				}
			}
			if (fas != null) {
				groupByFeature.putSingle(fas.feat, fas);
			} else {
				LOG.error("No feature assignment found for keyword : "+key+" "+ki);
			}
		}
		List<EStructuralFeature> features = ordered(groupByFeature.keySet(),byNameEcore);
		
		for (EStructuralFeature f : features) {
			newUseCase();
			rtout.write("<p>Metamodel feature: <em>");
			writeEFeature(f);
			rtout.write("</em>: ");
			writeHtml(getDocumentation(f));
			
			List<EClassifier> usages = new ArrayList<>();
			for (FeatureAssignment fa : groupByFeature.get(f)) {
				usages.add(fa.hostType);
			}
			Collections.sort(usages, byNameEcore);
			
			for (EClassifier c : usages) {
				rtout.write("</p>\n<p>Applyable on metamodel type: <em>");
				writeEClass(c);
				rtout.write("</em> ");
				writeHtml(getDocumentation(c));
				rtout.write("</p>\n");
				
				for(ExampleContent s: host.examples.getExamples(key, ERefType.reference, c.getName()))
				{
					newExample();
					rtout.write("<pre>\n");
					writeHtml(s.content);
					rtout.write("\n</pre>\n");
				}
			}
		}
	}
	private void documentEnumLiteral(MultiKey kv, KeyInfoEnum keyInfoEnum) throws IOException {
		newUseCase();
		EnumLiteralDeclaration enumLiteralDeclaration = keyInfoEnum.getLiteral();
		EEnumLiteral literal=enumLiteralDeclaration.getEnumLiteral();
		rtout.write("Enum literal: ");
		writeHtml(getDocumentation(literal));
		rtout.write("<br/>\n");
		EEnum en=literal.getEEnum();
		rtout.write("In enumeration: ");
		writeEClass(en);
		rtout.write(": ");
		writeHtml(getDocumentation(en));
		rtout.write("<br/>\n");
		
		AbstractRule rule = findRoot(enumLiteralDeclaration);
		List<EStructuralFeature> usedFromKeys = new ArrayList<>();
		
		for (Assignment a : callHierarchy.get(rule)) {
			EStructuralFeature f = getFeatureForAssignment(a);
			if (f != null && !usedFromKeys.contains(f)) {
				usedFromKeys.add(f);
			}
		}
		if (!usedFromKeys.isEmpty()) {
			rtout.write("<p>\n");
			Collections.sort(usedFromKeys,byNameEcore);
			rtout.write("Might set metamodel attributes: \n");
			UtilComma c = new UtilComma(" ,");
			for(EStructuralFeature f: usedFromKeys)
			{
				rtcout.write(c.getSeparator());
				writeEFeature(f);
				rtout.write("\n");
			}
			rtout.write("</p>\n");
		}
		for(ExampleContent s: host.examples.getExamples(kv.getValue(), ERefType.setEnumeration, en.getName()))
		{
			newExample();
			rtout.write("<pre>\n");
			writeHtml(s.content);
			rtout.write("\n</pre>\n");
		}
	}
		
	private <T> List<T> ordered(Collection<T> keySet,Comparator<? super T> c) {
		List<T> l = new ArrayList<>(keySet);
		Collections.sort(l,c);
		return l;
	}
	
	private void writeEFeature(EStructuralFeature feat) throws IOException {
		if (host.getMetamodelDoc() == null){
			writeHtml(feat.getName());
		} else {
			EClassifier c = feat.getEContainingClass();
			String link = host.getMetamodelDoc()+"#"+ EPackageDocGenHtml.escapeLabel(c.getEPackage().getNsPrefix()+c.getName())+"."+feat.getName();
			rtout.write("<a href=\"");
			rtcout.write(link);
			rtout.write("\">");
			writeHtml(c.getName() + "::" +feat.getName());
			rtout.write("</a>");
		}
	}
	
	private void writeEClass(EClassifier hostType) throws IOException {
		if (host.getMetamodelDoc() == null){
			writeHtml(hostType.getName());
		} else {
			String link = host.getMetamodelDoc()+"#"+ EPackageDocGenHtml.escapeLabel(hostType.getEPackage().getNsPrefix()+hostType.getName());
			rtout.write("<a href=\"");
			rtcout.write(link);
			rtout.write("\">");
			writeHtml(hostType.getName());
			rtout.write("</a>");
		}
	}
	/**
	 * Find the ParserRule which contains this keyword.
	 * @param kv
	 * @return
	 */
	private AbstractRule findRoot(EObject kv) {
		EObject p=kv;
		while(p!=null)
		{
			if(p instanceof AbstractRule)
			{
				AbstractRule directRoot = (AbstractRule) p;
			
				return directRoot;
			}
			p=p.eContainer();
		}
		return null;
	}
	private boolean isSkipped(String key) {
//		return false;
		return host.skippedKeys.contains(key);
	}
	@Override
	protected void doGenerate() throws Exception {
		rtout.write("<html>\n");
		ArrayList<String> keys = new ArrayList<>(keywords.keySet());
		Collections.sort(keys);
		for(String key: keys)
		{
			if(!isSkipped(key))
			{
				documentKey(key);
			}
		}
		rtout.write("</html>\n");
	}
	private String getDocumentation(EModelElement o)
	{
		String doc=DocumentationFieldUtils.getAnnotation(o, "documentation");
		if(doc==null)
		{
			doc="";
		}
		return doc;
	}
	private Assignment getAssignmentOfKeyword(ParserRule localRoot,MultiKey kv) {
		for (Keyword k : kv.getKeys()) {
			if(k.eContainer() instanceof Assignment)
			{
				//simple assignments
				Assignment a=(Assignment)k.eContainer();
				return a;
			} 
		}
		{
			if (kv.getFirstKey().eContainer() instanceof Alternatives){

				Alternatives alternatives = (Alternatives) kv.getFirstKey().eContainer();
				//handle cases such as feature=('cica'|'kutya') 
				if (alternatives.eContainer() instanceof Assignment) {
					return (Assignment) alternatives.eContainer();
				}
				//handle cases such as  (value?=('true') | 'false')
				for (AbstractElement alt : alternatives.getElements()){
					if (alt instanceof Assignment){
						return (Assignment) alt;
					}
				}
				//
				//handle cases such as ('hello' | 'world') feature=....
				//(key word aliases)
				return getNextAssignment(alternatives,1);
			}
			//default behavior : find cases such as 'keyword' feature=...
			Assignment a=getNextAssignment(kv.getFirstKey(),kv.getElementsLength());
			if(a!=null)
			{
				return a;
			}
		}
		
		return null;
	}
	
	private void initCallHierarchy(Grammar grammar) {
		callHierarchy = new MultiMapHashToHashSetImpl<>();
		for (AbstractRule r : grammar.getRules()){
			TreeIterator<EObject> ti = r.eAllContents();
			while (ti.hasNext()){
				EObject c = ti.next();
				if (c instanceof Assignment) {
					Assignment assignment = (Assignment) c;
//					if (isContainment(assignment)){
						if (assignment.getTerminal() instanceof RuleCall){
							RuleCall ruleCall = (RuleCall) assignment.getTerminal();
							callHierarchy.putSingle(ruleCall.getRule(),assignment);
							addAlternatives(assignment, ruleCall);
						}
//					}
				}
			}
		}
	}
	private void addAlternatives(Assignment assignment, RuleCall ruleCall) {
		if (ruleCall.getRule().getAlternatives() instanceof Alternatives){
			Alternatives alternatives = (Alternatives) ruleCall.getRule().getAlternatives();
			for (AbstractElement e : alternatives.getElements()){
				if (e instanceof RuleCall){
					RuleCall subRuleCall = (RuleCall) e;
					callHierarchy.putSingle(subRuleCall.getRule(),assignment);
					addAlternatives(assignment, subRuleCall);
				}
			}
		}
	}

	private EStructuralFeature getFeatureForAssignment(Assignment c) {
		AbstractRule r = findRoot(c);
		EStructuralFeature f = findFeatureByName(r.getType().getClassifier(), c.getFeature());
		return f;
	}
	
	private EStructuralFeature findFeatureByName(EClassifier classifier, String feature) {
		if(classifier instanceof EClass)
		{
			EClass c=(EClass) classifier;
			for(EStructuralFeature a: c.getEAllStructuralFeatures())
			{
				if(feature.equals(a.getName()))
				{
					return a;
				}
			}
		}
		return null;
	}
	private Assignment getNextAssignment(AbstractElement element, int keywordCount) {
		if(element.eContainer() instanceof Group)
		{
			Group g=(Group) element.eContainer();
			int index=g.getElements().indexOf(element)+keywordCount;
			for(int i=index;i<g.getElements().size();++i)
			{
				AbstractElement ae=g.getElements().get(i);
				if(ae instanceof Keyword)
				{
					// Only the closes assignment is returned!
					//but we accept skipped keywords such as '{' and '['. Continue search in this case
					if (!isSkipped(((Keyword) ae).getValue())){
						return null;
					}
				}
				if(ae instanceof Assignment)
				{
					return (Assignment) ae;
				}else if (ae instanceof Alternatives)
				{
					for (AbstractElement alt : ((Alternatives) ae).getElements()){
						if (alt instanceof Assignment){
							return (Assignment) alt;
						}
					}
					// Only the closes assignment is returned!
				}
			}
		}
		return null;
	}
	/**
	 * Find the class that is really instantiated with this keyword
	 * @param keyword
	 * @return non null is a class other than the class of the rule itself is found.
	 */
	private EClass findActualClass(AbstractElement keyword) {
		if(keyword.eContainer() instanceof Group)
		{
			Group g=(Group) keyword.eContainer();
			for(AbstractElement ar: g.getElements())
			{
				if(ar instanceof Action)
				{
					Action a=(Action) ar;
					EClassifier ret=a.getType().getClassifier();
					if(ret instanceof EClass)
					{
						return (EClass)ret;
					}
				}
			}
		}
		return null;
	}

}
