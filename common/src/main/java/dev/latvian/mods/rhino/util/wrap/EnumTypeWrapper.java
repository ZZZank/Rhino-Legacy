package dev.latvian.mods.rhino.util.wrap;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * a type wrapper for Enum that can wrap String and Number into corresponding Enum values
 * <p>
 * note that wrappers are created at runtime, since scanning all class just for Enum wrapper is, bad
 * @param <T> type class, should be a subclass of Enum, otherwise IllegalArgumentException will be thrown
 */
public class EnumTypeWrapper<T> implements TypeWrapperFactory<T> {
    private static final Map<Class<?>, EnumTypeWrapper<?>> WRAPPERS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> EnumTypeWrapper<T> get(Class<T> enumType) {
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException("Class " + enumType.getName() + " is not an enum!");
        }
        return (EnumTypeWrapper<T>) WRAPPERS.computeIfAbsent(enumType, EnumTypeWrapper::new);
    }

    public static String getName(Class<?> enumType, Enum<?> e, boolean cache) {
        if (cache) {
            return get(enumType).valueNames.getOrDefault(e, e.name());
        }
        return e.name();
    }

    public final Class<T> enumType;
    public final T[] indexValues;
    public final Map<String, T> nameValues;
    public final Map<T, String> valueNames;

    private EnumTypeWrapper(Class<T> enumType) {
        this.enumType = enumType;
        this.indexValues = enumType.getEnumConstants();
        this.nameValues = new HashMap<>();
        this.valueNames = new HashMap<>();

        for (T t : indexValues) {
            String name = getName(enumType, (Enum<?>) t, false).toLowerCase();
            nameValues.put(name, t);
            valueNames.put(t, name);
        }
    }

    @Override
    public T wrap(Object o) {
        if (o instanceof CharSequence) {
            String s = o.toString().toLowerCase();
            if (s.isEmpty()) {
                return null;
            }

            T t = nameValues.get(s);
            if (t == null) {
                throw new IllegalArgumentException("'" + s + "' is not a valid enum constant! Valid values are: " + nameValues.keySet().stream().map(s1 -> "'" + s1 + "'").collect(Collectors.joining(", ")));
            }

            return t;
        } else if (o instanceof Number) {
            int index = ((Number) o).intValue();
            if (index < 0 || index >= indexValues.length) {
                throw new IllegalArgumentException(index + " is not a valid enum index! Valid values are: 0 - " + (indexValues.length - 1));
            }

            return indexValues[index];
        }

        return (T) o;
    }
}