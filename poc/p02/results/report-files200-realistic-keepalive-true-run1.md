# PoC P-02 result (files=200, warmup=3, iterations=10, keepalive=true, libs=realistic(+47 jars), java=21.0.13.1)

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
round 1: KEEP=129.3ms REBUILD=119.0ms ratio=108.7%
round 2: KEEP=73.7ms REBUILD=120.1ms ratio=61.4%  (transition only: KEEP=12.6ms REBUILD=41.4ms)
round 3: KEEP=72.6ms REBUILD=168.9ms ratio=43.0%  (transition only: KEEP=10.5ms REBUILD=52.6ms)
