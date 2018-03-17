package outputModules.csv.exporters;

import java.util.HashMap;
import java.util.Map;

import ast.ASTNode;
import databaseNodes.EdgeKeys;
import databaseNodes.EdgeTypes;
import ddg.DataDependenceGraph.PHPDDG;
import ddg.DataDependenceGraph.DefUseRelation;
import outputModules.common.DDGExporter;
import outputModules.common.Writer;

import cfg.nodes.AbstractCFGNode;

public class PHPCSVDDGExporter extends CSVDDGExporter {
	public void writeDDGEdges(PHPDDG ddg, HashMap<String, ASTNode> symbolToParamNode) {
    
        Map<String, Object> properties = new HashMap<String, Object>();
    
        for( DefUseRelation ddgEdge : ddg.getDefUseEdges()) {
   			Long srcId, dstId; 
			if (ddgEdge.src instanceof ASTNode)
				srcId = ((ASTNode)ddgEdge.src).getNodeId();
			else if (ddgEdge.src instanceof AbstractCFGNode)
				srcId = ((AbstractCFGNode)ddgEdge.src).getNodeId();
			else
				continue;

			if (ddgEdge.dst instanceof ASTNode)
				dstId = ((ASTNode)ddgEdge.dst).getNodeId();
			else if (ddgEdge.dst instanceof AbstractCFGNode)
				dstId = ((AbstractCFGNode)ddgEdge.dst).getNodeId();
			else
				continue;
    
            Writer.setIdForObject(ddgEdge.src, srcId);
            Writer.setIdForObject(ddgEdge.dst, dstId);
			if (ddg.isArtificialRelation(ddgEdge)) {
				properties.put("childnum", symbolToParamNode.get(ddgEdge.symbol).getProperty("childnum"));
			}
            properties.put( EdgeKeys.VAR, ddgEdge.symbol);
            addDDGEdge(ddgEdge, properties);
			properties.remove("childnum");
        }   
        // clean up
        Writer.reset();
    }   
}
