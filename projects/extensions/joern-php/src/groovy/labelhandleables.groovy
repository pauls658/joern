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

	def labelHandleable() {
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
		}
	}

	def findNodeLoc(node) {
		def lineno = node.value("lineno")
		def fileName = g.V(node.id()).repeat(__.in("PARENT_OF")).until(has("type", "AST_TOPLEVEL")).toList()[0].value("name")
 		ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/vi", fileName, "+" + lineno);
 		processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
 		processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
 		processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);

 		Process p = processBuilder.start();
		// wait for termination.
		p.waitFor();

	}

	public HashSet safeBuiltInFuncs = [
		//"is_array",
		//"is_object"
	];

	def isSafeFunc(node) {
		// check if have implementation
		if (node.edges(Direction.OUT, "CALLS")[0] == null) {
			// in the future we can analyze the functions implementation,
			// but for now just assume its false
			return false
		} else {
			def funcName = node.value("name")
			return safeBuiltInFuncs.contains(funcName)
		}
	}

	public HashSet binaryBoolFlags = [
		"BINARY_BOOL_AND",
		"BINARY_BOOL_OR",
		"BINARY_BOOL_XOR",
		"BINARY_IS_IDENTICAL",
		"BINARY_IS_NOT_IDENTICAL",
		"BINARY_IS_EQUAL",
		"BINARY_IS_NOT_EQUAL",
		"BINARY_IS_SMALLER",
		"BINARY_IS_SMALLER_OR_EQUAL",
		"BINARY_IS_GREATER",
		"BINARY_IS_GREATER_OR_EQUAL",
	]
	
	public Neo4jVertex badNode = null
	def isEmptyComparison(node) {
		def hasEmptyString = false, hasVar = false
		def children = []
		children.addAll(node.vertices(Direction.OUT, "PARENT_OF"))
		if( !(children[0] != null && children[1] != null)) {
			println node.value("flags")
			return false
			
		}
		if (children[0].value("type").equals("AST_VAR"))
			hasVar = true
		else if (children[0].value("type").equals("string")) {
			hasEmptyString = children[0].value("code").equals("")
		}

		if (children[1].value("type").equals("AST_VAR"))
			hasVar = true
		else if (children[1].value("type").equals("string")) {
			hasEmptyString = children[1].value("code").equals("")
		}

		return hasEmptyString && hasVar
	}
	
	def isSafeBranch(node) {
		switch(node.value("type")) {
			case "AST_FOREACH":
				return true;
			case "AST_UNARY_OP":
				def child = node.vertices(Direction.OUT, "PARENT_OF")[0]
				return isSafeBranch(child)
			case "AST_BINARY_OP":
				def flag = node.value("flags")
				if (["BINARY_IS_EQUAL", "BINARY_IS_NOT_EQUAL"].any{flag.contains(it)}) {
					return isEmptyComparison(node)
				} else {
					return false
				}
			case "AST_CALL":
				return isSafeFunc(node)
			default:
				return false;
		}
	}

	def labelSafeBranches() {
		def branchNodes = g.V().match(__.as('a').out('FLOWS_TO').as('b'),
						__.as('a').out('FLOWS_TO').as('c'), 
						where('b', neq('c'))).\
					select('a').dedup().toList()
		def unsafeBranches = []
		for (node in branchNodes) {
			if (isSafeBranch(node)) {
				g.V(node.id()).property('safeBranch', true).next()
			} else {
				g.V(node.id()).property('safeBranch', false).next()
				unsafeBranches += node
			}
		}
		return unsafeBranches
	}

	def test() {
		def branchNodes = g.V().match(__.as('a').out('FLOWS_TO').as('b'),
					__.as('a').out('FLOWS_TO').as('c'), 
					where('b', neq('c'))).\
				select('a').dedup().toList()
		return branchNodes.findAll{ isSafeBranch(it) }
	}

}

o = new G()
if (args.size() == 0) {
	o.labelHandleable()
	o.labelSafeBranches()
	o.g.graph.tx().commit()
	o.g.graph.close()
}
