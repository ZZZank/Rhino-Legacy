package dev.latvian.mods.rhino.native_java.type;

import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.native_java.type.info.ArrayTypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.util.wrap.TypeWrapperFactory;
import lombok.AllArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * @author ZZZank
 */
@AllArgsConstructor
public class Converter {

    private final Context cx;

    public static final int JSTYPE_UNDEFINED = 0; // undefined type
    public static final int JSTYPE_NULL = 1; // null
    public static final int JSTYPE_BOOLEAN = 2; // boolean
    public static final int JSTYPE_NUMBER = 3; // number
    public static final int JSTYPE_STRING = 4; // string
    public static final int JSTYPE_JAVA_CLASS = 5; // JavaClass
    public static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
    public static final int JSTYPE_JAVA_ARRAY = 7; // JavaArray
    public static final int JSTYPE_OBJECT = 8; // Scriptable


    public Object javaToJS(Object value, Scriptable scope) {
        return javaToJS(value, scope, TypeInfo.NONE);
    }

    public Object javaToJS(Object value, Scriptable scope, TypeInfo target) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Scriptable) {
            return value;
        } else if (value instanceof Character) {
            return String.valueOf(((Character) value).charValue());
        } else {
            return cx.getWrapFactory().wrap(cx, scope, value, target);
        }
    }

    public final Object jsToJava(@Nullable Object from, TypeInfo target) throws EvaluatorException {
        if (target == null || !target.shouldConvert()) {
            return Wrapper.unwrapped(from);
        } else if (target.is(TypeInfo.RAW_SET)) {
            return setOf(from, target.param(0));
        } else if (target.is(TypeInfo.RAW_MAP)) {
            return mapOf(from, target.param(0), target.param(1));
        } else if (target instanceof ArrayTypeInfo) {
            return arrayOf(from, target.componentType());
        } else if (List.class.isAssignableFrom(target.asClass())) {
            return listOf(from, target.param(0));
        } else if (target.is(TypeInfo.CLASS)) {
            return classOf(from);
        }

        if (from == null || from.getClass() == target.asClass()) {
            return from;
        }

        return internalJsToJava(from, target);
    }

    private static int getJSTypeCode(Object from) {
        if (from == null) {
            return JSTYPE_NULL;
        } else if (from == Undefined.instance) {
            return JSTYPE_UNDEFINED;
        } else if (from instanceof CharSequence) {
            return JSTYPE_STRING;
        } else if (from instanceof Number) {
            return JSTYPE_NUMBER;
        } else if (from instanceof Boolean) {
            return JSTYPE_BOOLEAN;
        } else if (from instanceof Scriptable) {
            if (from instanceof NativeJavaClass) {
                return JSTYPE_JAVA_CLASS;
            } else if (from instanceof NativeJavaArray) {
                return JSTYPE_JAVA_ARRAY;
            } else if (from instanceof Wrapper) {
                return JSTYPE_JAVA_OBJECT;
            }
            return JSTYPE_OBJECT;
        } else if (from instanceof Class) {
            return JSTYPE_JAVA_CLASS;
        } else {
            Class<?> valueClass = from.getClass();
            if (valueClass.isArray()) {
                return JSTYPE_JAVA_ARRAY;
            }
            return JSTYPE_JAVA_OBJECT;
        }
    }

    protected Object internalJsToJava(Object from, TypeInfo target) {
        if (target instanceof ArrayTypeInfo) {
            // Make a new java array, and coerce the JS array components to the target (component) type.
            val componentType = target.componentType();

            if (from instanceof NativeArray array) {
                val length = array.getLength();

                val result = componentType.newArray((int) length);
                for (int i = 0; i < length; ++i) {
                    try {
                        Array.set(result, i, jsToJava(array.get(i, array), componentType));
                    } catch (EvaluatorException ee) {
                        return NativeJavaObject.reportConversionError(from, target);
                    }
                }

                return result;
            } else {
                // Convert a single value to an array
                Object result = componentType.newArray(1);
                Array.set(result, 0, jsToJava(from, componentType));
                return result;
            }
        }

        Object unwrappedValue = Wrapper.unwrapped(from);

        val typeWrapper = cx.getTypeWrappers().getWrapperFactory(unwrappedValue, target);

        if (typeWrapper != null) {
            return typeWrapper.wrap(cx, unwrappedValue, target);
        }

        switch (getJSTypeCode(from)) {
            case JSTYPE_NULL -> {
                // raise error if type.isPrimitive()
                if (target.isPrimitive()) {
                    return NativeJavaObject.reportConversionError(from, target);
                }
                return null;
            }
            case JSTYPE_UNDEFINED -> {
                if (target == TypeInfo.STRING || target == TypeInfo.OBJECT) {
                    return "undefined";
                }
                return NativeJavaObject.reportConversionError(from, target);
            }
            case JSTYPE_BOOLEAN -> {
                // Under LC3, only JS Booleans can be coerced into a Boolean value
                if (target.isBoolean() || target == TypeInfo.OBJECT) {
                    return from;
                } else if (target == TypeInfo.STRING) {
                    return from.toString();
                } else {
                    return internalJsToJavaLast(from, target);
                }
            }
            case JSTYPE_NUMBER -> {
                if (target == TypeInfo.STRING) {
                    return ScriptRuntime.toString(from);
                } else if (target == TypeInfo.OBJECT) {
                    return NativeJavaObject.coerceToNumber(TypeInfo.DOUBLE, from);
                } else if ((target.isPrimitive() && !target.isBoolean()) || ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
                    return NativeJavaObject.coerceToNumber(target, from);
                } else {
                    return internalJsToJavaLast(from, target);
                }
            }
            case JSTYPE_STRING -> {
                if (target == TypeInfo.STRING || target.asClass().isInstance(from)) {
                    return from.toString();
                } else if (target.isCharacter()) {
                    // Special case for converting a single char string to a
                    // character
                    // Placed here because it applies *only* to JS strings,
                    // not other JS objects converted to strings
                    if (((CharSequence) from).length() == 1) {
                        return ((CharSequence) from).charAt(0);
                    }
                    return NativeJavaObject.coerceToNumber(target, from);
                } else if ((target.isPrimitive() && !target.isBoolean()) || ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
                    return NativeJavaObject.coerceToNumber(target, from);
                } else {
                    return internalJsToJavaLast(from, target);
                }
            }
            case JSTYPE_JAVA_CLASS -> {
                if (target == TypeInfo.CLASS || target == TypeInfo.OBJECT) {
                    return unwrappedValue;
                } else if (target == TypeInfo.STRING) {
                    return unwrappedValue.toString();
                } else {
                    return internalJsToJavaLast(unwrappedValue, target);
                }
            }
            case JSTYPE_JAVA_OBJECT, JSTYPE_JAVA_ARRAY -> {
                if (target.isPrimitive()) {
                    if (target.isBoolean()) {
                        return internalJsToJavaLast(unwrappedValue, target);
                    }
                    return NativeJavaObject.coerceToNumber(target, unwrappedValue);
                }
                if (target == TypeInfo.STRING) {
                    return unwrappedValue.toString();
                }
                if (target.asClass().isInstance(unwrappedValue)) {
                    return unwrappedValue;
                }
                return internalJsToJavaLast(unwrappedValue, target);
            }
            case JSTYPE_OBJECT -> {
                if (target == TypeInfo.STRING) {
                    return ScriptRuntime.toString(from);
                } else if (target.isPrimitive()) {
                    if (target.isBoolean()) {
                        return internalJsToJavaLast(from, target);
                    }
                    return NativeJavaObject.coerceToNumber(target, from);
                } else if (target.asClass().isInstance(from)) {
                    return from;
                } else if (target == TypeInfo.DATE && from instanceof NativeDate) {
                    double time = ((NativeDate) from).getJSTimeValue();
                    // XXX: This will replace NaN by 0
                    return new Date((long) time);
                } else if (from instanceof Wrapper) {
                    if (target.asClass().isInstance(unwrappedValue)) {
                        return unwrappedValue;
                    }
                    return internalJsToJavaLast(unwrappedValue, target);
                } else if (target.asClass().isInterface()
                    && (from instanceof NativeObject || from instanceof NativeFunction || from instanceof ArrowFunction)
                ) {
                    // Try to use function/object as implementation of Java interface.
                    return NativeJavaObject.createInterfaceAdapter(cx, target.asClass(), (ScriptableObject) from);
                } else {
                    return internalJsToJavaLast(from, target);
                }
            }
        }

        return internalJsToJavaLast(from, target);
    }

    protected Object internalJsToJavaLast(Object from, TypeInfo target) {
        if (target instanceof TypeWrapperFactory<?> f) {
            return f.wrap(cx, from, target);
        }

        return NativeJavaObject.reportConversionError(from, target);
    }

    public ArrayValueProvider arrayValueProviderOf(Object value) {
        if (value instanceof Object[] arr) {
            return arr.length == 0 ? ArrayValueProvider.EMPTY : new ArrayValueProvider.FromPlainJavaArray(arr);
        } else if (value != null && value.getClass().isArray()) {
            int len = Array.getLength(value);
            return len == 0 ? ArrayValueProvider.EMPTY : new ArrayValueProvider.FromJavaArray(value, len);
        }

        if (value instanceof NativeArray array) {
            return ArrayValueProvider.fromNativeArray(array);
        } else if (value instanceof NativeJavaList list) {
            return ArrayValueProvider.fromJavaList(list.list, list);
        } else if (value instanceof List<?> list) {
            return ArrayValueProvider.fromJavaList(list, list);
        } else if (value instanceof Iterable<?> itr) {
            return ArrayValueProvider.fromIterable(itr);
        }
        return value == null
            ? ArrayValueProvider.FromObject.FROM_NULL
            : new ArrayValueProvider.FromObject(value);
    }

    public Object arrayOf(@Nullable Object from, TypeInfo target) {
        if (from instanceof Object[] arr) {
            if (target == null) {
                return from;
            }

            return arr.length == 0 ? target.newArray(0) : new ArrayValueProvider.FromPlainJavaArray(arr).createArray(cx, target);
        } else if (from != null && from.getClass().isArray()) {
            if (target == null) {
                return from;
            }

            int len = Array.getLength(from);
            return len == 0 ? target.newArray(0) : new ArrayValueProvider.FromJavaArray(from, len).createArray(cx, target);
        }

        return arrayValueProviderOf(from).createArray(cx, target);
    }

    public Object listOf(@Nullable Object from, TypeInfo target) {
        if (from instanceof NativeJavaList n) {
            if (target == null) {
                // No conversion necessary
                return n.list;
            } else if (target.equals(n.listType)) {
                // No conversion necessary
                return n.list;
            } else {
                var list = new ArrayList<>(n.list.size());

                for (var o : n.list) {
                    list.add(jsToJava(o, target));
                }

                return list;
            }
        }

        return arrayValueProviderOf(from).createList(cx, target);
    }

    public Object setOf(@Nullable Object from, TypeInfo target) {
        if (from instanceof NativeJavaList n) {
            if (target == null) {
                // No conversion necessary
                return new LinkedHashSet<>(n.list);
            } else if (target.equals(n.listType)) {
                // No conversion necessary
                return new LinkedHashSet<>(n.list);
            } else {
                var set = new LinkedHashSet<>(n.list.size());

                for (var o : n.list) {
                    set.add(jsToJava(o, target));
                }

                return set;
            }
        }

        return arrayValueProviderOf(from).createSet(cx, target);
    }

    public Object mapOf(@Nullable Object from, TypeInfo kTarget, TypeInfo vTarget) {
        if (from instanceof NativeJavaMap n) {
            if (!kTarget.shouldConvert() && !vTarget.shouldConvert()) {
                // No conversion necessary
                return n.map;
            } else if (kTarget.equals(n.mapKeyType) && vTarget.equals(n.mapValueType)) {
                // No conversion necessary
                return n.map;
            } else {
                if (n.map.isEmpty()) {
                    return Collections.emptyMap();
                }

                val map = new LinkedHashMap<>(n.map.size());

                for (val entry : ((Map<?, ?>) n.map).entrySet()) {
                    map.put(jsToJava(entry.getKey(), kTarget), jsToJava(entry.getValue(), vTarget));
                }

                return map;
            }
        } else if (from instanceof NativeObject obj) {
            val keys = obj.getIds();
            val map = new LinkedHashMap<>(keys.length);

            for (val key : keys) {
                map.put(jsToJava(key, kTarget), jsToJava(obj.get(key), vTarget));
            }

            return map;
        } else if (from instanceof Map<?, ?> m) {
            if (!kTarget.shouldConvert() && !vTarget.shouldConvert()) {
                // No conversion necessary
                return m;
            }

            val map = new LinkedHashMap<>(m.size());

            for (val entry : m.entrySet()) {
                map.put(jsToJava(entry.getKey(), kTarget), jsToJava(entry.getValue(), vTarget));
            }

            return map;
        } else {
            return NativeJavaObject.reportConversionError(from, TypeInfo.RAW_MAP);
        }
    }

    protected Object classOf(Object from) {
        if (from instanceof NativeJavaClass n) {
            return n.getClassObject();
        } else if (from instanceof Class<?> c) {
            if (cx.getClassShutter() == null || cx.getClassShutter().visibleToScripts(c.getName(), ClassShutter.TYPE_MEMBER)) {
                return c;
            } else {
                throw Context.reportRuntimeError("Class " + c.getName() + " not allowed");
            }
        } else {
            val s = ScriptRuntime.toString(from);

            if (cx.getClassShutter() == null || cx.getClassShutter().visibleToScripts(s, ClassShutter.TYPE_MEMBER)) {
                try {
                    return Class.forName(s);
                } catch (ClassNotFoundException e) {
                    throw Context.reportRuntimeError("Failed to load class " + s);
                }
            } else {
                throw Context.reportRuntimeError("Class " + from + " not allowed");
            }
        }
    }
}
