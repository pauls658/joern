package udg.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import ast.ASTNode;
import udg.useDefGraph.UseOrDef;

// Brandon
import udg.useDefAnalysis.ASTDefUseAnalyzer;

/**
 * Base-class and default implementation of UseDefEnvironment.
 */
public class UseDefEnvironment
{
	// Has the AST node associated with this environment (not the top-level BB node)
	protected ASTProvider astProvider; 
	protected LinkedList<String> symbols = new LinkedList<String>();

	public static final LinkedList<UseOrDef> emptyUseOrDef = new LinkedList<UseOrDef>();
	public static final LinkedList<String> emptySymbolList = new LinkedList<String>();

	public boolean isUse(ASTProvider child)
	{
		return false;
	}

	public boolean isDef(ASTProvider child)
	{
		return false;
	}

	/**
	 * Decides whether a given child node should be traversed by ASTDefUseAnalyzer.
	 */
	public boolean shouldTraverse(ASTProvider child)
	{
		return true;
	}

	public void setASTProvider(ASTProvider anASTProvider)
	{
		this.astProvider = anASTProvider;
	}

	public ASTProvider getASTProvider()
	{
		return astProvider;
	}

	/**
	 * Propagate all symbols to upstream
	 */
	public LinkedList<String> upstreamSymbols()
	{
		return this.symbols;
	}

	/**
	 * Add all given symbols without exception
	 **/
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		symbols.addAll(childSymbols);
	}

	/**
	 * By default, don't generate any uses or defs for symbols.
	 */
	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		return emptyUseOrDef;
	}

	/* Utilities below */

	protected LinkedList<UseOrDef> createDefsForAllSymbols(
			Collection<String> symbols)
	{
		return createDefOrUseForSymbols(symbols, true);
	}

	protected LinkedList<UseOrDef> createUsesForAllSymbols(
			Collection<String> symbols)
	{
		return createDefOrUseForSymbols(symbols, false);
	}

	private LinkedList<UseOrDef> createDefOrUseForSymbols(
			Collection<String> symbols, boolean isDef)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();
		for (String s : symbols)
		{
			UseOrDef useOrDef = new UseOrDef();
			useOrDef.isDef = isDef;
			useOrDef.symbol = s;
			useOrDef.astProvider = this.astProvider;
			retval.add(useOrDef);
		}
		return retval;
	}

	// Brandon
	
	public void preTraverse(ASTDefUseAnalyzer analyzer) {
		return;
	}


	public void postTraverse(ASTDefUseAnalyzer analyzer) {
		return;
	}

	public Long getNodeId() {
		ASTNodeASTProvider prov = (ASTNodeASTProvider)this.astProvider; // I like to live dangerously...
		return prov.getASTNode().getNodeId();
	}

}
