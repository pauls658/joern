package tools.php.ast2cpg;

import java.io.FileReader;
import java.io.IOException;

import cfg.nodes.AbstractCFGNode;
import org.apache.commons.cli.ParseException;

import ast.php.functionDef.FunctionDef;
import cfg.ASTToCFGConverter;
import cfg.CFG;
import cfg.PHPCFGFactory;
import cg.CG;
import cg.PHPCGFactory;
import ddg.CFGAndUDGToDefUseCFG;
//import ddg.DDGCreator;
import ddg.PHPDDGCreator;
import ddg.DataDependenceGraph.PHPDDG;
import ddg.DefUseCFG.DefUseCFG;
import inputModules.csv.KeyedCSV.exceptions.InvalidCSVFile;
import inputModules.csv.csvFuncExtractor.CSVFunctionExtractor;
import outputModules.common.Writer;
import outputModules.csv.MultiPairCSVWriterImpl;
import outputModules.csv.exporters.CSVCFGExporter;
import outputModules.csv.exporters.CSVCGExporter;
//import outputModules.csv.exporters.CSVDDGExporter;
import outputModules.csv.exporters.PHPCSVDDGExporter;
//import udg.CFGToUDGConverter;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefGraph.UseOrDef;
import udg.useDefGraph.UseOrDefRecord;
//import udg.useDefGraph.useDefGraph;

import ast.ASTNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import java.io.*;
public class Main {

	// command line interface
	static CommandLineInterface cmdLine = new CommandLineInterface();

	// converters
	static CSVFunctionExtractor extractor = new CSVFunctionExtractor();
	//static PHPCFGFactory cfgFactory = new PHPCFGFactory();
	static ASTToCFGConverter ast2cfgConverter = new ASTToCFGConverter();
	static PHPCFGToUDGConverter cfgToUDG = new PHPCFGToUDGConverter();
	static CFGAndUDGToDefUseCFG udgAndCfgToDefUseCFG = new CFGAndUDGToDefUseCFG();
	static PHPDDGCreator ddgCreator = new PHPDDGCreator();

	// exporters
	static CSVCFGExporter csvCFGExporter = new CSVCFGExporter();
	static PHPCSVDDGExporter csvDDGExporter = new PHPCSVDDGExporter();
	static CSVCGExporter csvCGExporter = new CSVCGExporter();

	public static void main(String[] args) throws InvalidCSVFile, IOException, InterruptedException, Exception {

		// parse command line
		parseCommandLine(args);

		// initialize readers
		String nodeFilename = cmdLine.getNodeFile();
		String edgeFilename = cmdLine.getEdgeFile();
		FileReader nodeFileReader = new FileReader(nodeFilename);
		FileReader edgeFileReader = new FileReader(edgeFilename);

		// initialize converters

		extractor.setInterpreters(new PHPCSVNodeInterpreter(), new PHPCSVEdgeInterpreter());

		extractor.initialize(nodeFileReader, edgeFileReader);
		ast2cfgConverter.setFactory(new PHPCFGFactory());
		PHPASTDefUseAnalyzer analyzer = new PHPASTDefUseAnalyzer();
		cfgToUDG.setPHPASTDefUseAnalyzer(analyzer);

		// initialize writers
		MultiPairCSVWriterImpl csvWriter = new MultiPairCSVWriterImpl();
		csvWriter.openEdgeFile( ".", "cpg_edges.csv");
		Writer.setWriterImpl( csvWriter);


		HashMap<Long, Tuple<HashSet<String>, HashSet<String>>> defUsesForBB = new HashMap<Long,Tuple<HashSet<String>, HashSet<String>>>();
		// args always def + use their params
		HashMap<Long, HashSet<String>> defUsesForArg = new HashMap<Long, HashSet<String>>();
		int numGlobalNodes = 0;
		// let's go...
		
		FileOutputStream BBdefFile = new FileOutputStream("defuse_csv/BB_def.csv");
		BBdefFile.write(("id,symbol\n").getBytes());
		FileOutputStream BBuseFile = new FileOutputStream("defuse_csv/BB_use.csv");
		BBuseFile.write(("id,symbol\n").getBytes());
		FileOutputStream argdefuseFile = new FileOutputStream("defuse_csv/arg_symbols.csv");
		argdefuseFile.write(("id,symbol\n").getBytes());

		FunctionDef rootnode;
		while ((rootnode = (FunctionDef)extractor.getNextFunction()) != null) {

			PHPCGFactory.addFunctionDef(rootnode);

			// convert() just calls newInstance() of PHPCFGFactory
			CFG cfg = ast2cfgConverter.convert(rootnode);
			csvCFGExporter.writeCFGEdges(cfg);

			PHPUseDefGraph udg = cfgToUDG.convert(cfg);

			if (!rootnode.getTypeAsString().equals("TopLevelFunctionDef")) {
				String funcid = Long.toString(rootnode.getNodeId());
				String prefix = funcid + "_local";
				cfgToUDG.makeSymbolsGlobal(udg, prefix);
				String exitId = Long.toString(((AbstractCFGNode)cfg.getExitNode()).getNodeId());
				for (String s : udg.getLocalSymbols()) {
					BBdefFile.write((exitId + "," + s + "\n").getBytes());
				}
			}

			FileOutputStream outFile;
			for (Map.Entry<Long, LinkedList<UseOrDef>> e : udg.getDefUseMap().entrySet()) {
			    // The id is for args is already updated
				for (UseOrDef uod : e.getValue()) {
					if (uod.symbol.isArg) {
						outFile = argdefuseFile;
					} else if (uod.isDef) {
						outFile = BBdefFile;
					} else {
						outFile = BBuseFile;
					}

					// name is always output
					outFile.write((Long.toString(e.getKey()) + "," + uod.symbol.name + "\n").getBytes());
					if (uod.symbol.isArray && uod.symbol.isIndexVar) {
						// For now just output the index as a use
						BBuseFile.write((Long.toString(e.getKey()) + "," + uod.symbol.index + "\n").getBytes());
					}
				}
			}

		}


		// now that we wrapped up all functions, let's finish off with the call graph
		CG cg = PHPCGFactory.newInstance();
		csvCGExporter.writeCGEdges(cg);

		csvWriter.closeEdgeFile();
		analyzer.cleanup();
		BBuseFile.close();
		BBdefFile.close();
		argdefuseFile.close();
	}

	private static void parseCommandLine(String[] args)	{

		try {
			cmdLine.parseCommandLine(args);
		}
		catch (RuntimeException | ParseException e) {
			printHelpAndTerminate(e);
		}
	}

	private static void printHelpAndTerminate(Exception e) {

		System.err.println(e.getMessage());
		cmdLine.printHelp();
		System.exit(0);
	}

}
