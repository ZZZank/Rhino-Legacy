package dev.latvian.mods.rhino.util.wrap;

import com.google.common.collect.ImmutableMap;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * a type wrapper for Enum that can wrap String and Number into corresponding Enum values
 * <p>
 * note that wrappers are created at runtime, since scanning all class just for Enum wrapper is, bad
 * @param <T> type class, should be a subclass of Enum, otherwise IllegalArgumentException will be thrown
 */
public class EnumTypeWrapper<T extends Enum<T>> implements TypeWrapperFactory<T> {
    private static final Map<Class<?>, EnumTypeWrapper<?>> WRAPPERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T>> EnumTypeWrapper<T> get(@NotNull Class<T> enumType) {
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException("Class " + enumType.getName() + " is not an enum!");
        }
        var cached = (EnumTypeWrapper<T>) WRAPPERS.get(enumType);
        if (cached == null) {
            cached = new EnumTypeWrapper<>(enumType);
            WRAPPERS.put(enumType, cached);
        }
        return cached;
    }

    public static <T extends Enum<T>> String getName(Class<T> enumType, Enum<T> e, boolean cache) {
        if (cache) {
            return get(enumType).valueNames.getOrDefault(e, e.name());
        }
        return e.name();
    }

    public final Class<T> enumType;
    public final T[] indexedValues;
    public final Map<String, T> nameValues;
    public final Map<T, String> valueNames;

    private EnumTypeWrapper(Class<T> enumType) {
        this.enumType = enumType;
        this.indexedValues = enumType.getEnumConstants();

        val nameValuesBuilder = ImmutableMap.<String, T>builder();
        val valueNamesBuilder = ImmutableMap.<T, String>builder();
        for (T value : indexedValues) {
            val name = getName(enumType, value, false).toLowerCase();
            nameValuesBuilder.put(name, value);
            valueNamesBuilder.put(value, name);
        }
        nameValues = nameValuesBuilder.build();
        valueNames = valueNamesBuilder.build();
    }

    @Override
    public T wrap(Object o) {
        if (o instanceof CharSequence) {
            val lowerCased = o.toString().toLowerCase();
            if (lowerCased.isEmpty()) {
                return null;
            }

            T t = nameValues.get(lowerCased);
            if (t == null) {
                throw new IllegalArgumentException("'" + lowerCased + "' is not a valid enum constant! Valid values are: " + nameValues.keySet().stream().map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
            }

            return t;
        } else if (o instanceof Number) {
            val index = ((Number) o).intValue();
            if (index < 0 || index >= indexedValues.length) {
                throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (
                    indexedValues.length - 1));
            }

            return indexedValues[index];
        }

        return (T) o;
    }
}