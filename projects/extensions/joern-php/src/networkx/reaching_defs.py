import json
import networkx as nx

def graph_from_json():
    g = nx.MultiDiGraph()

    rows = json.load(open("nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        g.add_node(i, **row) 

    rows = json.load(open("flows_to.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        g.add_edge(int(row[0]), int(row[1]), label="FLOWS_TO")

    rows = json.load(open("interproc.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        rel_props = row[2]
        rel_props["label"] = "INTERPROC"
        g.add_edge(int(row[0]), int(row[1]), **rel_props)

    return g

def profile_memory():
    from memory_profiler import memory_usage
    mem_usage = memory_usage(graph_from_json)
    print('Maximum memory usage: %s' % max(mem_usage))

def print_datalog_edge(start, end):
    print "edge(%d, %d)" % (start, end)

def copy_cfg(g, func_entry):
    global idc
    # orig id -> new id
    id_translation = {func_entry : idc}
    idc += 1

    func_exit = int(g.nodes[func_entry]["exit_id"])
    id_translation[func_exit] = idc
    idc += 1

    work = [func_entry]
    while work:
        cur = work.pop()
        for s in g.adj[cur]:
            if s == func_exit: continue

            e = g.get_edge_data(cur, s)[0]

            if e["label"] == "INTERPROC":
                arg_exit = e["exit_id"]

                #assert e["type"] == "entry" and \
                #        arg_exit not in id_translation
                if arg_exit in id_translation:
                    import pdb; pdb.set_trace()

                id_translation[arg_exit] = idc
                idc += 1

                if s in id_translation: 
                    # recursion
                    assert s == func_entry
                    print_datalog_edge(id_translation[cur], id_translation[s])
                    print_datalog_edge(id_translation[func_exit], id_translation[arg_exit])
                else:
                    call_entry, call_exit = copy_cfg(g, s)
                    print_datalog_edge(id_translation[cur], call_entry)
                    print_datalog_edge(call_exit, id_translation[arg_exit])
                    work.append(arg_exit)

            elif s not in id_translation:
                id_translation[s] = idc
                idc += 1
                work.append(s)
                print_datalog_edge(id_translation[cur], id_translation[s])

            else:
                print_datalog_edge(id_translation[cur], id_translation[s])

    return id_translation[func_entry], id_translation[func_exit]

idc = 0
def main():
    g = graph_from_json()
    copy_cfg(g, 23499)

if __name__ == "__main__":
    main()
