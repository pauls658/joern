package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashSet;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import udg.useDefAnalysis.ASTDefUseAnalyzer;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import ast.ASTNode;
import udg.useDefAnalysis.environments.EmitDefAndUseEnvironment;
import udg.useDefGraph.UseOrDef;

public class ArgumentListEnvironment extends EmitDefAndUseEnvironment  
{

	@Override
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		String id = String.valueOf(c.getASTNode().getNodeId());
		if (isDef(child)) {
			for (String symbol : childSymbols) {
				defSymbols.add("@dbr argsymbol " + id + " " + symbol);
			}
		}
		if (isUse(child)) {
			for (String symbol : childSymbols) {
				useSymbols.add("@dbr argsymbol " + id + " " + symbol);
			}
		}
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
		return true;
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		return true;
	}

	@Override
	public void preTraverse(ASTDefUseAnalyzer analyzer) {
		PHPASTDefUseAnalyzer phpAnalyzer = (PHPASTDefUseAnalyzer)analyzer;
		phpAnalyzer.pushArgListId(getNodeId());
	}

	@Override
	public void postTraverse(ASTDefUseAnalyzer analyzer) {
		PHPASTDefUseAnalyzer phpAnalyzer = (PHPASTDefUseAnalyzer)analyzer;
		phpAnalyzer.popArgListId();
	}

}
