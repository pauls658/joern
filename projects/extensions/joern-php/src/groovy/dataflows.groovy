class FSackObject {
	private Stack<Long> stack;
	public HashSet<Long> visited;
	public HashSet<Long> echos;
	public FSackObject() {
		stack = new Stack<Long>();
		visited = new HashSet<Long>();
		echos = new HashSet<Long>();
	}

	private FSackObject(Stack<Long> stack, HashSet<Long> visited, HashSet<Long> echos) {
		this.stack = stack.clone();
		this.visited = visited;
		this.echos = echos;
	}

	FSackObject pushStack(Long id) {
		stack.push(id)
		return this;
	}

	boolean stackEmpty() {
		return stack.empty();
	}

	FSackObject clearStack() {
		stack.clear();
		return this;
	}

	Long stackPeek() {
		return stack.peek();
	}

	FSackObject stackPop() {
		stack.pop();
		return this;
	}

	FSackObject addEcho(Long id) {
		echos.add(id);
		return this;
	}

	FSackObject clone() {
		return new FSackObject(this.stack, this.visited, this.echos);
	}

	boolean haveVisited(Long id) {
		return visited.contains(id);
	}

	FSackObject addVisited(Long id) {
		visited.add(id);
		return this;
	}

	FSackObject special() {
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

con = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/");
g = con.traversal();

def forwardTrav(g, start) {
    res = g\
    .withSack{new FSackObject()}{it.clone()}\
    .V(start)\
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
				filter{it.sack().stackEmpty() || it.sack().stackPeek() == it.get().value("call_id")},
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
}

class BSackObject {
	private Stack<Long> stack;
	public HashSet<Long> visited;
	public HashSet<Long> nset;
	public BSackObject() {
		stack = new Stack<Long>();
		visited = new HashSet<Long>();
		nset = new HashSet<Long>();
	}

	private BSackObject(Stack<Long> stack, HashSet<Long> visited, HashSet<Long> nset) {
		this.stack = stack.clone();
		this.visited = visited;
		this.nset = nset;
	}

	BSackObject pushStack(Long id) {
		stack.push(id)
		return this;
	}

	boolean stackEmpty() {
		return stack.empty();
	}

	BSackObject clearStack() {
		stack.clear();
		return this;
	}

	Long stackPeek() {
		return stack.peek();
	}

	BSackObject stackPop() {
		stack.pop();
		return this;
	}

	BSackObject addN(Long id) {
		nset.add(id);
		return this;
	}

	BSackObject clone() {
		return new BSackObject(this.stack, this.visited, this.nset);
	}

	boolean haveVisited(Long id) {
		return visited.contains(id);
	}

	BSackObject addVisited(Long id) {
		visited.add(id);
		return this;
	}

	BSackObject special() {
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

def backwardTrav(g, start, sensitiveReachable) {
    res = g\
    .withSack{new BSackObject()}{it.clone()}\
    .V(start)\
    .repeat(
        sack{m, v -> m.addVisited(v.id())}\
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
	).until(filter{it.sack().haveVisited(it.get().id())}) // stop if we visit some node we already have seen
	.sack().toList()[0]
}
