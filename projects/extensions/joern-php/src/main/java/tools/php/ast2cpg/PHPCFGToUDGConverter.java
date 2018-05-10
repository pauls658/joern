package tools.php.ast2cpg;

import java.util.Collection;
import java.util.HashSet;

import ast.ASTNode;
import cfg.CFG;
import cfg.nodes.ASTNodeContainer;
import cfg.nodes.CFGNode;
import udg.useDefAnalysis.ASTDefUseAnalyzer;
import udg.useDefGraph.UseDefGraph;
import udg.useDefGraph.UseOrDef;
import udg.ASTNodeASTProvider;

import udg.CFGToUDGConverter;

import java.util.LinkedList;
import java.util.HashMap;

class PHPCFGToUDGConverter extends CFGToUDGConverter {

	@Override
	public PHPUseDefGraph convert(CFG cfg)
    {   
        // Make sure that ASTDefUseAnalyzer was initialized by setLanguage(String)
        if( null == getASTDefUseAnalyzer())
            throw new RuntimeException("Trying to call PHPCFGToUDGConverter.convert(CFG)"
                    + " without calling CFGToUDGConverter.setLanguage(String) first.");

        // Incrementally create a UseDefGraph by generating
        // UseOrDefs for each statement separately and adding those
        // to the UseDefGraph

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
                Collection<UseOrDef> usesAndDefs = getASTDefUseAnalyzer()
                        .analyzeAST(provider);
				String nodeType = statementNode.getProperty("type");
				if (nodeType instanceof String && 
					(nodeType.equals("AST_GLOBAL") || nodeType.equals("AST_PARAM")))
				{
					if (usesAndDefs.size() != 1) {
						throw new RuntimeException("AST_GLOBAL/PARAM has more than one use/def");
					}
					UseOrDef uod = (UseOrDef) usesAndDefs.toArray()[0];

					if (nodeType.equals("AST_GLOBAL")) {
						if (!uod.isDef) {
							throw new RuntimeException("AST_GLOBAL is using a value");
						}
						useDefGraph.addNameBBMapping(uod.symbol, statementNode);
					} else {
						// AST_PARAM
						useDefGraph.addParamASTNode(uod.symbol, statementNode);
					}
				} else {
					// Used for later consideration
					useDefGraph.addDefBBMapping(usesAndDefs, statementNode);
				}

                addToUseDefGraph(useDefGraph, usesAndDefs, statementNode);
            } 
        }   

		LinkedList<Tuple<String, ASTNode>> globalDefs = new LinkedList<Tuple<String, ASTNode>>();
		HashMap<String, LinkedList<ASTNode>> defsMap = useDefGraph.getDefMap();
		for (String globalName : useDefGraph.getMap().keySet()) {
			LinkedList<ASTNode> BBs = defsMap.get(globalName);

			if (BBs == null) continue;

			for (ASTNode BB : BBs) {
				globalDefs.add(
						new Tuple<String, ASTNode>(globalName, BB)
						);
			}
		}

		useDefGraph.setGlobalDefs(globalDefs);
		return useDefGraph;
    }   

}
