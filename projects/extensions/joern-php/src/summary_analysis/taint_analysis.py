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
# {func start -> [call sites]}
CALLSITESOF = defaultdict(list)

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
funcnames = {}

# {var -> var name}
var_map = None
reverse_var_map = None
id_map = None
reverse_id_map = None

# {(id, ctx) -> set((def var, stmt, ctx))}
REACHINGDEFS = defaultdict(set)

class Summary():
    def __init__(self, start, end):
        self.start = start
        self.end = end
        self.summarries_for_vars = {}
        self.tainted_echos_for_vars = {}
        self.du_pairs_for_vars = {}
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
    # call_string: the current ctx. this should include the func/call site that called us
    # return:
    #   - generated definitions for live var, call string will not include this func/call site - you need to do that yourself!
    #   set((var, star def, loc, call string w/o this func))
    #   - tainted echos, does not include this func/call site like for above
    #   set((call string w/o this func, echo stmt))
    #   - the du pairs, ctx does not in include func/call site
    #   [((def var, star def, loc, ctx), (loc, ctx))]
    def gen_set_for_var(self, live_var, call_string):
        if live_var == "*":
            index = "*"
            # special case: everything is tainted
            if "*" not in self.summarries_for_vars:
                res, echos, du_pairs = summarize_var_for_region(None, (self.start, self.end), everything_is_tainted=True, call_string=call_string)
                self.summarries_for_vars["*"] = res
                self.tainted_echos_for_vars["*"] = echos
                self.du_pairs_for_vars["*"] = du_pairs
            ret = (self.summarries_for_vars["*"],
                   self.tainted_echos_for_vars["*"],
                   self.du_pairs_for_vars["*"])

        elif live_var[0] not in self.input_vars:
            return set(), set(), set()

        else:
            index = (live_var[0], live_var[1])
            star_live_var = (live_var[0], live_var[1], "*", "*")
            if (live_var[0], live_var[1]) not in self.summarries_for_vars:
                res, echos, du_pairs = summarize_var_for_region(star_live_var, (self.start, self.end), call_string=call_string)
                self.summarries_for_vars[index] = res
                self.tainted_echos_for_vars[index] = echos
                self.du_pairs_for_vars[index] = du_pairs
                if star_live_var not in res:
                    if live_var[1]:
                        KILL[self.start].add(live_var[0])
                    else:
                        DEF[self.start].add(live_var[0])

            ret = (map(lambda d: live_var if d == star_live_var else d, self.summarries_for_vars[index]),
                   self.tainted_echos_for_vars[index],
                   map(lambda (d, u): (live_var, u) if d == star_live_var else (d, u), self.du_pairs_for_vars[index]))
            #du_pairs = map(lambda (d, u): (live_var, u) if d == star_live_var else (d, u), self.du_pairs_for_vars[index])

        #output_du_pairs(du_pairs, call_string)

        return ret


def load_graph():
    global pred, succ, var_map, reverse_var_map, id_map, reverse_id_map, func_params, function, funcids, in_func, funcnames
    global DEF, STARDEF, USE, KILLS, NOKILL, CALLS, CALLEEOF, EXITS, ECHO, SOURCE, CTRLDEP, CALLSITESOF

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
        pieces = line.strip().split("\t")
        start, end, funcid = map(int, pieces[:3])
        function[start] = end
        funcids[start] = funcid
        funcnames[start] = pieces[3]
    fd.close()

    for start, end in function.iteritems():
        nodes = all_nodes_for_region((start, end))
        for n in nodes:
            in_func[n] = start

    for call_site in CALLS.keys():
        CALLSITESOF[in_func[call_site]].append(call_site)

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
    return map(lambda d: (d, (d, n) in STARDEF, n, ""), DEF[n])

# ctx operations
# ctx format: 0,[<call site>:<func entry>,]*
def get_top_ctx(ctx):
    return ctx.split(",")[-2]

def pop_top_ctx(ctx):
    return ctx[:ctx.rindex(",",0,-1) + 1]

all_ctx = []
def all_contexts(cur_ctx="0,"):
    global CALLSITESOF, CALLS, all_ctx

    cur_func = int(get_top_ctx(cur_ctx).split(":")[-1])
    all_ctx.append(cur_ctx)
    for call_site in CALLSITESOF[cur_func]:
        all_contexts(cur_ctx + str(call_site) + ":" + str(CALLS[call_site]) + ",")

def contexts_of_stmt(stmt):
    global in_func, all_ctx
    func = in_func[stmt]
    return filter(lambda ctx: (":" + ctx).endswith(":" + str(func) + ","), all_ctx)

# du_pairs: set(((var name, star def, loc, top half of ctx w/o top-most call), (stmt, ctx)))
# ctx: bottom half of ctx
def output_du_pairs(du_pairs, ctx):
    global REACHINGDEFS
    for (d, u) in du_pairs:
        REACHINGDEFS[(u[0], ctx + u[1])].add((d[0], d[2], ctx + d[3]))

# var: (var name, star def, loc, call string) OR None
# region: (region start, region end)
# init_OUT: [((var name, star def, loc, call string), n)]
# call_string: the bottom half of the call string. this should end with a "<call site>:<func entry>," or just "<func entry>,".
# this should include function/call site that called us
#
# return:
#   - generated definitions for live var, call string will not include this func/call site - you need to do that yourself!
#   set((var, star def, loc, call string w/o this func))
#   - tainted echos, does not include this func/call site like for above
#   set((call string w/o this func, echo stmt))
#   - the du pairs, ctx does not in include func/call site
#   [((def var, star def, loc, ctx), (loc, ctx))]
def summarize_var_for_region(var, region, init_OUT=[], everything_is_tainted=False, call_string=None):
    global pred, succ, SUMMARY, DEF, USE, ECHO, BRANCH, CTRLDEP, tainted_branches
    if not call_string:
        raise Exception("call string was not defined")

    # set((var name, star def, loc, call string))
    cur_in = set()
    tmp = set()
    # set((var name, star def, loc, call string))
    gen = set()

    # sys.stderr.write("summarize_var_for_region args:\n")
    # sys.stderr.write("var: " + str(var) + "\n")
    # sys.stderr.write("region : " + str(region) + "\n")
    # sys.stderr.write("init_OUT : " + str(init_OUT) + "\n")
    # sys.stderr.write("everything_is_tainted : " + str(everything_is_tainted) + "\n")


    # set(node id)
    changed = all_nodes_for_region(region)
    all_nodes = set(changed)

    echos = set()
    # [((def var, star def, loc, ctx), (loc, ctx))]
    du_pairs = set()

    # {node -> (var name, star def, loc, call string)}
    OUT = {}
    for n in all_nodes:
        OUT[n] = set()

    for d, n in init_OUT:
        OUT[n].add(d)

    if everything_is_tainted:
        for n in all_nodes:
            if n in CALLS:
                this_call_site = str(n) + ":" + str(CALLS[n]) + ","
                tmp_gen, tmp_echos, tmp_du_pairs = SUMMARY[CALLS[n]].gen_set_for_var("*", call_string + this_call_site)
                OUT[n].update(map(lambda d: (d[0], d[1], d[2], this_call_site + d[3]), tmp_gen))
                du_pairs.update(map(lambda (d, l): ((d[0], d[1], d[2], this_call_site + d[3]), (l[0], this_call_site + l[1])), tmp_du_pairs))
                for tmp_e in tmp_echos:
                    echos.add((this_call_site + tmp_e[0], tmp_e[1]))
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
            for iv in cur_in:
                this_call_site = str(cur) + ":" + str(CALLS[cur]) + ","
                tmp_gen, tmp_echos, tmp_du_pairs = SUMMARY[CALLS[cur]].gen_set_for_var(iv, call_string + this_call_site)
                gen.update(map(lambda d: (d[0], d[1], d[2], this_call_site + d[3]) if d != iv else d, tmp_gen))
                du_pairs.update(map(lambda (d, l): ((d[0], d[1], d[2], this_call_site + d[3]) if d != iv else d, (l[0], this_call_site + l[1])), tmp_du_pairs))
                for tmp_e in tmp_echos:
                    echos.add((this_call_site + tmp_e[0], tmp_e[1]))
            IN_minus_KILL(cur_in, CALLS[cur])
        else:
            used_defs = filter(lambda d: d[0] in USE[cur], cur_in)
            if used_defs:
                # regular statement, and we are using a livedef
                # first add the du pairs
                du_pairs.update(map(lambda d: (d, (cur, "")), used_defs))

                gen.update(map(
                        lambda d: (d, (d, cur) in STARDEF, cur, ""),
                        DEF[cur]))
                if cur in ECHO:
                    echos.add(("", cur))

                if BRANCH.get(cur, 0) != 0:
                    if cur not in tainted_branches:
                        tainted_branches[cur] = 1
                    for n in CTRLDEP[cur]:
                        if n in ECHO:
                            echos.add(("", n))

                        tmp.clear()
                        if n in CALLS:
                            this_call_site = str(n) + ":" + str(CALLS[n]) + ","
                            tmp_gen, tmp_echos, tmp_du_pairs = SUMMARY[CALLS[n]].gen_set_for_var("*", call_string + this_call_site)
                            tmp.update(map(lambda d: (d[0], d[1], d[2], this_call_site + d[3]), tmp_gen))
                            du_pairs.update(map(lambda (d, l): ((d[0], d[1], d[2], this_call_site + d[3]), (l[0], this_call_site + l[1])), tmp_du_pairs))
                            for tmp_e in tmp_echos:
                                echos.add((this_call_site + tmp_e[0], tmp_e[1]))
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


    return OUT[region[1]], echos, du_pairs


tainted_branches = OrderedDict()
def do_taint_analysis():
    global SOURCE, DEF, STARDEF, CALLEEOF, in_func, function
    # ctx format: 0,[<call site>:<func entry>,]*
    # set((ctx, stmt))
    tainted_echos = set()
    # { ctx -> set((var name, star def, loc, top half of call string not including the region), loc) }
    work = OrderedDict()
    for s in SOURCE:
        for ctx in contexts_of_stmt(s):
            if ctx not in work:
                work[ctx] = set()
            for d in DEF[s]:
                work[ctx].add(((d, (d, s) in STARDEF, s, ""), s))

    # gen:
    # set((var name, star def, loc, top half of ctx not including the func))
    # tmp_echos:
    # set((top half of ctx not including func, stmt))
    # du_pairs:
    # set(((var name, star def, loc, top half of ctx not including func), stmt))
    # ctx:
    # should include the func
    while work:
        ctx, init_OUT = work.popitem(last=False)
        top_ctx = get_top_ctx(ctx)
        func = int(top_ctx.split(":")[-1])
        gen, tmp_echos, du_pairs = summarize_var_for_region(None, (func, function[func]), init_OUT=init_OUT, call_string=ctx)
        tainted_echos.update(map(lambda e: (ctx + e[0], e[1]), tmp_echos))

        output_du_pairs(du_pairs, ctx)

        # gen = map(lambda d: (d[0], d[1], d[2], top_ctx + "," +  d[3]), gen)

        if ctx != "0,":
            next_ctx = pop_top_ctx(ctx)
            next_ctx_call_site = int(get_top_ctx(ctx).split(":")[0])
            if next_ctx not in work:
                work[next_ctx] = set()
            work[next_ctx].update(map(lambda d: ((d[0], d[1], d[2], top_ctx + "," + d[3]), next_ctx_call_site), gen))

    fd = open("tmp/tainted_echos", "w+")
    for e in tainted_echos:
        fd.write(e[0] + "\t" + str(e[1]) + "\n")
    fd.close()

    fd = open("tmp/tainted_branches", "w+")
    fd.write("\n".join(map(str, sorted(map(lambda e: id_map[e], reversed(tainted_branches))))) + "\n")
    fd.close()

def init_summarries():
    for (start, end) in function.iteritems():
        s = Summary(start, end)
        SUMMARY[start] = s

def write_data_deps():
    global REACHINGDEFS
    fd = open("tmp/data_deps", "w+")
    for stmt, rds in REACHINGDEFS.iteritems():
        fd.write(stmt[1] + "\t" + str(stmt[0]))
        for rd in rds:
            # (def var, loc, ctx)
            fd.write("\t" + str(rd[0]) + "\t" + str(rd[1]) + "\t" + rd[2])
        fd.write("\n")
    fd.close()

def print_ctx(ctx):
    print ",".join(map(lambda x: funcnames[int(x.split(":")[-1])], ctx.split(",")[:-1]))

def test():
    global SUMMARY, function, SOURCE, funcnames
    load_graph()
    all_contexts()
    init_summarries()
    tainted_ctxs = defaultdict(list)
    for s in SOURCE:
        tainted_ctxs[s].extend(contexts_of_stmt(s))
    for s in tainted_ctxs:
        for ctx in tainted_ctxs[s]:
            print_ctx(ctx)
    pass

def main():
    load_graph()
    all_contexts()
    init_summarries()
    do_taint_analysis()
    write_data_deps()

if __name__ == "__main__":
    main()
