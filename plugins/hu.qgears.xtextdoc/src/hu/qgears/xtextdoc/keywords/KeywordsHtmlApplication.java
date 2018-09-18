package hu.qgears.xtextdoc.keywords;

import java.io.File;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import hu.qgears.xtextdoc.keywords.KeywordsHtml.Args;

public class KeywordsHtmlApplication implements IApplication {

	private static final String ARG_SOURCE_FOLDER = "src_folder";
	private static final String ARG_XCORE = "xcore";
	private static final String ARG_XTEXT = "xtext";
	private static final String ARG_OUTPUT_FOLDER = "output_folder";
	private static final String ARG_EXAMPLE_FOLDER = "ex_folder";
	private static final String ARG_EXAMPLE_EXT = "ex_ext";
	
	private static final String ARG_LIST_SEPARATOR = "&&";
	
	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] arguments = (String[])context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		Options opts = new Options();
		
		opts.addOption(ARG_SOURCE_FOLDER, true, "Source folder of model files and example projects.");
		opts.addOption(ARG_XCORE,true,"The model xcore files ("+ARG_LIST_SEPARATOR+" separated).");
		opts.addOption(ARG_XTEXT,true,"The model xtext grammar files ("+ARG_LIST_SEPARATOR+" separated).");
		opts.addOption(ARG_OUTPUT_FOLDER,true,"The generated HTML document folder.");
		opts.addOption(ARG_EXAMPLE_FOLDER,true,"The example models folders ("+ARG_LIST_SEPARATOR+" separated).");
		opts.addOption(ARG_EXAMPLE_EXT,true,"The example models extensions ("+ARG_LIST_SEPARATOR+" separated).");

		CommandLineParser parser = new BasicParser();
		CommandLine cli = parser.parse(opts, arguments);
		HelpFormatter hf = new HelpFormatter();
		
		
		if(cli.hasOption(ARG_SOURCE_FOLDER) && cli.hasOption(ARG_XCORE) && cli.hasOption(ARG_XTEXT)
				&& cli.hasOption(ARG_OUTPUT_FOLDER) && cli.hasOption(ARG_EXAMPLE_FOLDER) && cli.hasOption(ARG_EXAMPLE_EXT)){
			KeywordsHtml keywordsHtml = new KeywordsHtml();
			
			String sourceFolder = cli.getOptionValue(ARG_SOURCE_FOLDER);
			String xcore = cli.getOptionValue(ARG_XCORE);
			String xtext = cli.getOptionValue(ARG_XTEXT);
			String outFolder = cli.getOptionValue(ARG_OUTPUT_FOLDER);
			String exampleFolder = cli.getOptionValue(ARG_EXAMPLE_FOLDER);
			String exExt = cli.getOptionValue(ARG_EXAMPLE_EXT);
			
			Args args = keywordsHtml.new Args();
			args.xcore = parseStr(sourceFolder, xcore);
			args.xtext = parseStr(sourceFolder, xtext);
			args.output = new File(outFolder);
			args.examples.examplesFolders = parseStr(sourceFolder, exampleFolder);
			args.examples.exampleExtensions = exExt.split(ARG_LIST_SEPARATOR);
			keywordsHtml.run(args);
			System.out.println("Keywords HTML documentation generation finished without errors.");
		}
		else{
			hf.printHelp("KeywordsHtmlApplication", opts);
			throw new RuntimeException(
					"At least the following argumens must be specified: -src_folder -xcore -xtext -output_folder -ex_folder -ex_ext");
		}
		return EXIT_OK;
	}

	private File[] parseStr (String sourceFolder, String str) {
		String[] pieces = str.split(ARG_LIST_SEPARATOR);
		File[] files = new File[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			files[i] = new File(sourceFolder, pieces[i]);
		}
		return files;
	}
	
	@Override
	public void stop() {
		
	}

}
