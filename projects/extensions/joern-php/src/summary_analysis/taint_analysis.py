from collections import defaultdict, OrderedDict
from common import *
import re, sys
from make_graph import Symbol

pred = defaultdict(list) # AKA parent
succ = defaultdict(list) # AKA adjacent

# {node -> set(var)}
DEF = defaultdict(set)

# set((var, loc))
STARDEF = set()

# {node -> set(var)}
USE = defaultdict(set)

# {node -> set(var)}
KILL = defaultdict(set)
# set(stmt)
NOKILL = set()

# {func start -> Summary}
SUMMARY = {}

# {call site -> func start}
CALLS = {}
# {func start -> [caller]}
CALLEEOF = defaultdict(list)

# {node -> branch type}
BRANCH = {}

# {branch -> [dependent node]}
CTRLDEP = defaultdict(list)

# set(stmt)
EXITS = set()
ECHO = set()
SOURCE = set()

# {func start -> set(func param)}
func_params = defaultdict(list)
function = OrderedDict()
funcids = {}
in_func = {}

# {var -> var name}
var_map = None
reverse_var_map = None
id_map = None
reverse_id_map = None


output = {}

class Summary():
    def __init__(self, start, end):
        self.start = start
        self.end = end
        self.summarries_for_vars = {}
        self.tainted_echos_for_vars = {}
        # set((var))
        self.input_vars = set()
        self.get_input_vars()

    def do_it_all(self):
        global var_map
        for v in self.input_vars:
            if var_map[v].startswith(Symbol.field_prefix):
                in_var = (v, True, self.start, "")
            elif var_map[v].endswith(Symbol.unknown_index):
                self.gen_set_for_var((v, True, self.start, ""))
                in_var = (v, False, self.start, "")
            else:
                in_var = (v, False, self.start, "")
            self.gen_set_for_var(in_var)
        #self.gen_set_for_var("*")

    def print_summarries_for_vars(self):
        global var_map
        for v, gen in self.summarries_for_vars.iteritems():
            print var_map[v[0]] + ": " + ", ".join(map(lambda va: var_map[va[0]], gen))

    ret_re = re.compile("[0-9].*_ret")
    def get_input_vars(self):
        """
        Input vars to this function include: formal params, globals, and
        fields (because of our approximation).
        We can determine the globals and fields by taking all of the
        used variables, and subtracting from it the set of variables
        killed at the function's end. The params are stored in
        func_params.
        """
        global USE, KILL, func_params, var_map, reverse_var_map, funcids

        for n in all_nodes_for_region((self.start, self.end)):
            if n in CALLS:
                self.input_vars |= SUMMARY[CALLS[n]].input_vars
            else:
                self.input_vars |= USE[n]
        self.input_vars.difference_update(KILL[self.end])
        self.input_vars.difference_update(filter(lambda v: Summary.ret_re.match(var_map[v]), self.input_vars))
        not_our_vars = filter(
                lambda v: var_map[v][0] in "0987654321" and \
                          not var_map[v].startswith(str(funcids[self.start])),
                        self.input_vars)
        KILL[self.end].update(not_our_vars)
        if str(funcids[self.start]) + "_extra_param" in reverse_var_map:
            KILL[self.end].add(reverse_var_map[str(funcids[self.start]) + "_extra_param"])
        self.input_vars.difference_update(not_our_vars)
        self.input_vars.update(func_params[self.start])



    # live_var: (var name, star def, loc)
    def gen_set_for_var(self, live_var):
        if live_var == "*":
            # special case: everything is tainted
            if "*" not in self.summarries_for_vars:
                res, echos = summarize_var_for_region(None, (self.start, self.end), everything_is_tainted=True)
                self.summarries_for_vars["*"] = res
                self.tainted_echos_for_vars["*"] = echos
            return self.summarries_for_vars["*"], self.tainted_echos_for_vars["*"]

        if live_var[0] not in self.input_vars:
            return set(), set()
        elif (live_var[0], live_var[1]) not in self.summarries_for_vars:
            res, echos = summarize_var_for_region(live_var, (self.start, self.end))
            self.summarries_for_vars[(live_var[0], live_var[1])] = res
            self.tainted_echos_for_vars[(live_var[0], live_var[1])] = echos

            if live_var not in res:
                if live_var[1]:
                    KILL[self.start].add(self.start)
                else:
                    DEF[self.start].add(live_var[0])

        return self.summarries_for_vars[(live_var[0], live_var[1])], self.tainted_echos_for_vars[(live_var[0], live_var[1])]


def load_graph():
    global pred, succ, var_map, reverse_var_map, id_map, reverse_id_map, func_params, function, funcids, in_func
    global DEF, STARDEF, USE, KILLS, NOKILL, CALLS, CALLEEOF, EXITS, ECHO, SOURCE, CTRLDEP

    fd = open("tmp/edge.csv", "rb")
    for line in fd:
        start, end = map(int, line.strip().split("\t"))
        succ[start].append(end)
        pred[end].append(start)
    fd.close()

    fd = open("tmp/def.csv", "rb")
    for line in fd:
        stmt, var = map(int, line.strip().split("\t"))
        DEF[stmt].add(var)
    fd.close()

    fd = open("tmp/star_def.csv", "rb")
    for line in fd:
        stmt, var = map(int, line.strip().split("\t"))
        STARDEF.add((var, stmt))
    fd.close()

    fd = open("tmp/use.csv", "rb")
    for line in fd:
        stmt, var = map(int, line.strip().split("\t"))
        USE[stmt].add(var)
    fd.close()

    fd = open("tmp/kill.csv", "rb")
    for line in fd:
        stmt, var = map(int, line.strip().split("\t"))
        KILL[stmt].add(var)
    fd.close()

    fd = open("tmp/nokill.csv", "rb")
    for line in fd:
        stmt = int(line.strip())
        NOKILL.add(stmt)
    fd.close()

    fd = open("tmp/call.csv", "rb")
    for line in fd:
        call_site, callee = map(int, line.strip().split("\t"))
        CALLS[call_site] = callee
        CALLEEOF[callee].append(call_site)
    fd.close()

    fd = open("tmp/exit.csv", "rb")
    for line in fd:
        EXITS.add(int(line.strip()))
    fd.close()

    fd = open("tmp/sink.csv", "rb")
    for line in fd:
        ECHO.add(int(line.strip()))
    fd.close()

    fd = open("tmp/source.csv", "rb")
    for line in fd:
        SOURCE.add(int(line.strip()))
    fd.close()

    fd = open("tmp/param.csv", "rb")
    for line in fd:
        func, param = map(int, line.strip().split("\t"))
        func_params[func].append(param)
    fd.close()

    fd = open("tmp/branch.csv", "rb")
    for line in fd:
        n, branch_type = map(int, line.strip().split("\t"))
        BRANCH[n] = branch_type
    fd.close()

    fd = open("tmp/ctrldep.csv", "rb")
    for line in fd:
        n, branch = map(int, line.strip().split("\t"))
        CTRLDEP[branch].append(n)
    fd.close()

    fd = open("tmp/function.csv", "rb")
    for line in fd:
        start, end, funcid = map(int, line.strip().split("\t"))
        function[start] = end
        funcids[start] = funcid
    fd.close()

    for start, end in function.iteritems():
        nodes = all_nodes_for_region((start, end))
        for n in nodes:
            in_func[n] = start

    var_map, reverse_var_map = load_var_map()
    id_map, reverse_id_map = load_id_map()

def all_nodes_for_region(region):
    global succ
    reachable = set()
    work = [region[0]]
    while work:
        cur = work.pop()
        if cur in reachable:
            continue
        reachable.add(cur)
        if cur != region[1]:
            work.extend(succ[cur])
        if cur != region[0]: # hack for handling recursion...
            work.extend(pred[cur])
    assert region[1] in reachable
    return reachable

# in_set: set((var name, isStart, loc))
# n: a node id
def IN_minus_KILL(in_set, n):
    global DEF, STARDEF, KILL, NOKILL
    in_set.difference_update(
            filter( 
                # lambda should match defs we _want_
                # to remove
                lambda d: n not in NOKILL and \
                          ((d[0] in DEF[n] and \
                        (d[0], d[2]) not in STARDEF) or \
                        d[0] in KILL[n]),
                in_set
            )
    )

def add_succ(cur, changed, all_nodes, region):
    global succ
    if cur != region[1]:
        changed.update(succ[cur])
    else:
        # same hack to handle recursion...
        changed.update(filter(lambda x: x in all_nodes, succ[cur]))

def make_all_defs_live(n):
    global DEF, STARDEF
    raise NotImplementedError()
    tmp = set()
    for d in DEF[n]:
        tmp.add((d, (d, n) in STARDEF, n))
    return tmp

# var: (var name, star def, loc, call string) OR None
# region: (region start, region end)
# init_OUT: [((var name, star def, loc, call string), n)]
def summarize_var_for_region(var, region, init_OUT=[], everything_is_tainted=False):
    global pred, succ, SUMMARY, DEF, USE, ECHO, BRANCH, CTRLDEP, tainted_branches
    # set((var name, star def, loc, call string))
    cur_in = set()
    tmp = set()
    # set((var name, star def, loc, call string))
    gen = set()

    #sys.stderr.write("summarize_var_for_region args:\n")
    #sys.stderr.write("var: " + str(var) + "\n")
    #sys.stderr.write("region : " + str(region) + "\n")
    #sys.stderr.write("init_OUT : " + str(init_OUT) + "\n")
    #sys.stderr.write("everything_is_tainted : " + str(everything_is_tainted) + "\n")


    # set(node id)
    changed = all_nodes_for_region(region)
    all_nodes = set(changed)

    echos = set()

    # {node -> (var name, star def, loc, call string)}
    OUT = {}
    for n in all_nodes:
        OUT[n] = set()

    for d, n in init_OUT:
        OUT[n].add(d)

    if everything_is_tainted:
        for n in all_nodes:
            if n in CALLS:
                tmp_gen, tmp_echos = SUMMARY[CALLS[n]].gen_set_for_var("*")
                OUT[n].update(tmp_gen)
                echos.update(tmp_echos)
            else:
                OUT[n].update(make_all_defs_live(n))

    if var:
        OUT[region[0]].add(var)

    while changed:
        cur = changed.pop()
        
        if cur in EXITS:
            OUT[cur].clear()
            continue

        cur_in.clear()
        for p in pred[cur]:
            cur_in |= OUT[p]

        gen.clear()
        if cur in CALLS:
            # function summary
            for input_var in cur_in:
                tmp_gen, tmp_echos = SUMMARY[CALLS[cur]].gen_set_for_var(input_var)
                gen.update(map(lambda d: (d[0], d[1], d[2], str(cur) + ":" + d[3]), tmp_gen))
                echos.update(tmp_echos)
            IN_minus_KILL(cur_in, CALLS[cur])
        elif filter(lambda d: d[0] in USE[cur], cur_in):
            # regular statement, and we are using a livedef
            gen.update(map(
                    lambda d: (d, (d, cur) in STARDEF, cur, ""),
                    DEF[cur]))
            if cur in ECHO:
                echos.add(cur)

            if BRANCH.get(cur, 0) != 0 and False: # disable implicit flows temporarily
                if cur not in tainted_branches:
                    tainted_branches[cur] = 1
                for n in CTRLDEP[cur]:
                    if n in ECHO:
                        echos.add(n)

                    tmp.clear()
                    if n in CALLS:
                        # TODO: prepend call site
                        tmp_gen, tmp_echos = SUMMARY[CALLS[n]].gen_set_for_var("*")
                        tmp.update(tmp_gen)
                        echos.update(tmp_echos)
                    else:
                        tmp.update(make_all_defs_live(n))
                    if tmp - OUT[n]:
                        OUT[n].update(tmp)
                        add_succ(n, changed, all_nodes, region)

            IN_minus_KILL(cur_in, cur)

        cur_in.update(gen)
        if cur_in != OUT[cur]:
            OUT[cur].update(cur_in)
            add_succ(cur, changed, all_nodes, region)

    return map(lambda d: (d[0], d[1], d[2], str(region[0]) + "," + d[3]), OUT[region[1]]), echos


tainted_branches = OrderedDict()
def do_taint_analysis():
    global SOURCE, DEF, STARDEF, CALLEEOF, in_func, function
    tainted_echos = set()
    # {func -> set(((var name, star def, loc), live at stmt))
    work = OrderedDict()
    for s in SOURCE:
        func = in_func[s]
        if func not in work:
            work[func] = set()
        for d in DEF[s]:
            work[func].add(((d, (d, s) in STARDEF, s, ""), s))

    while work:
        func, init_OUT = work.popitem()
        out, tmp_echos = summarize_var_for_region(None, (func, function[func]), init_OUT)
        tainted_echos.update(tmp_echos)

        for call_site in CALLEEOF[func]:
            caller_region = in_func[call_site]
            if caller_region not in work:
                work[caller_region] = set()
            work[caller_region].update(map(lambda d: (d, call_site), out))

    fd = open("tmp/tainted_echos", "w+")
    fd.write("\n".join(map(str, sorted(map(lambda e: id_map[e], tainted_echos)))) + "\n")
    fd.close()

    fd = open("tmp/tainted_branches", "w+")
    fd.write("\n".join(map(str, sorted(map(lambda e: id_map[e], reversed(tainted_branches))))) + "\n")
    fd.close()

def init_summarries():
    for (start, end) in function.iteritems():
        s = Summary(start, end)
        SUMMARY[start] = s

def test():
    global SUMMARY
    load_graph()
    init_summarries()
    SUMMARY[4888].do_it_all()
    pass

def main():
    load_graph()
    init_summarries()
    do_taint_analysis()

if __name__ == "__main__":
    main()
