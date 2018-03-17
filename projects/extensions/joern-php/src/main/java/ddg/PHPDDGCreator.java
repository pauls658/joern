package ddg;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ddg.DataDependenceGraph.PHPDDG;
import ddg.DefUseCFG.DefUseCFG;
import misc.HashMapOfSets;
import ast.ASTNode;

public class PHPDDGCreator extends DDGCreator {
	@Override
	public PHPDDG createDDGFromReachingDefs()
    {   
        PHPDDG ddg = new PHPDDG();

        for (Object statement : cfg.getStatements())
        {   
			if (statement instanceof ASTNode && ((ASTNode)statement).getNodeId() == 740)
				System.out.println("");
            HashSet<Object> inForBlock = in.getListForKey(statement);
            if (inForBlock == null)
                continue;
            List<Object> usedSymbols = cfg.getSymbolsUsed()
                    .get(statement);
            if (usedSymbols == null)
                continue;

            for (Object d : inForBlock)
            {   
                Definition def = (Definition) d;

                if (usedSymbols.contains(def.identifier))
                    ddg.add(def.statement, statement, def.identifier);
            }   
        }   

        return ddg;
    } 
}
