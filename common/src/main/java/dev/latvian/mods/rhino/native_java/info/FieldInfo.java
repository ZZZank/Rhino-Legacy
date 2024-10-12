package dev.latvian.mods.rhino.native_java.info;

import java.lang.reflect.Field;

/**
 * @author ZZZank
 */
public class FieldInfo {
    public final Field field;
    public String name = "";

    public FieldInfo(Field f) {
        field = f;
    }
}
