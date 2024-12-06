package dev.latvian.mods.rhino.native_java.info;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ZZZank
 */
public final class MethodSignature {
    public static final Class<?>[] NO_ARG = new Class[0];

    private final String name;
    private final Class<?>[] args;

    public MethodSignature(Method method) {
        this(method.getName(), method.getParameterTypes());
    }

    public MethodSignature(String name, Class<?>[] args) {
        this.name = name;
        this.args = args.length == 0 ? NO_ARG : args;
    }

    public String name() {
        return name;
    }

    public List<Class<?>> args() {
        return Collections.unmodifiableList(Arrays.asList(args));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MethodSignature ms
            && ms.name.equals(name)
            && Arrays.equals(args, ms.args);
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ args.length;
    }
}
