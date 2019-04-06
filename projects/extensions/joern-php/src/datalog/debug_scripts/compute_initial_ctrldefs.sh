#!/bin/bash

grep -oE "^[0-9]*" livectrldef.csv > t
python debug_tools.py mapstmts t > livectrldef.mapped

grep -oE "^[0-9]*" tmp/ctrldef.csv > t
python debug_tools.py mapstmts t > initialctrldef.mapped

python -c "
res = set(open('livectrldef.mapped', 'rb').read().split('\n'))
orig = set(open('initialctrldef.mapped', 'rb').read().split('\n'))
open('intersect', 'wb+').write('\n'.join(res & orig))
"

rm t
rm livectrldef.mapped
rm initialctrldef.mapped
