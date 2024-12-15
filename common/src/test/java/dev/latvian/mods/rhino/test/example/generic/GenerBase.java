package dev.latvian.mods.rhino.test.example.generic;

import dev.latvian.mods.rhino.test.TestConsole;

/**
 * @author ZZZank
 */
public class GenerBase<T extends GenerBase<T>> {
    public final T self;

    public GenerBase() {
        this.self = (T) this;
    }

    public static GenerBase<?> of() {
        return new GenerBase<>();
    }

    public static Impl1 impl1() {
        return new Impl1();
    }

    public static GenerBase<Impl1> ofImpl() {
        return new Impl1();
    }

    public void accept(T dummy) {
        TestConsole.log("dummy got");
    }
}
