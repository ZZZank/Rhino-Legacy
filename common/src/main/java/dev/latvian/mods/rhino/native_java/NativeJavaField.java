package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.type.TypeConsolidator;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author ZZZank
 */
public class NativeJavaField {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    private static final byte ACCESS_GETTER = 1;
    private static final byte ACCESS_SETTER = ACCESS_GETTER << 1;

    public final Field raw;
    private final Class<?> from;
    public final String name;

    private TypeInfo type;
    public final boolean isStatic;
    public final boolean isFinal;

    private byte access = 0;
    private MethodHandle getter;
    private MethodHandle setter;

    public NativeJavaField(Field f, Class<?> from, String name) {
        this.raw = f;
        this.from = from;
        this.name = name;
        val mod = f.getModifiers();
        this.isStatic = Modifier.isStatic(mod);
        this.isFinal = Modifier.isFinal(mod);
    }

    /**
     * @param instance the {@code this}. Will be ignored if this field is static
     * @param value the value to be assigned to this field
     */
    public void set(Object instance, Object value) {
        try {
            setInternal(instance, value);
        } catch (IllegalAccessException e) {
            val accessible = VMBridge.vm.tryToMakeAccessible(raw);
            if (!accessible) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            try { // retry after recovery
                access = (byte) (access & (~ACCESS_SETTER));
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

    private Object getInternal(Object instance) throws IllegalAccessException, Throwable {
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

    /**
     * @param instance the {@code this}. Will be ignored if this field is static
     */
    public Object get(Object instance) {
        try {
            return getInternal(instance);
        } catch (IllegalAccessException e) {
            val accessible = VMBridge.vm.tryToMakeAccessible(raw);
            if (!accessible) {
                throw Context.throwAsScriptRuntimeEx(e);
            }
            try { // retry after recovery
                access = (byte) (access & (~ACCESS_GETTER));
                return getInternal(instance);
            } catch (Throwable ex) {
                throw Context.throwAsScriptRuntimeEx(ex);
            }
        } catch (Throwable e) {
            throw Context.throwAsScriptRuntimeEx(e);
        }
    }

    public TypeInfo getType() {
        if (type == null) {
            type = TypeInfo.of(raw.getGenericType());
            if (from != null) {
                type = type.consolidate(TypeConsolidator.getMapping(from));
            }
        }
        return type;
    }
}
