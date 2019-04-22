import java.nio.file.Files;

class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();
    
    public HashSet handleableFunctions = [
		"json_encode"
	];
    
    def functionCall(Neo4jVertex v) {
    	if (g.V(v.id()).out('CALLS').toList().size() != 0) {
    		return true;
    	} else {
    		def name = g.V(v.id()).has('type', 'AST_CALL') // we only have handlers for builtin funcs, which are always AST_CALL's
    		.out('PARENT_OF').has('childnum', 0).out('PARENT_OF').values('code')[0]
    		def res = name in handleableFunctions;
			return res
    	}
    }
    
    def binaryOp(Neo4jVertex v) {
    	def flags = g.V(v.id()).values('flags').toList()[0];
    	return flags.contains("BINARY_CONCAT");
    }
    
    def assignOp(Neo4jVertex v) {
    	def flags = g.V(v.id()).values('flags').toList()[0];
    	return flags.contains("BINARY_CONCAT");
    }
    
    def canHandleArtAST(Neo4jVertex v) {
    	switch (v.value('type')) {
			case "return":
				def c = g.V(v.id()).out("ASSOC").next();
				return functionCall(c);
			default:
				def c = g.V(v.id()).out("CALL_ID").next();
				return functionCall(c);
		}
	}

    def canHandleAST(Neo4jVertex v) {
    	switch (v.value('type')) {
    		case "AST_CALL":
    		case "AST_STATIC_CALL":
    		case "AST_NEW":
			case "AST_METHOD_CALL":
    			return [functionCall(v), false];
    
    		case "AST_BINARY_OP":
    			return [binaryOp(v), false];
    
    		case "AST_ASSIGN_OP":
    			return [assignOp(v), false];
    
    		case "AST_ASSIGN":
    		case "AST_ASSIGN_REF":
    		case "AST_FOREACH":
    		case "AST_FOR":
    		case "AST_DO_WHILE":
    		//case "AST_GLOBAL": we can't be sure about type of global yet
    		case "AST_VAR":
    		case "AST_RETURN": 
    		case "AST_PARAM":
    		case "AST_PROP":
    		case "AST_PROP_DECL":
    		case "AST_CLASS":
    		case "AST_ENCAPS_LIST":
			case "AST_ARG_LIST":
    		case "AST_ARRAY":
    		case "AST_NAME":
    		case "NULL":
    		case "string":
				return [true, false];
    		case "AST_ECHO":
				return [true, false];
    		default:
    			return [false, false];
    	}
    }
   
	class Canary {
		public Boolean dead;
		Canary() {
			dead = false;
		}

		Canary kill() {
			dead = true;
			return this;
		}
	}

	public HashSet<String> stopTypes = [
		"AST_STMT_LIST",
		"AST_TOPLEVEL",
		"AST_CLASS"
	]

	def getVarPaths(id) {
		g\
		.V(id)\
		repeat(
			out('PARENT_OF').has("type", without(stopTypes))
		).until(has("type", "AST_VAR").out('PARENT_OF').has("type", "string")).path().toList()

	}

    def canHandle(BB) {
		if (BB.value("type") in stopTypes)
			return false;

		if ("ART_AST" in BB.labels())
			return canHandleArtAST(BB);

		def var_paths = getVarPaths(BB.id());
		for (path in var_paths) {
			for (v in path) {
				def stop, handleable;
				(handleable, stop) = canHandleAST(v);
				if (!handleable) {
					//println "Could not handle type: " + v.value("type")
					return false;
				}
				if (stop) break;
			}
		}
		return true;
    }

	def getChildren(node) {
		def children = []
		children.addAll(node.vertices(Direction.OUT, "PARENT_OF"))
		return children
	}

	def getCallNodeFromArtArg(artifBB) {
		def type = artifBB.value("type")
		switch (type) {
			case "arg_entry":
			case "arg_exit":
				return artifBB.vertices(Direction.OUT, "CALL_ID")[0]
			case "return":
				return artifBB.vertices(Direction.OUT, "ASSOC")[0]
			default:
				return null
		}
	}

	// method calls where non of args or returns are
	// ctrl tainted
	public HashSet methodCallWhitelist = [
		"fetch1",
		"query"
	]

	public HashSet functionCallWhiteList = [
		"is_object","is_array",	"is_string", "defined", "define", "dirname", "gettype", "trim", "count", "substr", "strlen", "explode", "preg_split", "strtoupper", "strtolower", "array_change_key_case", "stripslashes", "preg_quote", "htmlspecialchars", "sizeof", "mb_convert_encoding", "is_scalar", "implode", "reset", "get_object_vars", "floor", "sprintf", "rtrim", "strtotime", "is_integer", "date", "mktime", "urlencode", "asort", "is_a", "get_class", "intval", "base64_encode", "mb_substr", "mb_strlen", "parse_url", "strtotime", "array_splice", "array_shift", "strrev", "preg_match"
	]

	def secondArgIsNonEmptyString(callNode) {
		def secondArg = g.V(callNode.id()).out("PARENT_OF").has("type", "AST_ARG_LIST").out("PARENT_OF").has("childnum", 1).next()
		if (secondArg.value("type").equals("string")) {
			return !secondArg.value("code").equals("");
		} else if (secondArg.value("type").equals("AST_ARRAY")) {
			def children = getChildren(secondArg);
			for (n in children) {
				def e = n.vertices(Direction.OUT, "PARENT_OF")[0]
				if (!e.value("type").equals("string")) {
					return false
				} else if (e.value("code").equals("")) {
					return false
				}
			}
			return true
		} else {
			return false
		}
	}

	def getStaticCallName(callNode) {
		def staticNameNode = g.V(callNode.id()).out("PARENT_OF").has("childnum", 0).out("PARENT_OF").next()
		if (staticNameNode.value("type").equals("string"))
			return staticNameNode.value("code")
		else
			return ""
	}

	def isCtrlTaintedBuiltin(artifBB) {
		def callNode = getCallNodeFromArtArg(artifBB)
		def callType = callNode.value("type")
		def callName = null;

		if ("name" in callNode.keys())
			callName = callNode.value("name")
		else
			return false // statically unknown method call

		switch (callType) {
			case "AST_METHOD_CALL":
				return !(callName in methodCallWhitelist)
			case "AST_CALL":
				if (callName.equals("str_replace")) {
					return !secondArgIsNonEmptyString(callNode)
				} else if (callName.equals("preg_replace")) {
					return !secondArgIsNonEmptyString(callNode)
				}
				return !(callName in functionCallWhiteList)
			case "AST_STATIC_CALL":
				def staticClassName = getStaticCallName(callNode)
				if (staticClassName.equals("PEAR") && callName.equals("raiseError"))
					return false
				else
					return true
			case "AST_NEW":
				return true
		}
	}

	def isCtrlTainted(bb) {
		def type = bb.value("type")
		switch (type) {
			case "AST_ASSIGN":
			case "AST_ASSIGN_REF":
				def rhs = g.V(bb.id()).out("PARENT_OF").has("childnum", 1).next()
				return isCtrlTainted(rhs)
			case "AST_ARRAY":
			case "AST_EXPR_LIST":
				def children = getChildren(bb)
				for (child in children) {
					if (isCtrlTainted(child))
						return true
				}
				return false
			// cases where the node has one AST child
			// and it doesn't affect anything
			case "AST_UNSET":
			case "AST_CAST":
			case "AST_RETURN":
			case "AST_ECHO":
			case "AST_PRINT":
			case "AST_POST_DEC":
			case "AST_POST_INC":
			case "AST_PRE_DEC":
			case "AST_PRE_INC":
			case "AST_UNARY_OP":
				def child = getChildren(bb)[0]
				return isCtrlTainted(child)
			// cases with two children
			// where we want to process each child
			case "AST_DIM": // $a[$i]
			case "AST_BINARY_OP": 
			case "AST_PROP": // $o->$p
			case "AST_ASSIGN_OP":
			case "AST_ARRAY_ELEM": // part of array( "key" => $var, ... )
				def children = getChildren(bb)
				return isCtrlTainted(children[0]) && isCtrlTainted(children[1])

			case "AST_CONDITIONAL": // $v = $b ? $x : $y;
				def condition = g.V(bb).out("PARENT_OF").has("childnum", 0).next()
				def s1 = g.V(bb).out("PARENT_OF").has("childnum", 1).next()
				def s2 = g.V(bb).out("PARENT_OF").has("childnum", 2).next()
				return isCtrlTainted(s1) && isCtrlTainted(s2) && (getBranchType(condition) == 1 || getBranchType(condition) == 0)
			// actual handling for function calls
			// for exit and entry, 
			case "arg_entry":
			case "arg_exit":
				if (g.V(bb.id()).out("CALL_ID").out("CALLS").toList().size() > 0) {
					return false
				} else {
					return isCtrlTaintedBuiltin(bb)
				}
			case "return":
				if (g.V(bb.id()).out("ASSOC").out("CALLS").toList().size() > 0) {
					return false
				} else {
					return isCtrlTaintedBuiltin(bb)
				}
			case "AST_VAR":
			case "integer":
			case "string":
			case "AST_CONST":
				return false;
			case "AST_CALL":
			case "AST_STATIC_CALL":
			case "AST_METHOD_CALL":
			case "AST_NEW":
				// the call node is not considered part of the CFG.
				// If this function will cause control tainting, we
				// will mark it when we process the artificial return
				// node
				return false;
			// obvious/not interesting cases go here
			case "AST_ISSET":
			case "AST_ENCAPS_LIST":
			case "AST_PARAM":
			case "CFG_FUNC_ENTRY":
			case "CFG_FUNC_EXIT":
			case "AST_GLOBAL":
			case "AST_INCLUDE_OR_EVAL":
			case "NULL":
			case "AST_EXIT":
			case "AST_FOREACH":
				return false
			default:
				return true;
		}
	}

	def labelHandleableAndCtrlTainted() {
		def bbs = 
		g.V().\
		where(
			or(
				or(__.in('FLOWS_TO'), __.in('INTERPROC')),
				or(out('FLOWS_TO'), out('INTERPROC'))
			)\
		).toList();
		for (bb in bbs) {
			if (canHandle(bb)) {
				g.V(bb.id()).property('handleable', true).next()
			} else {
				g.V(bb.id()).property('handleable', false).next()
			}

			// maybe should consider assign in stmt
			// if BB is AST_CALL, it is a call where return is ignored
			//if ("branch" in bb.keys() && (bb.value("branch") == true) || bb.value("type").equals("AST_CALL")) { 
				g.V(bb.id()).property('ctrltainted', false).next()
			//} else if (isCtrlTainted(bb)) {
			//	g.V(bb.id()).property('ctrltainted', true).next()
			//} else {
			//	g.V(bb.id()).property('ctrltainted', false).next()
			//}
		}
	}

	// use findCtrlDefSrc
	//def findCtrlTainted(ccfgId) {
	//	def actualId = g.V().hasLabel("CCFG").has("id", ccfgId).next().id()
	//	g.V(actualId).repeat(__.in("REACHES").simplePath()).until(has("ctrltainted", true)).limit(1).path()
	//}

	def findNodeLoc(node) {
		if (node instanceof java.lang.Long)
			node = g.V(node).next()

		if (!("lineno" in node.keys())) {
			println "No line number for " + node.value("type") + " node:"
			println node
			return
		}
		def lineno = node.value("lineno")
		def fid = (int)node.value("funcid")
		def fileName = g.V(fid).\
			until(and(has("type", "AST_TOPLEVEL"), 
			__.filter{ "flags" in it.get().keys() && "TOPLEVEL_FILE" in it.get().value("flags") })).\
			repeat(__.in("PARENT_OF")).toList()[0].value("name")

 		ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/vi", fileName, "+" + lineno);
 		processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
 		processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
 		processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

 		Process p = processBuilder.start();
		// wait for termination.
		p.waitFor();

	}

	public HashSet typeZeroBuiltInFuncs = [
		"is_array",
		"is_object",
		"is_string",
		"is_bool",
		"is_numeric",
		"is_float",
		"is_integer",
		"is_string",
		"is_object",
		"function_exists",
		"trim",
		"strlen",
		"count",
		"is_a",
		"each",
		"file_exists"
	]

	def getFuncType(node) {
		// check if have implementation
		if (node.edges(Direction.OUT, "CALLS")[0] != null) {
			// we have implementation, so this will be a type 1
			// branch
			return 0
		} else {
			def funcName = node.value("name")
			
			if (typeZeroBuiltInFuncs.contains(funcName))
				return 0
			else if (typeZeroBuiltInFuncs.contains(funcName))
				return 0
			else if ((funcName.equals("stristr") || funcName.equals("strstr")) && secondArgIsNonEmptyString(node))
				return 0
			else
				return 2
		}
	}

	public HashSet logicalConnectives = [
		"BINARY_BOOL_AND",
		"BINARY_BOOL_OR",
		"BINARY_BOOL_XOR"
	]
	public HashSet logicalEquality = [
		"BINARY_IS_EQUAL",
		"BINARY_IS_NOT_EQUAL",
		"BINARY_IS_IDENTICAL",
		"BINARY_IS_NOT_IDENTICAL",
		"BINARY_IS_SMALLER",
		"BINARY_IS_SMALLER_OR_EQUAL",
		"BINARY_IS_GREATER",
		"BINARY_IS_GREATER_OR_EQUAL"
	]

	

	def getConstName(constNode) {
		return g.V(constNode.id()).out("PARENT_OF").out("PARENT_OF").properties("code").next().value()
	}

	public HashSet isVarFuncWhitelist = [
		"strtolower",
		"strtoupper",
		"substr",
		"count",
		"trim"
	]

	def isVar(node) {
		def type = node.value("type")
		switch (type) {
			case "AST_UNARY_OP":
				return isVar(getChildren(node)[0])
			case "AST_CALL":
				if (node.value("name") in isVarFuncWhitelist || g.V(node.id()).out("CALLS").toList().size() > 0)
					return true
				else if (node.value("name") in ["strstr","stristr"] && secondArgIsNonEmptyString(node))
					return true
				else
					return false
			case "AST_VAR":
			case "AST_PROP":
			case "AST_DIM":
				return true;
			default:
				return false;
		}
	}

	def isEmpty(node) {
		if (node.value("type").equals("AST_UNARY_OP")) {
			return isEmpty(getChildren(node)[0])
		}

		if (node.value("type").equals("string")) {
			return node.value("code").equals("")
		} else if (node.value("type").equals("AST_CONST")) {
			def constName = getConstName(node).toLowerCase()
			return (constName in ["true", "false", "null"])
		} else if (node.value("type").equals("integer")) {
			return node.value("code").equals("0")
		}
		return false
	}

	// checks if a predicate is of the form:
	// -   $var == ""
    // -   $var == {false,true,0}
	def isEmptyComparison(node) {
		def hasEmptyString = false, hasVar = false
		def children = getChildren(node)
		if( !(children[0] != null && children[1] != null)) {
			println node.value("flags")
			return false
		}
		if (isVar(children[0])) {
			hasVar = true
		} else if (isEmpty(children[0])){
			hasEmptyString = true
		}

		if (isVar(children[1])) {
			hasVar = true
		} else if (isEmpty(children[1])) {
			hasEmptyString = true
		}

		return hasEmptyString && hasVar
	}

	def constTypes = [
		"string",
		"AST_CONST",
		"integer",
		"NULL"
	]
	def isConst(node) {
		if (node.value("type").equals("AST_UNARY_OP")) {
			return isConst(getChildren(node)[0])
		}
		return node.value("type") in constTypes
	}

	def isConstComparsion(node) {
		def hasConstString = false, hasVar = false
		def children = getChildren(node)
		if( !(children[0] != null && children[1] != null)) {
			println node.value("flags")
			return false
		}
		if (isVar(children[0])) {
			hasVar = true
		} else if (isConst(children[0])){
			hasConstString = true
		}
		if (isVar(children[1])) {
			hasVar = true
		} else if (isConst(children[1])){
			hasConstString = true
		}
		return hasConstString && hasVar
	}

	def getSwitchBranchType(node) {
		return g.V(node.id()).out("PARENT_OF").has("type", "AST_SWITCH_LIST").out("PARENT_OF").out("PARENT_OF").has("childnum", 0).filter{ !isConst(it.get()) && !(it.get().value("type").equals("AST_CALL") && it.get().value("name") in typeZeroBuiltInFuncs) }.hasNext() ? 2 : 0
	}
	
	def getBranchType(node) {
		// special case 
		def parent = g.V(node.id()).in("PARENT_OF").next()
		if (parent.value("type").equals("AST_SWITCH")) {
			return getSwitchBranchType(parent)
		}
		switch(node.value("type")) {
			case "AST_FOREACH":
				return 0;
			case "AST_EXPR_LIST":
				def children = getChildren(node)
				def ret = 0
				for (child in children) {
					ret = Integer.max(ret, getBranchType(child))
				}
				return ret
			case "AST_UNARY_OP":
				def child = node.vertices(Direction.OUT, "PARENT_OF")[0]
				return getBranchType(child)
			case "AST_BINARY_OP":
				def flag = node.value("flags")
				def hasAssign = g.V(node.id()).out("PARENT_OF").has("type", "AST_ASSIGN").toList().size() > 0
				if (logicalConnectives.any{flag.contains(it)} || hasAssign) {
					def children = getChildren(node)
					return Integer.max(getBranchType(children[0]), getBranchType(children[1]))
				} else if (logicalEquality.any{flag.contains(it)}) {
					return (isEmptyComparison(node) || isConstComparsion(node) ? 0 : 2)
				} else {
					return 2
				}
			case "AST_ASSIGN":
				def rhs = g.V(node.id()).out("PARENT_OF").has("childnum", 1).next()
				return getBranchType(rhs)
			case "AST_EMPTY":
			case "AST_ISSET":
				return 0
			case "AST_CALL":
			case "AST_STATIC_CALL":
			case "AST_METHOD_CALL":
				// in branches, call node is the "branch" in the CFG
				return getFuncType(node)
			case "AST_VAR": // if ($var)
			case "AST_PROP": // if ($o->var)
			case "AST_DIM":
			case "AST_CONST":
				return 0;
			default:
				return 2;
		}
	}

	def callAnalysis() {
		// first label the things that give a var a type:
		def tassigns = g.V().has("type", "AST_ASSIGN")\
				.where(and(
					out("PARENT_OF").has("childnum", 0).has("type", "AST_VAR"),
					out("PARENT_OF").has("childnum", 1).has("type", "AST_NEW"))).toList()
		for (n in tassigns) {
			def typeName = g.V(n.id()).out("PARENT_OF").has("childnum", 1).next().value("name")
			g.V(n.id()).property("givesType", typeName).next();
		}
		
		def methodCalls = g.V().has("type", "AST_METHOD_CALL").where(out("CALLS").count().is(gt(1))).toList()
		//def methodCalls = [g.V(221174).next()]
		for (c in methodCalls) {
			def var = g.V(c.id()).in("CALL_ID").has("childnum", -1).next().value("uses")
			def defs = g.V().has("defs", var).toList()
			//println "var: " + var + ", defs" + defs
			// if all defs for this var give a type, and there is only one type, then we can resolve the call
			def varType = null
			for (d in defs) {
				if ("givesType" in d.keys()) {
					if (varType == null) {
						varType = d.value("givesType")
					} else {
						varType = null
						break
					}
				} else {
					varType = null
					break
				}
			}

			// remove outgoing interproc edges that do not match the type
			if (varType != null) {
				//println "Found vartype for: " + c.value("name") + " at lineno " + c.value("lineno")
				// make sure we find at least one implementation... otherwise we probably did something wrong
				if (g.V(c.id()).out("CALLS").has("classname", varType).toList().size() > 0) {
					println "Removing extra call edges for: " + c.value("name") + " at lineno " + c.value("lineno")
					g.V(c.id()).in("CALL_ID").outE("INTERPROC").where(__.not(inV().has("classname", varType))).drop().toList()
				}
			}
		}
	}

	def labelBranches() {
		def branchNodes = g.V().match(__.as('a').out('FLOWS_TO').as('b'),
						__.as('a').out('FLOWS_TO').as('c'), 
						where('b', neq('c'))).\
					select('a').dedup().toList()
		def unsafeBranches = []
		for (node in branchNodes) {
			def branchType = getBranchType(node)
			g.V(node.id()).property('branch', branchType).next()
			if (branchType == 2)
				unsafeBranches += node
		}
		return unsafeBranches
	}

	ArrayList nodeIds = new ArrayList()
	int curNode = 0;
	int prevNode = 0;
	def loadNodeIds() {
		def fpath = "/home/brandon/joern/projects/extensions/joern-php/src/datalog/tbranches"
		File f = new File(fpath)
		def lines = f.readLines()
		for (l in lines) {
			nodeIds += l.toInteger()
		}
	}

	def openToNextNode() {
		findNodeLoc(g.V(nodeIds[curNode]).next())
		prevNode = nodeIds[curNode]
		curNode++;
	}

	def reachableBranches(n) {
		def visited = new HashSet();
		def branches = new HashSet();
		def branchesOrder = []
		def work = []
		def rets = []
		work.addAll(n)
		while (!work.isEmpty()) {
			def cur = work.pop()
			if (cur in visited) continue
			visited.add(cur)
			def adj = g.V(cur).out("REACHES").toList()
			for (v in adj) {
				def origNode = g.V(v.value("orig_id")).next()
				if (origNode.value("type").equals("AST_RETURN")) {
					rets += v.id()
				}
				if ("branch" in origNode.keys()) {
					if (!(origNode.id() in branches))
						branchesOrder += origNode.id()
					branches.add(origNode.id())
				}
				work += v.id()
			}
		}
		return [branches, branchesOrder]
	}

	def findTheBranches(origID) {
		def copiedIDs = g.V().has("orig_id", origID).in("CTRLDEP").id().toList()
		return reachableBranches(copiedIDs)
	}
	
	def addTypesToCCFG() {
		def ccfgNodes = g.V().hasLabel("CCFG").toList()
		for (n in ccfgNodes) {
			def type = g.V(n.value("orig_id")).values("type").next()
			g.V(n.id()).property("type", type).next()
		}
	}

	def findDataflowPath(start, end) {
		def visited = new HashSet();
		return g.V(start).repeat(out("REACHES").not(has("type", "AST_FOREACH")).filter{ !(it.get().id() in visited) }.sideEffect{ visited.add(it.get().id()) }).until(has("orig_id", end)).path().limit(1).next()
	}

	def findCtrlDefSrc(start) {
		def ccfgNodes = g.V().hasLabel("CCFG").has("orig_id", start).id().toList()
		for (n in ccfgNodes) {
			def visited = new HashSet()
			visited.clear()
			try {
				return g.V(n).repeat(__.in("REACHES").simplePath().filter{ !(it.get().id() in visited) }.sideEffect{ visited.add(it.get().id()) }).until(has("ctrltainted", true)).limit(1).path().next()
			} catch (NoSuchElementException e) {

			}
		}
	}

	def test() {
		loadNodeIds()
		nodeIds = nodeIds.findAll{ getBranchType(g.V(it).next()) == 2 }
	}

}

o = new G()
if (args.size() == 0) {
	o.labelBranches()
	o.labelHandleableAndCtrlTainted()
	o.callAnalysis()
	o.g.graph.tx().commit()
	o.g.graph.close()
}
