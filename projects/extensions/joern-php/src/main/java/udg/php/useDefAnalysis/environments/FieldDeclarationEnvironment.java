package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

public class FieldDeclarationEnvironment extends UseDefEnvironment
{
	private Collection<Symbol> defSymbols = new LinkedList<Symbol>();
	private Collection<Symbol> useSymbols = new LinkedList<Symbol>();
	
	public void addChildSymbols( LinkedList<Symbol> childSymbols, ASTProvider child)
	{
		this.symbols.addAll( childSymbols);

		// the left child is a StringExpression containing the field's name
		if( isDef( child))
			defSymbols.add( new Symbol(child.getEscapedCodeStr()));
		// the right side may contain symbols that are USE'd to determine the
		// value assigned to the field
		if( isUse( child))
			useSymbols.addAll( childSymbols);
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
