package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;

import udg.ASTNodeASTProvider;
import udg.ASTProvider;
import udg.php.useDefAnalysis.Symbol;
import udg.php.useDefGraph.UseOrDef;

import udg.php.useDefAnalysis.PHPASTDefUseAnalyzer;

public class ArrayIndexingEnvironment extends UseDefEnvironment
{
	private Collection<Symbol> useSymbols = new LinkedList<Symbol>();
	PHPASTDefUseAnalyzer phpAnalyzer;
	private int ourDepth;
	private Symbol arraySymbol;

	private boolean emitUse = false;

	private boolean wasBottomDim = false;
	
	// pass the 'code' of the array upstream (i.e., the array's name)
	// by recursion, this is already contained in the symbols field
	@Override
	public LinkedList<Symbol> upstreamSymbols()
	{	
		return symbols;
	}

	private boolean isBottomDim() {
		if (this.phpAnalyzer.maxDimDepth == this.ourDepth) {
			wasBottomDim = true;
		}
		return wasBottomDim;
	}
	
	public void addChildSymbols( LinkedList<Symbol> childSymbols, ASTProvider child)
	{
		if (((ASTNodeASTProvider)this.astProvider).getASTNode().getNodeId() == 138556)
			System.out.print("");
		if (this.isBottomDim()) {
			// this is the bottom most array dimension,
			// need to add extra info to the array name
			if (child.getChildNumber() == 0) {
				assert childSymbols.size() == 1;
				// Save the array symbol til we know more about the index
				this.arraySymbol = childSymbols.get(0);
				this.arraySymbol.isArray = true;

				// need the dimension before we can add it to the symbols
			} else {
				if (child.getTypeAsString().equals("StringExpression")) {
					// Great! constant array index
					this.arraySymbol.setIndex(child.getEscapedCodeStr());
					this.arraySymbol.isIndexVar = false;
				} else if (child.getTypeAsString().equals("Variable")) {
					// ok... variable index... maybe we can resolve it later
					assert childSymbols.size() == 1;
					this.arraySymbol.setIndex(childSymbols.get(0).name);
					this.arraySymbol.isIndexVar = true;
				} else {
					// Bollocks! unknown access
					this.arraySymbol.setIndex(null);
				}
				// We got the array name in the last step
				// At this point, we are ready to report symbols
				// The array name will always go into the upstream symbols.
				// If we have something in the array index, then the symbol
				// will be reported with the array name. Otherwise, report
				// the use symbols.
				if (this.arraySymbol.index == null) {
					// we did not resolve the index, so we pass these
					// up as uses. TODO: only if not analyzing arg list
					this.useSymbols.addAll(childSymbols);
				}
				// Regardless, always indicate this is any array symbol
				this.symbols.add(this.arraySymbol);
			}
		} else {
			// not the bottom-most. We only handle the first dimension
			// If we are analyzing an arg list, we always want to pass the symbols upstream
			if( isUse( child) && !phpAnalyzer.analyzingArgList()) { // index
				useSymbols.addAll( childSymbols);
			} else { // the name
				symbols.addAll(childSymbols);
			}
		}
	}

	public Collection<UseOrDef> useOrDefsFromSymbols(ASTProvider child)
	{
		LinkedList<UseOrDef> retval = new LinkedList<UseOrDef>();

		if( isUse( child))
			retval.addAll(createUsesForAllSymbols(useSymbols));

		// if we are analyzing a standalone array access, then the
		// array's name should also be emitted as USE
		if( this.emitUse)
			retval.addAll(createUsesForAllSymbols(upstreamSymbols()));
		
		return retval;
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		int childNum = child.getChildNumber();
		return 1 == childNum;
	}
	
	public void setEmitUse( boolean emitUse) {
		this.emitUse = emitUse;
	}

	@Override
	public void preTraverse(PHPASTDefUseAnalyzer analyzer) {
		this.phpAnalyzer = (PHPASTDefUseAnalyzer)analyzer;
		this.phpAnalyzer.dimDepth++;
		this.ourDepth = phpAnalyzer.dimDepth;
		this.phpAnalyzer.maxDimDepth++;
	}

	@Override
	public void postTraverse(PHPASTDefUseAnalyzer analyzer) {
		this.phpAnalyzer.dimDepth--;
		assert this.phpAnalyzer.dimDepth >= 0;
		if (this.phpAnalyzer.dimDepth == 0) {
			// We reached the top level array dimension
			this.phpAnalyzer.maxDimDepth = 0;
		}
	}
}
