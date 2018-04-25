package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import udg.useDefAnalysis.environments.EmitDefAndUseEnvironment;
import udg.useDefGraph.UseOrDef;

public class FunctionCallEnvironment extends EmitDefAndUseEnvironment  
{

	@Override
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		if (isDef(child))
			defSymbols.addAll(childSymbols);
		if (isUse(child))
			useSymbols.addAll(childSymbols);
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
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		return c.getASTNode().getProperty("type").equals("AST_ARG_LIST");
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		return true;
	}

}
