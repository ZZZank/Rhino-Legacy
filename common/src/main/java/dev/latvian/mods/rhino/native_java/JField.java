package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author ZZZank
 */
public class JField {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final byte ACCESS_GETTER = 1;
    private static final byte ACCESS_SETTER = ACCESS_GETTER << 1;

    public final Field raw;
    public final String name;

    public final TypeInfo type;
    public final boolean isStatic;
    public final boolean isFinal;

    private byte access = 0;
    private MethodHandle getter;
    private MethodHandle setter;

    public JField(Field f, Class<?> from, String name) {
        this.raw = f;
        this.name = name;
        this.type = TypeInfo.of(f.getGenericType());
        val mod = f.getModifiers();
        this.isStatic = Modifier.isStatic(mod);
        this.isFinal = Modifier.isFinal(mod);
    }

    public void set(Object instance, Object value) {
        try {
            setInternal(instance, value);
        } catch (IllegalAccessException e) {
            val accessible = VMBridge.vm.tryToMakeAccessible(raw);
            if (!accessible) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            try { // retry after recovery
                setInternal(instance, value);
            } catch (Throwable ex) {
                throw Context.throwAsScriptRuntimeEx(ex);
            }
        } catch (Throwable e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    private void setInternal(Object instance, Object value) throws IllegalAccessException, Throwable {
        if ((access & ACCESS_SETTER) == 0) {
            access = (byte) (access | ACCESS_SETTER);
            setter = LOOKUP.unreflectSetter(raw);
        }
        if (isStatic) {
            setter.invoke(value);
        } else {
            setter.invoke(instance, value);
        }
    }

    private Object setInternal(Object instance) throws IllegalAccessException, Throwable {
        if ((access & ACCESS_GETTER) == 0) {
            access = (byte) (access | ACCESS_GETTER);
            getter = LOOKUP.unreflectGetter(raw);
        }
        if (isStatic) {
            return getter.invoke();
        } else {
            return getter.invoke(instance);
        }
    }

    public Object get(Object instance) {
        try {
            return setInternal(instance);
        } catch (IllegalAccessException e) {
            val accessible = VMBridge.vm.tryToMakeAccessible(raw);
            if (!accessible) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            try { // retry after recovery
                return setInternal(instance);
            } catch (Throwable ex) {
                throw Context.throwAsScriptRuntimeEx(ex);
            }
        } catch (Throwable e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }
}
