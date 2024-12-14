package dev.latvian.mods.rhino.native_java.type;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.VariableTypeInfo;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * @author ZZZank
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeConsolidator {
    private static final Map<Class<?>, Map<VariableTypeInfo, TypeInfo>> MAPPINGS = new Reference2ObjectOpenHashMap<>();

    @NotNull
    public static Map<VariableTypeInfo, TypeInfo> getMapping(Class<?> type) {
        val got = getImpl(type);
        return got == null ? Collections.emptyMap() : got;
    }

    public static TypeInfo consolidateOrNone(VariableTypeInfo variable, Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(variable, TypeInfo.NONE);
    }

    public static TypeInfo[] consolidateAll(TypeInfo[] original, Map<VariableTypeInfo, TypeInfo> mapping) {
        TypeInfo[] consolidatedAll = null;
        for (int i = 0; i < original.length; i++) {
            val type = original[i];
            val consolidated = type.consolidate(mapping);
            if (consolidated != type) {
                if (consolidatedAll == null) {
                    consolidatedAll = new TypeInfo[original.length];
                    System.arraycopy(original, 0, consolidatedAll, 0, i);
                } else {
                    consolidatedAll[i] = consolidated;
                }
            } else if (consolidatedAll != null) {
                consolidatedAll[i] = consolidated;
            }
        }
        return consolidatedAll == null ? original : consolidatedAll;
    }

    private static Map<VariableTypeInfo, TypeInfo> getImpl(Class<?> type) {
        if (type == null || type.isPrimitive() || type == Object.class) {
            return null;
        }
        var got = MAPPINGS.get(type);
        if (got == null) {
            synchronized (MAPPINGS) {
                if (got == null) {
                    MAPPINGS.put(type, got = collect(type));
                }
            }
        }
        return got;
    }

    private static Map<VariableTypeInfo, TypeInfo> collect(Class<?> type) {
        val mapping = new IdentityHashMap<VariableTypeInfo, TypeInfo>();

        //collect current `level` mapping
        //current level types will only be consolidated by mappings from its subclasses
        val parent = type.getSuperclass();
        val superType = type.getGenericSuperclass();
        if (superType instanceof ParameterizedType parameterized) {
            val args = parameterized.getActualTypeArguments();
            val params = parent.getTypeParameters();
            for (int i = 0; i < args.length; i++) {
                mapping.put(
                    (VariableTypeInfo) TypeInfo.of(params[i]), // T
                    TypeInfo.of(args[i]) // replacing T, might be already consolidated or NOT
                );
            }
        }
        //mapping from super
        val superMapping = getImpl(parent);
        if (superMapping == null) {
            return Collections.unmodifiableMap(mapping);
        }
        val merged = new IdentityHashMap<VariableTypeInfo, TypeInfo>();
        for (val entry : superMapping.entrySet()) {
            merged.put(entry.getKey(), entry.getValue().consolidate(mapping));
        }
        merged.putAll(mapping);

        if (merged.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(merged);
    }
}
