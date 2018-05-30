package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashSet;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import ast.ASTNode;
import udg.useDefAnalysis.environments.EmitDefAndUseEnvironment;
import udg.useDefGraph.UseOrDef;

public class ArgumentListEnvironment extends EmitDefAndUseEnvironment  
{

	@Override
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		String id = String.valueOf(c.getASTNode().getNodeId());
		if (isDef(child)) {
			for (String symbol : childSymbols) {
				defSymbols.add("@dbr argsymbol " + id + " " + symbol);
			}
		}
		if (isUse(child)) {
			for (String symbol : childSymbols) {
				useSymbols.add("@dbr argsymbol " + id + " " + symbol);
			}
		}
	}

	@Override
	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		retval.addAll(createDefsForAllSymbols(defSymbols));
		retval.addAll(createUsesForAllSymbols(useSymbols));

		return retval;
	}

	@Override
	public LinkedList<String> upstreamSymbols()
	{
		return symbols;
	}

	@Override
	public boolean isDef( ASTProvider child)
	{
		return true;
		/**
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		if (this.name == null || !this.nonDefingFunctions.contains(this.name)) {
			// Def all the args
			return c.getASTNode().getProperty("type").equals("AST_ARG_LIST");
		} else {
			// this is a built-in func, and we know it does not def anything
			return false;
		}
		*/
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		return true;
	}

}
