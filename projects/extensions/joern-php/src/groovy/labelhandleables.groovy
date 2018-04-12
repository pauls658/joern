import java.nio.file.Files;

class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();
    
    public HashSet handleableFunctions = [
		"json_encode"
	];
    
    def nonMethodCall(Neo4jVertex v) {
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
				return true;
			//TODO: art ast cases:
			//	have imp -> true
			// 	no imp -> check whitelist
			default:
				return false;
		}
	}

    def canHandleAST(Neo4jVertex v) {
    	switch (v.value('type')) {
    		case "AST_CALL":
    		case "AST_STATIC_CALL":
    		case "AST_NEW":
    			return nonMethodCall(v);
    
    		case "AST_BINARY_OP":
    			return binaryOp(v);
    
    		case "AST_ASSIGN_OP":
    			return assignOp(v);
    
    		case "AST_ASSIGN":
    		case "AST_DIM":
    		case "AST_ASSIGN_REF":
    		case "AST_ECHO":
    		case "AST_FOREACH":
    		case "AST_FOR":
    		case "AST_DO_WHILE":
    		case "AST_GLOBAL":
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
				return true;
    		default:
    			return false;
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

	public canHandleCache = [:];
 
    def canHandle(BB) {
		if (BB.value("type") in stopTypes)
			return false;

		if ("ART_AST" in BB.labels())
			return canHandleArtAST(BB);

		def var_paths = getVarPaths(BB.id());
		for (path in var_paths) {
			for (v in path) {
				if (!canHandleAST(v)) {
					//println "Could not handle type: " + v.value("type")
					canHandleCache[BB.id()] = false;
					return false;
				}
			}
		}
		canHandleCache[BB.id()] = true;
		return true;
    }

	def labelHandleable() {
		def bbs = 
		g.V().\
		where(
			or(
				or(__.in('REACHES'), __.in('INTERPROC')),
				or(out('REACHES'), out('INTERPROC'))
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

}

o = new G()
o.labelHandleable()
o.g.graph.tx().commit()
o.g.graph.close()
