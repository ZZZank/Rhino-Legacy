package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * @author ZZZank
 */
public class JField {
    public final Field raw;
    public final String remappedName;
    public final int index;

    public final TypeInfo type;
    public final boolean isStatic;
    public final boolean isFinal;

    public JField(Field f, Class<?> from, int index, Context cx) {
        raw = f;
        remappedName = cx.getRemapper().remapField(from, f);
        this.index = index;
        this.type = TypeInfo.of(f.getGenericType());
        val mod = f.getModifiers();
        this.isStatic = Modifier.isStatic(mod);
        this.isFinal = Modifier.isFinal(mod);
    }
}
