package dev.latvian.mods.rhino.native_java.type;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.VariableTypeInfo;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.ParameterizedType;
import java.util.*;

/**
 * @author ZZZank
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TypeConsolidator {
    private static final Map<Class<?>, Map<VariableTypeInfo, TypeInfo>> MAPPINGS = new Reference2ObjectOpenHashMap<>();

    private static final boolean DEBUG = false;

    @NotNull
    public static Map<VariableTypeInfo, TypeInfo> getMapping(Class<?> type) {
        if (DEBUG) {
            System.out.println("getting mapping from: " + type);
        }
        val got = getImpl(type);
        return got == null ? Collections.emptyMap() : got;
    }

    @NotNull
    public static TypeInfo consolidateOrNone(VariableTypeInfo variable, Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(variable, TypeInfo.NONE);
    }

    @NotNull
    public static TypeInfo[] consolidateAll(
        @NotNull TypeInfo @NotNull [] original,
        @NotNull Map<VariableTypeInfo, TypeInfo> mapping
    ) {
        val len = original.length;
        if (DEBUG) {
            System.out.println("consolidating" + Arrays.toString(original));
        }
        if (len == 0) {
            return original;
        } else if (len == 1) {
            val consolidated = original[0].consolidate(mapping);
            return consolidated != original[0] ? new TypeInfo[]{consolidated} : original;
        }
        TypeInfo[] consolidatedAll = null;
        for (int i = 0; i < len; i++) {
            val type = original[i];
            val consolidated = type.consolidate(mapping);
            if (consolidated != type) {
                if (consolidatedAll == null) {
                    consolidatedAll = new TypeInfo[len];
                    System.arraycopy(original, 0, consolidatedAll, 0, i);
                }
                consolidatedAll[i] = consolidated;
            } else if (consolidatedAll != null) {
                consolidatedAll[i] = consolidated;
            }
        }
        return consolidatedAll == null ? original : consolidatedAll;
    }

    @Nullable
    private static Map<VariableTypeInfo, TypeInfo> getImpl(Class<?> type) {
        if (type == null || type.isPrimitive() || type == Object.class) {
            return null;
        }
        var got = MAPPINGS.get(type);
        if (got == null) {
            synchronized (MAPPINGS) {
                got = MAPPINGS.computeIfAbsent(type, TypeConsolidator::collect);
            }
        }
        return got;
    }

    @NotNull
    private static Map<VariableTypeInfo, TypeInfo> collect(Class<?> type) {
        val mapping = new IdentityHashMap<VariableTypeInfo, TypeInfo>();

        //collect current `level` mapping
        //current level types will only be consolidated by mappings from its subclasses
        val parent = type.getSuperclass();
        val superType = type.getGenericSuperclass();
        if (superType instanceof ParameterizedType parameterized) {
            val args = parameterized.getActualTypeArguments();
            final var params = parent.getTypeParameters();
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
            return postMapping(mapping);
        }
        val merged = new IdentityHashMap<VariableTypeInfo, TypeInfo>();
        for (val entry : superMapping.entrySet()) {
            merged.put(entry.getKey(), entry.getValue().consolidate(mapping));
        }
        merged.putAll(mapping);
        return postMapping(merged);
    }

    private static Map<VariableTypeInfo, TypeInfo> postMapping(Map<VariableTypeInfo, TypeInfo> mapping) {
        switch (mapping.size()) {
            case 0:
                return Collections.emptyMap();
            case 1:
                val entry = mapping.entrySet().iterator().next();
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            default:
                return Collections.unmodifiableMap(mapping);
        }
    }
}
