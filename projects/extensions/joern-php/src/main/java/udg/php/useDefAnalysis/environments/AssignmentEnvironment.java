package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

public class AssignmentEnvironment extends UseDefEnvironment
{
	private Collection<Symbol> defSymbols = new LinkedList<Symbol>();
	private Collection<Symbol> useSymbols = new LinkedList<Symbol>();
	
	public void addChildSymbols( LinkedList<Symbol> childSymbols, ASTProvider child)
	{
		if( isDef( child))
			defSymbols.addAll( childSymbols);
		if( isUse( child)) {
			this.symbols.addAll(childSymbols);
			useSymbols.addAll(childSymbols);
		}
	}

	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		if( isDef( child))
			retval.addAll(createDefsForAllSymbols(defSymbols));

		if( isUse( child))
			retval.addAll(createUsesForAllSymbols(useSymbols));

		return retval;
	}
	
	@Override
	public boolean isDef( ASTProvider child)
	{
		int childNum = child.getChildNumber();
		return 0 == childNum;
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		int childNum = child.getChildNumber();
		return 1 == childNum;
	}
}
