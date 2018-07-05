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
import udg.useDefGraph.UseOrDefRecord;
//import udg.useDefGraph.UseDefGraph;

import ast.ASTNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
		cfgToUDG.setASTDefUseAnalyzer(analyzer);

		// initialize writers
		MultiPairCSVWriterImpl csvWriter = new MultiPairCSVWriterImpl();
		csvWriter.openEdgeFile( ".", "cpg_edges.csv");
		Writer.setWriterImpl( csvWriter);


		HashMap<Long, Tuple<HashSet<String>, HashSet<String>>> defUsesForBB = new HashMap<Long,Tuple<HashSet<String>, HashSet<String>>>();
		// args always def + use their params
		HashMap<Long, HashSet<String>> defUsesForArg = new HashMap<Long, HashSet<String>>();
		//HashMap<Long, HashSet<String>> usesForBB = new HashMap<Long, HashSet<String>>();
		//HashMap<Long, HashSet<String>> globalsForFuncId = new HashMap<Long, LinkedList<String>>();
		//LinkedList<Tuple<String, ASTNode>> defBBs = new LinkedList<Tuple<String, ASTNode>>();
		int numGlobalNodes = 0;
		// let's go...
		
		FileOutputStream funcGlobalsFile = new FileOutputStream("func_id_globals.csv");
		funcGlobalsFile.write(("id,globals\n").getBytes());
		FileOutputStream BBdefuseFile = new FileOutputStream("BB_def_uses.csv");
		BBdefuseFile.write(("id,defs,uses\n").getBytes());
		FileOutputStream argdefuseFile = new FileOutputStream("arg_def_uses.csv");
		argdefuseFile.write(("id,symbols\n").getBytes());

		FunctionDef rootnode;
		while ((rootnode = (FunctionDef)extractor.getNextFunction()) != null) {

			PHPCGFactory.addFunctionDef(rootnode);

			// convert() just calls newInstance() of PHPCFGFactory
			CFG cfg = ast2cfgConverter.convert(rootnode);
			csvCFGExporter.writeCFGEdges(cfg);

			PHPUseDefGraph udg = cfgToUDG.convert(cfg);
			String globals = String.join(";", udg.getMap().keySet());
			funcGlobalsFile.write((String.valueOf(rootnode.getNodeId()) + "," +
						globals + "\n").getBytes());

			defUsesForBB.clear();
			defUsesForArg.clear();
			for (Map.Entry<String, List<UseOrDefRecord>> entry : udg.getUseDefDict().entrySet()) {
				if (entry.getKey().startsWith("@dbr")) {
					String[] pieces = entry.getKey().split(" ");
					switch (pieces[1]) {
						case "argsymbol":
							Long id = Long.parseLong(pieces[2]);
							HashSet<String> it = defUsesForArg.get(id);
							if (it == null) {
								it = new HashSet<String>();
								defUsesForArg.put(id, it);
							}
							it.add(pieces[3]);
							break;
						default:
							throw new Exception("oh dang");
					}
					continue;
				}


				for (UseOrDefRecord udr : entry.getValue()) {
					Long nodeId = udr.getAstNode().getNodeId();
 					Tuple<HashSet<String>, HashSet<String>> it;
					it = defUsesForBB.get(nodeId);
					if (it == null) {
						it = new Tuple<HashSet<String>,HashSet<String>>(new HashSet<String>(), new HashSet<String>());
						defUsesForBB.put(nodeId, it);
					}
					HashSet<String> s;
					if (udr.isDef()) {
						s = it.x;
					} else {
						s = it.y;
					}
					s.add(entry.getKey());
				}
			}

			for (Map.Entry<Long, Tuple<HashSet<String>, HashSet<String>>> entry : defUsesForBB.entrySet()) {
				String defs = String.join(";", entry.getValue().x);
				String uses = String.join(";", entry.getValue().y);
				BBdefuseFile.write((String.valueOf(entry.getKey()) + "," + defs + "," + uses + "\n").getBytes());
			}

			for (Map.Entry<Long, HashSet<String>> entry : defUsesForArg.entrySet()) {
				String symbols = String.join(";", entry.getValue());
				argdefuseFile.write((String.valueOf(entry.getKey()) + "," + symbols + "\n").getBytes());
			}

		}


		// now that we wrapped up all functions, let's finish off with the call graph
		CG cg = PHPCGFactory.newInstance();
		csvCGExporter.writeCGEdges(cg);

		csvWriter.closeEdgeFile();
		analyzer.cleanup();
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
