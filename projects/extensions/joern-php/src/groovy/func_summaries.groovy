import java.nio.file.Files;

class G {
   	public GraphTraversalSource g = Neo4jGraph.open("/var/lib/neo4j/data/databases/cur.db/").traversal();
	
	def main() {
		g.V().filter{ "FUNCDECL" in it.get().labels() }\
		.repeat(out
	}
}
o = new G()
//o.main(args[0], args[1])
//o.g.graph.tx().commit()
//o.g.graph.close()
