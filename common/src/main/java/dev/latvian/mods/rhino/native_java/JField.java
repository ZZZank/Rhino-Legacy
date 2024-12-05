package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.reflectasm.FieldAccess;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author ZZZank
 */
public class JField {
    private static final Map<Class<?>, FieldAccess> ACCESSES = new IdentityHashMap<>();

    public final Field raw;
    public final String name;
    public final int index;

    public final TypeInfo type;
    public final boolean isStatic;
    public final boolean isFinal;
    private final FieldAccess access;

    public JField(Field f, Class<?> from, String name) {
        this.raw = f;
        this.name = name;
        this.type = TypeInfo.of(f.getGenericType());
        val mod = f.getModifiers();
        this.isStatic = Modifier.isStatic(mod);
        this.isFinal = Modifier.isFinal(mod);
        this.access = ACCESSES.computeIfAbsent(from, FieldAccess::get);
        this.index = access.getIndex(this.raw);
    }

    public void set(Object instance, Object value) {
        try {
            access.set(instance, index, value);
        } catch (IllegalAccessError e) {
            VMBridge.vm.tryToMakeAccessible(raw);
            access.set(instance, index, value);
        }
    }

    public Object get(Object instance) {
        try {
            return access.get(instance, index);
        } catch (IllegalAccessError e) {
            VMBridge.vm.tryToMakeAccessible(raw);
            return access.get(instance, index);
        }
    }
}
