package ddg.DataDependenceGraph;

import cfg.nodes.CFGExitNode;

import java.util.HashSet;

public class PHPDDG extends DDG {

	private HashSet<DefUseRelation> artificialRetRelations = new HashSet<DefUseRelation>();

	@Override
	public void add(Object srcId, Object dstId, String symbol)
    {   
        DefUseRelation statementPair = new DefUseRelation(srcId, dstId, symbol);
        getDefUseEdges().add(statementPair);
		if (dstId instanceof CFGExitNode) {
			artificialRetRelations.add(statementPair);
		}
    };

	public boolean isArtificialRelation(DefUseRelation rel) {
		return artificialRetRelations.contains(rel);
	}
}
