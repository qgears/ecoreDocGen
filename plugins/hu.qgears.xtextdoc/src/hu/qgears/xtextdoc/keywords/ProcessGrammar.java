package hu.qgears.xtextdoc.keywords;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.EModelElement;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.AbstractRule;
import org.eclipse.xtext.Action;
import org.eclipse.xtext.Assignment;
import org.eclipse.xtext.CrossReference;
import org.eclipse.xtext.EnumLiteralDeclaration;
import org.eclipse.xtext.EnumRule;
import org.eclipse.xtext.Group;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.ParserRule;
import org.eclipse.xtext.RuleCall;

import hu.qgears.commons.MultiMapHashImpl;
import hu.qgears.commons.MultiMapHashToHashSetImpl;
import hu.qgears.commons.UtilComma;
import hu.qgears.commons.UtilFile;
import hu.qgears.documentation.DocumentationFieldUtils;
import hu.qgears.xtextdoc.examples.ERefType;
import hu.qgears.xtextdoc.examples.ExampleContent;
import hu.qgears.xtextdoc.generator.NameComparator;
import hu.qgears.xtextdoc.util.AbstractHTMLTemplate2;
import hu.qgears.xtextdoc.util.KeywordUsecase;
import hu.qgears.xtextdoc.util.MultiKey;
import hu.qgears.xtextdoc.util.UtilIterable;

public class ProcessGrammar extends AbstractHTMLTemplate2{
	private MultiMapHashToHashSetImpl<EEnum, KeywordUsecase> enumUsecases=new MultiMapHashToHashSetImpl<>();
	private List<String> keys;
	private MultiMapHashToHashSetImpl<String, MultiKey> keywords;
	/**
	 * Map the rule to the root rule that calls it directly or indirectly.
	 */
	private MultiMapHashToHashSetImpl<ParserRule, ParserRule> ruleToRoot=new MultiMapHashToHashSetImpl<>();

	KeywordsHtml host;
	
	
	public ProcessGrammar(KeywordsHtml host) {
		super();
		this.host = host;
	}
	public void process(Resource r) throws Exception {
		Set<String> types=new HashSet<>();
		keywords=new MultiMapHashToHashSetImpl<>();
		TreeIterator<EObject> iter=r.getAllContents();
		boolean first=true;
		while(iter.hasNext())
		{
			EObject o=iter.next();
			String t=""+o.getClass();
			if(types.add(t))
			{
				// System.out.println(t);
			}
			if(o instanceof Keyword)
			{
				Keyword k=(Keyword) o;
				if(!isSkipped(k.getValue()))
				{
					MultiKey allKeys=findWidestMultikey(k);
					keywords.putSingle(allKeys.getValue(), allKeys);
				}
			}
			if(o instanceof ParserRule)
			{
				ParserRule p=(ParserRule) o;
				if(first)
				{
					for(EObject c:new UtilIterable(p))
					{
						if(c instanceof RuleCall)
						{
							RuleCall rc=(RuleCall) c;
							if(rc.getRule() instanceof ParserRule)
							{
								ParserRule second=(ParserRule) rc.getRule();
								for(EObject c2: new UtilIterable(second))
								{
									if(c2 instanceof RuleCall)
									{
										RuleCall rc2=(RuleCall) c2;
										if(rc2.getRule() instanceof ParserRule)
										{
											processRootRule((ParserRule)rc2.getRule());
										}
									}
								}
							}
						}
					}
				}
				first=false;
//				if(host.rootParserRuleNames.contains(p.getName()))
//				{
//					for(EObject c:new UtilIterable(p))
//					{
//						if(c instanceof RuleCall)
//						{
//							RuleCall rc=(RuleCall) c;
//							if(rc.getRule() instanceof ParserRule)
//							{
//								// System.out.println("REF: "+rc.getRule().getName());
//								processRootRule((ParserRule)rc.getRule());
//							}
//						}
//					}
//				}
				findFirstKeyword(p);
			}
		}
		keys=new ArrayList<>(keywords.keySet());
		Collections.sort(keys);
		String s=generate();
		String name=r.getURI().toString();
		host.a.output.mkdirs();
		UtilFile.saveAsFile(new File(host.a.output, name.substring(name.lastIndexOf("/")+1)+".html"), s);
	}
	private void processRootRule(ParserRule root) {
		HashSet<ParserRule> processed=new HashSet<>();
		ruleToRoot.putSingle(root, root);
		processRootRule(root, root, processed);
	}
	private void processRootRule(ParserRule root, ParserRule rule, HashSet<ParserRule> processed) {
		for(EObject o:  new UtilIterable(rule))
		{
			if(o instanceof RuleCall)
			{
				RuleCall call=(RuleCall) o;
				AbstractRule tg=call.getRule();
				if(tg instanceof ParserRule)
				{
					ParserRule ptg=(ParserRule) tg;
					if(processed.add(ptg))
					{
						ruleToRoot.putSingle(ptg, root);
						processRootRule(root, ptg, processed);
					}
				}
				if(tg instanceof EnumRule)
				{
					EnumRule er=(EnumRule) tg;
					System.out.println("Enum rule reference: "+er.getName());
					EClassifier cla=er.getType().getClassifier();
					enumUsecases.putSingle((EEnum)cla, new KeywordUsecase(root, rule));
				}
			}
//			if(o instanceof Assignment)
//			{
//				Assignment a=(Assignment) o;
//				
//				System.out.println("Enum rule reference: "+a.getTerminal());
//				if(a.getTerminal() instanceof EnumRule)
//				{
//				}
//			}
		}
	}
	

	private void findFirstKeyword(ParserRule p) {
		System.out.print(""+p.getType().getClassifier().getName()+": ");
		for(EObject c:new UtilIterable(p))
		{
			if(c instanceof Keyword)
			{
				System.out.println(((Keyword) c).getValue());
				return;
			}
		}
		System.out.println("ERROR - no keyword");
		return;
	}
	private MultiKey findWidestMultikey(Keyword kv) {
		if(kv.eContainer() instanceof Group)
		{
			Group g=(Group)kv.eContainer();
			int first=g.getElements().indexOf(kv);
			int last=g.getElements().indexOf(kv);
			while(first>0 && g.getElements().get(first-1) instanceof Keyword && !isSkipped(((Keyword)g.getElements().get(first-1)).getValue()))
			{
				first--;
			}
			while(last<g.getElements().size()-1 && g.getElements().get(last+1) instanceof Keyword&& !isSkipped(((Keyword)g.getElements().get(last+1)).getValue()))
			{
				last++;
			}
			return new MultiKey(g.getElements().subList(first, last+1));
		}
		else
		{
			return new MultiKey(kv);
		}
	}
	List<ParserRule> getRootRules(ParserRule rule)
	{
		List<ParserRule> ret=new ArrayList<>(ruleToRoot.get(rule));
		Collections.sort(ret, new NameComparator());
		return ret;
	}
	private void documentKey(String key, HashSet<MultiKey> list) throws IOException {
		if("previewLanguage".equals(key))
		{
			System.out.println("ALMA");
		}
		rtout.write("<h2> Keyword <a href=\"");
		rtcout.write(hashTag);
		writeHtml(key);
		rtout.write("\" id=\"");
		writeHtml(key);
		rtout.write("\">");
		writeHtml(key);
		rtout.write("</a></h2>\n");
		MultiMapHashImpl<ParserRule, MultiKey> rulesWhereExits=new MultiMapHashImpl<>();
		for(MultiKey kv: list)
		{
			EObject container=kv.getElement().eContainer();
			if(container instanceof EnumLiteralDeclaration)
			{
				EnumLiteralDeclaration enumliteraldeclaration=(EnumLiteralDeclaration) container;
				EEnumLiteral literal=enumliteraldeclaration.getEnumLiteral();
				rtout.write("Enum literal: ");
				writeHtml(getDocumentation(literal));
				rtout.write("<br/>\n");
				EEnum en=literal.getEEnum();
				rtout.write("In enumeration: ");
				writeHtml(en.getName());
				rtout.write(": ");
				writeHtml(getDocumentation(en));
				rtout.write("<br/>\n");
				HashSet<KeywordUsecase> a=enumUsecases.get(en);
				rtout.write("Used in types: \n");
				for(KeywordUsecase p: a)
				{
					rtout.write("<em>");
					writeHtml(p.root.getName());
					rtout.write("</em>:<em>");
					writeHtml(p.localRule.getName());
					rtout.write("</em>, \n");
				}
				for(ExampleContent s: host.examples.getExamples(key, ERefType.setEnumeration, en.getName()))
				{
					rtout.write("<h3>Example</h3>\n<pre>\n");
					writeHtml(s.content);
					rtout.write("\n</pre>\n");
				}
			}
			ParserRule root=findRoot(kv.getElement());
			if(root!=null)
			{
				rulesWhereExits.putSingle(root, kv);
			}else
			{
				// System.out.println("ERR- root is null!"+kv);
			}
		}
		Map<FeatureAssignment, FeatureAssignment> assignments=new HashMap<FeatureAssignment, FeatureAssignment>();
		for(ParserRule localRoot: rulesWhereExits.keySet())
		{
			if(localRoot.getType().getClassifier() instanceof EClass)
			{
				EClass cla=(EClass)localRoot.getType().getClassifier();
//				if(cla.isAbstract())
				{
					for(MultiKey kv: rulesWhereExits.get(localRoot))
					{
//						rtcout.write(separator.getSeparator());
						System.out.print("'"+key+"': ");
						Assignment a=getAssignmentOfKeyword(kv);
						FeatureAssignment assignment=null;
						if(a!=null)
						{
							if(a!=null)
							{
								AbstractElement value=a.getTerminal();
								// System.out.print("assign: "+ a.getFeature()+" "+a.getOperator());
								EStructuralFeature feat=findFeatureByName(localRoot.getType().getClassifier(), a.getFeature());
								if(value instanceof RuleCall)
								{
									RuleCall rc=(RuleCall) value;
									assignment=new FeatureAssignmentRule(feat, rc.getRule());
								} else if (value instanceof CrossReference)
								{
									CrossReference cr=(CrossReference) value;
									assignment=new FeatureAssignmentCrossReference(feat, cr.getType().getClassifier());
								}
								else
								{
									if(feat!=null)
									{
										assignment=new FeatureAssignmentBoolean(feat);
									}
								}
							}else
							{
								System.out.print("Container: "+ kv.getElement().eContainer().getClass());
							}
						}
						if(assignment!=null)
						{
							if(assignments.containsKey(assignment))
							{
								assignment=assignments.get(assignment);
							}else
							{
								assignments.put(assignment, assignment);
							}
						}
						EClass c=findActualClass(kv.getElement());
						EClassifier type;
						if(c!=null)
						{
							System.out.print("\t"+c.getName()+": ");
							type=c;
						}else
						{
							System.out.print("\t"+localRoot.getType().getClassifier().getName()+": ");
							type=localRoot.getType().getClassifier();
						}
						if(kv.isFirstKeyword(localRoot))
						{
							if(assignment!=null)
							{
								assignment.createsType.add(type);
							}else
							{
								rtout.write("<p>Creates: <em>");
								writeHtml(type.getName());
								rtout.write("</em>: ");
								writeHtml(getDocumentation(type));
								rtout.write("</p>\n");
								for(ExampleContent s: host.examples.getExamples(key, ERefType.creator, type.getName()))
								{
									rtout.write("<h3>Example</h3>\n<pre>\n");
									writeHtml(s.content);
									rtout.write("\n</pre>\n");
								}
							}
						}
						else
						{
							if(assignment!=null)
							{
								assignment.usedOnTypes.add(type);
							}else
							{
								rtout.write("Used on type: ");
								writeHtml(type.getName());
								rtout.write(". Documentation: ");
								writeHtml(getDocumentation(type));
								rtout.write("<br/>\n");
								for(ExampleContent s: host.examples.getExamples(key, null, type.getName()))
								{
									rtout.write("<h3>Example</h3>\n<pre>\n");
									writeHtml(s.content);
									rtout.write("\n</pre>\n");
								}
							}
						}
						for(ParserRule p: ruleToRoot.get(localRoot))
						{
							System.out.print(p.getName()+", ");
						}
						System.out.println();
					}
				}
			}
		}
		UtilComma separator=new UtilComma("<hr/>");
		rtcout.write(separator.getSeparator());
		for(FeatureAssignment fas: assignments.keySet())
		{
			rtcout.write(separator.getSeparator());
			rtout.write("\n");
			for(EClassifier t: fas.createsType)
			{
				rtout.write("<p>Creates: <em>");
				writeHtml(t.getName());
				rtout.write("</em>: ");
				writeHtml(getDocumentation(t));
				rtout.write("</p>\n");
			}
			if(fas instanceof FeatureAssignmentRule)
			{
				rtout.write("<p>Metamodel feature: <em>");
				rtcout.write(fas.feat.getName());
				rtout.write("</em>: ");
				writeHtml(getDocumentation(fas.feat));
				rtout.write("</p>\n<p>Applyable on metamodel type: <em>");
				writeHtml(fas.hostType.getName());
				rtout.write("</em> ");
				writeHtml(getDocumentation(fas.hostType));
				rtout.write("</p>\n");
				FeatureAssignmentRule fasr=(FeatureAssignmentRule) fas;
				// <p>Rule: #Hfasr.rule.toString()#</p>
				if(!fasr.rule.getType().getClassifier().getName().equals("EString") &&
					!fasr.rule.getType().getClassifier().getName().equals("EInt"))
				{
					rtout.write("<p>Metamodel type to create: <em>");
					writeHtml(fasr.rule.getType().getClassifier().getName());
					rtout.write("</em>: ");
					writeHtml(getDocumentation(fasr.rule.getType().getClassifier()));
					rtout.write("</p>\n");
				}
				System.out.print("creates: "+fasr.rule.getType().getClassifier().getName());
				for(ExampleContent s: host.examples.getExamples(key, ERefType.creator, fas.hostType.getName()))
				{
					rtout.write("<h3>Example</h3>\n<pre>\n");
					writeHtml(s.content);
					rtout.write("\n</pre>\n");
				}
			} else if (fas instanceof FeatureAssignmentCrossReference)
			{
				rtout.write("<p>Metamodel feature: <em>");
				rtcout.write(fas.feat.getName());
				rtout.write("</em>: ");
				writeHtml(getDocumentation(fas.feat));
				rtout.write("</p>\n<p>Applyable on metamodel type: <em>");
				writeHtml(fas.hostType.getName());
				rtout.write("</em> ");
				writeHtml(getDocumentation(fas.hostType));
				rtout.write("</p>\n");
				FeatureAssignmentCrossReference fascr=(FeatureAssignmentCrossReference) fas;
				rtout.write("<p>Reference to: <em>");
				writeHtml(fascr.classifier.getName());
				rtout.write("</em>: ");
				writeHtml(getDocumentation(fascr.classifier));
				rtout.write(" </p>\n");
				System.out.print("creates: "+fascr.classifier.getName());
				for(ExampleContent s: host.examples.getExamples(key, ERefType.reference, fas.hostType.getName()))
				{
					rtout.write("<h3>Example</h3>\n<pre>\n");
					writeHtml(s.content);
					rtout.write("\n</pre>\n");
				}
			}else if (fas instanceof FeatureAssignmentBoolean)
			{
				rtout.write("<p>Sets feature to true: <em>");
				rtcout.write(fas.feat.getName());
				rtout.write("</em>: ");
				writeHtml(getDocumentation(fas.feat));
				rtout.write("</p>\n<p>Applyable on metamodel type: <em>");
				writeHtml(fas.hostType.getName());
				rtout.write("</em> ");
				writeHtml(getDocumentation(fas.hostType));
				rtout.write("</p>\n");
				@SuppressWarnings("unused")
				FeatureAssignmentBoolean fasb=(FeatureAssignmentBoolean) fas;
				for(ExampleContent s: host.examples.getExamples(key, ERefType.setTrue, fas.hostType.getName()))
				{
					rtout.write("<h3>Example</h3>\n<pre>\n");
					writeHtml(s.content);
					rtout.write("\n</pre>\n");
				}
			}else
			{
				throw new RuntimeException("Not implemented type");
			}
			rtout.write("<p>\n");
			for(EClassifier c: fas.usedOnTypes)
			{
				if(c!=fas.hostType)
				{
					rtout.write("<em>");
					writeHtml(c.getName());
					rtout.write("</em> \n");
				}
			}
			rtout.write("</p>\n");
		}
	}
	/**
	 * Find the ParserRule which contains this keyword.
	 * @param kv
	 * @return
	 */
	private ParserRule findRoot(AbstractElement kv) {
		EObject p=kv;
		while(p!=null)
		{
			if(p instanceof ParserRule)
			{
				return (ParserRule) p;
			}
			p=p.eContainer();
		}
		return null;
	}
	private boolean isSkipped(String key) {
		return host.skippedKeys.contains(key);
	}
	@Override
	protected void doGenerate() throws Exception {
		rtout.write("<html>\n");
		for(String key: keys)
		{
			if(!isSkipped(key))
			{
				documentKey(key, keywords.get(key));
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
	private Assignment getAssignmentOfKeyword(MultiKey kv) {
		if(kv.getElement().eContainer() instanceof Assignment)
		{
			Assignment a=(Assignment)kv.getElement().eContainer();
			return a;
		}else
		{
			Assignment a=getNextAssignment(kv);
			if(a!=null)
			{
				return a;
			}
		}
		return null;
	}
	private EStructuralFeature findFeatureByName(EClassifier classifier, String feature) {
		if(classifier instanceof EClass)
		{
			EClass c=(EClass) classifier;
			for(EAttribute a: c.getEAllAttributes())
			{
				if(feature.equals(a.getName()))
				{
					return a;
				}
			}
			for(EReference r: c.getEAllReferences())
			{
				if(feature.equals(r.getName()))
				{
					return r;
				}
			}
		}
		return null;
	}
	private Assignment getNextAssignment(MultiKey keyword) {
		if(keyword.getElement().eContainer() instanceof Group)
		{
			Group g=(Group) keyword.getElement().eContainer();
			int index=g.getElements().indexOf(keyword.getElement())+keyword.getElementsLength();
			for(int i=index;i<g.getElements().size();++i)
			{
				AbstractElement ae=g.getElements().get(i);
				if(ae instanceof Keyword)
				{
					// Only the closes assignment is returned!
					return null;
				}
				if(ae instanceof Assignment)
				{
					return (Assignment) ae;
				}else
				{
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
