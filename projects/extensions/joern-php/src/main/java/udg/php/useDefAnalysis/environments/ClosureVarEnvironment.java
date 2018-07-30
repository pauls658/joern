package udg.php.useDefAnalysis.environments;

import java.util.LinkedList;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;

public class ClosureVarEnvironment extends UseDefEnvironment
{

	// pass the 'code' of the closure variable upstream (i.e., the closure variable's name)
	@Override
	public LinkedList<Symbol> upstreamSymbols()
	{	
		// A ClosureVar has exactly one StringExpression child whose code string contains
		// the variable's name.
		String code = astProvider.getChild(0).getEscapedCodeStr();
		symbols.add(new Symbol(code));
		return symbols;
	}
	
	// a ClosureVar has only one StringExpression child, and it should not be traversed
	@Override
	public boolean shouldTraverse(ASTProvider child)
	{
		return false;
	}
}
