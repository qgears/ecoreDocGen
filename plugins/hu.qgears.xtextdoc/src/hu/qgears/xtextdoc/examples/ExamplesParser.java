package hu.qgears.xtextdoc.examples;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import hu.qgears.commons.MultiMapHashImpl;
import hu.qgears.commons.UtilFile;
import hu.qgears.commons.UtilString;

public class ExamplesParser {
	public static class Args
	{
		public File[] examplesFolders;
		public String[] exampleExtensions;
		@Override
		public String toString() {
			return "Args [examplesFolders=" + Arrays.toString(examplesFolders) + ", exampleExtensions="
					+ Arrays.toString(exampleExtensions) + "]";
		}
		
	}
	private Args args;
	private MultiMapHashImpl<String, ExampleContent> keywordToContent=new MultiMapHashImpl<>();
	public ExamplesParser(hu.qgears.xtextdoc.examples.ExamplesParser.Args examples) {
		this.args=examples;
	}

	public void run() throws IOException {
		for(File dir: args.examplesFolders)
		{
			for(File f: UtilFile.listAllFiles(dir))
			{
				for(String ext:args.exampleExtensions)
				{
					if(f.getName().endsWith(ext))
					{
						//System.out.println("Found example file: "+f.getName());
						parseExampleFile(f);
					}
				}
			}
		}
	}

	private void parseExampleFile(File f) throws IOException {
		String s=UtilFile.loadAsString(f);
		int idx=s.indexOf("@example");
		while(idx>=0)
		{
			int lineend=s.indexOf("\n", idx);
			String tag=""+s.substring(idx, lineend);
			List<String> kws=UtilString.split(tag, " \t\r\n");
			if(kws.contains("*/"))
			{
				int idxclose=kws.indexOf("*/");
				kws=kws.subList(0, idxclose);
			}
			if(kws.size()>1)
			{
				String keyword=kws.get(1);
				//System.out.println("Example of: '"+keyword+"'");
				String content=findExampleContent(s, idx);
				ExampleContent c=new ExampleContent(content);
				keywordToContent.putSingle(keyword, c);
				// Akos: '_' mark in annotation may mean a space or an underscore
				if (keyword.contains("_")) {
					keyword=keyword.replaceAll("_", " ");
					//System.out.println("Example of: '"+keyword+"'");
					keywordToContent.putSingle(keyword, c);
				}
				if(kws.size()>2)
				{
					String reftype=kws.get(2);
					if("true".equals(reftype))
					{
						c.type=ERefType.setTrue;
					}else if(Character.isAlphabetic(reftype.charAt(0)))
					{
						c.type=ERefType.creator;
						c.typeName=reftype;
					}else if(reftype.charAt(0)=='[')
					{
						c.type=ERefType.reference;
						c.typeName=reftype.substring(1, reftype.length()-1);
					}else if(reftype.charAt(0)=='$')
					{
						c.parentTypeName=reftype.substring(1, reftype.length()-1);
					}else if(reftype.charAt(0)=='(')
					{
						c.type=ERefType.setEnumeration;
						c.typeName=reftype.substring(1, reftype.length()-1);
					}
					else
					{
						// TODO report properly
						System.err.println("Problematic example reference: "+f.getAbsolutePath()+" "+kws.get(2));
					}
				}
			}
			idx=s.indexOf("@example", idx+1);
		}
	}

	private String findExampleContent(String s, int idx) {
		int begin=s.lastIndexOf("/*", idx);
		int i=0;
		try {
			int depth=0;
			for(i=idx;i<s.length();++i)
			{
				if(s.charAt(i)=='{')
				{
					depth++;
				}
				if(s.charAt(i)=='}')
				{
					depth--;
					if(depth==0)
					{
						return s.substring(begin, i+1);
					}
				}
			}
			return s.substring(begin, i);
		} catch (StringIndexOutOfBoundsException e) {
			System.err.println("["+begin+","+i+"]");
			e.printStackTrace();
			throw e;
		}
	}

	public List<ExampleContent> getExamples(String key, ERefType type, String parentTypeName) {
		List<ExampleContent> ret=new ArrayList<>();
		if (key.equals("true")) {
			//System.out.println("");
		}
		for(ExampleContent c: keywordToContent.get(key))
		{
			if (c.parentTypeName!= null && !c.parentTypeName.equals(parentTypeName)) {
				continue;
			}
			if(c.type==null || (c.type)==type)
			{
				ret.add(c);
			}
		}
		return ret;
	}
}

