package dev.latvian.mods.rhino.native_java.info;

import dev.latvian.mods.rhino.native_java.original.MethodSignature;

import java.lang.reflect.Method;

/**
 * @author ZZZank
 */
public class MethodInfo {
    public final Method method;
    public final MethodSignature sig;
    public boolean hidden = false;

    public MethodInfo(Method m, MethodSignature signature) {
        method = m;
        this.sig = signature;
    }
}
