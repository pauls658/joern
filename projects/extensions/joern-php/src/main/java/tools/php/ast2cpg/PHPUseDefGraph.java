package tools.php.ast2cpg;

import udg.useDefGraph.UseDefGraph;
import ast.ASTNode;
import udg.useDefGraph.UseOrDef;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collection;
import java.util.Iterator;

class PHPUseDefGraph extends UseDefGraph {
	/* maps literal global name to its AST_GLOBAL node. IDK why this is a linked list... */
	HashMap<String, LinkedList<ASTNode>> globalNameToBB = new HashMap<String, LinkedList<ASTNode>>();

	HashMap<String, LinkedList<ASTNode>> globalNameToDefBB = new HashMap<String, LinkedList<ASTNode>>();
	LinkedList<Tuple<String, ASTNode>> globalDefs;
	public HashMap<String, ASTNode> nameToParamNode = new HashMap<String, ASTNode>();

	public void addNameBBMapping(String globalName, ASTNode BB) {
		LinkedList<ASTNode> BBs = globalNameToBB.get(globalName);
		if (BBs == null) {
			BBs = new LinkedList<ASTNode>();
			globalNameToBB.put(globalName, BBs);
		}
		BBs.add(BB);
	}

	public void addDefBBMapping(Collection<UseOrDef> symbols, ASTNode BB) {
		Iterator<UseOrDef> it = symbols.iterator();
		while (it.hasNext()) {
			UseOrDef cur = it.next();
			if (cur.isDef) {
				LinkedList<ASTNode> BBs = globalNameToDefBB.get(cur.symbol);
				if (BBs == null) {
					BBs = new LinkedList<ASTNode>();
					globalNameToDefBB.put(cur.symbol, BBs);
				}
				BBs.add(BB);
			}
		}		
	}

	public HashMap<String, LinkedList<ASTNode>> getMap() {
		return globalNameToBB;
	}

	public HashMap<String, LinkedList<ASTNode>> getDefMap() {
		return globalNameToDefBB;
	}

	public void setGlobalDefs(LinkedList<Tuple<String, ASTNode>> globalDefs) {
		this.globalDefs = globalDefs;
	}

	public LinkedList<Tuple<String, ASTNode>> getGlobalDefs() {
		return this.globalDefs;
	}

	public void addParamASTNode(String identifier, ASTNode param) {
		nameToParamNode.put(identifier, param);
	}

}
