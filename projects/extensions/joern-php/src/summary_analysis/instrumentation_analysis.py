from collections import defaultdict
import networkx as nx
from common import *

id_map = {}
rev_id_map = defaultdict(list)
post_dom_tree = defaultdict(list)
def load_graph():
    global id_map, post_dom_tree
    g = nx.MultiDiGraph()
    c_nodes = load_cypher_graph_nodes()

    fd = open("tmp/id_map.csv", "r")
    for line in fd:
        new, orig = map(int, line.split(","))
        id_map[new] = orig
        rev_id_map[orig].append(new)
        g.add_node(new)
        g.nodes[new]["has_concat"] = c_nodes[orig]["has_concat"]
        g.nodes[new]["funcid"] = c_nodes[orig]["funcid"]
    fd.close()

    fd = open("tmp/handleable.csv", "r")
    for line in fd:
        new, handleable = map(int, line.split(","))
        handleable = bool(handleable)
        g.nodes[new]["handleable"] = handleable
    fd.close()

    fd = open("tmp/cfg_exit", "r")
    # get the traslated id of cfg exit
    cfg_exit = rev_id_map[int(fd.read().strip())][0]
    fd.close()

    fd = open("tmp/edge.csv", "r")
    for line in fd:
        start, end = map(int, line.split("\t"))
        # reverse the direction right now to compute
        # the post dominators
        g.add_edge(end, start, label="FLOWS_TO")
    fd.close()

    # Need to compute this before we add REACHES edges.
    # reverse the order of the dict so we can know who
    # is post dominated by a given node
    for k, v in nx.immediate_dominators(g, cfg_exit).iteritems():
        post_dom_tree[v].append(k)

    fd = open("datadep.csv", "r")
    for line in fd:
        d, use, var_id = map(int, line.split("\t"))
        g.add_edge(d, use, label="REACHES", var=var_id)
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

def instr_analysis(g, pdoms, cur, fid, visited):
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
    elif all([pred in pdoms and  
             g.node[pred]["handleable"]
             for pred in preds]):
        # if all preds are post dominated by the sink
        # and all preds are handleable, we can take
        # another step back
        pts = []
        for p in preds:
            pts.extend(instr_analysis(g, pdoms, p, fid, visited))
        if any([g.node[i]["funcid"] != fid for i in pts]):
            return [cur]
        else:
            return pts
    else:
        return [cur]


def find_instrumentation_point2(g, sink):
    pdoms = post_dominates(sink)
    visited = set([sink])
    instr_points = instr_analysis(g, pdoms, sink, g.node[sink]["funcid"], visited)

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
    tainted = load_souffle_res()
    var_map, _ = load_var_map()
    # tainted sink -> [instr points]
    instr_result = defaultdict(lambda: defaultdict(set))
    ccfg_ids = defaultdict(list)
    for sink in tainted:
        for i, vs in find_instrumentation_point2(g, sink).iteritems():
            instr_i = id_map[i]
            ccfg_ids[instr_i].append(i)
            instr_result[id_map[sink]][instr_i].update(vs)

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

