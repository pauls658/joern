from collections import defaultdict
import networkx as nx

id_map = {}
rev_id_map = defaultdict(list)
post_dom_tree = defaultdict(list)
def load_graph():
    global id_map, post_dom_tree
    g = nx.MultiDiGraph()

    fd = open("tmp/id_map.csv", "r")
    for line in fd:
        new, orig = map(int, line.split(","))
        id_map[new] = orig
        rev_id_map[orig].append(new)
        g.add_node(new)
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


def find_instrumentation_point(g, sink):
    pdoms = post_dominates(sink)
    instr_points = []

    visited = set([sink])
    work = [sink]
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
        else:
            # otherwise, we stop here
            instr_points.append(cur)

    return instr_points
       
def test(sink):
    global rev_id_map, id_map
    g = load_graph()
    return map(lambda x: id_map[x], find_instrumentation_point(g, rev_id_map[sink][0]))

if __name__ == "__main__":
    main()
