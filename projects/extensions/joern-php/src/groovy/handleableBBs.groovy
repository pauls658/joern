class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();
    
    public HashSet handleableFunctions = [];
    
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

	public canHandleCache = [:];
 
    def canHandle(BB) {
		if (!("BB" in BB.labels()) || BB.value("type") in stopTypes)
			return true; // we only care about basic blocks

		if (BB.id() in canHandleCache)
			return canHandleCache[BB.id()];

		def var_paths = g\
		.V(BB.id())\
		repeat(
			out('PARENT_OF').has("type", without(stopTypes))
		).until(has("type", "AST_VAR").out('PARENT_OF').has("type", "string")).path().toList()

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

	def setCanHanle(BBid) {
		
	}

	def labelHandleableFromFile(String filename) {
		File f = new File(filename);
		f.eachLine { line ->
			line.trim();
			def BBid = line.toInteger();
			def v = g.V(BBid).next();
			if(!canHandle(v)) {
				println "Could not handle: " + BBid;
				println "at lineno: " + v.value("lineno");
			}
		}
	}

	class SackObject {
    	protected Stack<Long> stack;
    	public HashSet<Long> visited;
    	public SackObject() {
    		this.stack = new Stack<Long>();
    		this.visited = new HashSet<Long>();
    	}
    
    	protected SackObject(Stack<Long> stack, HashSet<Long> visited) {
    		this.stack = stack.clone();
    		this.visited = visited;
    	}
    
    	SackObject pushStack(Long id) {
    		stack.push(id)
    		return this;
    	}
    
    	boolean stackEmpty() {
    		return stack.empty();
    	}
    
    	SackObject clearStack() {
    		stack.clear();
    		return this;
    	}
    
    	Long stackPeek() {
    		return stack.peek();
    	}
    
    	SackObject stackPop() {
    		stack.pop();
    		return this;
    	}
    
    	boolean haveVisited(Long id) {
    		return visited.contains(id);
    	}
    
    	SackObject addVisited(Long id) {
			if (id == 732)
				println "Adding 732"
    		visited.add(id);
    		return this;
    	}
    
    	SackObject special() {
    		System.out.println("Hello from the special() function!");
    		System.out.println("Visited: " + this.visited.toString());
    		System.out.println("Stack: " + this.stack.toString());
    		return this;
    	}
    
    	@Override
    	String toString() {
    		return "Stack: " + stack.toString();
    	}
    }

    class FSackObject extends SackObject {
    	public HashSet<Long> echos;

    	public FSackObject() {
			super();
    		echos = new HashSet<Long>();
    	}
   
    	protected FSackObject(Stack<Long> stack, HashSet<Long> visited, HashSet<Long> echos) {
			super(stack, visited);
    		this.echos = echos;
    	}
    
    	FSackObject addEcho(Long id) {
    		echos.add(id);
    		return this;
    	}
    
    	FSackObject clone() {
    		return new FSackObject(this.stack, this.visited, this.echos);
    	}
    
    }

    def forwardTrav(starts) {
		FSackObject s = new FSackObject();
        def res = g\
        .withSack{s}{it.clone()}\
        .V(starts)\
        .repeat(
            sack{m, v -> m.addVisited(v.id())}\
            .union(outE("REACHES"), outE("INTERPROC"))\
    		.choose(hasLabel("INTERPROC"),
    		// if {
    			choose(has("type", "arg_entry"),
    			// if { // we are entering a function
            		sack{m, e -> m.pushStack(Long.valueOf(e.value("call_id")))},
    			choose(or(has("type", "return"), has("type", "arg_exit")),
    			// } else if { // we are leaving a function
    				filter{it.sack().stackEmpty() || it.sack().stackPeek() == it.get().value("call_id")}\
					.sack{m, e -> if (!m.stackEmpty()) {m.stackPop()} else {m}},
    			// } else { // global, class member -> clear the stack
    				sack{m, v -> m.clearStack()} // no more calling context
    			// }
    			))
    		// }
    		).inV()\
    		.choose(has("type", "AST_ECHO"),
    		// if {
    			sack{m, v -> m.addEcho(Long.valueOf(v.id()))}
    		// }
    		)
    	).until(filter{it.sack().haveVisited(it.get().id())}) // stop if we visit some node we already have seen
    	.sack().toList()[0]
		//.path().toList()
		return res
    }

    class BSackObject extends SackObject {
    	public HashSet<Long> nset;
    	public BSackObject() {
			super();
    		nset = new HashSet<Long>();
    	}
    
    	protected BSackObject(Stack<Long> stack, HashSet<Long> visited, HashSet<Long> nset) {
			super(stack, visited);
    		this.nset = nset;
    	}
    
    	BSackObject addN(Long id) {
    		nset.add(id);
    		return this;
    	}
    
    	BSackObject clone() {
    		return new BSackObject(this.stack, this.visited, this.nset);
    	}
    
    }
   
	def getBB(Neo4jVertex v) {
		if ("BB" in v.labels())
			return v;
		else
			return g.V(v.value("bb_id")).next();
	}
 
    def backwardTrav(start, sensitiveReachable) {
		BSackObject s = new BSackObject(); 
        def res = g\
        .withSack{s}{it.clone()}\
        .V(start)\
        .repeat(
            sack{m, v -> m.addVisited(v.id())}\
			.choose(filter{ !canHandle(getBB(it.get())) },
			// if {
				sack{m, v -> m.addN(v.id())}
			// }
			)
            .union(inE("REACHES"), inE("INTERPROC"))\
    		.choose(hasLabel("INTERPROC"),
    		// if {
    			choose(or(has("type", "return"), has("type", "arg_exit")),
    			// if { // we are entering a function
            		sack{m, e -> m.pushStack(Long.valueOf(e.value("call_id")))},
    			choose(has("type", "arg_entry"),
    			// } else if { // we are leaving a function through a param
    				filter{it.sack().stackEmpty() || it.sack().stackPeek() == it.get().value("call_id")},
    			// } else { // global edge -> clear the stack
    				sack{m, v -> m.clearStack()} // no more calling context
    			// }
    			))
    		// }
    		).outV()
    	).until(filter{it.sack().haveVisited(it.get().id()) || !(it.get().id() in sensitiveReachable) }) // stop if we visit some node we already have seen
    	.sack().toList()[0]
		return res;
    }

	// should output two lists:
	//	1) a list of nodes to add propogation code to
	//  2) a list of nodes (and var names?) that should be fully tainted.
	//	   we should insert statements just before the node that fully
	//	   taints the variables

	HashSet<String> callTypes = [
		"AST_CALL",
		"AST_METHOD_CALL",
		"AST_STATIC_CALL",
		"AST_NEW"
	]

	// Gets the artificial arg/ret nodes for a function call by its name
	def getArgsRetsByName(String funcName) {
		def nodes = g.V().has("code", funcName).has("type", "string")\
		.in('PARENT_OF').in('PARENT_OF')\
		.sideEffect{assert(it.get().value("type") in callTypes)}\
		.union(__.in('ASSOC'), __.in('CALL_ID').has("type", "arg_exit")).toList()
		return nodes
	}

	def analysis() {
		def sensitiveFunctions = [
			"get_inbox"
		]

		LinkedList<Long> startNodes = [];
		for (funcName in sensitiveFunctions) {
			startNodes.addAll(getArgsRetsByName(funcName));
		}

		FSackObject fsack = forwardTrav(startNodes);
		HashSet<Long> sensitiveReachable = fsack.visited;

		BSackObject bsack = backwardTrav(fsack.echos, sensitiveReachable);
		return bsack;
	}
}
