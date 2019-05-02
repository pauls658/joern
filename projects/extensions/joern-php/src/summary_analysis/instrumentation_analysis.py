from collections import defaultdict
import networkx as nx
from common import *

id_map = {}
rev_id_map = defaultdict(list)
var_map = None
rev_var_map = None
def load_graph():
    global id_map, rev_id_map, ctx_id_map, var_map, rev_var_map
    g = nx.MultiDiGraph()
    c_nodes = load_cypher_graph_nodes()

    fd = open("tmp/id_map.csv", "r")
    for line in fd:
        new, orig = map(int, line.split(","))
        id_map[new] = orig
        rev_id_map[orig].append(new)
    fd.close()

    var_map, rev_var_map = load_var_map()

    fd = open("tmp/data_deps", "r")
    for line in fd:
        pieces = map(lambda s: s.strip(), line.split("\t"))
        use_ctx = (pieces[0], int(pieces[1]))
        h = c_nodes[id_map[use_ctx[1]]]["handleable"]
        fid = c_nodes[id_map[use_ctx[1]]]["funcid"]
        g.add_node(use_ctx, handleable=h, funcid=fid)
        pieces = pieces[2:]
        for i in range(0, len(pieces), 3):
            def_ctx = (pieces[i+2], int(pieces[i+1]))
            h = c_nodes[id_map[def_ctx[1]]]["handleable"]
            fid = c_nodes[id_map[def_ctx[1]]]["funcid"]
            g.add_node(def_ctx, handleable=h, funcid=fid)
            g.add_edge(def_ctx, use_ctx, label="REACHES", var=int(pieces[i]))
    fd.close()


    return g

# computes the set of nodes post dominated by n
def post_dominates(n):
    global post_dom_tree
    post_dominates = set()
    # DFS, no need for visited set since this is a tree
    work = [n]
    while work:
        cur = work.pop()
        post_dominates.add(cur)
        work.extend(post_dom_tree[cur])
    return post_dominates

def instr_analysis(g, cur, fid, visited):
    preds = filter(
            lambda (s,e,d): d["label"] == "REACHES",
            g.in_edges([cur], data=True))

    if not preds:
        return [cur]

    preds = filter(lambda (s,e,d): s not in visited, preds)
    preds = map(lambda (s,e,d): s, preds)
    visited.update(preds)

    if not preds:
        return []
    elif all([
             g.node[pred]["handleable"]
             for pred in preds]):
        # if all preds are post dominated by the sink
        # and all preds are handleable, we can take
        # another step back
        pts = []
        for p in preds:
            pts.extend(instr_analysis(g, p, fid, visited))
        if any([g.node[i]["funcid"] != fid for i in pts]):
            return [cur]
        else:
            return pts
    else:
        return [cur]


def find_instrumentation_point2(g, sink):
    visited = set([sink])
    instr_points = instr_analysis(g, sink, g.node[sink]["funcid"], visited)

    # grab the tainted variable names
    ret = defaultdict(list)
    for i in instr_points:
        for s,e,d in filter(lambda (s,e,d): d["label"] == "REACHES",
                g.in_edges(i, data=True)):
            ret[i].append(d["var"])
        
    return ret



"""
finds instrumentation points given a CFG with one entry
and one exit and the data dependence edges.
Instrumentation points are returned as pair with a CFG ID and a set
of tainted variables.
"""
def find_instrumentation_point(g, sink):
    pdoms = post_dominates(sink)
    instr_points = []
    visited = set([sink])
    work = [sink]
    has_concat = False
    #import pdb; pdb.set_trace()
    while work:
        cur = work.pop()
        preds = filter(
                lambda (s,e,d): d["label"] == "REACHES",
                g.in_edges([cur], data=True))

        if not preds:
            instr_points.append(cur) # we reached the sink
            continue

        preds = filter(lambda (s,e,d): s not in visited, preds)
        preds = map(lambda (s,e,d): s, preds)
        visited.update(preds)

        if not preds:
            continue
        elif all([pred in pdoms and  
                 g.node[pred]["handleable"]
                 for pred in preds]):
            # if all preds are post dominated by the sink
            # and all preds are handleable, we can take
            # another step back
            work.extend(preds)
            has_concat |= any(map(lambda n: g.node[n]["has_concat"], preds))
        else:
            # otherwise, we stop here
            instr_points.append(cur)

    if not has_concat: # no point in instrumenting anything if 
                       # we can't make any good gainz
        instr_points = [sink]

    ret = defaultdict(list)
    for i in instr_points:
        for s,e,d in filter(lambda (s,e,d): d["label"] == "REACHES",
                g.in_edges(i, data=True)):
            ret[i].append(d["var"])
        
    return ret
       
def test(sink):
    global rev_id_map, id_map
    g = load_graph()
    #return map(lambda x: id_map[x], find_instrumentation_point(g, rev_id_map[sink][0]))
    return find_instrumentation_point(g, rev_id_map[sink][0])

def main():
    global id_map, rev_id_map
    g = load_graph()

    tainted_echos = set()
    fd = open("tmp/tainted_echos", "rb")
    for l in fd:
        pieces = l.strip().split("\t")
        tainted_echos.add((pieces[0], int(pieces[1])))
    fd.close()

    instr_result = defaultdict(lambda: defaultdict(set))
    ccfg_ids = defaultdict(list)
    for sink in tainted_echos:
        for ctx, vs in find_instrumentation_point2(g, sink).iteritems():
            instr_i = id_map[ctx[1]]
            ccfg_ids[instr_i].append(ctx[1])
            instr_result[id_map[sink[1]]][instr_i].update(vs)

    for sink in sorted(instr_result.keys()):
        line = str(sink) + ":\n"
        for instr_point, vs in instr_result[sink].iteritems():
            line += "\t"
            line += str(instr_point) + ": " + ",".join(map(lambda v: var_map[v], vs)) + "\n"
            line += "\t(" + ",".join(map(str, ccfg_ids[instr_point])) + ")"
            line += "\n"
        print line

if __name__ == "__main__":
    main()

