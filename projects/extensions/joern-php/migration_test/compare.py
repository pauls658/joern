from collections import defaultdict

old = defaultdict(lambda: (set(), set()))
new = defaultdict(lambda: (set(), set()))

# TODO: strip line, and filter out empty strings

def parse_symbols(s):
    return filter(lambda x: x, s.strip().split(";"))

with open("migration_test/BB_def_uses.csv", "rb") as fd:
    for line in fd:
        i, defs, uses = line.split(",")
        d, u = old[i]
        d.update(parse_symbols(defs))
        u.update(parse_symbols(uses))

with open("migration_test/arg_def_uses.csv", "rb") as fd:
    for line in fd:
        i, symbols = line.split(",")
        _, u = old[i]
        u.update(parse_symbols(symbols))

with open("defuse_csv/BB_def.csv", "rb") as fd:
    for line in fd:
        i, symbols = line.split(",")
        d, _ = new[i]
        d.update(parse_symbols(symbols))

with open("defuse_csv/BB_use.csv", "rb") as fd:
    for line in fd:
        i, symbols = line.split(",")
        _, u = new[i]
        u.update(parse_symbols(symbols))

with open("defuse_csv/arg_symbols.csv", "rb") as fd:
    for line in fd:
        i, symbols = line.split(",")
        _, u = new[i]
        u.update(parse_symbols(symbols))

import pdb; pdb.set_trace()
