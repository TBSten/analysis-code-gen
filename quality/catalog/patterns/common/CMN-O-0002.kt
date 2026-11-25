// PATTERN: CMN-O-0002
// TITLE: 背景コード: Java のクラス・インターフェース・ジェネリクスと、それを使う Kotlin
// PROCESSORS: *
// EXPECT: ok
// ORIGIN: own
// STATUS: candidate
// FILE: main/com/example/bg/java/Box.java
package com.example.bg.java;

import java.util.List;

public class Box<T extends Comparable<T>> {
    private final List<T> items;

    public Box(List<T> items) {
        this.items = items;
    }

    public List<T> getItems() {
        return items;
    }

    public static Box<String> empty() {
        return new Box<>(java.util.Collections.emptyList());
    }
}
// FILE: main/com/example/bg/java/Listener.java
package com.example.bg.java;

@FunctionalInterface
public interface Listener {
    void onEvent(String name, int code);
}
// FILE: main/com/example/bg/java/Usage.kt
package com.example.bg.java

class Usage(private val box: Box<String>) {
    val first = box.items.firstOrNull()
    val listener = Listener { name, code -> println("$name $code") }
}
