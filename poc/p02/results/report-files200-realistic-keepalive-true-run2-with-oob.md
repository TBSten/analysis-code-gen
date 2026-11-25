# PoC P-02 result (files=200, warmup=3, iterations=10, keepalive=true, libs=realistic(+47 jars), java=21.0.13.1)

## correctness profile=DECL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | KEEP_OOB==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1004
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | KEEP_OOB==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=1805
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | KEEP_OOB==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=2205

## correctness profile=FULL
rounds: KEEP=3 REBUILD=3 NO_INVALIDATE=3
round 1: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: true | KEEP_OOB==REBUILD: true | errors(R/K)=867/867 unresolved(R/K)=2934/2934 dumpLines=7274
round 2: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | KEEP_OOB==REBUILD: true | errors(R/K)=400/400 unresolved(R/K)=1000/1000 dumpLines=10209
round 3: KEEP==REBUILD: true | NO_INVALIDATE==REBUILD: false | KEEP_OOB==REBUILD: true | errors(R/K)=0/0 unresolved(R/K)=0/0 dumpLines=13409

## timing profile=DECL (median ms of transition+analysis; round 1 = initial build+analysis)
round 1: KEEP_OOB=125.0ms(115.4%) KEEP=114.9ms REBUILD=108.3ms ratio=106.1%
round 2: KEEP_OOB=70.3ms(66.9%) KEEP=68.5ms REBUILD=105.0ms ratio=65.2%  (transition only: KEEP=12.1ms REBUILD=37.3ms)
round 3: KEEP_OOB=68.7ms(49.9%) KEEP=67.7ms REBUILD=137.5ms ratio=49.2%  (transition only: KEEP=10.1ms REBUILD=49.1ms)
