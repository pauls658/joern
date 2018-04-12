import java.nio.file.Files;

class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();

	def varTypes = [
		"AST_VAR",
		"AST_PROP",
		"string",
		"AST_DIM"
	] as HashSet;

	def getForeachInfo(id) {
		def vars = g.V(id).out('PARENT_OF').has("childnum", within(1,2)).id().toList()
		def stmtlist = g.V(id).out('PARENT_OF').has("childnum", 3).id().toList()[0]
		return ["stmt":stmtlist, "childnum":-0.5, "vars":vars]
	}

	def getVarPath(varid) {
		g.V(varid)\
		.repeat(
			outE('PARENT_OF')
		).until(inV().has("type", "string"))\
		.path().toList()[0].toList()[1..-1]
	}

	def copyASTNode(Neo4jVertex v) {
		def (type, lineno) = [v.value("type"), v.value("lineno")]
		if (type.equals("string")) {
			def code = v.value("code")
			g.addV("AST").property("type", type).property("lineno", lineno)\
			property("code", code).id().toList()[0]
		} else {
			g.addV("AST").property("type", type).property("lineno", lineno).id().toList()[0]
		}
	}

	def copyVar(var_path) {
		def prev_node = copyASTNode(var_path[0].outVertex());
		for (rel in var_path) {
			def newid = copyASTNode(rel.inVertex())
			g.V(prev_node).addE('PARENT_OF').to(V(newid)).property("child_rel", rel.value("child_rel")).next() // fuck gremlin
		}
		return prev_node;
	}

	def makeVarPairs(varid) {
		def var_path = getVarPath(varid);
		def leftid = copyVar(var_path);
		def rightid = copyVar(var_path);
		return [leftid, rightid];
	}

	def doNewStmtInstr(type, infos) {
		def scriptDir = new File(getClass().protectionDomain.codeSource.location.path).parent;
		def outFile = new File("/home/brandon/joern/projects/extensions/joern-php/src/groovy/instrument/" + type + ".csv");
		Files.deleteIfExists(outFile.toPath());
		outFile.createNewFile();
		outFile << "stmtid,childid,leftid,rightid\n";
		for (info in infos) {
			for (varid in info["vars"]) {
				def (left, right) = makeVarPairs(varid);
				outFile << info["stmt"] + "," + info["childnum"] + "," + left + "," + right + "\n";
			}
		}
	}

	def main(type, file) {
		def ids = [];
		def csvFile = new File((String)file);
		csvFile.eachLine{ String line, lineno ->
			if (lineno != 1)
				ids += Integer.parseInt(line.trim())
		}
		def infoFunc;
		switch (type) {
			case "AST_FOREACH":
				infoFunc = this.&getForeachInfo
				break
			default:
				throw Exception("Fuck you I'm an exception");
		}

		def infos = [];
		for (id in ids) {
			infos << infoFunc(id)
		}
		doNewStmtInstr(type, infos);
	}
}
o = new G()
o.main(args[0], args[1])
o.g.graph.tx().commit()
o.g.graph.close()
