import java.nio.file.Files;

class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();
    
    public HashSet handleableFunctions = [
		"json_encode",
		"str_repeat"
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
    	return flags.contains("BINARY_CONCAT")
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
    			return [functionCall(v), true];
    
    		case "AST_BINARY_OP":
				if (isEmptyComparison(v))
					return [true, true]
				else
    				return [binaryOp(v), false];
    
    		case "AST_ASSIGN_OP":
    			return [assignOp(v), false];
    
    		case "AST_ASSIGN":
				return [true, false]
    		case "AST_ASSIGN_REF":
    		case "AST_FOREACH":
    		case "AST_FOR":
    		case "AST_DO_WHILE":
    		//case "AST_GLOBAL": we can't be sure about type of global yet
    		case "AST_VAR":
			case "AST_DIM":
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
			out('PARENT_OF')
		).until(has("type", "AST_VAR").out('PARENT_OF').has("type", "string")).out("PARENT_OF").path().toList()

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
		children.addAll(g.V(node).out("PARENT_OF").order().by("childnum").toList())
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
		"is_object", "is_null", "is_array",	"is_string", "defined", "define", "dirname", "gettype", "trim", "count", "substr", "strlen", "explode", "preg_split", "strtoupper", "strtolower", "array_change_key_case", "stripslashes", "preg_quote", "htmlspecialchars", "sizeof", "mb_convert_encoding", "is_scalar", "implode", "reset", "get_object_vars", "floor", "sprintf", "rtrim", "strtotime", "is_integer", "is_int", "date", "mktime", "urlencode", "asort", "is_a", "get_class", "intval", "base64_encode", "mb_substr", "mb_strlen", "parse_url", "strtotime", "array_splice", "array_shift", "strrev", "preg_match"
	]

	def nthArgIsNonEmptyString(callNode, arg) {
		def secondArg = g.V(callNode.id()).out("PARENT_OF").has("type", "AST_ARG_LIST").out("PARENT_OF").has("childnum", arg).next()
		if (secondArg.value("type").equals("string")) {
			return !secondArg.value("code").equals("");
		} else if (secondArg.value("type").equals("AST_ARRAY")) {
			return g.V(secondArg).repeat(out("PARENT_OF"))\
			.until(
				or(
					__.not(has("type", "string")),
					and(has("type", "string"), has("code", ""))
				)
			).hasNext()
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
					return !nthArgIsNonEmptyString(callNode, 1)
				} else if (callName.equals("preg_replace")) {
					return !nthArgIsNonEmptyString(callNode, 1)
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
			case "AST_BINARY_OP": // TODO: there is a problem here
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

	def constPropagatingFuncs = [
		"strtoupper",
		"strtolower"
	]

	def bbDefsConst(bb) {
		def type = bb.value("type")
		switch(type) {
			case "AST_ASSIGN":
				return g.V(bb).has("type", "AST_ASSIGN")\
						.out("PARENT_OF").has("childnum", 1)\
						.filter{ it && isConst(it.get()) }.hasNext()
			default:
				return false
		}
	}

	def bbPropagatesConst(bb) {
		def type = bb.value("type")
		switch(type) {
			case "AST_ASSIGN":
				def childs = getChildren(bb)
				if (childs[1].value("type") in ["AST_VAR", "AST_DIM"])
					return true
				else if ("name" in childs[1].keys() && childs[1].value("name") in constPropagatingFuncs)
					return true
				else
					return false
			case "return":
				return g.V(bb.value("call_id")).values("name").next() in constPropagatingFuncs
			default:
				return false
		}
	}

	def constTransform(bb) {
		def type = bb.value("type")
		switch (type) {
			case "AST_ASSIGN":
				def childs = getChildren(bb)
				def lhsVar = g.V(childs[0]).out("PARENT_OF").next()
				if (lhsVar.value("type").equals("string")) {
					def varName = lhsVar.value("code")
					if (childs[1].value("type").equals("AST_CALL")) {
						if (childs[1].value("name") in constPropagatingFuncs) {
							return g.V(childs[1]).out("PARENT_OF")\
							.has("type", "AST_ARG_LIST").out("PARENT_OF").out("PARENT_OF")\
							.has("code", varName).hasNext()
						} else { return false; }
					} else { return false; }
				} else { return false; }

			default:
				return false
		}
	}

	def constMap = [:]
	def nodesForDef = [:]
	def usesForNode = [:]

	def makeConstMap() {
		def bbs = 
		g.V().\
		where(
			or(
				or(__.in('FLOWS_TO'), __.in('INTERPROC')),
				or(out('FLOWS_TO'), out('INTERPROC'))
			)\
		).toList();
		for(bb in bbs) {
			if (constTransform(bb)) continue

			g.V(bb).property("defsConst", bbDefsConst(bb)).next()
			def defs = "defs" in bb.keys() ? bb.value("defs").split(";") : []
			def uses = "uses" in bb.keys() ? bb.value("uses").split(";") : []
			for (d in defs) {
				d = d.replace("*", "").split("\\[")[0]
				if (!(d in nodesForDef))
					nodesForDef[d] = []
				nodesForDef[d].add(bb.id())
				constMap[d] = false
			}
			usesForNode[bb.id()] = []
			for (u in uses) {
				u = u.replace("*", "").split("\\[")[0]					
				usesForNode[bb.id()].add(u)
			}

 			g.V(bb).property("propagatesConst", bbPropagatesConst(bb)).next()
		}

		def isC = true
		for (d in constMap.keySet()) {
			isC = true
			for (defspot in nodesForDef[d]) {
				if (!g.V(defspot).next().value("defsConst")) {
					isC = false; break;
				}
			}
			constMap[d] = isC
		}

		def change = true
		while (change) {
			change = false
			for (d in constMap.keySet()) {
				if (constMap[d]) continue

				isC = true
				for (defspot in nodesForDef[d]) {
					// check if we assign a const here
					if (g.V(defspot).next().value("defsConst")) continue

					// check if all uses at this spot are const
					for (use in usesForNode[defspot])
						if (!constMap[use]) {
							isC = false
							break
						}
				}
				change |= !(constMap[d] == isC)
				constMap[d] = isC
			}
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
		"is_null",
		"is_bool",
		"is_numeric",
		"is_float",
		"is_integer",
		"is_int",
		"version_compare",
		"is_string",
		"is_object",
		"is_readable",
		"is_dir",
		"mkdir",
		"mb_internal_encoding",
		"unserialize",
		"fopen",
		"feof",
		"fgets",
		"headers_sent",
		"sizeof",
		"copy",
		"function_exists",
		"trim",
		"strlen",
		"count",
		"extension_loaded",
		"is_a",
		"each",
		"file_exists",
		"array_shift",
		"array_pop",
		"strtolower"
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
			else if ((funcName in ["stristr", "strstr", "strpos", "substr_count", "in_array"]) && nthArgIsNonEmptyString(node, 1))
				return 0
			else if ((funcName in ["preg_match", "preg_match_all"]) && nthArgIsNonEmptyString(node, 0)) // regex constant
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

	public HashSet logicalGtLt = [
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
		"trim",
		"strlen"
	]

	/*
	This function is called "isVar", but really it tells us if the non-constant side
	of an equality comparison is acceptable.
	*/
	def isVar(node) {
		def type = node.value("type")
		switch (type) {
			case "AST_UNARY_OP":
				return isVar(getChildren(node)[0])
			case "AST_CALL":
			case "AST_METHOD_CALL":
			case "AST_STATIC_CALL":
				if (node.value("name") in isVarFuncWhitelist || g.V(node.id()).out("CALLS").toList().size() > 0)
					return true
				else if (node.value("name") in ["strstr","stristr"] && nthArgIsNonEmptyString(node, 1))
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
		"NULL",
		"AST_ARRAY_ELEM"
	]

	def isConst(node) {
		if (node.value("type").equals("AST_UNARY_OP")) {
			return isConst(getChildren(node)[0])
		}
		if (node.value("type").equals("AST_ARRAY")) {
			return !g.V(node).repeat(out("PARENT_OF"))\
			.until(__.not(has("type", within(constTypes))))\
			.hasNext()
		}
		if (node.value("type") in constTypes)
			return true
		if (node.value("type").equals("AST_VAR")) {
			def varName = g.V(node).repeat(out("PARENT_OF")).until(has("type", "string")).next().value("code")
			def bb = g.V(node).repeat(__.in("PARENT_OF")).until(filter{ "BB" in it.get().labels() }).next()
			def fullVarName = null
			for (u in usesForNode[bb.id()]) {
				if (u.contains(varName) && !u.matches("^[0-9]*(_actual)?_ret")) {
					fullVarName = u
				}
			}
			if (!fullVarName)
				return false
			else
				return constMap[fullVarName]
		}
		return false
	}

	def isIntComparison(node) {
		def uses = "uses" in node.keys() ? node.value("uses").split(";") : []
		for (use in uses) {
			// check if along all paths, one of the vars is always used as an integer
			def allPathsInt =  g.V(node)\
			.repeat(
				__.in("FLOWS_TO")\
				.where(
					__.not(and(
						out("PARENT_OF").has("type", "AST_BINARY_OP")\
						.filter{ it && it.get().value("flags").contains("BINARY_ADD") },
						filter{ ("uses" in it.get().keys() && it.get().value("uses").contains(use)) || ("defs" in it.get().keys() && it.get().value("defs").contains(use))}
					))
				)
			).times(20).hasNext()
			if (allPathsInt) return true
		}
		return false
	}

	def isConstComparsion(node) {
		def hasConstString = false, hasVar = false
		def children = getChildren(node)
		if( !(children[0] != null && children[1] != null)) {
			println node.value("flags")
			return false
		}

		return (isVar(children[0]) && isConst(children[1]) ||
				isVar(children[1]) && isConst(children[0]))
	}

	public HashSet countingFunction = [
		"count",
		"strlen"
	]

	def isCountComparison(node) {
		return g.V(node.id()).out("PARENT_OF").has("type", "AST_CALL").has("name", within(countingFunction)).hasNext()
	}

	def getSwitchBranchType(node) {
		return g.V(node.id()).out("PARENT_OF").has("type", "AST_SWITCH_LIST").out("PARENT_OF").out("PARENT_OF").has("childnum", 0).filter{ !isConst(it.get()) && !(it.get().value("type").equals("AST_CALL") && it.get().value("name") in typeZeroBuiltInFuncs) }.hasNext() ? 2 : 0
	}

	def getForLoopType(node) {
		def exprList = g.V(node.id()).out("PARENT_OF").has("childnum", 0).next()
		def exprs = g.V(exprList.id()).out("PARENT_OF").order().by("childnum").toList()
		if (exprs.size() != 1) 
			return 2

		if (exprs[0].value("type").equals("AST_ASSIGN")) {
			// the initialization is a single assignment
			def loopVar = g.V(exprs[0].id()).out("PARENT_OF").has("childnum", 0).out("PARENT_OF").toList()
			if (loopVar.size() == 1 && loopVar[0].value("type").equals("string")) {
				// the LHS is a variable. get it's name
				def loopVarName = loopVar[0].value("code")
				def childs = getChildren(node)
				// check if we init the loop var to a counting func, and then perform a comparison on it
				def hasCtFunc = g.V(exprs[0].id()).out("PARENT_OF").has("name", within(countingFunction)).hasNext()
				def checksLen = g.V(childs[1]).out("PARENT_OF")\
				.has("type", "AST_BINARY_OP").where(
					values("flags").filter{ it.get().size() == 1 && it.get()[0] in logicalEquality }
				).out("PARENT_OF").out("PARENT_OF").has("code", loopVarName).hasNext()
				def reassigned = g.V(childs[3]).repeat(out("PARENT_OF"))\
				.until(
					and(
						values("defs").filter{ it && it.get().contains(loopVarName) },
						__.not(filter{ isCountComparison(it.get()) })
					)
				).hasNext()
				if (hasCtFunc && checksLen && !reassigned)
					return 0

				def stmts = g.V(node.id()).out("PARENT_OF").has("type", "AST_STMT_LIST").toList()
				if (stmts.size() != 1)
					return 2

				for (vp in getVarPaths(stmts[0])) {
					if (!vp[-1].value("type").equals("string") || !vp[-1].value("code").equals(loopVarName))
						// not our variable
						continue
					else if (!vp[-3].value("type").equals("AST_DIM")) {
						// only safe if variable is only used as dimension
						return 2
					}
					
				}
				// all uses of loop var were dimensions
				return 0
			} else {
				return 2
			}
		} else {
			return 2
		}
	}

	def getBranchType(node) {
		if (node instanceof Integer)
			node = (Long) node
		if (node instanceof java.lang.Long)
			node = g.V(node).next()
		// special case 
		def parent = g.V(node.id()).in("PARENT_OF").next()
		if (parent.value("type").equals("AST_SWITCH")) {
			return getSwitchBranchType(parent)
		} else if (parent.value("type").equals("AST_FOR")) {
			if (getForLoopType(parent) == 0)
				return 0
		}
		switch(node.value("type")) {
			case "AST_FOREACH":
				return 0;
			case "AST_EXPR_LIST": // for (...; i < 0, i > 1;...)
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
				if (logicalGtLt.any{flag.contains(it)}) {
					return 0
				} else if (logicalConnectives.any{flag.contains(it)} || hasAssign) {
					def children = getChildren(node)
					return Integer.max(getBranchType(children[0]), getBranchType(children[1]))
				} else if (logicalEquality.any{flag.contains(it)}) {
					return (isEmptyComparison(node) || isConstComparsion(node) || isCountComparison(node) || isIntComparison(node) ? 0 : 2)
				} else {
					return 2
				}
			case "AST_ASSIGN":
				// this will perform a null/empty check on the left hand side after
				// evaluating the right hand side
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
			case "integer":
			case "string":
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
		
		def methodCalls = g.V().has("type", "AST_METHOD_CALL").has("name").where(out("CALLS").count().is(gt(1))).toList()
		for (c in methodCalls) {
			def var = g.V(c.id()).in("CALL_ID").has("childnum", -1).next().value("uses")
			def defs = g.V().has("defs", var).toList()
			//println "var: " + var + ", defs" + defs
			// if all defs for this var give a type, and there is only one type, then we can resolve the call
			def varType = null
			for (d in defs) {
				if ("givesType" in d.keys()) {
					if (varType == null || varType.equals(d.value("givesType"))) {
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

	def branchNodes = null
	def loadBranchNodes() {
		branchNodes = g.V().match(__.as('a').out('FLOWS_TO').as('b'),
						__.as('a').out('FLOWS_TO').as('c'), 
						where('b', neq('c'))).\
					select('a').dedup().toList()
	}

	def labelBranches() {
		def unsafeBranches = []
		for (node in branchNodes) {
			def branchType = getBranchType(node)
			g.V(node.id()).property('branch', branchType).next()
			if (branchType == 2)
				unsafeBranches += node
		}
		return unsafeBranches
	}

	def getArrayIndexes(arrayNode) {
		def indexes = []
		def cur = arrayNode
		while (cur.value("type") == "AST_DIM") {
			def childs = getChildren(cur)
			if (!childs[1].value("type").equals("AST_VAR"))
				return false

			def varNameNode = getChildren(childs[1])
			if (!varNameNode[0].value("type").equals("string"))
				return false

			indexes += varNameNode[0].value("code")
			cur = childs[0]
		}

		return indexes
	}

	def checkDefsArray(node, array) {
		def arrayName = array[0]
		if ("defs" in node.keys() && node.value("defs").contains(arrayName)) {
			// we should be able to find the array on the lhs. if not, then we fail
			def arrayNode = g.V(node).out("PARENT_OF").has("childnum", 0).has("type", "AST_DIM").toList()
			if (arrayNode.size() != 1) return false
			def arrayLHSIndexes = getArrayIndexes(arrayNode[0])
			if( array[1] == arrayLHSIndexes) {
				println "Found def:" + node
				return true
			} else {
				return false
			}
		} else {
			return false
		}
	}

	def checksArrayIndexIsSet(branch) {
		if (!branch.value("type").equals("AST_ISSET"))
			return false

		def child = g.V(branch).out("PARENT_OF").next()
		if (!child.value("type").equals("AST_DIM"))
			return false


		def arrayName = "uses" in branch.keys() ? branch.value("uses").split(";").findAll{ it.contains("[") } : []
		if (arrayName.size() != 1)
			return false
		arrayName = arrayName[0].split("\\[")[0]

		def indexes = getArrayIndexes(child)
		if (indexes)
			return [arrayName, getArrayIndexes(child)]
		else
			return false
	}

	def arrayAlwaysDefdOnFalseBranch(branch, array) {
		def CFGbranches = g.V(branch).out("FLOWS_TO").toList();
		def branches = [];
		branches.addAll(CFGbranches);
		if ("call_id" in branches[0].keys()) branches[0] = g.V(branches[0].value("call_id")).next()
		if ("call_id" in branches[1].keys()) branches[1] = g.V(branches[1].value("call_id")).next()
		def ifNode = g.V(branch).repeat(__.in("PARENT_OF")).until(has("type", "AST_IF")).toList()
		if (ifNode.size() != 1) {
			//println "Could not find top level if node for branch: " +  branch
			return [branch]
		}
		ifNode = ifNode[0]
		def falseBranchIndex = 0
		def trueBranchIndex = 1
		def ifElem = g.V(branches[0]).until(__.in("PARENT_OF").is(ifNode)).repeat(__.in("PARENT_OF")).toList()
		if (ifElem.size() == 0) {
			falseBranchIndex = 1
			trueBranchIndex = 0
			ifElem = g.V(branches[1]).until(__.in("PARENT_OF").is(ifNode)).repeat(__.in("PARENT_OF")).toList()
		}
		if (ifElem.size() != 1) {
			//println "Node top if found for: " + branches[0] + " or " + branches[1] + " top if is: " + ifNode
			//println "Could not find the statements of the branch: " +  branch
			return [branch]
		}
		ifElem = ifElem[0]

		def falseBranch = null
		def trueBranch = null
		if (ifElem.value("childnum") == 1) {
			falseBranch = CFGbranches[falseBranchIndex]
			trueBranch = CFGbranches[trueBranchIndex]
		} else {
			trueBranch = CFGbranches[falseBranchIndex]
			falseBranchIndex = falseBranchIndex == 0 ? 1 : 0
			falseBranch = CFGbranches[falseBranchIndex]
		}

		// First check for defs on false branch
		def visited = new HashSet();
		def work = [falseBranch.id()]
		def arrayDefs = []
		while (work) {
			def cur = work.pop()
			if (cur in visited) continue
			visited.add(cur)

			def obj = g.V(cur).next()

			if (checkDefsArray(obj, array)) {
				arrayDefs += obj
				continue
			}
			
			if (obj.value("type").equals("CFG_FUNC_EXIT"))
				return [branch]

			def interprocEdges = g.V(obj).outE("INTERPROC").toList()
			if (interprocEdges.size() > 0) {
				for (e in interprocEdges) {
					work += e.value("exit_id")
				}
			} else {
				def succs = g.V(obj).out("FLOWS_TO").toList()
				if (succs.size() == 0) {
					println "No succs for: " + obj
				}
				for (s in succs) {
					work += s
				}
			}
		}

		// Now check only uses on true branch
		visited.clear()
		work = [trueBranch.id()]
		def arrayUses = []
		while (work) {
			def cur = work.pop()
			if (cur in visited) continue
			visited.add(cur)

			def obj = g.V(cur).next()

			if ("defs" in obj.keys() && obj.value("defs").contains(array[0]))
				return [branch]
			
			if ("uses" in obj.keys() && obj.value("uses").contains(array[0]))
				arrayUses += obj
			
			if (obj.value("type").equals("CFG_FUNC_EXIT"))
				continue

			def interprocEdges = g.V(obj).outE("INTERPROC").toList()
			if (interprocEdges.size() > 0) {
				for (e in interprocEdges) {
					work += e.value("exit_id")
				}
			} else {
				def succs = g.V(obj).out("FLOWS_TO").toList()
				if (succs.size() == 0) {
					println "No succs for: " + obj
				}
				for (s in succs) {
					work += s
				}
			}
		}

		// now check origins of all other defs
		def sources = []
		visited.clear()
		work.clear()
		def tmpDefs = g.V().filter{ "defs" in it.get().keys() && it.get().value("defs").contains(array[0]) }.toList()
		for (d in tmpDefs) {
			if (d in arrayDefs)
				continue
			for (use in usesForNode(d)) {
				if (!use.equals(array[0]) && (!use[0].isInteger() || use.startsWith("field_prefix"))) {
					println "Fail: uses a non-local: " + use + " at " + d
					return [branch]
				}
				work.add(new Tuple(d.id(), use))
			}
		}
		
		while (work) {
			def cur = work.pop()
			if (cur in visited) continue
			visited.add(cur)

			def obj = g.V(cur[0]).next()

			if (obj.value("type").equals("CFG_FUNC_ENTRY"))
				continue

			if (g.V(obj).in("INTERPROC").hasNext()) {
				println "Fail: has interproc " + obj
				return [branch]
			}

			if ("defs" in obj.keys() && obj.value("defs").contains(cur[1])) {
				def uses = usesForNode(obj)
				if (uses.size() == 0)
					sources += obj
				for (use in uses) {
					if (!use.equals(array[0]) && (!use[0].isInteger() || use.startsWith("field_prefix")))
						println "Fail: uses a non-local: " + use + " at " + d
					work.add(new Tuple(obj.id(), use))
				}
			}

			def preds = g.V(cur[0]).in("FLOWS_TO").id().toList()
			for (p in preds)
				work.add(new Tuple(p, cur[1]))
		}

		for (d in arrayDefs) {
			def oldDefs = d.value("defs")
			def newDefs = oldDefs.replaceAll(array[0] + "[^;]*", "123tmpcache")
			g.V(d).property("defs", newDefs).next()
		}		

		for (u in arrayUses) {
			def oldUses = u.value("uses")
			def newUses = oldUses.replaceAll(array[0] + "[^;]*", "123tmpcache")
			g.V(u).property("uses", newUses).next()
		}

		def exit = g.V(branch.value("funcid")).out("EXIT").next()
		def newExitDefs = "123tmpcache"
		if ("defs" in exit.keys())
			newExitDefs += ";" + exit.value("defs")
		g.V(exit).property("defs", newExitDefs).next()

		def others = g.V().has("uses").filter{ it.get().value("uses").contains(array[0]) }.toList()
		for (u in others) {
			def oldUses = u.value("uses")
			def newUses = oldUses.replaceAll(array[0] + "[^;]*", "")
			g.V(u).property("uses", newUses).next()
		}

		return true
	}

	def usesForNode(node) {
		def uses = "uses" in node.keys() ? node.value("uses") : null
		if (uses == null)
			return []
		uses = uses.split(";")
		def ret = []
		for (use in uses)
			ret += use.split("\\[")[0].replace("*", "")
		return ret
	}

	def findCachingPattern() {
		for (branch in branchNodes) {
			def array = checksArrayIndexIsSet(branch)
			if (array) {
				def didIt = arrayAlwaysDefdOnFalseBranch(branch, array)
				if (didIt == true) {
					println "Found caching pattern: " + branch + " " + array
				}
			}
		}
	}

	ArrayList nodeIds = new ArrayList()
	int curNode = 0;
	int prevNode = 0;
	def loadNodeIds() {
		def fpath = "/home/brandon/joern/projects/extensions/joern-php/src/summary_analysis/tmp/tainted_branches"
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

	HashSet sources;
	def findPathToSource(start) {
		sources = new HashSet()
		def fpath = "/home/brandon/joern/projects/extensions/joern-php/src/datalog/tmp/source.csv"
		File f = new File(fpath)
		def lines = f.readLines()
		for (l in lines) {
			sources.add(Integer.parseInt(l))
		}
		def ccfgNodes = g.V().hasLabel("CCFG").has("orig_id", start).id().toList()
		for (n in ccfgNodes) {
			println "Processing: " + n
			def visited = new HashSet()
			visited.clear()
			try {
				return g.V(n).repeat(__.in("REACHES").filter{ !(it.get().id() in visited) }.sideEffect{ visited.add(it.get().id()) }).until(__.filter{ ((int)it.get().value("id")) in sources }).limit(1).path().next()
				//g.V(n).repeat(__.in("REACHES").simplePath().filter{ !(it.get().id() in visited) }.sideEffect{ visited.add(it.get().id()) }).hasNext()
			} catch (NoSuchElementException e) {

			}
			return visited
		}
	}

	def test() {
		loadNodeIds()
		nodeIds = nodeIds.findAll{ getBranchType(g.V(it).next()) == 2 }
	}

}

o = new G()
if (args.size() == 0) {
	o.loadBranchNodes()
	o.makeConstMap()
	o.labelBranches()
	o.findCachingPattern()
	o.labelHandleableAndCtrlTainted()
	o.callAnalysis()
	o.g.graph.tx().commit()
	o.g.graph.close()
}
