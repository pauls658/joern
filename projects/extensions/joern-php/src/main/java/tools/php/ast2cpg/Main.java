package tools.php.ast2cpg;

import java.io.FileReader;
import java.io.IOException;

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
//import udg.useDefGraph.UseDefGraph;

import ast.ASTNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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

	public static void main(String[] args) throws InvalidCSVFile, IOException, InterruptedException {

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
		cfgToUDG.setASTDefUseAnalyzer(new PHPASTDefUseAnalyzer());

		// initialize writers
		MultiPairCSVWriterImpl csvWriter = new MultiPairCSVWriterImpl();
		csvWriter.openEdgeFile( ".", "cpg_edges.csv");
		Writer.setWriterImpl( csvWriter);


		HashMap<String, LinkedList<ASTNode>> globalNameToBB = new HashMap<String, LinkedList<ASTNode>>();
		LinkedList<Tuple<String, ASTNode>> defBBs = new LinkedList<Tuple<String, ASTNode>>();
		int numGlobalNodes = 0;
		// let's go...
		FunctionDef rootnode;
		while ((rootnode = (FunctionDef)extractor.getNextFunction()) != null) {

			PHPCGFactory.addFunctionDef( rootnode);

			// convert() just calls newInstance() of PHPCFGFactory
			CFG cfg = ast2cfgConverter.convert(rootnode);
			csvCFGExporter.writeCFGEdges(cfg);

			PHPUseDefGraph udg = cfgToUDG.convert(cfg);
			for (Map.Entry<String, LinkedList<ASTNode>> entry : udg.getMap().entrySet()) {
				LinkedList<ASTNode> bbs = globalNameToBB.get(entry.getKey());
				if (bbs == null) {
					bbs = new LinkedList<ASTNode>();
					globalNameToBB.put(entry.getKey(), bbs);
				}
				bbs.addAll(entry.getValue());
				numGlobalNodes++;
			}

			defBBs.addAll(udg.getGlobalDefs());
			DefUseCFG defUseCFG = udgAndCfgToDefUseCFG.convert(cfg, udg); // nothing really useful done here, just an "easier format" for finding reaching defs
			PHPDDG ddg = (PHPDDG) ddgCreator.createForDefUseCFG(defUseCFG);
			csvDDGExporter.writeDDGEdges(ddg, udg.nameToParamNode);
		}

		FileOutputStream newPropsFile = new FileOutputStream("new_props.csv");
		newPropsFile.write(("id,globalName\n").getBytes());
		int count = 0, edgecount = 0;
		//System.out.println("Number global nodes: " + numGlobalNodes +  ", Number BB defs: " + defBBs.size());
		for (Tuple<String, ASTNode> def : defBBs) {
			String globalName = def.x;
			ASTNode BB = def.y;
			newPropsFile.write((BB.getNodeId() + "," + globalName + "\n").getBytes());
			/*
			//LinkedList<ASTNode> globalNodes = globalNameToBB.get(globalName);
			//System.out.println("Processing " + globalName + "th node with " + globalNodes.size() + " global nodes");
			count++;
			String id2 = BB.getProperty("funcid");
			for (ASTNode globalNode : globalNodes) {
				String id1 = globalNode.getProperty("funcid");
				if (id1 instanceof String && id2 instanceof String && !id1.equals(id2)) {
					ddg.add(BB, globalNode, globalName);
					edgecount++;
				}
			}
			*/
		}

		// now that we wrapped up all functions, let's finish off with the call graph
		CG cg = PHPCGFactory.newInstance();
		csvCGExporter.writeCGEdges(cg);

		csvWriter.closeEdgeFile();
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
