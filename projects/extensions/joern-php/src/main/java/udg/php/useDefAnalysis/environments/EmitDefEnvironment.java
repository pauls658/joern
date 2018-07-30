package udg.php.useDefAnalysis.environments;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

import java.util.Collection;
import java.util.LinkedList;

// emit all symbols as DEF and don't hand
// anything upstream.

public class EmitDefEnvironment extends UseDefEnvironment
{

	protected Collection<Symbol> defSymbols = new LinkedList<Symbol>();

	public void addChildSymbols(LinkedList<Symbol> childSymbols,
			ASTProvider child)
	{
		defSymbols.addAll(childSymbols);
	}

	public LinkedList<Symbol> upstreamSymbols()
	{
		// empty, unless a child-class adds something
		return symbols;
	}
	
	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		return createDefsForAllSymbols(defSymbols);
	}
}
