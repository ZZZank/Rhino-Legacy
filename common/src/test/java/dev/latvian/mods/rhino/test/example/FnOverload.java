package dev.latvian.mods.rhino.test.example;

import java.util.function.Consumer;

/**
 * @author ZZZank
 */
public class FnOverload {

    public static void of(String name, Consumer<String> accepter) {
        accepter.accept("inner::" + name);
    }

    public static String of(String name, String additional) {
        return "inner::" + name + ":with:" + additional;
    }

    public static String of(String name) {
        return "inner::" + name;
    }

    public static CharSequence deTyped(String name, String additional) {
        return "inner::" + name;
    }
}
