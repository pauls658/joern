import java.util.Stack;
import java.util.HashSet;

class SackObject {
	private Stack<Long> stack;
	public HashSet<Long> visited;
	private HashSet<String> defdGlobals;
	public SackObject() {
		stack = new Stack<Long>();
		visited = new HashSet<Long>();
		defdGlobals = new HashSet<String>();
	}

	private SackObject(Stack<Long> stack, HashSet<Long> visited, HashSet<String> defdGlobals) {
		this.stack = stack.clone();
		this.visited = visited;
		this.defdGlobals = null;
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

	SackObject addGlobalDef(String name) {
		defdGlobals.add(name);
		return this;
	}

	SackObject clone() {
		return new SackObject(this.stack, this.visited, this.defdGlobals);
	}

	boolean haveVisited(Long id) {
		return visited.contains(id);
	}

	SackObject addVisited(Long id) {
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
		//return "Defined globals: " + defdGlobals.toString() + "\n" +\
		return "Stack: " + stack.toString();
	}
}

con = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/");
g = con.traversal();

def forwardTrav(g, start) {
    res = g\
    .withSack{new SackObject()}{it.clone()}\
    .V(start)\
    .repeat(
        sack{m, v -> m.addVisited(v.id())}\
        .choose(has("type", "AST_RETURN"),
        	// if
        	out("INTERPROC")\
        	.filter{it.sack().stackEmpty() || it.sack().stackPeek() == it.get().value("call_id")} // go back to calling node, or everywhere if no calling node
        	.sack{m, v -> m.stackPop()}, // remove top item from stack
        	// else
			choose(has("type", "AST_GLOBAL"),
				// if
				sack{m, v -> m.clearStack()} // no more calling context
			)\
        	.union(outE("REACHES"), outE("INTERPROC"))\
        	.choose(hasLabel("INTERPROC"),
        		// if
        		sack{m, e -> m.pushStack(Long.valueOf(e.value("call_id")))}\
        		.inV(),
        		// else
        		inV()
        	)
        )\
	)\
	//.emit(has("id", "653"))\
    .until(filter{it.sack().haveVisited(it.get().id())}) // either we find a node we have visited
    			  //union(out("REACHES"), out("INTERPROC")).count().is(0))) // or there are no more edges
	//.choose(has("id", "653"),
	//	sack{m, v -> m.special()})
/*
	.path().toList()
	ret = []
	for ( l in res ) {
		ret += [l.findAll{ it instanceof Neo4jVertex }]
	}
	return ret
*/
	.sack().toList()[0].visited
}

def backwardTrav(g, start, sensitiveReachable) {
    res = g\
    .withSack{new SackObject()}{it.clone()}\
    .V(start)\
    .repeat(
        sack{m, v -> m.addVisited(v.id())}\
        .union(inE("REACHES"), inE("INTERPROC"))\
		.choose(hasLabel("INTERPROC"),
		// if {
			choose(inV().or(has("type", "return"), has("type", "arg_exit")),
			// if { // we are entering a function
        		sack{m, e -> m.pushStack(Long.valueOf(e.value("call_id")))},
			choose(inV().has("type", "AST_PARAM"),
			// } else if { // we are leaving a function through a param
				filter{it.sack().stackEmpty() || it.sack().stackPeek() == it.get().value("call_id")},
			// } else { // global edge -> clear the stack
				sack{m, v -> m.clearStack()} // no more calling context
			// }
			))
		// }
		).outV()
	).until(filter{it.sack().haveVisited(it.get().id())}) // stop if we visit some node we already have seen
	.sack().toList()[0].visited
/*
    res = g\
    .withSack{new SackObject()}{it.clone()}\
    .V(start)\
    .repeat(
        sack{m, v -> m.addVisited(v.id())}\
        .choose(hasLabel("ART_AST").or(has("type", "return"), has("type", "arg_exit")),
        // if {
        	union(inE("REACHES"), inE("INTERPROC"))\
			.choose(hasLabel("INTERPROC"),
        	// if {
        		sack{m, e -> m.pushStack(Long.valueOf(e.value("call_id")))}\
        		.outV(),
        	// } else {
        		outV()
			// }
        	),
        // } else {
			choose(has("type", "AST_GLOBAL"),
			// if {
				sack{m, v -> m.clearStack()} // no more calling context
			// }
			)\
        	.union(outE("REACHES"), outE("INTERPROC"))\
        // }
        )\
	)\
	//.emit(has("id", "653"))\
    .until(filter{it.sack().haveVisited(it.get().id())}) // either we find a node we have visited
    			  //union(out("REACHES"), out("INTERPROC")).count().is(0))) // or there are no more edges
	//.choose(has("id", "653"),
	//	sack{m, v -> m.special()})
*/
/*
	.path().toList()
	ret = []
	for ( l in res ) {
		ret += [l.findAll{ it instanceof Neo4jVertex }]
	}
	return ret
*/
}

def dumbTrav(g) {
    g\
    .V(666)\
    .repeat(
		union(out("REACHES"), out("INTERPROC"))\
    	.simplePath()
	).until(
		union(outE("REACHES"), outE("INTERPROC")).count().is(0)
	).path()\
    .by(valueMap("type","lineno"))
}
