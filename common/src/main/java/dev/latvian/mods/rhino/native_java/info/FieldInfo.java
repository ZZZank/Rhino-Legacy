package dev.latvian.mods.rhino.native_java.info;

import dev.latvian.mods.rhino.util.remapper.Remapper;

import java.lang.reflect.Field;

/**
 * @author ZZZank
 */
public class FieldInfo {
    public final Field field;
    public final String name;

    public FieldInfo(Field f) {
        field = f;
        name = f.getName();
    }

    public FieldInfo(Field f, Remapper remapper) {
        field = f;
        name = remapper.remapField(f.getDeclaringClass(), f);
    }
}
