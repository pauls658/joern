package tools.php.ast2cpg;

import java.util.*;

import ast.ASTNode;
import cfg.CFG;
import cfg.nodes.ASTNodeContainer;
import cfg.nodes.AbstractCFGNode;
import cfg.nodes.CFGNode;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefAnalysis.environments.UseDefEnvironment;
import udg.useDefGraph.UseDefGraph;
import udg.php.useDefGraph.UseOrDef;
import udg.ASTNodeASTProvider;

import udg.CFGToUDGConverter;

class PHPCFGToUDGConverter extends CFGToUDGConverter {
    private PHPASTDefUseAnalyzer phpAnalyzer;
    // Name of PHP super global arrays
	private HashSet<String> superglobals = new HashSet<>();
	// Maps literal strings to an ID for ease of output encoding
	private HashMap<String, Long> constantMap;
	private Long constantCounter;

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
		constantMap = new HashMap<>();
		constantCounter = 0l;
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

        translateLiteralStrings(useDefGraph);

		return useDefGraph;
    }

	private String translateLiteral(String index) {
	    Long constId = constantMap.get(index);
	    if (constId == null) {
	    	constId = constantCounter;
	    	constantCounter++;
	    	constantMap.put(index, constId);
		}
		return Long.toString(constId);
	}

	public HashMap getConstantMap() {
		return constantMap;
	}

    /*
    Translates the literal strings of the use def graph to integers for ease of encoding
     */
	private void translateLiteralStrings(PHPUseDefGraph useDefGraph) {
	    for (Map.Entry<Long, LinkedList<UseOrDef>> e : useDefGraph.getDefUseMap().entrySet()) {
	    	for (UseOrDef uod : e.getValue()) {
	    		if (uod.symbol.isArray && uod.symbol.arrayType == Symbol.ARRAY_CONST_INDEX) {
	    			uod.symbol.index = Symbol.indexPrefix + "_" + translateLiteral(uod.symbol.index);
				} // either no index, or variable index. either case we don't rewrite
			}
		}
	}

	private String addGlobalPrefix(String prefix, String symbol) {
		// Just in case we didn't copy the Symbol. We don't want to add the prefix twice
		assert !symbol.startsWith(prefix);
		return prefix + "_" + symbol;
	}

	// Determines which variables are non-global, and therefore should be rewritten. Also
	// builds the local
	public void makeSymbolsGlobal(PHPUseDefGraph useDefGraph, String prefix) {
	    for (Map.Entry<Long, LinkedList<UseOrDef>> e : useDefGraph.getDefUseMap().entrySet()) {
			for (UseOrDef uod : e.getValue()) {
				if (!useDefGraph.isGlobalSymbol(uod.symbol.origName) && !superglobals.contains(uod.symbol.origName)) {
				    String newName = addGlobalPrefix(prefix, uod.symbol.name);
				    if (!useDefGraph.isParamRef(uod.symbol.origName))
				        useDefGraph.addLocalSymbol(newName);
					uod.symbol.name = newName;
				}

				// Check if symbol has a variable index, and rewrite if necessary
				if (uod.symbol.isArray &&
					uod.symbol.isIndexVar &&
					!useDefGraph.isGlobalSymbol(uod.symbol.origIndex) &&
					!superglobals.contains(uod.symbol.origIndex)) {
				    String newName = addGlobalPrefix(prefix, uod.symbol.index);
					if (!useDefGraph.isParamRef(uod.symbol.origIndex))
					    useDefGraph.addLocalSymbol(newName);
				    uod.symbol.index = newName;
				}
			}
		}
	}

}
