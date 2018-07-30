package udg.php.useDefAnalysis.environments;

import udg.ASTNodeASTProvider;
import udg.ASTProvider;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

import java.util.Collection;
import java.util.LinkedList;

// Brandon

/**
 * Base-class and default implementation of UseDefEnvironment.
 */
public class UseDefEnvironment
{
	// Has the AST node associated with this environment (not the top-level BB node)
	protected ASTProvider astProvider; 
	protected LinkedList<Symbol> symbols = new LinkedList<Symbol>();

	public static final LinkedList<UseOrDef> emptyUseOrDef = new LinkedList<UseOrDef>();
	public static final LinkedList<Symbol> emptySymbolList = new LinkedList<Symbol>();

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
	public LinkedList<Symbol> upstreamSymbols()
	{
		return this.symbols;
	}

	/**
	 * Add all given symbols without exception
	 **/
	public void addChildSymbols(LinkedList<Symbol> childSymbols,
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
			Collection<Symbol> symbols)
	{
		return createDefOrUseForSymbols(symbols, true);
	}

	protected LinkedList<UseOrDef> createUsesForAllSymbols(
			Collection<Symbol> symbols)
	{
		return createDefOrUseForSymbols(symbols, false);
	}

	private LinkedList<UseOrDef> createDefOrUseForSymbols(
			Collection<Symbol> symbols, boolean isDef)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();
		for (Symbol s : symbols)
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
	
	public void preTraverse(PHPASTDefUseAnalyzer analyzer) {
		return;
	}


	public void postTraverse(PHPASTDefUseAnalyzer analyzer) {
		return;
	}

	public Long getNodeId() {
		ASTNodeASTProvider prov = (ASTNodeASTProvider)this.astProvider; // I like to live dangerously...
		return prov.getASTNode().getNodeId();
	}

}
