package dev.latvian.mods.rhino.util.remapper;

import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import dev.latvian.mods.rhino.util.RemapForJS;
import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * an impl of {@link Remapper} that will check {@link RemapForJS} and {@link RemapPrefixForJS} annotations
 */
public class AnnotatedRemapper implements Remapper {
    public static final AnnotatedRemapper INSTANCE = new AnnotatedRemapper();

    private static final Map<Class<?>, Set<String>> prefixRemapCache = new HashMap<>();

    private AnnotatedRemapper() {}

    @Override
    public String remapClass(Class<?> from) {
        val remap = from.getAnnotation(RemapForJS.class);
        if (remap != null) {
            return remap.value();
        }
        return NOT_REMAPPED;
    }

    private static Set<String> computeRemapPrefixes(Class<?> clazz) {
        var prefixes = prefixRemapCache.get(clazz);
        if (prefixes == null) {
            prefixes = new HashSet<>(3);
            for (val anno : clazz.getAnnotationsByType(RemapPrefixForJS.class)) {
                val s = anno.value().trim();
                if (!s.isEmpty()) {
                    prefixes.add(s);
                }
            }
            prefixRemapCache.put(clazz, prefixes);
        }
        return prefixes;
    }

    @Override
    public String remapField(Class<?> from, Field field) {
        val remap = field.getAnnotation(RemapForJS.class);
        if (remap != null) {
            return remap.value();
        }
        val prefixRemap = computeRemapPrefixes(from);
        val original = field.getName();
        for (val prefix : prefixRemap) {
            if (original.startsWith(prefix)) {
                return original.substring(prefix.length());
            }
        }
        return NOT_REMAPPED;
    }

    @Override
    public String remapMethod(Class<?> from, Method method) {
        //full remap
        val remap = method.getAnnotation(RemapForJS.class);
        if (remap != null) {
            return remap.value();
        }
        //prefix remap
        val prefixes = computeRemapPrefixes(from);
        val original = method.getName();
        for (val prefix : prefixes) {
            if (original.startsWith(prefix)) {
                return original.substring(prefix.length());
            }
        }
        //fallback
        return NOT_REMAPPED;
    }
}