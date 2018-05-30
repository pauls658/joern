package udg.php.useDefAnalysis.environments;

import java.util.Collection;
import java.util.LinkedList;
import java.util.HashSet;

import udg.ASTProvider;
import udg.ASTNodeASTProvider;
import ast.ASTNode;
import udg.useDefAnalysis.environments.EmitDefAndUseEnvironment;
import udg.useDefGraph.UseOrDef;

public class FunctionCallEnvironment extends EmitDefAndUseEnvironment  
{

	private HashSet<String> nonDefingFunctions;
	private String name;

	public FunctionCallEnvironment(HashSet<String> in, ASTProvider aProv) {
		this.nonDefingFunctions = in;
		ASTNodeASTProvider func = (ASTNodeASTProvider)aProv;
		ASTNode a = func.getASTNode();
		if (a.getProperty("type").equals("AST_CALL")) {
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
		ASTNodeASTProvider c = (ASTNodeASTProvider)child;
		//return c.getASTNode().getProperty("type").equals("AST_ARG_LIST");
		if (this.name == null || !this.nonDefingFunctions.contains(this.name)) {
			// Def all the args
			return c.getASTNode().getProperty("type").equals("AST_ARG_LIST");
		} else {
			// this is a built-in func, and we know it does not def anything
			return false;
		}
	}
	
	@Override
	public boolean isUse( ASTProvider child)
	{
		return true;
	}

}
