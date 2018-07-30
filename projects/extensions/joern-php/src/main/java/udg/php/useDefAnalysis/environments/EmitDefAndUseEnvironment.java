package udg.php.useDefAnalysis.environments;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

import java.util.Collection;
import java.util.LinkedList;

public class EmitDefAndUseEnvironment extends UseDefEnvironment
{

	protected Collection<Symbol> defSymbols = new LinkedList<Symbol>();
	protected Collection<Symbol> useSymbols = new LinkedList<Symbol>();

	public void addChildSymbols(LinkedList<Symbol> childSymbols,
			ASTProvider child)
	{
		if (isDef(child))
			defSymbols.addAll(childSymbols);
		if (isUse(child))
			useSymbols.addAll(childSymbols);
	}

	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		if (isDef(child))
			retval.addAll(createDefsForAllSymbols(defSymbols));

		if (isUse(child))
			retval.addAll(createUsesForAllSymbols(useSymbols));

		return retval;
	}

	public LinkedList<Symbol> upstreamSymbols()
	{
		return emptySymbolList;
	}

}
