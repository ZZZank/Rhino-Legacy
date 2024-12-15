package dev.latvian.mods.rhino.test;

import java.util.function.Consumer;

/**
 * @author ZZZank
 */
public class FnInterfaceFrame {

    public static void create(String name, Consumer<String> accepter) {
        accepter.accept("inner::" + name);
    }

    public static String create(String name, String additional) {
        return "inner::" + name;
    }

    public static String create(String name) {
        return "inner::" + name;
    }

    public static CharSequence createDeTyped(String name, String additional) {
        return "inner::" + name;
    }
}
