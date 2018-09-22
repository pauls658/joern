package udg;

import java.util.Collection;
import java.util.HashSet;

import ast.ASTNode;
import cfg.CFG;
import cfg.nodes.ASTNodeContainer;
import cfg.nodes.CFGNode;
import udg.useDefAnalysis.ASTDefUseAnalyzer;
import udg.useDefGraph.UseDefGraph;
import udg.useDefGraph.UseOrDef;

public class CFGToUDGConverter
{
	private ASTDefUseAnalyzer astAnalyzer;

	public UseDefGraph convert(CFG cfg)
	{
		// Make sure that ASTDefUseAnalyzer was initialized by setLanguage(String)
		if( null == this.astAnalyzer)
			throw new RuntimeException("Trying to call CFGToUDGConverter.convert(CFG)"
					+ " without calling CFGToUDGConverter.setLanguage(String) first.");

		// Incrementally create a useDefGraph by generating
		// UseOrDefs for each statement separately and adding those
		// to the useDefGraph

		UseDefGraph useDefGraph = new UseDefGraph();

		Collection<CFGNode> statements = cfg.getVertices();

		for (CFGNode cfgNode : statements)
		{
			// skip empty blocks
			if (cfgNode instanceof ASTNodeContainer)
			{
				ASTNode statementNode = ((ASTNodeContainer) cfgNode)
						.getASTNode();
				ASTNodeASTProvider provider = new ASTNodeASTProvider();
				provider.setNode(statementNode);
				Collection<UseOrDef> usesAndDefs = astAnalyzer
						.analyzeAST(provider);
				addToUseDefGraph(useDefGraph, usesAndDefs, statementNode);
			}
		}

		return useDefGraph;
	}

	// After finishing a block, this function is called on the
	// list of generated defs/uses with the top-level statement
	// node and the useDefGraph
	protected void addToUseDefGraph(UseDefGraph useDefGraph,
			Collection<UseOrDef> usesAndDefs, ASTNode statementNode)
	{
		HashSet<String> insertedForStatementDef = new HashSet<String>();
		HashSet<String> insertedForStatementUse = new HashSet<String>();

		for (UseOrDef useOrDef : usesAndDefs)
		{

			ASTNodeASTProvider astProvider = (ASTNodeASTProvider) useOrDef.astProvider;
			// CHECK?
			ASTNode useOrDefNode = astProvider.getASTNode(); // the node that reported the def/use

			if (useOrDef.isDef)
			{

				if (!insertedForStatementDef.contains(useOrDef.symbol))
				{
					useDefGraph.addDefinition(useOrDef.symbol, statementNode);
					insertedForStatementDef.add(useOrDef.symbol);
				}

				if (useOrDefNode != null && useOrDefNode != statementNode)
					useDefGraph.addDefinition(useOrDef.symbol, useOrDefNode);
			}
			else
			{

				if (!insertedForStatementUse.contains(useOrDef.symbol))
				{
					useDefGraph.addUse(useOrDef.symbol, statementNode);
					insertedForStatementUse.add(useOrDef.symbol);
				}

				// Add use-links from AST nodes to symbols
				if (useOrDef.astProvider != null
						&& useOrDefNode != statementNode)
					useDefGraph.addUse(useOrDef.symbol, useOrDefNode);
			}
		}
	}

	public void setASTDefUseAnalyzer(ASTDefUseAnalyzer analyzer)
	{
		this.astAnalyzer = analyzer;
	}

	public ASTDefUseAnalyzer getASTDefUseAnalyzer() {
		return this.astAnalyzer;
	}
}
