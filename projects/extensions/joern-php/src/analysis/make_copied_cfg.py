import json
import networkx as nx

def graph_from_json():
    g = nx.MultiDiGraph()

    rows = json.load(open("tmp/nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        g.add_node(i, **row) 

    rows = json.load(open("tmp/flows_to.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        g.add_edge(int(row[0]), int(row[1]), label="FLOWS_TO")

    rows = json.load(open("tmp/interproc.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        rel_props = row[2]
        rel_props["label"] = "INTERPROC"
        g.add_edge(int(row[0]), int(row[1]), **rel_props)

    return g

stmt_defs = {}
stmt_uses = {}
var_map = {}
def load_def_use_info():
    global stmt_defs
    global stmt_uses
    global var_map
    distinct_vars = set()
    # defs/uses for stmts by stmt_id
    rows = json.load(open("tmp/def_use.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"] # id, defs, uses
        stmt_id = int(row[0])
        if row[1] is not None:
            defs = row[1].split(";")
            distinct_vars.update(defs)
            stmt_defs[stmt_id] = defs
        if row[2] is not None:
            uses = row[2].split(";")
            distinct_vars.update(uses)
            stmt_uses[stmt_id] = uses

    # symbol_map 
    var_id = 0
    for symbol in distinct_vars:
        var_map[symbol] = var_id
        var_id += 1

sinks = set()
def load_sinks():
    global sinks
    sinks.update(json.load(open("tmp/sinks.json", "rb"))["results"][0]["data"][0]["row"][0])

sources = set()
def load_sources():
    global sources
    sources.update(json.load(open("tmp/sources.json", "rb"))["results"][0]["data"][0]["row"][0])

def profile_memory():
    from memory_profiler import memory_usage
    mem_usage = memory_usage(graph_from_json)
    print('Maximum memory usage: %s' % max(mem_usage))

def write_graph_edge(start, end):
    global graph_fd
    global g
    global id_map
    graph_fd.write("edge %d %d\n" % (start, end))
            #(start, end, g.nodes[id_map[start]]["type"], g.nodes[id_map[end]]["type"]))

def write_graph_def(stmt_id, var_id):
    global graph_fd
    graph_fd.write("def %d %d\n" % (stmt_id, var_id))

def write_graph_use(stmt_id, var_id):
    global graph_fd
    graph_fd.write("use %d %d\n" % (stmt_id, var_id))

def write_graph_sink(i):
    global graph_fd
    graph_fd.write("sink %d\n" % (i))

def write_graph_source(i):
    global graph_fd
    graph_fd.write("source %d\n" % (i))

def write_graph_node_def_use():
    global idc
    global stmt_defs
    global stmt_uses
    global var_map
    global id_map
    for i in range(0, idc): # idc itself has not been used yet
        orig_id = id_map[i]
        for d in stmt_defs.get(orig_id, []):
            write_graph_def(i, var_map[d])
        for u in stmt_uses.get(orig_id, []):
            write_graph_use(i, var_map[u])

def write_copied_cfg(g, func_entry, depth=0):
    global idc

    # orig id -> new id
    id_translation = {func_entry : idc}
    id_map[idc] = func_entry
    idc += 1
    
    func_exit = int(g.nodes[func_entry]["exit_id"])
    id_translation[func_exit] = idc
    id_map[idc] = func_exit
    idc += 1

    work = [func_entry]
    while work:
        cur = work.pop()
        for s in g.adj[cur]:
            #if s == func_exit: continue

            e = g.get_edge_data(cur, s)[0]

            if e["label"] == "INTERPROC":
                arg_exit = e["exit_id"]

                #assert e["type"] == "entry" and \
                #        arg_exit not in id_translation
                if arg_exit in id_translation:
                    import pdb; pdb.set_trace()

                id_translation[arg_exit] = idc
                id_map[idc] = arg_exit
                idc += 1

                if s in id_translation: 
                    # recursion
                    assert s == func_entry
                    write_graph_edge(id_translation[cur], id_translation[s])
                    write_graph_edge(id_translation[func_exit], id_translation[arg_exit])
                else:
                    # regular function call
                    call_entry, call_exit = write_copied_cfg(g, s, depth + 1)
                    write_graph_edge(id_translation[cur], call_entry)
                    write_graph_edge(call_exit, id_translation[arg_exit])
                # the only way to the arg exit is through the function,
                # so in either case we want to add it
                work.append(arg_exit)

            elif s not in id_translation:
                # we haven't visited s yet, add it to work
                id_translation[s] = idc
                id_map[idc] = s
                idc += 1
                work.append(s)
                write_graph_edge(id_translation[cur], id_translation[s])

            else:
                # we have visited s, or it is the func_entry/exit, in
                # either case, do not continue
                write_graph_edge(id_translation[cur], id_translation[s])

    return id_translation[func_entry], id_translation[func_exit]

def write_graph_sinks_and_sources():
    global sinks
    global sources
    global idc
    global id_map
    for i in range(0, idc):
        if id_map[i] in sinks:
            write_graph_sink(i)
        elif id_map[i] in sources:
            write_graph_source(i)

def save_maps():
    global idc
    global id_map
    out_fd = open("tmp/id_map.csv", "w+")
    for i in range(0, idc):
        out_fd.write(str(i) + "," + str(id_map[i]) + "\n")
    out_fd.close()

    # dont need this actually
    global var_map
    out_fd = open("tmp/var_map.csv", "w+")
    for name, i in var_map.iteritems():
        out_fd.write(str(i) + "," + name + "\n")
    out_fd.close()


idc = 0
# copied id -> orig id
id_map = {}
graph_fd = open("tmp/graph.txt", "w+")
g = None
copied_funcs = 0
unique_funcs = set()
def main():
    global graph_fd, copied_funcs
    global g

    load_sinks()
    load_sources()
    load_def_use_info()

    g = graph_from_json()
    write_copied_cfg(g, 31307)
    print "Total funcs: " + str(copied_funcs)
    print "Unique funcs: " + str(len(unique_funcs))
    write_graph_node_def_use()
    write_graph_sinks_and_sources()

    save_maps()

    graph_fd.close()

if __name__ == "__main__":
    main()
