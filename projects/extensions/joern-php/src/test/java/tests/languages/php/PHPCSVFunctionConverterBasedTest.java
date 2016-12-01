package tests.languages.php;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;

import ast.ASTNode;
import ast.expressions.CallExpressionBase;
import ast.functionDef.FunctionDefBase;
import ast.php.functionDef.FunctionDef;
import ast.php.functionDef.TopLevelFunctionDef;
import cfg.ASTToCFGConverter;
import cfg.CFG;
import cfg.CFGEdge;
import cfg.PHPCFGFactory;
import cfg.nodes.ASTNodeContainer;
import cfg.nodes.AbstractCFGNode;
import cfg.nodes.CFGNode;
import cg.CG;
import cg.CGEdge;
import cg.PHPCGFactory;
import ddg.CFGAndUDGToDefUseCFG;
import ddg.DDGCreator;
import ddg.DataDependenceGraph.DDG;
import ddg.DataDependenceGraph.DefUseRelation;
import ddg.DefUseCFG.DefUseCFG;
import inputModules.csv.KeyedCSV.exceptions.InvalidCSVFile;
import udg.CFGToUDGConverter;
import udg.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.useDefGraph.UseDefGraph;
import udg.useDefGraph.UseOrDefRecord;

public class PHPCSVFunctionConverterBasedTest extends PHPCSVFunctionExtractorBasedTest {

	private ASTToCFGConverter ast2cfgConverter;
	private CFGToUDGConverter cfgToUDG;
	private CFGAndUDGToDefUseCFG udgAndCfgToDefUseCFG;
	private DDGCreator ddgCreator;


	/* ***** */
	/* Setup */
	/* ***** */

	@Override
	@Before
	public void init() {
		super.init();

		// initialize AST to CFG converter
		this.ast2cfgConverter = new ASTToCFGConverter();
		this.ast2cfgConverter.setFactory(new PHPCFGFactory());

		// initialize CFG to UDG converter
		this.cfgToUDG = new CFGToUDGConverter();
		this.cfgToUDG.setASTDefUseAnalyzer(new PHPASTDefUseAnalyzer());

		// initialize CFG+UDG to DDG converter
		this.udgAndCfgToDefUseCFG = new CFGAndUDGToDefUseCFG();
		this.ddgCreator = new DDGCreator();
	}


	/* ************/
	/* Extractors */
	/* ************/

	/*
	 * Note: for testing, use either:
	 *
	 * - getTopFuncAST(String) which hands a given CSV sample to the PHP CSV node and egde
	 *   interpreters directly and assumes that this sample only contains top-level code to
	 *   be tested.
	 *
	 * - getAllFuncASTs(String) which hands a given CSV sample to the PHP CSV function extractor
	 *   first, which in turn uses the PHP CSV node and edge interpreters to build ASTs for
	 *   each found function. This is useful in case a particular function (or several) are to
	 *   be analyzed.
	 */

	/**
	 * This function is used for tests that handle code contained entirely
	 * in a single artificial toplevel PHP function.
	 *
	 * Precondition: This function assumes that the
	 * AST node with the lowest id in the CSV nodes file is a TopLevelFunctionDef.
	 * (Note that directory, file and entry/exit nodes are not AST nodes.)
	 * For CSV files generated by the phpjoern parser, this is indeed always
	 * the case.
	 */
	protected FunctionDefBase getTopFuncAST( String testDir)
			throws IOException, InvalidCSVFile {

		handleCSVFiles( testDir);

		ASTNode node = ast.getNodeWithLowestId();

		assert node instanceof TopLevelFunctionDef;

		return (FunctionDefBase)node;
	}

	protected CFG getTopCFGForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		FunctionDefBase node = getTopFuncAST(testDir);
		CFG cfg = getCFGForFuncAST(node);

		return cfg;
	}

	protected UseDefGraph getTopUDGForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		FunctionDefBase node = getTopFuncAST(testDir);
		CFG cfg = getCFGForFuncAST(node);
		UseDefGraph udg = getUDGForCFG(cfg);

		return udg;
	}

	protected DDG getTopDDGForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		FunctionDefBase node = getTopFuncAST(testDir);
		CFG cfg = getCFGForFuncAST(node);
		DDG ddg = getDDGForCFG(cfg);

		return ddg;
	}

	protected HashMap<String,CFG> getAllCFGsForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		HashMap<String,CFG> cfgs = new HashMap<String,CFG>();

		HashMap<String, FunctionDef> functions = super.getAllFuncASTs(testDir);
		for( String name : functions.keySet()) {

			CFG cfg = getCFGForFuncAST(functions.get(name));
			cfgs.put( name, cfg);
		}

		return cfgs;
	}

	protected HashMap<String,UseDefGraph> getAllUDGsForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		HashMap<String,UseDefGraph> udgs = new HashMap<String,UseDefGraph>();

		HashMap<String, FunctionDef> functions = super.getAllFuncASTs(testDir);
		for( String name : functions.keySet()) {

			CFG cfg = getCFGForFuncAST(functions.get(name));
			UseDefGraph udg = getUDGForCFG(cfg);
			udgs.put( name, udg);
		}

		return udgs;
	}

	protected HashMap<String,DDG> getAllDDGsForCSVFiles(String testDir)
			throws IOException, InvalidCSVFile {

		HashMap<String,DDG> ddgs = new HashMap<String,DDG>();

		HashMap<String, FunctionDef> functions = super.getAllFuncASTs(testDir);
		for( String name : functions.keySet()) {

			CFG cfg = getCFGForFuncAST(functions.get(name));
			DDG ddg = getDDGForCFG(cfg);
			ddgs.put( name, ddg);
		}

		return ddgs;
	}

	/**
	 * For two CSV files in a given folder, initializes a function extractor, extracts all
	 * functions and stores them in the PHPCGFactory, then uses the PHPCGFactory
	 * to build a call graph and returns it.
	 */
	protected CG getCGForCSVFiles(String testDir) throws IOException, InvalidCSVFile {

		HashSet<FunctionDef> funcs = super.getAllFuncASTsUnkeyed( testDir);

		for( FunctionDef func : funcs)
			PHPCGFactory.addFunctionDef( func);

		CG cg = PHPCGFactory.newInstance();

		System.out.println();
		System.out.println("CG\n~~");
		System.out.println(cg);

		return cg;
	}


	/* ******************** */
	/* Conversion functions */
	/* ******************** */

	/**
	 * Creates and returns a CFG for a given AST function node.
	 */
	protected CFG getCFGForFuncAST(FunctionDefBase node)
			throws IOException, InvalidCSVFile
	{
		CFG cfg = this.ast2cfgConverter.convert(node);

		System.out.println();
		System.out.println("CFG (" + node + ")\n~~~");
		System.out.println(cfg);

		return cfg;
	}

	/**
	 * Creates and returns a UDG for a given CFG.
	 */
	protected UseDefGraph getUDGForCFG(CFG cfg)
			throws IOException, InvalidCSVFile
	{
		UseDefGraph udg = this.cfgToUDG.convert(cfg);

		System.out.println();
		System.out.println("UDG\n~~~");
		System.out.println(udg);

		return udg;
	}

	/**
	 * Creates and returns a DDG for a given CFG.
	 */
	protected DDG getDDGForCFG(CFG cfg)
			throws IOException, InvalidCSVFile
	{
		DefUseCFG defUseCFG = udgAndCfgToDefUseCFG.convert(cfg, getUDGForCFG(cfg));
		DDG ddg = ddgCreator.createForDefUseCFG(defUseCFG);

		System.out.println();
		System.out.println("DDG\n~~~");
		System.out.println(ddg);

		return ddg;
	}



	/* ****************************** */
	/* Test helper functions for CFGs */
	/* ****************************** */

	/**
	 * Checks whether an edge exists in a given CFG from a given
	 * source node to a given destination node with a given label.
	 */
	protected boolean edgeExists(CFG cfg, long srcId, long dstId, String label) {

		Collection<CFGEdge> cfgEdges = cfg.getEdges();

		for(CFGEdge edge : cfgEdges) {

			if( getCFGNodeId(edge.getSource()) == srcId
					&& getCFGNodeId(edge.getDestination()) == dstId
					&& edge.getLabel().equals( label))
				return true;
		}

		return false;
	}

	protected boolean edgeExists(CFG cfg, CFGNode src, CFGNode dst, String label) {

		return edgeExists( cfg, getCFGNodeId(src), getCFGNodeId(dst), label);
	}

	protected boolean edgeExists(CFG cfg, CFGNode src, long dstId, String label) {

		return edgeExists( cfg, getCFGNodeId(src), dstId, label);
	}

	protected boolean edgeExists(CFG cfg, long srcId, CFGNode dst, String label) {

		return edgeExists( cfg, srcId, getCFGNodeId(dst), label);
	}

	private long getCFGNodeId(CFGNode node) {

		// CFG nodes that are AST node containers have their ids stored in their AST node;
		// abstract nodes such as entry or exit nodes have their id set internally.
		return (node instanceof ASTNodeContainer)
				? ((ASTNodeContainer)node).getASTNode().getNodeId()
				: ((AbstractCFGNode)node).getNodeId();
	}

	protected Collection<CFGNode> getNodesOfType(CFG cfg, String typeName) {

		Collection<CFGNode> vertices = cfg.getVertices();

		return vertices.stream().
				filter(x -> x.getClass().getSimpleName().equals(typeName))
				.collect(Collectors.toList());
	}



	/* ****************************** */
	/* Test helper functions for UDGs */
	/* ****************************** */

	protected boolean containsDef( UseDefGraph udg, String symbol, long id) {
		return containsUseOrDef( udg, symbol, id, true);
	}

	protected boolean containsUse( UseDefGraph udg, String symbol, long id) {
		return containsUseOrDef( udg, symbol, id, false);
	}

	/**
	 * Checks whether a collection of UseOrDef elements contains a definition/use
	 * of a given symbol for a given node.
	 */
	private boolean containsUseOrDef( UseDefGraph udg, String symbol, long id, boolean isDef) {

		List<UseOrDefRecord> useOrDefRecords = udg.getUsesAndDefsForSymbol(symbol);

		for( UseOrDefRecord useOrDefRecord : useOrDefRecords) {
			if( useOrDefRecord.getAstNode().getNodeId().equals( id)
					&&  useOrDefRecord.isDef() == isDef)
				return true;
		}
		return false;
	}

	protected int numberOfDefsForSymbol( UseDefGraph udg, String symbol) {

		return numberOfDefsOrUsesForSymbol( udg, symbol,  true);
	}

	protected int numberOfUsesForSymbol( UseDefGraph udg, String symbol) {

		return numberOfDefsOrUsesForSymbol( udg, symbol, false);
	}

	/**
	 * Checks whether a collection of UseOrDef elements contains an expected number
	 * of definitions/uses of a given symbol.
	 */
	private int numberOfDefsOrUsesForSymbol( UseDefGraph udg, String symbol, boolean isDef) {

		List<UseOrDefRecord> useOrDefRecords = udg.getUsesAndDefsForSymbol(symbol);

		int number = 0;

		for( UseOrDefRecord useOrDefRecord : useOrDefRecords) {
			if( useOrDefRecord.isDef() == isDef)
				number++;
		}

		return number;
	}



	/* ****************************** */
	/* Test helper functions for DDGs */
	/* ****************************** */

	/**
	 * Checks whether an edge exists in a given DDG from a given
	 * source node to a given destination node for a given symbol.
	 */
	protected boolean edgeExists( DDG ddg, String symbol, long srcid, long dstid) {

		for (DefUseRelation ddgEdge : ddg.getDefUseEdges()) {

			assert ddgEdge.src instanceof ASTNode;
			assert ddgEdge.dst instanceof ASTNode;

			if( ddgEdge.symbol.equals(symbol)
					&& ((ASTNode)ddgEdge.src).getNodeId().equals( srcid)
					&& ((ASTNode)ddgEdge.dst).getNodeId().equals( dstid))
				return true;
		}

		return false;
	}



	/* ****************************** */
	/* Test helper functions for UDGs */
	/* ****************************** */

	/**
	 * Checks whether an edge exists in a given CG from a given
	 * source node to a given destination node.
	 */
	protected boolean edgeExists( CG cg, long srcid, long dstid) {

		for (CGEdge cgEdge : cg.getEdges()) {

			ASTNode srcNode = cgEdge.getSource().getASTNode();
			ASTNode dstNode = cgEdge.getDestination().getASTNode();

			assert srcNode instanceof CallExpressionBase;
			assert dstNode instanceof FunctionDef;

			if( srcNode.getNodeId().equals( srcid) && dstNode.getNodeId().equals( dstid))
				return true;
		}

		return false;
	}



	/* ********** */
	/* DEPRECATED */
	/* ********** */

	/* "lower-level" functions that take an AST as two CSV strings instead of a directory name,
	 * and use PHPCFGFactory.convert(ASTNode) instead of AST2CFGConverter.convert(FunctionDef) */

	/**
	 * Creates and returns an AST for two given CSV strings (nodes and edges),
	 * and returns the AST node with the lowest id.
	 */
	@Deprecated
	protected ASTNode getASTForCSVLines(String nodeLines, String edgeLines)
			throws IOException, InvalidCSVFile
	{
		handleCSVLines(nodeLines, edgeLines);

		return ast.getNodeWithLowestId();
	}

	/**
	 * Creates an AST for two given CSV strings and computes a CFG.
	 */
	@Deprecated
	protected CFG getCFGForCSVLines(String nodeLines, String edgeLines)
			throws IOException, InvalidCSVFile {

		ASTNode node = getASTForCSVLines(nodeLines, edgeLines);
		CFG cfg = PHPCFGFactory.convert(node);

		System.out.println();
		System.out.println("CFG (" + node + ")\n~~~");
		System.out.println(cfg);

		return cfg;
	}

	/**
	 * Creates an AST for two given CSV strings and computes a UDG.
	 */
	@Deprecated
	protected UseDefGraph getUDGForCSVLines(String nodeLines, String edgeLines)
			throws IOException, InvalidCSVFile
	{
		CFG cfg = getCFGForCSVLines(nodeLines, edgeLines);
		return getUDGForCFG(cfg);
	}

	/**
	 * Creates an AST for two given CSV strings and computes a DDG.
	 */
	@Deprecated
	protected DDG getDDGForCSVLines(String nodeLines, String edgeLines)
			throws IOException, InvalidCSVFile
	{
		CFG cfg = getCFGForCSVLines(nodeLines, edgeLines);
		return getDDGForCFG(cfg);
	}

}