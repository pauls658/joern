package tools.php.ast2cpg;

import java.util.*;

import ast.ASTNode;
import cfg.CFG;
import cfg.nodes.ASTNodeContainer;
import cfg.nodes.AbstractCFGNode;
import cfg.nodes.CFGNode;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefAnalysis.environments.UseDefEnvironment;
import udg.useDefGraph.UseDefGraph;
import udg.php.useDefGraph.UseOrDef;
import udg.ASTNodeASTProvider;

import udg.CFGToUDGConverter;

class PHPCFGToUDGConverter extends CFGToUDGConverter {
    private PHPASTDefUseAnalyzer phpAnalyzer;
	private HashSet<String> superglobals = new HashSet<>();

	public PHPCFGToUDGConverter() {
		superglobals.add("_GET");
		superglobals.add("_POST");
		superglobals.add("GLOBALS");
		superglobals.add("_SERVER");
		superglobals.add("_REQUEST");
		superglobals.add("_FILES");
		superglobals.add("_ENV");
		superglobals.add("_COOKIE");
		superglobals.add("_SESSION");
	}

	public void setPHPASTDefUseAnalyzer(PHPASTDefUseAnalyzer analyzer)
	{
		this.phpAnalyzer = analyzer;
	}

	// Iterates over each CFG node and calls traverseAst on the top-level AST node
	@Override
	public PHPUseDefGraph convert(CFG cfg)
    {   
        // Make sure that ASTDefUseAnalyzer was initialized by setLanguage(String)
        if( null == this.phpAnalyzer)
            throw new RuntimeException("Trying to call PHPCFGToUDGConverter.convert(CFG)"
                    + " without calling CFGToUDGConverter.setLanguage(String) first.");

        // Incrementally create a useDefGraph by generating
        // UseOrDefs for each statement separately and adding those
        // to the useDefGraph

        PHPUseDefGraph useDefGraph = new PHPUseDefGraph();

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
				this.phpAnalyzer.BBInit();
                Collection<UseOrDef> usesAndDefs = this.phpAnalyzer.analyzeAST(provider);
				this.phpAnalyzer.BBFinish();

				String nodeType = statementNode.getProperty("type");
				if (nodeType instanceof String && 
					(nodeType.equals("AST_GLOBAL") || nodeType.equals("AST_PARAM"))) {
					if (usesAndDefs.size() != 1) {
						throw new RuntimeException("AST_GLOBAL/PARAM has more than one use/def");
					}
					UseOrDef uod = (UseOrDef) usesAndDefs.toArray()[0];

					if (nodeType.equals("AST_GLOBAL")) {
						if (!uod.isDef) {
							throw new RuntimeException("AST_GLOBAL is using a value");
						}
						useDefGraph.addFuncGlobal(uod.symbol.name);
					} else {
						// AST_PARAM
						useDefGraph.addParam(uod.symbol.name, statementNode);
					}
				}
				useDefGraph.addUseDefsForBlock(statementNode, usesAndDefs);
            }
        }

        String prefix = Long.toString(((AbstractCFGNode)cfg.getEntryNode()).getNodeId()) + "_local";
		makeSymbolsGlobal(useDefGraph, prefix);

		return useDefGraph;
    }

    private String addGlobalPrefix(String prefix, String symbol) {
		// Just in case we didn't copy the Symbol. We don't want to add the prefix twice
		if (!symbol.startsWith(prefix))
			return prefix + "_" + symbol;
		else
			return symbol;
	}

	// Determines which variables are non-global, and therefore should be rewritten. Also
	// builds the local
	public void makeSymbolsGlobal(PHPUseDefGraph useDefGraph, String prefix) {
	    for (Map.Entry<Long, LinkedList<UseOrDef>> e : useDefGraph.getUseDefsForBlock().entrySet()) {
			for (UseOrDef uod : e.getValue()) {
				if (!useDefGraph.isGlobalSymbol(uod.symbol.name) && !superglobals.contains(uod.symbol.name)) {
				    String newName = addGlobalPrefix(prefix, uod.symbol.name);
				    if (!useDefGraph.isParamRef(uod.symbol.name))
				        useDefGraph.addLocalSymbol(newName);
					uod.symbol.name = newName;
				}

				// Check if symbol has a variable index, and rewrite if necessary
				if (uod.symbol.isArray &&
					uod.symbol.isIndexVar &&
					!useDefGraph.isGlobalSymbol(uod.symbol.name) &&
					!superglobals.contains(uod.symbol.name)) {
				    String newName = addGlobalPrefix(prefix, uod.symbol.name);
					if (!useDefGraph.isParamRef(uod.symbol.index))
					    useDefGraph.addLocalSymbol(newName);
				    uod.symbol.index = newName;
				}
			}
		}
	}

}
