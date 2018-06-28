package udg.php.useDefAnalysis.environments;

import udg.ASTProvider;
import udg.useDefAnalysis.environments.EmitUseEnvironment;
import udg.useDefGraph.UseOrDef;
import java.util.Collection;
import java.util.LinkedList;

import udg.useDefAnalysis.ASTDefUseAnalyzer;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;

public class BaseExpressionEnvironment extends EmitUseEnvironment
{
	private boolean analyzingArgList;

	@Override
	public LinkedList<String> upstreamSymbols()
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
	public void preTraverse(ASTDefUseAnalyzer analyzer) {
		PHPASTDefUseAnalyzer phpAnalyzer = (PHPASTDefUseAnalyzer)analyzer;
		this.analyzingArgList = phpAnalyzer.analyzingArgList();
	}
}


