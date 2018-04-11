from collections import defaultdict
import sys,os
import glob

import json
import networkx as nx

def graph_from_json():
    g = nx.MultiDiGraph()

    rows = json.load(open("nodes.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"][0]
        i = int(row["id"])
        g.add_node(i, **row) 

    rows = json.load(open("rels.json", "rb"))["results"][0]["data"]
    for row in rows:
        row = row["row"]
        rel_props = row[1]
        i = int(rel_props["id"])
        g.add_edge(int(row[0]), int(row[2]), **rel_props)

    return g

def reachables(g, start):
    reachable = set()
    echos = set()
    for nid in nx.dfs_preorder_nodes(g, start):
        if g.node[nid]['type'] == "AST_ECHO":
            echos.add(nid)
        reachable.add(nid)
    return reachable, echos

def post_doms(g, echos):
    rg = g.reverse(copy=True)
    # exit_id -> [echos]
    groupd_echos = defaultdict(list)
    for eid in echos: groupd_echos[g.node[eid]['exit_id']].append(eid)
    # echo -> post doms
    res = {}

    loops, total = 0, float(len(echos)) #debugging only
    for (exit_id, echos) in groupd_echos.iteritems():
        idoms = nx.immediate_dominators(rg, exit_id)
        for echo in echos:
            post_doms = []
            q = [echo]
            while q:
                cur = q.pop()
                preds = list(g.predecessors(cur))
                try:
                    if all([idoms[pred] == cur and g.node[pred].get('handleable', True) for pred in preds]):
                        q.extend(preds)
                    else:
                        post_doms.append(cur)
                except KeyError as e:
                    print e
                    print "exit_id: %d, echo_id: %d" % (exit_id, echo)
                    sys.exit(1)
    
            res[echo] = post_doms
            loops += 1
            print "%.2f complete" % (float(loops)/total)

    return res

def dump_pdoms(g, pdoms):
    script_dir = os.path.dirname(os.path.realpath(__file__))
    files = glob.glob(script_dir + "/pdoms/*")    
    for f in files:
        os.remove(f)
    # node type -> list of dom nodes
    doms_by_type = defaultdict(list)
    for node, doms in pdoms.iteritems():
        for dom in doms:
            doms_by_type[g.node[dom]['type']].append(dom)

    for t, doms in doms_by_type.iteritems():
        with open("%s/pdoms/%s.csv" % (script_dir, t), "w+") as out:
            out.write("pdoms\n")
            for dom in doms:
                out.write("%s\n" %
                        (str(dom)))
    
def analyze(g, start):
    reachable, echos = reachables(g, start)
    return post_doms(g, echos)

def main():
    g = graph_from_json() 
    pdoms = analyze(g, 161951) 
    dump_pdoms(g, pdoms)

if __name__ == "__main__":
    main()
