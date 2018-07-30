package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

public class IncDecEnvironment extends UseDefEnvironment
{
	private Collection<Symbol> defSymbols = new LinkedList<Symbol>();
	private Collection<Symbol> useSymbols = new LinkedList<Symbol>();
	
	public void addChildSymbols( LinkedList<Symbol> childSymbols, ASTProvider child)
	{
		this.symbols.addAll( childSymbols);
			
		// additionally collect defs and uses symbols in separate lists,
		// to be used for useOrDefsFromSymbols
		if( isDef( child))
			defSymbols.addAll( childSymbols);
		if( isUse( child))
			useSymbols.addAll( childSymbols);
	}

	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		// We make the isDef/isUse checks "pro forma" here to keep in line
		// with the normal design of environments. However, in this
		// environment it is clear that (1) there is always exactly one child symbol
		// and (2) it is both a use and a def child.
		
		if( isDef( child))
			retval.addAll(createDefsForAllSymbols(defSymbols));

		if( isUse( child))
			retval.addAll(createUsesForAllSymbols(useSymbols));

		return retval;
	}
	
	@Override
	public boolean isUse(ASTProvider child)
	{
		return true;
	}

	@Override
	public boolean isDef(ASTProvider child)
	{
		return true;
	}
}
