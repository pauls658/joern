import os, sys
from joern.all import JoernSteps
import time

j = JoernSteps()
j.connectToDatabase()

command = open(sys.argv[1], "r").read()
out = open( "indexer.out", "w")
start = time.time()
#print command
print "[INFO] Running ", command
res = j.runCypherQuery( command)
for r in res:
        #out.write(str(r) + "\n")
        print str(r)
out.flush()
print "[INFO] Took %.2fs" % (time.time() - start)
out.close()
