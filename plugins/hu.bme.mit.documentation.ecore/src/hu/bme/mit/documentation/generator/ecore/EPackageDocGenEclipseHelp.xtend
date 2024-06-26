/* Copyright (c) 2010-2012, Abel Hegedus, Istvan Rath and Daniel Varro
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Abel Hegedus - initial structure
 *   Adam Horvath - HTML specifics
 *
 *******************************************************************************/
 package hu.bme.mit.documentation.generator.ecore

import com.google.common.collect.Lists
import hu.qgears.documentation.DocumentationFieldUtils
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Reader
import java.io.StringReader
import java.util.ArrayList
import java.util.List
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.eclipse.emf.ecore.EAttribute
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EClassifier
import org.eclipse.emf.ecore.EDataType
import org.eclipse.emf.ecore.EEnum
import org.eclipse.emf.ecore.EModelElement
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.emf.ecore.EOperation
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.EStructuralFeature
import org.eclipse.emf.ecore.ETypedElement
import org.tautua.markdownpapers.ast.Document
import org.tautua.markdownpapers.parser.Parser
import org.w3c.dom.Element
import javax.xml.transform.OutputKeys

/**
 * 
 * @author chreex
 */
class EPackageDocGenEclipseHelp implements IDocGenerator{
	
	
	
	private static final boolean fqnPackageNames = Boolean.getBoolean("ecoredoc.fqn.packagenames")
	private static final String tocRootName = System.getProperty("ecoredoc.eclipsehelp.tocname","Metamodel documentation")
	
	private static final val NODE_ID_TOC = "toc";
	private static final val NODE_ID_TOPIC = "topic";
	
	private static final val ATTR_ID_HREF = "href";
	private static final val ATTR_ID_LABEL = "label";
	
	private val documentBuilderFactory = DocumentBuilderFactory.newInstance
	private val tocBuilder = documentBuilderFactory.newDocumentBuilder;
	private val toc = tocBuilder.newDocument
	private val tocRoot = toc.createElement(NODE_ID_TOC)
		
	private EPackage pckg
    private StringBuilder builder
    private List<String> filter
	private File outputDir
	private File outputFile
	
	new() {
		tocRoot.setAttribute("xmlns:htm", "http://www.w3.org/1999/xhtml");
		tocRoot.setAttribute("label",tocRootName);
		toc.appendChild(tocRoot);
	}
    
    /**
     * Sets the output <em>directory</em>. Output files will be generated into
     * this directory. This method must be called prior calling
     * {@link documentEPackage} method.
     */
	override setOutputFile(File outputFile) {
		this.outputDir = outputFile;
	}
    
    def private getFileNameForPackage(EPackage pckg) {
    	if (!fqnPackageNames && pckg.nsPrefix !== null && !pckg.nsPrefix.isEmpty){
    		return pckg.nsPrefix + ".html"
    	} else {
	    	return ePackageFqName(pckg) + ".html"
    	}
    }
    
    /**
     * Generates package documentation into a separate HTML file.
     * @param sb ignored
     * @param pckg the package, of which a HTML file will be generated
     * @param nameRefFilter TODO
     * @param genHeader ignored 
     */
    override documentEPackage(StringBuilder sb, EPackage pckg, 
    	List<String> nameRefFilter, boolean genHeader){
        this.builder = new StringBuilder();
        this.pckg = pckg
        this.filter = Lists::newArrayList(nameRefFilter)
        this.outputFile = new File(outputDir, getFileNameForPackage(pckg));
        
        val String genDirectory = "eclipsehelp-gen/"
        
        val packageTopic = pckg.addToTOC(genDirectory)
        pckg.EClassifiers.sortBy[name].filter(EClass).forEach[ cls | {
        	var subTopic = toc.createElement(NODE_ID_TOPIC)
			    subTopic.setAttribute(ATTR_ID_LABEL, cls.name)
			    subTopic.setAttribute(ATTR_ID_HREF, genDirectory + getFileNameForPackage(cls.EPackage)
			    	+ "#" + escapeLabel(cls.EPackage.nsPrefix + "." + cls.name))
			    packageTopic.appendChild(subTopic)
        }]
        
		if (!(outputDir.exists && outputDir.isDirectory) && !outputDir.mkdirs) {
			throw new RuntimeException("Could not create output directory: "
				+ outputDir.absolutePath);
		}
		
		'''
		<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
		<html>
		<head>
			<title>«ePackageFqName(pckg)»</title>
			<link rel="stylesheet" type="text/css" href="../style.css" />
			</head>
		<body>
		'''.appendToBuilder			
        
        pckg.documentEPackageHeader.appendToBuilder
                		
        pckg.EClassifiers.sortBy[name].forEach[ classifier |
        	
			if(classifier instanceof EDataType){
        		if(classifier instanceof EEnum){
        			val eenum = classifier as EEnum
        			eenum.documentEEnumHeader.appendToBuilder
        		}
        	}
        	else if(classifier instanceof EClass){
        		val cls = classifier as EClass
        		        			        		
    			val allSuperClasses =  cls.EAllSuperTypes //new ArrayList;
    			//getAllSuperClassesRecursively(cls, allSuperClasses);
    			// getEAllSuperTypes?

        		cls.documentEClassHeader
        		
        		if (!cls.ESuperTypes.empty){
    				'''<h6>Supertypes</h6>'''.appendToBuilder    	
        			allSuperClasses.sortBy[name].forEach[
						val superCls = it as EClass
	    				'''<span>'''.appendToBuilder
        				'''<a href="«linkToType(superCls)»">«superCls.name»</a> | '''.appendToBuilder
        				'''</span>'''.appendToBuilder    	
					]
        		}
        		
        		val br = cls.backref
        		if (!br.knownSubtypes.empty) {
        			'''<h6>Known subtypes</h6>'''.appendToBuilder
        			br.knownSubtypes.sortBy[name].forEach[
        				'''<span>'''.appendToBuilder
        				'''<a href="«linkToType(it)»">«it.name»</a> | '''.appendToBuilder    	
        				'''</span>'''.appendToBuilder
        			]
        		}
        		if (!br.usedByReferences.empty) {
        			'''
        				<h6>Used by</h6>
        				«FOR useRef: br.usedByReferences.sortBy[EContainingClass.name + name]»
        					<span>
        					<a href="«getFileNameForPackage(useRef.EContainingClass.EPackage)»#«
        					»«escapeLabel(useRef.EContainingClass.EPackage.nsPrefix+useRef.EContainingClass.name)+"."+useRef.name»">«
        					»«useRef.EContainingClass.name +"::"+  useRef.name»</a> |
        					</span>
        				«ENDFOR»
        			'''.appendToBuilder
        			
        			
        			/*'''<h6>Used by</h6>'''.appendToBuilder
        			br.usedByReferences.sortBy[EContainingClass.name + name].forEach[
        				'''<span>'''.appendToBuilder
        				'''<a href="«getFileNameForPackage(it.EContainingClass.EPackage)»#«escapeLabel(it.EContainingClass.EPackage.nsPrefix+it.EContainingClass.name)+"."+it.name»">«it.EContainingClass.name +"::"+  it.name»</a> | '''.appendToBuilder    	
        				'''</span>'''.appendToBuilder
        			]*/
        		}
        		
        		'''
    				«FOR superCls : allSuperClasses.sortBy[name]»
	    				«val id = escapeLabel(cls.EPackage.nsPrefix+"."+cls.name) + "."  + escapeLabel(superCls.EPackage.nsPrefix+"."+superCls.name)»
	    				
	    				«IF !superCls.EAttributes.empty
	    					|| !superCls.EReferences.empty
							|| !superCls.EOperations.empty» 
							«superCls.documentInheritedRefs(id)»
						«ENDIF»
    				«ENDFOR»
    				«cls.documentEClass("" + escapeLabel(cls.EPackage.nsPrefix+"."+cls.name), false)»
        		'''.appendToBuilder
        		
        		/*
				'''<table>'''.appendToBuilder
    			allSuperClasses.sortBy[name].forEach[
					val superCls = it as EClass
    				val id = escapeLabel(cls.EPackage.nsPrefix+"."+cls.name) + "."  + escapeLabel(superCls.EPackage.nsPrefix+"."+superCls.name);
    				
    				if (!superCls.EAttributes.empty
    					|| !superCls.EReferences.empty
						|| !superCls.EOperations.empty
    				) {
	    				superCls.documentInheritedRefs(id).appendToBuilder   		
    				}
    				
				]        		
				cls.documentEClass("" + escapeLabel(cls.EPackage.nsPrefix+"."+cls.name), false)
				'''</table>'''.appendToBuilder
				*/
        	} 
        	
        ]
        
        generatePackageDocTail();
        
        val pkgDocWriter = new BufferedWriter(new FileWriter(outputFile));
        pkgDocWriter.append(builder);
        pkgDocWriter.close
    }
	
	def Element addToTOC(EPackage ePackage, String tocFolder)
	{
		val packageTopic = toc.createElement(NODE_ID_TOPIC)	
		packageTopic.setAttribute(ATTR_ID_HREF, tocFolder + getFileNameForPackage(pckg));
		packageTopic.setAttribute(ATTR_ID_LABEL, ePackageFqName(pckg));
		tocRoot.appendChild(packageTopic);
		return packageTopic
	}
	
	def private documentEClass(EClass cls, String id, boolean isSuperClass) {
		'''
			«IF !cls.EAttributes.empty»
				<table>
					<tr>
						<th colspan="3"><div class="tableHeader">Attributes
						«IF isSuperClass »
							inherited from <a href="«linkToType(cls)»">«cls.name»</a>    	
						«ENDIF»
						</div></th>
					</tr>
					<tr>
						<th><div class="columnHeader">Name</div></th>
						<th><div class="columnHeader">Properties</div></th>
						<th><div class="columnHeader">Documentation</div></th>
					</tr>
					
					«FOR attrib: cls.EAttributes.sortBy[name]»
						<tr>
							«documentEAttributeHeader(attrib, id)»
							</td>
							<td>
								«findGenModelDocumentation(attrib, attrib.derived)»
							</td>
						</tr>
					«ENDFOR»
				</table>
			«ENDIF»
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".attr","")»
			
			«IF !cls.EReferences.empty»
				<table>
					<tr>
						<th colspan="3"><div class="tableHeader">References
						«IF isSuperClass»
							inherited from <a href="«getFileNameForPackage(cls.EPackage)»#«escapeLabel(cls.EPackage.nsPrefix+"."+cls.name)»">«cls.name»</a>
						«ENDIF»
						</div></th>
					</tr>
					<tr>
						<th><div class="columnHeader">Name</div></th>
						<th><div class="columnHeader">Properties</div></th>
						<th><div class="columnHeader">Documentation</div></th>
					</tr>
					
					«FOR ref: cls.EReferences.sortBy[name]»
						<tr>
							«documentEReferenceHeader(ref, id)»
							</td>
							<td>
								«findGenModelDocumentation(ref, ref.derived)»
							</td>
						</tr>
					«ENDFOR»
				</table>
				«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".ref","")»
			«ENDIF»
			
			«IF !cls.EOperations.empty»
				<table>
					<tr>
						<th colspan="3"><div class="tableHeader">Operations
						«IF isSuperClass»
							inherited from <a href="«linkToType(cls)»">«cls.name»</a>    	
						«ENDIF»
						</div></th>
					</tr>

					<tr>
						<th><div class="columnHeader">Name</div></th>
						<th><div class="columnHeader">Properties</div></th>
						<th><div class="columnHeader">Documentation</div></th>
					</tr>
					
					«FOR op: cls.EOperations.sortBy[name]»
						<tr>
							«documentEOperationHeader(op, id)»
							</td>
							<td>
								«findGenModelDocumentation(op, false)»
							</td>
						</tr>
					«ENDFOR»
				</table>
			«ENDIF»
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".op","")»
		'''
	}
	
	
	
	def linkToType(EClassifier cls) {
		'''«getFileNameForPackage(cls.EPackage)»#«escapeLabel(cls.EPackage.nsPrefix+"."+cls.name)»'''
	}
	
	def private documentInheritedRefs(EClass cls, String id) {

		'''
		«IF !cls.EAttributes.empty»
			<table>
				<tr>
					<th colspan="3"><div class="tableHeader">Attributes inherited from 
					<a href="«linkToType(cls)»">«cls.name»</a>:
					</div>
					</th>
				</tr>
				«FOR attrib: cls.EAttributes.sortBy[name]»
					<tr>
						«attrib.documentEAttributeHeader(id)»
						</td>				
						<td>
							«findGenModelDocumentation(attrib,attrib.derived)»
						</td>
					</tr>
				«ENDFOR»
			</table>
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".attr","")»
		«ENDIF»
		
		«IF !cls.EReferences.empty»
			<table>
				<tr>
					<th colspan="3"><div class="tableHeader">References
					inherited from <a href="«linkToType(cls)»">«cls.name»</a>    	
					</div></th>
				</tr>
				<tr>
					<th><div class="columnHeader">Name</div></th>
					<th><div class="columnHeader">Properties</div></th>
					<th><div class="columnHeader">Documentation</div></th>
				</tr>
				«FOR ref : cls.EReferences.sortBy[name] »
					<tr>
						«ref.documentEReferenceHeader(id)»
						</td>
						<td>
							«findGenModelDocumentation(ref,ref.derived)»
						</td>
					</tr>
				«ENDFOR»
			</table>
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".ref","")»
		«ENDIF»
		
		«IF !cls.EOperations.empty»
			<table>
				<tr>
					<th colspan="3"><div class="tableHeader">Operations
					inherited from <a href="«linkToType(cls)»">«cls.name»</a>    	
					</div></th>
				</tr>
				<tr>
					<th><div class="columnHeader">Name</div></th>
					<th><div class="columnHeader">Properties</div></th>
					<th><div class="columnHeader">Documentation</div></th>
				</tr>
				«FOR operation : cls.EOperations.sortBy[name]»
					<tr>
						«documentEOperationHeader(operation, id)»
						</td>
						<td>
							«findGenModelDocumentation(operation, false)»
						</td>
					</tr>
				«ENDFOR»
				«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".op","")»
			</table>
		«ENDIF»
		'''
	}
	
	/* 
	 * def private void getAllSuperClassesRecursively(EClass cls, List<EClass> list) {
		for (EClass superCls : cls.ESuperTypes) {
			if (!list.contains(superCls)) {
				list.add(superCls);
			}
			getAllSuperClassesRecursively(superCls, list);
		}
	}*/
    
    def private appendToBuilder(CharSequence s){
    	builder.append(s)
    }
    
    def private documentEPackageHeader(EPackage pckg) {
		val packageName = ePackageFqName(pckg)
		val title = "The <span class=\"packageName\">" + packageName + "</span> package"
		
    	'''
			«documentHeader("h1", title, packageName, pckg.nsPrefix, pckg)»
			<div class="">EPackage properties:</div>
			«documentProperty("Namespace Prefix", '''«escapeText(pckg.nsPrefix)»''')»
			
			«documentProperty("Namespace URI", '''«pckg.nsURI»''')»
			
        '''
    }

    def private String ePackageFqName(EPackage pckg)
	{
		var current = pckg;
		var List<String> list = new ArrayList;
		var ret = new StringBuilder;
		while(current!=null){
			list.add(0,current.name);
			current = current.eContainer as EPackage;
		}
		var i = 0;
		val len = list.size;
		for(String pElement:list){
			ret.append(pElement);
			if(i < len - 1 ){
				ret.append(".");
			}
			i = i + 1;
		}
		ret.toString();
	}    
    def private documentEClassifierHeader(EClassifier cls)
    '''
    «documentHeader("h2", cls.name, cls.name, cls.EPackage.nsPrefix+"."+cls.name, cls)»
    '''
    
    def private documentEDataTypeHeader(EDataType dt)
    '''
    «dt.documentEClassifierHeader»
    '''
    
    def private documentEEnumHeader(EEnum eenum)
    '''
	«eenum.documentEDataTypeHeader»
	<table>
		<tr>
			<th colspan="3"><div class="tableHeader">Literals</div></th>
		</tr>
		<tr>
			<th><div class="columnHeader">Name</div></th>
			<th><div class="columnHeader">Value</div></th>
			<th><div class="columnHeader">Documentation</div></th>
		</tr>
		«FOR literal : eenum.ELiterals»
			<tr>
				<td>
					<span class="teletype">«escapeText(literal.literal)»</span>
				</td>
				<td>
					«literal.value»
				</td>
				<td>
					«literal.findGenModelDocumentation(false)»
				</td>	
			</tr>
	    «ENDFOR»
	</table>
	«anchorDef(eenum.EPackage.nsPrefix+"."+eenum.name+".lit","")»
    '''
    
    def private documentEClassHeader(EClass cls){
	    '''«cls.documentEClassifierHeader»'''.appendToBuilder
	    var hasPropList = false;
	    	    
	    if(cls.isInterface()){
	      '''<div class="eclassProps">EClass properties:<div class="eclassPropList">
	      	<span class="label">Interface</span>'''.appendToBuilder
	      	hasPropList=true;
	    }
		if(cls.isAbstract()){
			if(cls.isInterface()){
		      ''', '''.appendToBuilder 
			}else{
				'''<div class="eclassProps">EClass properties:<div class="eclassPropList">'''.appendToBuilder
				hasPropList=true;
			}
			'''<span class="label">Abstract</span>'''.appendToBuilder			
		}

		if(hasPropList){
			'''</div></div>'''.appendToBuilder
		}
    }
    
    def private documentENamedElement(ENamedElement elem, String parentId, String color)
    '''
    <div id="«parentId+"."+elem.name»" class="teletype">«IF color != null»<div style="color:«color»">«ENDIF»<a href="#«parentId+"."+elem.name»" >«escapeText(elem.name)»</a>«IF color != null»</div>«ENDIF»</div>
    '''
    
    def private documentETypedElement(ETypedElement elem, String parentId, String color)
    '''
		<td>
			«elem.documentENamedElement(parentId, color)»
		</td>
		<td>
			«documentProperty("T", elem.preparePossibleReference)»
			<div class="label">Cardinality: [«elem.lowerBound»..«IF elem.upperBound == -1»*«ELSE»«elem.upperBound»«ENDIF»]</div>
			«IF !elem.ordered»
				<div class="label">Unordered</div>
			«ENDIF»
			«IF !elem.unique»
				<div class="label">Not unique</div>
			«ENDIF»
		
    '''
    
    
    def private preparePossibleReference(ETypedElement elem){
    	if(elem.EGenericType != null){
	    	elem.EGenericType.EClassifier.preparePossibleReference
    	} else {
    		'''<div class="alert">MISSING TYPE elem!</div>'''
    	}
    }
    
    def private preparePossibleReference(EClassifier cls){
    	if(cls==null)
    	{
    		return '''<div class="alert">MISSING TYPE cls!</div>'''
    	}
    	val typePckg = cls.EPackage
    	val typeName = cls.name
    	if(typePckg != null && filter.findFirst[typePckg.nsURI.contains(it)] == null){
    		'''<a href="«linkToType(cls)»">«typeName»</a>'''
    	} else {
    		'''«typeName»'''
    	}
    }
    
    def private documentEStructuralFeatureHeader(EStructuralFeature feat, String parentId)
    '''
	    «IF feat.derived»
		    «feat.documentETypedElement(parentId, "blue")»
		     <div class="label">Derived</div>
	    «ELSE»
	    	«feat.documentETypedElement(parentId, null)»
	    «ENDIF»
	    «IF !feat.changeable»
	    <div class="label">Non-changeable</div>
	    «ENDIF»
	    «IF feat.volatile»
	    <div class="label">Volatile</div>
	    «ENDIF»
	    «IF feat.transient»
	    <div class="label">Transient</div>
	    «ENDIF»
	    «IF feat.unsettable»
	    <div class="label">Unsettable</div>
	    «ENDIF»
	    «IF feat.defaultValueLiteral != null»
	    «documentProperty("Default", escapeText(feat.defaultValueLiteral))»
	    «ENDIF»
	    «IF feat.derived»
	    <div class="label">Derived</div>
	    «ENDIF»
    '''
    
    def private documentEAttributeHeader(EAttribute attr, String parentId)
    '''
	    «attr.documentEStructuralFeatureHeader(parentId)»
	    «IF attr.ID»
	    <div class="label">Identifier</div>
	    «ENDIF»
    '''
    
    def private documentEReferenceHeader(EReference ref, String parentId)
    '''
	    «ref.documentEStructuralFeatureHeader(parentId)»
	    «IF ref.containment»
	    <div class="label">Containment</div>
	    «ENDIF»
	    «IF ref.container»
	    <div class="label">Container</div>
	    «ENDIF»
	    «IF ref.EOpposite != null»
	    «documentProperty("Op", ref.EOpposite.name)»
	    «ENDIF»
    '''
    //ref.EOpposite.EContainingClass.preparePossibleReference+".\\allowbreak "+
    
    def private documentEOperationHeader(EOperation op, String parentId)
    '''
	    «op.documentETypedElement(parentId, null)»
	    «IF op.EType != null»
	    <div class="label">Returns:</div>
	    «op.preparePossibleReference»[«op.lowerBound»..«IF op.upperBound == ETypedElement::UNBOUNDED_MULTIPLICITY»*«ELSE»«op.upperBound»«ENDIF»]
	    «ENDIF»
	    «IF !op.EParameters.empty»
	    <div class="label">Parameters:
	    <ul>
	    «FOR param : op.EParameters»
	    	<li>«param.preparePossibleReference»[«param.lowerBound»..«IF param.upperBound == ETypedElement::UNBOUNDED_MULTIPLICITY»*«ELSE»«param.upperBound»«ENDIF»] <span class="teletype>"«escapeText(param.name)»</span></li>
	    «ENDFOR»
	    </ul>
	    «ENDIF»
    '''
    
    def private documentProperty(CharSequence key, CharSequence value)
	'''
	    <div class="keyValue"><span class="label">«key»: </span><span class="teletype">«value»</span></div>
    '''

    def private documentHeader(String sectionClass, String sectionTitle, String shortTitle, String label, EModelElement element)
    '''
	    <«sectionClass» id="«escapeLabel(label)»">«anchorDef(escapeLabel(label),sectionTitle)»</«sectionClass»>
	    
	    «IF element != null»
	    «element.findGenModelDocumentation»
	    «ENDIF»
    '''
    
    def private escapeText(String text){
    	
    	'''«text.replaceAll("&","&amp;").replaceAll("<","&lt;")»'''
    }
    
    def private escapeLabel(String text){
    	'''«text.replaceAll("_","").replaceAll("\\.","")»'''
    }
    
    def private findGenModelDocumentation(EModelElement element){
    	element.findGenModelDocumentation(true)
    }
    
    def private findGenModelDocumentation(EModelElement element, boolean required){
    	val doc = findAnnotation(element, "http://www.eclipse.org/emf/2002/GenModel", "documentation")
    	if(doc!=null){
    		val Reader docReader = new StringReader(doc);
    		val Parser parser = new Parser(docReader);
    	
    		val Document markdownDoc = parser.parse();
    		val builder = new StringBuilder();
    		val latexVisitor = new FixedHtmlEmitter(builder);
    		markdownDoc.accept(latexVisitor);
   			val documentationFields = DocumentationFieldUtils.getDocumentationFields(element);
    		documentationFields.forEach[
    			val value = it.getValue();
    			if (value != null) {
					builder.append(it.getKey() + ": " + value);
					builder.append("<br>");
    			}
			]	
    		return builder.toString;
    	}
    	else {
    		if(required){
    			return '''<div class="alert">Missing Documentation!</div>'''
    		} else {
		    	return ''''''
    		}
    	} 
    }
    
    def private findAnnotation(EModelElement elem, String source, String key){
    	val annotations = elem.EAnnotations
    	if(annotations != null){
	    	val ann = annotations.findFirst[it.source == source]
	    	if(ann != null){
	    		val det = ann.details
	    		if(det != null){
	    			return det.get(key)
	    		}
	    	}
    	}
    }
    
    def private anchorDef(CharSequence id,CharSequence text){
    	'''<a href="#«id»">«text»</a>'''
    }
    
	def private generatePackageDocTail() {
		'''
	       </body>
	       </html>
	    '''.appendToBuilder
	}
	
	override generateTail() {
		val transformerFactory = TransformerFactory.newInstance
		val transformer = transformerFactory.newTransformer
		val domSource = new DOMSource(toc)
		val streamResult = new StreamResult(new File(outputDir, "toc.xml"));
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.transform(domSource, streamResult);
	}
	
	def private backref(EClass cls) {
		EcoreHelper.getBackReferences(cls)
	}
	
	override getOutputType() {
		OutputType.DIRECTORY;
	}
	
}
