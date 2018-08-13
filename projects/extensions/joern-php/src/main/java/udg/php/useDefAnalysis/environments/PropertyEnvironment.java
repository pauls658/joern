package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTProvider;
import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

public class PropertyEnvironment extends UseDefEnvironment
{
	private boolean emitUse = false;
	private PHPASTDefUseAnalyzer phpAnalyzer;
	private int ourDepth;
	private Collection<Symbol> useSymbols = new LinkedList<>();

	private boolean isTopProp() {
		return ourDepth == 1;
	}

	@Override
	public LinkedList<Symbol> upstreamSymbols() {
		if (this.astProvider.getChild(1).getTypeAsString().equals("StringExpression")) {
			Symbol s = new Symbol(Symbol.fieldPrefix + "_" +
					this.astProvider.getChild(1).getEscapedCodeStr());
			s.isField = true;
			s.star = true;
			this.symbols.add(s);
		} else {
			System.out.println("Statically unknown property!");
		}
		return this.symbols;
	}

	public void addChildSymbols(LinkedList<Symbol> childSymbols, ASTProvider child)
	{
	    this.useSymbols.addAll(childSymbols);
	}

	// If we are the top AST_PROP, we should report our field symbol upstream. Otherwise,
	// the field symbol will be reported as a use immediately in addChildSymbols
	/*@Override
	public LinkedList<Symbol> upstreamSymbols()
	{
		if (isTopProp() && this.astProvider.getChild(1).getTypeAsString().equals("StringExpression")) {
			Symbol s = new Symbol(Symbol.fieldPrefix + "_" +
					this.astProvider.getChild(1).getEscapedCodeStr());
			s.isField = true;
			s.star = true;
			this.symbols.add(s);
		} else if (!this.astProvider.getChild(1).getTypeAsString().equals("StringExpression")) {
			System.out.println("Statically unknown property!");
		}
		return this.symbols;
	}
	
	// add the *object's name* of the property access expression to the child symbols
	public void addChildSymbols(LinkedList<Symbol> childSymbols, ASTProvider child)
	{
	    //if (((ASTNodeASTProvider)this.astProvider).getASTNode().getNodeId() == 210)
		//	System.out.print("");
		int childNum = child.getChildNumber();
		if (!isTopProp() && childNum == 1 &&
				child.getTypeAsString().equals("StringExpression")) {
			Symbol s = new Symbol(Symbol.fieldPrefix + "_" + child.getEscapedCodeStr());
			s.isField = true;
			s.star = true;
			this.useSymbols.add(s);
		} else {
			// If we are the top property, we will report our symbol upstream
			// when we call upstreamSymbols() with the parent env (or if emitUse
			// is set.
			this.useSymbols.addAll(childSymbols);
		}
	}*/

	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = createUsesForAllSymbols(this.useSymbols);
		if (this.emitUse && child.getChildNumber() == 1)
			retval.addAll(createUsesForAllSymbols(upstreamSymbols()));
		return retval;
	}

	public void setEmitUse( boolean emitUse) {
		this.emitUse = emitUse;
	}

	@Override
	public void preTraverse(PHPASTDefUseAnalyzer analyzer) {
		this.phpAnalyzer = analyzer;
		this.phpAnalyzer.propDepth++;
		this.ourDepth = phpAnalyzer.propDepth;
	}

	@Override
	public void postTraverse(PHPASTDefUseAnalyzer analyzer) {
	    this.phpAnalyzer.propDepth = 0;
	}

}
