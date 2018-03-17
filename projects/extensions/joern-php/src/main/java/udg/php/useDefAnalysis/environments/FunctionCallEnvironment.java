package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.useDefAnalysis.environments.EmitDefAndUseEnvironment;
import udg.useDefGraph.UseOrDef;

public class FunctionCallEnvironment extends EmitDefAndUseEnvironment  
{

	@Override
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		defSymbols.addAll(childSymbols);
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

}
