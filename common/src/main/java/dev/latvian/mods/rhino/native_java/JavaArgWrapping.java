package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeJavaArray;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Array;

/**
 * @author ZZZank
 */
public final class JavaArgWrapping {

    public static Object[] wrapRegularArgs(final Context cx, final Object[] args, final TypeInfo[] types) {
        val len = args.length;
        if (len != types.length) {
            throw new IllegalArgumentException();
        }
        var wrapped = args;
        for (int i = 0; i < len; i++) {
            val arg = args[i];
            val wrapTo = Context.jsToJava(cx, arg, types[i]);
            if (arg != wrapTo) {
                if (wrapped == args) {
                    wrapped = wrapped.clone();
                }
                wrapped[i] = wrapTo;
            }
        }
        return wrapped;
    }

    public static Object[] wrapVarArgs(final Context cx, final Object[] args, final TypeInfo[] types) {
        val explicitLen = types.length - 1;
        if (args.length < explicitLen) {
            throw new IllegalArgumentException(String.format(
                "not enough args for vararg method, expected %s at least, but got %s",
                explicitLen,
                args.length
            ));
        }

        // marshall explicit parameters
        val wrapped = new Object[types.length];
        for (int i = 0; i < explicitLen; i++) {
            wrapped[i] = Context.jsToJava(cx, args[i], types[i]);
        }

        // Handle special situation where a single variable parameter
        // is given, and it is a Java or ECMA array or is null.
        if (args.length == types.length) {
            val last = args[explicitLen];
            if (last == null || last instanceof NativeArray || last instanceof NativeJavaArray) {
                // convert the ECMA array into a native array
                wrapped[explicitLen] = Context.jsToJava(cx, last, types[explicitLen]);
                // all args converted, return
                return wrapped;
            }
        }

        // marshall the variable parameters
        val varArgType = types[explicitLen].componentType();
        val varArgLen = args.length - explicitLen;
        val varArgs = varArgType.newArray(varArgLen);
        for (int i = 0; i < varArgLen; i++) {
            Array.set(varArgs, i, Context.jsToJava(cx, args[explicitLen + i], varArgType));
        }
        wrapped[explicitLen] = varArgs;

        return wrapped;
    }
}
