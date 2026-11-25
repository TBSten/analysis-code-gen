# PoC P-02 result (files=200, warmup=3, iterations=10, java=21.0.13.1)

## correctness profile=DECL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1004
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1805
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=2205

## correctness profile=FULL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | errors(R/K)=867/867 unresolved(R/K)=2934/2934 dumpLines=7274
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=400/400 unresolved(R/K)=1000/1000 dumpLines=10209
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=13409

## timing profile=DECL (median ms of transition+analysis; round 1 = initial build+analysis)
round 1: KEEP=123.9ms REBUILD=114.6ms ratio=108.1%
round 2: KEEP=83.5ms REBUILD=123.5ms ratio=67.6%
round 3: KEEP=82.6ms REBUILD=170.2ms ratio=48.5%

## timing profile=FULL (median ms of transition+analysis; round 1 = initial build+analysis)
round 1: KEEP=1242.3ms REBUILD=1174.0ms ratio=105.8%
round 2: KEEP=1007.1ms REBUILD=1231.6ms ratio=81.8%
round 3: KEEP=1152.8ms REBUILD=1429.7ms ratio=80.6%
