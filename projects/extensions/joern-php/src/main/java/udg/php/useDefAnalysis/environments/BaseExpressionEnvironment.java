package udg.php.useDefAnalysis.environments;

import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;
import java.util.Collection;
import java.util.LinkedList;

import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;

public class BaseExpressionEnvironment extends EmitUseEnvironment
{
	private boolean analyzingArgList;

	@Override
	public LinkedList<Symbol> upstreamSymbols()
	{
		if (analyzingArgList) {
			symbols.addAll(useSymbols);
		}
		return symbols;
	}

	@Override
	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		if (analyzingArgList) {
			return emptyUseOrDef;
		} else {
			LinkedList<UseOrDef> retval = createUsesForAllSymbols(useSymbols);
			return retval;
		}
	}

	@Override
	public void preTraverse(PHPASTDefUseAnalyzer phpAnalyzer) {
		this.analyzingArgList = phpAnalyzer.analyzingArgList();
	}
}


