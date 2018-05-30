from collections import defaultdict
import sys,os
import glob
from copy import deepcopy

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
        rel_props["label"] = row[3]
        i = int(rel_props["id"])
        g.add_edge(int(row[0]), int(row[2]), **rel_props)

    return g

class TravObj(object):
    def __init__(self, G, node):
        self.stack = []
        self.node = node
        self.G = G

    def has_ctx(self):
        return bool(self.stack)

    def get_ctx(self):
        return ".".join(map(str, self.stack))

    def __iter__(self):
        self.iter = iter(self.G.adj[self.node])
        return self

    def copy_pop(self, node_id):
        ret = TravObj(self.G, node_id)
        ret.stack = deepcopy(self.stack)
        ret.stack.pop()
        return ret

    def copy_push(self, node_id, ctx):
        ret = TravObj(self.G, node_id)
        ret.stack = deepcopy(self.stack)
        ret.stack.append(ctx)
        return ret

    def copy(self, node_id):
        ret = TravObj(self.G, node_id)
        ret.stack = deepcopy(self.stack)
        return ret

    def next(self):
        # exits either with the return, or
        # stop iteration exception
        while True:
            c = next(self.iter)
            #if self.G.nodes[c].get("globalName", "nope") == "username":
            #if self.node == 364:
            #if self.G.nodes[c].get("globalName", None) is not None:
                #import pdb; pdb.set_trace()
            e = self.G.get_edge_data(self.node, c)[0]
            if e["label"] == "INTERPROC":
                if "type" not in e:
                    import pdb; pdb.set_trace()
                    #return TravObj(self.G, c)
                elif e["type"] == "arg_entry":
                    return self.copy_push(c, int(e["call_id"]))
                elif e["type"] == "arg_exit" or e["type"] == "return":
                    if not self.stack:
                        return TravObj(self.G, c)
                    elif e["call_id"] == self.stack[-1]:
                        return self.copy_pop(c)
                    # else skip child
                else:
                    # global or something... clear stack
                    import pdb; pdb.set_trace()
                    return TravObj(self.G, c)
            else:
                # REACHES
                return self.copy(c)

class CTXSensitiveDFS(object):
    """
    In general, all we only use node ids. e.g. "start" is 
    a node id rather than an actual node.
    """
    def __init__(self, G, start):
        self.ctx_visited = set()
        self.globally_visited = set()
        self.G = G
        self.start = start
        self.reachable = set()
        self.echos = set()

    def have_visited(self, trav_obj):
        if trav_obj.node in self.globally_visited:
            return True
        elif trav_obj.has_ctx():
            return (trav_obj.node, trav_obj.get_ctx()) in self.ctx_visited
        else:
            return False
        
    def add_visited_global(self, trav_obj):
        self.globally_visited.add(trav_obj.node)

    def add_visited_ctx(self, trav_obj):
        self.ctx_visited.add(
                (trav_obj.node, trav_obj.get_ctx())
        )

    def add_visited(self, trav_node):
        self.reachable.add(trav_node.node)
        if self.G.node[trav_node.node]["type"] == "AST_ECHO":
            self.echos.add(trav_node.node)
        if trav_node.has_ctx():
            self.add_visited_ctx(trav_node)
        else:
            self.add_visited_global(trav_node)

    def ctx_sensitive_DFS(self):
        trav_obj = TravObj(self.G, self.start)
        stack = [(trav_obj, iter(trav_obj))]
        self.add_visited(trav_obj)
        while stack:
            trav_obj, reaching_iter = stack[-1]
            try:
                child = next(reaching_iter)
                if not self.have_visited(child):
                    self.add_visited(child)
                    stack.append((child, iter(child)))
            except StopIteration:
                stack.pop()
        return self.reachable, self.echos


def reachables(g, start):
    reachable = set()
    echos = set()
    for nid in nx.dfs_preorder_nodes(g, start):
        if g.node[nid]['type'] == "AST_ECHO":
            echos.add(nid)
        reachable.add(nid)
    return reachable, echos

def post_doms(g, echos, reachable):
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
                preds = list(filter(lambda x: x in reachable, g.predecessors(cur)))
                try:
                    if not preds:
                        post_doms.append(cur)
                    elif all([(idoms[pred] == cur and 
                            g.node[pred]["handleable"]) 
                            for pred in preds]):
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
    trav = CTXSensitiveDFS(g, start)
    reachable, echos = trav.ctx_sensitive_DFS()
    return post_doms(g, echos, reachable)

def main():
    g = graph_from_json() 
    #pdoms = analyze(g, 78) 
    pdoms = analyze(g, 103172) 
    dump_pdoms(g, pdoms)

starts = [39180, 39295, 39301, 39303, 39309, 39315, 39317, 39323, 39332, 39333, 39376, 39387, 39420, 39437, 39465, 39466, 39475, 39484, 39491, 39496, 39502, 39509, 39517, 39613, 39722, 39974, 40002, 40072, 40134, 40135, 40136, 40151, 40155, 40161, 40333, 40353]
def debug(start=39722):
    g = graph_from_json() 
    trav = CTXSensitiveDFS(g, start)
    return trav.ctx_sensitive_DFS()

if __name__ == "__main__":
    main()
