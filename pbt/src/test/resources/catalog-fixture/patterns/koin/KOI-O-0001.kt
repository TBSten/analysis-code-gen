// PATTERN: KOI-O-0001
// PROCESSORS: koin
// EXPECT: ok
// FILE: com/example/a/A.kt
package com.example.a

class A
// FILE: com/example/a/B.kt
package com.example.a

class B(val a: A)
