# PoC P-02 result (files=1000, warmup=2, iterations=5, keepalive=true, libs=realistic(+47 jars), java=21.0.13.1)

## correctness profile=DECL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=5004
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=9006
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=11006

## timing profile=DECL (median ms of transition+analysis; round 1 = initial build+analysis)
round 1: KEEP=437.6ms REBUILD=437.4ms ratio=100.0%
round 2: KEEP=368.5ms REBUILD=508.9ms ratio=72.4%  (transition only: KEEP=61.4ms REBUILD=210.2ms)
round 3: KEEP=367.6ms REBUILD=649.2ms ratio=56.6%  (transition only: KEEP=57.6ms REBUILD=232.2ms)
