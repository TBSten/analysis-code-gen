# PoC P-02 result (files=200, warmup=3, iterations=10, keepalive=false, libs=minimal(+0 jars), java=21.0.13.1)

## correctness profile=DECL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1004
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1805
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=2205

## timing profile=DECL (median ms of transition+analysis; round 1 = initial build+analysis)
round 1: KEEP=156.2ms REBUILD=163.5ms ratio=95.5%
round 2: KEEP=104.7ms REBUILD=149.9ms ratio=69.9%  (transition only: KEEP=14.5ms REBUILD=45.2ms)
round 3: KEEP=77.5ms REBUILD=162.8ms ratio=47.6%  (transition only: KEEP=12.4ms REBUILD=55.3ms)
