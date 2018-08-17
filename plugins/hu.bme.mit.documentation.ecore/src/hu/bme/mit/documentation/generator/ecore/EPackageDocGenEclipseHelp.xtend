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
import java.util.GregorianCalendar
import java.util.List
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

/**
 * @author Abel Hegedus
 * @author Adam Horvath
 * 
 */
class EPackageDocGenEclipseHelp implements IDocGenerator{
	private EPackage pckg
    private StringBuilder builder
    private List<String> filter
	private File outputDir
	private File outputFile
    
    /**
     * Sets the output <em>directory</em>. Output files will be generated into
     * this directory. This method must be called prior calling
     * {@link documentEPackage} method.
     */
	override setOutputFile(File outputFile) {
		this.outputDir = outputFile;
	}
    
    def private getFileNameForPackage(EPackage pckg) {
    	ePackageFqName(pckg) + ".html"
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
        
        val gc = new GregorianCalendar()
        val now = gc.getTime().toString()
        
		if (!(outputDir.exists && outputDir.isDirectory) && !outputDir.mkdirs) {
			throw new RuntimeException("Could not create output directory: "
				+ outputDir.absolutePath);
		}
        
        if(genHeader){
        	
        }
        pckg.documentEPackageHeader.appendToBuilder
                		
        pckg.EClassifiers.sortBy[name].forEach[

        	if(it instanceof EClass){
	        		
        		val cls = it as EClass

        		cls.documentEClassHeader
        		
        		if (!cls.ESuperTypes.empty){
        			var List<EClass> list = new ArrayList;
        			getAllSuperClassesRecursively(cls, list);
        			list.sortBy[name].forEach[
						val superCls = it as EClass
	    				val id = escapeLabel(cls.EPackage.nsPrefix+"."+cls.name) + "."  + escapeLabel(superCls.EPackage.nsPrefix+"."+superCls.name);
	    				'''<h6>'''.appendToBuilder    	
	    				'''<b>Supertype:</b> <a href="«getFileNameForPackage(superCls.EPackage)»#«escapeLabel(superCls.EPackage.nsPrefix+"."+superCls.name)»">«superCls.name»</a>'''.appendToBuilder    	
    					''' <a id="«id».toggleButton" href="javascript:toggle('«id»', '«id».toggleButton');">[show]</a>'''.appendToBuilder
	    				'''<div id="«id»" style="display: none" href="javascript:toggle();">'''.appendToBuilder				
	    				'''«superCls.findGenModelDocumentation»'''.appendToBuilder
	    				
	    				if (!superCls.EAttributes.empty
	    					|| !superCls.EReferences.empty
    						|| !superCls.EOperations.empty
	    				) {
		    				superCls.documentEClass(id)      		
	    				}
	    				
	    				'''</div>'''.appendToBuilder	
	    				'''</h6>'''.appendToBuilder	
					]	        		
        		}
        		
        		val br = cls.backref
        		if (!br.knownSubtypes.empty) {
        			'''<h6>Known subtypes</h6>'''.appendToBuilder
        			br.knownSubtypes.sortBy[name].forEach[
        				'''<span>'''.appendToBuilder
        				'''<a href="«getFileNameForPackage(it.EPackage)»#«escapeLabel(it.EPackage.nsPrefix+"."+it.name)»">«it.name»</a> | '''.appendToBuilder    	
        				'''</span>'''.appendToBuilder
        			]
        		}
        		if (!br.usedByReferences.empty) {
        			'''<h6>Used by</h6>'''.appendToBuilder
        			br.usedByReferences.sortBy[EContainingClass.name + name].forEach[
        				'''<span>'''.appendToBuilder
        				'''<a href="«getFileNameForPackage(it.EContainingClass.EPackage)»#«escapeLabel(it.EContainingClass.EPackage.nsPrefix+it.EContainingClass.name)+"."+it.name»">«it.EContainingClass.name +"::"+  it.name»</a> | '''.appendToBuilder    	
        				'''</span>'''.appendToBuilder
        			]
        		}
        		cls.documentEClass("" + escapeLabel(cls.EPackage.nsPrefix+"."+cls.name))
        	} else if(it instanceof EDataType){
        		if(it instanceof EEnum){
        			val eenum = it as EEnum
        			eenum.documentEEnumHeader.appendToBuilder
        		}
        	}
        	
        ]
        
        generatePackageDocTail();
        
        val pkgDocWriter = new BufferedWriter(new FileWriter(outputFile));
        pkgDocWriter.append(builder);
        pkgDocWriter.close
    }
	
	def private documentEClass(EClass cls, String id) {
		if(!cls.EAttributes.empty){
			'''
			<table>
			<tr>
				<th colspan="3"><div class="tableHeader">Attributes</div></th>
			</tr>
			<tr>
				<th><div class="columnHeader">Name</div></th>
				<th><div class="columnHeader">Properties</div></th>
				<th><div class="columnHeader">Documentation</div></th>
			</tr>
			'''.appendToBuilder
			cls.EAttributes.sortBy[name].forEach[
				'''<tr>'''.appendToBuilder
				documentEAttributeHeader(id).appendToBuilder
				''' </td> '''.appendToBuilder
				'''<td>'''.appendToBuilder
				findGenModelDocumentation(derived).appendToBuilder
				'''</td>
				</tr>'''.appendToBuilder
			]
			'''
			</table>
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".attr","")»
			'''.appendToBuilder
			
		}
		
		
		if(!cls.EReferences.empty){
			//"paragraph".documentHeader("References", cls.EPackage.nsPrefix+"."+cls.name+".ref", null).appendToBuilder
			'''
			<table>
			<tr>
				<th colspan="3"><div class="tableHeader">References</div></th>
			</tr>
			<tr>
				<th><div class="columnHeader">Name</div></th>
				<th><div class="columnHeader">Properties</div></th>
				<th><div class="columnHeader">Documentation</div></th>
			</tr>
			'''.appendToBuilder
			cls.EReferences.sortBy[name].forEach[
				'''<tr>'''.appendToBuilder
				documentEReferenceHeader(id).appendToBuilder
				'''
				</td> 
				<td> '''.appendToBuilder
				findGenModelDocumentation(derived).appendToBuilder
				'''</td>
				</tr>'''.appendToBuilder
			]
			'''
			</table>
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".ref","")»
			'''
			.appendToBuilder
			
		}
		
		if(!cls.EOperations.empty){
	    	'''
			<table>
			<tr>
				<th colspan="3"><div class="tableHeader">Operations</div></th>
			</tr>
			<tr>
				<th><div class="columnHeader">Name</div></th>
				<th><div class="columnHeader">Properties</div></th>
				<th><div class="columnHeader">Documentation</div></th>
			</tr>
			'''.appendToBuilder
			cls.EOperations.sortBy[name].forEach[
				'''<tr>'''.appendToBuilder
				documentEOperationHeader(id).appendToBuilder
				''' </td><td> '''.appendToBuilder
				findGenModelDocumentation(false).appendToBuilder
				'''</td>
				</tr>'''.appendToBuilder
			]
			'''
			</table>
			«anchorDef(cls.EPackage.nsPrefix+"."+cls.name+".op","")»
			'''.appendToBuilder
			
		}
	}
	
	def private getAllSuperClassesRecursively(EClass cls, List<EClass> list) {
		for (EClass superCls : cls.ESuperTypes) {
			if (!list.contains(superCls)) {
				list.add(superCls);
			}
			getAllSuperClassesRecursively(superCls, list);
		}
	}
    
    def private appendToBuilder(CharSequence s){
    	builder.append(s)
    }
    
    def private documentEPackageHeader(EPackage pckg)
    	'''
    	«val packageName = ePackageFqName(pckg)»
		«val title = "The <span class=\"packageName\">" + packageName + "</span> package"»
		«documentHeader("h1", title, packageName, pckg.nsPrefix, pckg)»
		<div class="">EPackage properties:</div>
		«documentProperty("Namespace Prefix", '''«escapeText(pckg.nsPrefix)»''')»
		
		«documentProperty("Namespace URI", '''«pckg.nsURI»''')»
		
        '''

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
	    var boolean hasPropList = false;
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
//		if(!cls.ESuperTypes.isEmpty()){
//			var boolean genProps = false;
//			if(!cls.isInterface() && !cls.isAbstract()){
//				'''<div class="eclassProps">EClass properties:'''.appendToBuilder
//				genProps=true;
//			}
//			'''
//			<div class="eclassSupertypes">Supertypes:
//			«FOR st : cls.ESuperTypes SEPARATOR ", "»
//			<span class="teletype">«st.preparePossibleReference»</span>
//	      	«ENDFOR»
//			</div>'''.appendToBuilder
//			if(genProps){
//				'''</div>'''.appendToBuilder
//			}
//		}
		if(hasPropList){
			'''</div></div>'''.appendToBuilder
		}
    }
    
    def private documentENamedElement(ENamedElement elem, String parentId, String color)
    '''
    <div id="«parentId+"."+elem.name»" class="teletype">«IF color != null»<div style="color:«color»">«ENDIF»<a href="#«parentId+"."+elem.name»" >«escapeText(elem.name)»</a>«IF color != null»</div>«ENDIF»</div>
    '''
    
    //(«typePckg.nsURI»)
    // <«typePckg.name»>
    def private documentETypedElement(ETypedElement elem, String parentId, String color)
    '''
    	<td>«elem.documentENamedElement(parentId, color)»</td>
    	<td>«documentProperty("T", elem.preparePossibleReference)»
    <div class="label">Cardinality: [«elem.lowerBound»..«IF elem.upperBound == -1»*«ELSE»«elem.upperBound»«ENDIF»]</div>
    «IF !elem.ordered»
    <div class="label">Unordered</div>
    «ENDIF»
    «IF !elem.unique»
    <div class="label">Not unique</div>
    «ENDIF»'''
    
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
    		'''<a href="#«escapeLabel(typePckg.nsPrefix+ "." + typeName)»">«typeName»</a>'''
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
	}
	
	 def private backref(EClass cls) {
	 	EcoreHelper.getBackReferences(cls)
	 }
	
	override getOutputType() {
		OutputType.DIRECTORY;
	}
	
}
