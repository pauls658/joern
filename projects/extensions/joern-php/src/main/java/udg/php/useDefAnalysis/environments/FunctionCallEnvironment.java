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

public class FunctionCallEnvironment extends EmitDefAndUseEnvironment  
{

	private HashSet<String> nonDefingFunctions;
	private String name;
	private String type;
	private Long Id;

	public FunctionCallEnvironment(HashSet<String> in, ASTProvider aProv) {
		this.nonDefingFunctions = in;
		ASTNodeASTProvider func = (ASTNodeASTProvider)aProv;
		ASTNode a = func.getASTNode();
		this.Id = a.getNodeId();
		this.type = a.getProperty("type");
		if (this.type.equals("AST_CALL")) {
			a = a.getChild(0); // the name
			if (a.getProperty("type").equals("AST_NAME")) {
				a = a.getChild(0);
				this.name = a.getProperty("code");
			}
		} else {
			// always def args
			this.name = null;
		}
	}

	@Override
	public void addChildSymbols(LinkedList<String> childSymbols,
			ASTProvider child)
	{
		if (isDef(child))
			defSymbols.addAll(childSymbols);
		if (isUse(child))
			useSymbols.addAll(childSymbols);
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
		// def/uses of args happen in the arg list environment
		return false;
		/**
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		if (this.name == null || !this.nonDefingFunctions.contains(this.name)) {
			// Def all the args
			return c.getASTNode().getProperty("type").equals("AST_ARG_LIST");
		} else {
			// this is a built-in func, and we know it does not def anything
			return false;
		}
		*/
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		// If we do field sensitive analysis and this is a method call, we may want
		// to output "use" for childnum == 0 (the reference variable). Currently,
		// we do field-based analysis so we can skip this.
		// If this is a function reference (function call using a variable), we don't
		// care because we don't consider functions to be tainted.
		return false;
	}

	@Override
	public void postTraverse(ASTDefUseAnalyzer analyzer) {
		PHPASTDefUseAnalyzer phpAnalyzer = (PHPASTDefUseAnalyzer)analyzer;
		phpAnalyzer.addToCallOrder(this.Id);
	}
}
