package dev.latvian.mods.rhino.util;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;

public class JavaPortingHelper {

    public static Path ofPath(String first, String... more) {
        return FileSystems.getDefault().getPath(first, more);
    }

    public static String repeat(String str, int times) {
        return String.join("", Collections.nCopies(times, str));
    }

    public static boolean isBlank(String str) {
        return str.trim().isEmpty();
    }

    /*
     * Returns the {@code Class} representing the element type of array class.
     * If this class does not represent an array class, then this method returns
     * {@code null}.
     */
    private static Class<?> elementType(Class<?> clazz) {
        if (!clazz.isArray()) {
            return null;
        }
        Class<?> c = clazz;
        while (c.isArray()) {
            c = c.getComponentType();
        }
        return c;
    }

    public static String getPackageName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        Class<?> c = clazz.isArray() ? elementType(clazz) : clazz;
        if (c.isPrimitive()) {
            return "java.lang";
        }
        String className = c.getName();
        int dot = className.lastIndexOf('.');
        if (dot == -1) {
            return "";
        }
        return className.substring(0, dot);
//        return className.substring(0, dot).intern();
    }

    public static void appendDescriptor(final Class<?> clazz, final StringBuilder builder) {
        Class<?> current = clazz;
        while (current.isArray()) {
            builder.append('[');
            current = current.getComponentType();
        }
        if (current.isPrimitive()) {
            if (current == Integer.TYPE) {
                builder.append('I');
            } else if (current == Void.TYPE) {
                builder.append('V');
            } else if (current == Boolean.TYPE) {
                builder.append('Z');
            } else if (current == Byte.TYPE) {
                builder.append('B');
            } else if (current == Character.TYPE) {
                builder.append('C');
            } else if (current == Short.TYPE) {
                builder.append('S');
            } else if (current == Double.TYPE) {
                builder.append('D');
            } else if (current == Float.TYPE) {
                builder.append('F');
            } else if (current == Long.TYPE) {
                builder.append('J');
            } else {
                throw new AssertionError();
            }
        } else {
            builder.append('L').append(internalNameOf(current)).append(';');
        }
    }

    public static String internalNameOf(final Class<?> clazz) {
        return clazz.getName().replace('.', '/');
    }

    @NotNull
    public static String sig(Class<?> returnType, Class<?>... argTypes) {
        val builder = new StringBuilder();
        builder.append('(');
        for (val argType : argTypes) {
            appendDescriptor(argType, builder);
        }
        builder.append(')');
        appendDescriptor(returnType, builder);
        return builder.toString();
    }
}
