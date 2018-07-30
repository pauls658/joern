package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashSet;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import ast.ASTNode;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

public class ArgumentListEnvironment extends EmitDefAndUseEnvironment  
{

	@Override
	public void addChildSymbols(LinkedList<Symbol> childSymbols,
			ASTProvider child)
	{
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		Long id = c.getASTNode().getNodeId();
		for (Symbol symbol : childSymbols) {
			symbol.isArg = true;
			symbol.argId = id;
			useSymbols.add(symbol);
		}
	}

	@Override
	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		retval.addAll(createUsesForAllSymbols(useSymbols));

		return retval;
	}

	@Override
	public LinkedList<Symbol> upstreamSymbols()
	{
		return emptySymbolList; // TODO: Return empty symbol list since we always emit our symbols?
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
	public void preTraverse(PHPASTDefUseAnalyzer analyzer) {
		analyzer.pushArgListId(getNodeId());
	}

	@Override
	public void postTraverse(PHPASTDefUseAnalyzer analyzer) {
		analyzer.popArgListId();
	}

}
