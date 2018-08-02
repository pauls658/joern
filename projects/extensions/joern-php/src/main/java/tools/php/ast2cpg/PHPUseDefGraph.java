package tools.php.ast2cpg;

import udg.useDefGraph.UseDefGraph;
import ast.ASTNode;
import udg.php.useDefGraph.UseOrDef;

import java.util.*;

class PHPUseDefGraph extends UseDefGraph {

	private HashSet<String> funcGlobals = new HashSet<String>();
	// Params that are pass by ref (not-killable)
	private HashSet<String> paramRefs = new HashSet<String>();
	// Params that are pass by val (killable)
	private HashSet<String> paramVals = new HashSet<String>();
	// Variables local to this function
    private HashSet<String> localSymbols = new HashSet<>();
	// A list of all defs and uses for a specific block
	// Key is the blocks ID
	private HashMap<Long, LinkedList<UseOrDef>> useDefsForBlock = new HashMap<>();

	private void addUoDForId(Long id, UseOrDef uod) {
		LinkedList<UseOrDef> val = useDefsForBlock.get(id);
		if (val == null) {
			val = new LinkedList<UseOrDef>();
			useDefsForBlock.put(id, val);
		}
		val.add(uod);
	}

	public void addUseDefsForBlock(ASTNode statementNode, Collection<UseOrDef> uods) {
		for (UseOrDef uod : uods) {
			if (uod.symbol.isArg) {
				addUoDForId(uod.symbol.argId, uod);
			} else {
				addUoDForId(statementNode.getNodeId(), uod);
			}
		}
	}

	public HashMap<Long, LinkedList<UseOrDef>> getDefUseMap() {
		return useDefsForBlock;
	}

	public void addFuncGlobal(String g) {
		this.funcGlobals.add(g);
	}

	public void addParam(String p, ASTNode n) {
		String flags = n.getProperty("flags");
		if (flags instanceof String && flags.contains("PARAM_REF"))
			this.paramRefs.add(p);
		else
		    this.paramVals.add(p);
	}

	public void addLocalSymbol(String s) {
		localSymbols.add(s);
	}

	public HashSet<String> getLocalSymbols() {
		return localSymbols;
	}

	public boolean isParamRef(String p) {
		return paramRefs.contains(p);
	}

	public boolean isGlobalSymbol(String symbol) {
		return funcGlobals.contains(symbol);
	}

}
