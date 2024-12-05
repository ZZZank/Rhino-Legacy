/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.ReflectsKit;
import dev.latvian.mods.rhino.native_java.original.FieldAndMethods;
import dev.latvian.mods.rhino.native_java.original.JavaMembers;
import dev.latvian.mods.rhino.native_java.original.NativeJavaPackage;
import dev.latvian.mods.rhino.native_java.type.info.ArrayTypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;
import lombok.Getter;
import lombok.val;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * This class reflects non-Array Java objects into the JavaScript environment.  It
 * reflect fields directly, and uses NativeJavaMethod objects to reflect (possibly
 * overloaded) methods.<p>
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaPackage
 * @see NativeJavaClass
 */
public class NativeJavaObject implements Scriptable, SymbolScriptable, Wrapper, Serializable {

	private static final Object COERCED_INTERFACE_KEY = "Coerced Interface";
	private static final long serialVersionUID = -6948590651130498591L;

	protected Scriptable prototype;
	protected Scriptable parent;

	protected transient Object javaObject;
	protected transient TypeInfo typeInfo;
	@Getter
	protected transient JavaMembers members;
	private transient Map<String, FieldAndMethods> fieldAndMethods;
	protected transient boolean isAdapter;

	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.isAdapter = false;
		initMembers(Context.getContext(), scope);
	}

	public NativeJavaObject(Context cx, Scriptable scope, Object javaObject, TypeInfo typeInfo) {
		this(cx, scope, javaObject, typeInfo, false);
	}

	public NativeJavaObject(Context cx, Scriptable scope, Object javaObject, TypeInfo typeInfo, boolean isAdapter) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.typeInfo = typeInfo;
		this.isAdapter = isAdapter;
//		initMembers(cx, scope);
		initMembers(cx, scope);
	}

	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> dynamicType;
		if (javaObject != null) {
			dynamicType = javaObject.getClass();
		} else {
			dynamicType = typeInfo.asClass();
		}
		members = JavaMembers.lookupClass(cx, scope, dynamicType, typeInfo.asClass(), isAdapter);
		fieldAndMethods = members.getFieldAndMethodsObjects(this, javaObject, false);
	}

	@Override
	public boolean has(String name, Scriptable start) {
		return members.has(name, false);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		return false;
	}

	@Override
	public boolean has(Symbol key, Scriptable start) {
		return false;
	}

	@Override
	public Object get(String name, Scriptable start) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}
		// TODO: passing 'this' as the scope is bogus since it has
		//  no parent scope
		return members.get(this, name, javaObject, false);
	}

	@Override
	public Object get(Symbol key, Scriptable start) {
		// Native Java objects have no Symbol members
		return Scriptable.NOT_FOUND;
	}

	@Override
	public Object get(int index, Scriptable start) {
		throw members.reportMemberNotFound(Integer.toString(index));
	}

	@Override
	public void put(String name, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false);
		} else {
			prototype.put(name, prototype, value);
		}
	}

	@Override
	public void put(Symbol symbol, Scriptable start, Object value) {
		// We could be asked to modify the value of a property in the
		// prototype. Since we can't add a property to a Java object,
		// we modify it in the prototype rather than copy it down.
		String name = symbol.toString();
		if (prototype == null || members.has(name, false)) {
			members.put(this, name, javaObject, value, false);
		} else if (prototype instanceof SymbolScriptable) {
			((SymbolScriptable) prototype).put(symbol, prototype, value);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		throw members.reportMemberNotFound(Integer.toString(index));
	}

	@Override
	public boolean hasInstance(Scriptable value) {
		// This is an instance of a Java class, so always return false
		return false;
	}

	@Override
	public void delete(String name) {
		if (fieldAndMethods != null) {
			Object result = fieldAndMethods.get(name);
			if (result != null) {
				Deletable.deleteObject(result);
				return;
			}
		}

		Deletable.deleteObject(members.get(this, name, javaObject, false));
	}

	@Override
	public void delete(Symbol key) {
	}

	@Override
	public void delete(int index) {
	}

	@Override
	public Scriptable getPrototype() {
		if (prototype == null && javaObject instanceof String) {
			return TopLevel.getBuiltinPrototype(ScriptableObject.getTopLevelScope(parent), TopLevel.Builtins.String);
		}
		return prototype;
	}

	/**
	 * Sets the prototype of the object.
	 */
	@Override
	public void setPrototype(Scriptable m) {
		prototype = m;
	}

	/**
	 * Returns the parent (enclosing) scope of the object.
	 */
	@Override
	public Scriptable getParentScope() {
		return parent;
	}

	/**
	 * Sets the parent (enclosing) scope of the object.
	 */
	@Override
	public void setParentScope(Scriptable m) {
		parent = m;
	}

	@Override
	public Object[] getIds() {
		return members.getIds(false);
	}

	@Override
	public Object unwrap() {
		return javaObject;
	}

	@Override
	public String getClassName() {
		return "JavaObject";
	}

	@Override
	public Object getDefaultValue(Class<?> hint) {
		Object value;
		if (hint == null) {
			if (javaObject instanceof Boolean) {
				hint = ScriptRuntime.BooleanClass;
			}
			if (javaObject instanceof Number) {
				hint = ScriptRuntime.NumberClass;
			}
		}
		if (hint == null || hint == ScriptRuntime.StringClass) {
			value = javaObject.toString();
		} else {
			String converterName;
			if (hint == ScriptRuntime.BooleanClass) {
				converterName = "booleanValue";
			} else if (hint == ScriptRuntime.NumberClass) {
				converterName = "doubleValue";
			} else {
				throw Context.reportRuntimeError0("msg.default.value");
			}
			Object converterObject = get(converterName, this);
			if (converterObject instanceof Function f) {
                value = f.call(Context.getContext(), f.getParentScope(), this, ScriptRuntime.emptyArgs);
			} else {
				if (hint == ScriptRuntime.NumberClass && javaObject instanceof Boolean) {
					boolean b = (Boolean) javaObject;
					value = b ? ScriptRuntime.wrapNumber(1.0) : ScriptRuntime.zeroObjInt;
				} else {
					value = javaObject.toString();
				}
			}
		}
		return value;
	}

	/**
	 * Determine whether we can/should convert between the given type and the
	 * desired one.  This should be superceded by a conversion-cost calculation
	 * function, but for now I'll hide behind precedent.
	 */
	public static boolean canConvert(Context cx, Object fromObj, Class<?> to) {
		return getConversionWeight(cx, fromObj, to) < CONVERSION_NONE;
	}

	private static final int JSTYPE_UNDEFINED = 0; // undefined type
	private static final int JSTYPE_NULL = 1; // null
	private static final int JSTYPE_BOOLEAN = 2; // boolean
	private static final int JSTYPE_NUMBER = 3; // number
	private static final int JSTYPE_STRING = 4; // string
	private static final int JSTYPE_JAVA_CLASS = 5; // JavaClass
	private static final int JSTYPE_JAVA_OBJECT = 6; // JavaObject
	private static final int JSTYPE_JAVA_ARRAY = 7; // JavaArray
	private static final int JSTYPE_OBJECT = 8; // Scriptable

	public static final byte CONVERSION_TRIVIAL = 1;
	public static final byte CONVERSION_NONTRIVIAL = 0;
	public static final byte CONVERSION_NONE = 99;

	/**
	 * Derive a ranking based on how "natural" the conversion is.
	 * The special value CONVERSION_NONE means no conversion is possible,
	 * and CONVERSION_NONTRIVIAL signals that more type conformance testing
	 * is required.
	 * Based on
	 * <a href="http://www.mozilla.org/js/liveconnect/lc3_method_overloading.html">
	 * "preferred method conversions" from Live Connect 3</a>
	 */
	public static int getConversionWeight(Context cx, Object fromObj, Class<?> to) {
		if (cx.hasTypeWrappers() && cx.getTypeWrappers().getWrapperFactory(to, fromObj) != null) {
			return CONVERSION_NONTRIVIAL;
		}

		int fromCode = getJSTypeCode(fromObj);

		switch (fromCode) {

			case JSTYPE_UNDEFINED:
				if (to == ScriptRuntime.StringClass || to == ScriptRuntime.ObjectClass) {
					return 1;
				}
				break;

			case JSTYPE_NULL:
				if (!to.isPrimitive()) {
					return 1;
				}
				break;

			case JSTYPE_BOOLEAN:
				// "boolean" is #1
				if (to == Boolean.TYPE) {
					return 1;
				} else if (to == ScriptRuntime.BooleanClass) {
					return 2;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_NUMBER:
				if (to.isPrimitive()) {
					if (to == Double.TYPE) {
						return 1;
					} else if (to != Boolean.TYPE) {
						return 1 + getSizeRank(to);
					}
				} else {
					if (to == ScriptRuntime.StringClass) {
						// native numbers are #1-8
						return 9;
					} else if (to == ScriptRuntime.ObjectClass) {
						return 10;
					} else if (ScriptRuntime.NumberClass.isAssignableFrom(to)) {
						// "double" is #1
						return 2;
					}
				}
				break;

			case JSTYPE_STRING:
				if (to == ScriptRuntime.StringClass) {
					return 1;
				} else if (to.isInstance(fromObj)) {
					return 2;
				} else if (to.isPrimitive()) {
					if (to == Character.TYPE) {
						return 3;
					} else if (to != Boolean.TYPE) {
						return 4;
					}
				}
				break;

			case JSTYPE_JAVA_CLASS:
				if (to == ScriptRuntime.ClassClass) {
					return 1;
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				}
				break;

			case JSTYPE_JAVA_OBJECT:
			case JSTYPE_JAVA_ARRAY:
				val javaObj = fromObj instanceof Wrapper wrapper
					? wrapper.unwrap()
					: fromObj;
				if (to.isInstance(javaObj)) {
					return CONVERSION_NONTRIVIAL;
				}
				if (to == ScriptRuntime.StringClass) {
					return 2;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return (fromCode == JSTYPE_JAVA_ARRAY) ? CONVERSION_NONE : 2 + getSizeRank(to);
				}
				break;

			case JSTYPE_OBJECT:
				// Other objects takes #1-#3 spots
				if (to != ScriptRuntime.ObjectClass && to.isInstance(fromObj)) {
					// No conversion required, but don't apply for java.lang.Object
					return 1;
				}
				if (to.isArray()) {
					if (fromObj instanceof NativeArray) {
						// This is a native array conversion to a java array.
						// Array conversions are all equal, and preferable to object
						// and string conversion, per LC3.
						return 2;
					}
				} else if (to == ScriptRuntime.ObjectClass) {
					return 3;
				} else if (to == ScriptRuntime.StringClass) {
					return 4;
				} else if (to == ScriptRuntime.DateClass) {
					if (fromObj instanceof NativeDate) {
						// This is a native date to java date conversion
						return 1;
					}
				} else if (to.isInterface()) {

					if (fromObj instanceof NativeFunction) {
						// See comments in createInterfaceAdapter
						return 1;
					}
					if (fromObj instanceof NativeObject) {
						return 2;
					}
					return 12;
				} else if (to.isPrimitive() && to != Boolean.TYPE) {
					return 4 + getSizeRank(to);
				}
				break;
		}

		return CONVERSION_NONE;
	}

	public final int getConversionWeight(Context cx, Object from, TypeInfo target) {
		if (cx.hasTypeWrappers() && cx.getTypeWrappers().getWrapperFactory(from, target) != null) {
			return CONVERSION_NONTRIVIAL;
		}

		if (target instanceof ArrayTypeInfo || Collection.class.isAssignableFrom(target.asClass())) {
			return CONVERSION_NONTRIVIAL;
		} else if (target.is(TypeInfo.CLASS)) {
			return from instanceof Class<?> || from instanceof NativeJavaClass ? CONVERSION_TRIVIAL : CONVERSION_NONTRIVIAL;
		} else if (from == null) {
			if (!target.isPrimitive()) {
				return CONVERSION_TRIVIAL;
			}
		} else if (from == Undefined.instance) {
			if (target == TypeInfo.STRING || target == TypeInfo.OBJECT) {
				return CONVERSION_TRIVIAL;
			}
		} else if (from instanceof CharSequence) {
			if (target == TypeInfo.STRING) {
				return CONVERSION_TRIVIAL;
			} else if (target.asClass().isInstance(from)) {
				return 2;
			} else if (target.isPrimitive()) {
				if (target.isCharacter()) {
					return 3;
				} else if (!target.isBoolean()) {
					return 4;
				}
			}
		} else if (from instanceof Number) {
			if (target.isPrimitive()) {
				if (target.isDouble()) {
					return CONVERSION_TRIVIAL;
				} else if (!target.isBoolean()) {
					return CONVERSION_TRIVIAL + getSizeRank(target);
				}
			} else {
				if (target == TypeInfo.STRING) {
					// native numbers are #1-8
					return 9;
				} else if (target == TypeInfo.OBJECT) {
					return 10;
				} else if (ScriptRuntime.NumberClass.isAssignableFrom(target.asClass())) {
					// "double" is #1
					return 2;
				}
			}
		} else if (from instanceof Boolean) {
			// "boolean" is #1
			if (target.isBoolean()) {
				return CONVERSION_TRIVIAL;
			} else if (target == TypeInfo.OBJECT) {
				return 3;
			} else if (target == TypeInfo.STRING) {
				return 4;
			}
		} else if (from instanceof Class || from instanceof NativeJavaClass) {
			if (target.is(TypeInfo.CLASS)) {
				return CONVERSION_NONTRIVIAL;
			} else if (target == TypeInfo.OBJECT) {
				return 3;
			} else if (target == TypeInfo.STRING) {
				return 4;
			}
		}

		int fromCode = getJSTypeCode(from);

		switch (fromCode) {
			case JSTYPE_JAVA_OBJECT, JSTYPE_JAVA_ARRAY -> {
				Object javaObj = Wrapper.unwrapped(from);
				if (target.asClass().isInstance(javaObj)) {
					return CONVERSION_NONTRIVIAL;
				} else if (target == TypeInfo.STRING) {
					return 2;
				} else if (target.isPrimitive() && !target.isBoolean()) {
					return (fromCode == JSTYPE_JAVA_ARRAY) ? CONVERSION_NONE : 2 + getSizeRank(target);
				} else if (target instanceof ArrayTypeInfo) {
					return 3;
				} else {
					return internalConversionWeightLast(from, target);
				}
			}
			case JSTYPE_OBJECT -> {
				// Other objects takes #1-#3 spots
				if (target != TypeInfo.OBJECT && target.asClass().isInstance(from)) {
					// No conversion required, but don't apply for java.lang.Object
					return CONVERSION_TRIVIAL;
				}
				if (target instanceof ArrayTypeInfo) {
					if (from instanceof NativeArray) {
						// This is a native array conversion to a java array
						// Array conversions are all equal, and preferable to object
						// and string conversion, per LC3.
						return 2;
					} else {
						return CONVERSION_TRIVIAL;
					}
				} else if (target == TypeInfo.OBJECT) {
					return 3;
				} else if (target == TypeInfo.STRING) {
					return 4;
				} else if (target == TypeInfo.DATE) {
					if (from instanceof NativeDate) {
						// This is a native date to java date conversion
						return CONVERSION_TRIVIAL;
					}
				} else if (target.isFunctionalInterface()) {
					if (from instanceof NativeFunction) {
						// See comments in createInterfaceAdapter
						return CONVERSION_TRIVIAL;
					}
					if (from instanceof NativeObject) {
						return 2;
					}
					return 12;
				} else if (target.isPrimitive() && !target.isBoolean()) {
					return 4 + getSizeRank(target);
				}
			}
		}

		return internalConversionWeightLast(from, target);
	}

	public int internalConversionWeightLast(Object fromObj, TypeInfo target) {
		return CONVERSION_NONE;
	}

	public static int getSizeRank(TypeInfo aType) {
		if (aType.isDouble()) {
			return 1;
		} else if (aType.isFloat()) {
			return 2;
		} else if (aType.isLong()) {
			return 3;
		} else if (aType.isInt()) {
			return 4;
		} else if (aType.isShort()) {
			return 5;
		} else if (aType.isCharacter()) {
			return 6;
		} else if (aType.isByte()) {
			return 7;
		} else if (aType.isBoolean()) {
			return CONVERSION_NONE;
		} else {
			return 8;
		}
	}

	public static Object coerceToNumber(TypeInfo target, Object value) {
		Class<?> valueClass = value.getClass();

		// Character
		if (target.isCharacter()) {
			if (valueClass == ScriptRuntime.CharacterClass) {
				return value;
			}
			return (char) toInteger(value, target, Character.MIN_VALUE, Character.MAX_VALUE);
		}

		// Double, Float
		if (target == TypeInfo.OBJECT || target.isDouble()) {
			return valueClass == ScriptRuntime.DoubleClass ? value : Double.valueOf(toDouble(value));
		}

		if (target.isFloat()) {
			if (valueClass == ScriptRuntime.FloatClass) {
				return value;
			}
			double number = toDouble(value);
			if (Double.isInfinite(number) || Double.isNaN(number) || number == 0.0) {
				return (float) number;
			}

			double absNumber = Math.abs(number);
			if (absNumber < Float.MIN_VALUE) {
				return (number > 0.0) ? +0.0f : -0.0f;
			} else if (absNumber > Float.MAX_VALUE) {
				return (number > 0.0) ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY;
			} else {
				return (float) number;
			}
		}

		// Integer, Long, Short, Byte
		if (target.isInt()) {
			if (valueClass == ScriptRuntime.IntegerClass) {
				return value;
			}
			return (int) toInteger(value, target, Integer.MIN_VALUE, Integer.MAX_VALUE);
		}

		if (target.isLong()) {
			if (valueClass == ScriptRuntime.LongClass) {
				return value;
			}
			/* Long values cannot be expressed exactly in doubles.
			 * We thus use the largest and smallest double value that
			 * has a value expressible as a long value. We build these
			 * numerical values from their hexidecimal representations
			 * to avoid any problems caused by attempting to parse a
			 * decimal representation.
			 */
			final double max = Double.longBitsToDouble(0x43dfffffffffffffL);
			final double min = Double.longBitsToDouble(0xc3e0000000000000L);
			return toInteger(value, target, min, max);
		}

		if (target.isShort()) {
			if (valueClass == ScriptRuntime.ShortClass) {
				return value;
			}
			return (short) toInteger(value, target, Short.MIN_VALUE, Short.MAX_VALUE);
		}

		if (target.isByte()) {
			if (valueClass == ScriptRuntime.ByteClass) {
				return value;
			}
			return (byte) toInteger(value, target, Byte.MIN_VALUE, Byte.MAX_VALUE);
		}

		return toDouble(value);
	}

	protected static long toInteger(Object value, TypeInfo type, double min, double max) {
		double d = toDouble(value);

		if (Double.isInfinite(d) || Double.isNaN(d)) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(value), type);
		}

		if (d > 0.0) {
			d = Math.floor(d);
		} else {
			d = Math.ceil(d);
		}

		if (d < min || d > max) {
			// Convert to string first, for more readable message
			reportConversionError(ScriptRuntime.toString(value), type);
		}
		return (long) d;
	}

	public static double toDouble(Object value) {
		if (value instanceof Number num) {
			return num.doubleValue();
		} else if (value instanceof String str) {
			return ScriptRuntime.toNumber(str);
		} else if (value instanceof Scriptable) {
			if (value instanceof Wrapper wrapper) {
				// XXX: optimize tail-recursion?
				return toDouble(wrapper.unwrap());
			}
			return ScriptRuntime.toNumber(value);
		} else if (value instanceof DoubleSupplier supplier) {
			return supplier.getAsDouble();
		}
		Method meth;
		try {
			meth = value.getClass().getMethod("doubleValue", (Class[]) null);
		} catch (NoSuchMethodException | SecurityException e) {
			meth = null;
		}
		if (meth != null) {
			try {
				return ((Number) meth.invoke(value, (Object[]) null)).doubleValue();
			} catch (IllegalAccessException | InvocationTargetException e) {
				// XXX: ignore, or error message?
				reportConversionError(value, Double.TYPE);
			}
		}
		return ScriptRuntime.toNumber(value.toString());
    }

	public static Object reportConversionError(Object value, TypeInfo type) {
		throw Context.reportRuntimeError2("msg.conversion.not.allowed", String.valueOf(value), type.signature());
	}

	static int getSizeRank(Class<?> aType) {
		if (aType == Double.TYPE) {
			return 1;
		} else if (aType == Float.TYPE) {
			return 2;
		} else if (aType == Long.TYPE) {
			return 3;
		} else if (aType == Integer.TYPE) {
			return 4;
		} else if (aType == Short.TYPE) {
			return 5;
		} else if (aType == Character.TYPE) {
			return 6;
		} else if (aType == Byte.TYPE) {
			return 7;
		} else if (aType == Boolean.TYPE) {
			return CONVERSION_NONE;
		} else {
			return 8;
		}
	}

	private static int getJSTypeCode(Object value) {
		if (value == null) {
			return JSTYPE_NULL;
		} else if (value == Undefined.instance) {
			return JSTYPE_UNDEFINED;
		} else if (value instanceof CharSequence) {
			return JSTYPE_STRING;
		} else if (value instanceof Number) {
			return JSTYPE_NUMBER;
		} else if (value instanceof Boolean) {
			return JSTYPE_BOOLEAN;
		} else if (value instanceof Scriptable) {
            if (value instanceof NativeJavaClass) {
                return JSTYPE_JAVA_CLASS;
            } else if (value instanceof NativeJavaArray) {
                return JSTYPE_JAVA_ARRAY;
            } else if (value instanceof Wrapper) {
                return JSTYPE_JAVA_OBJECT;
            }
            return JSTYPE_OBJECT;
        } else if (value instanceof Class) {
			return JSTYPE_JAVA_CLASS;
		}
        return value.getClass().isArray() ? JSTYPE_JAVA_ARRAY : JSTYPE_JAVA_OBJECT;
    }

	public static Object createInterfaceAdapter(Context cx, Class<?> type, ScriptableObject so) {
		// XXX: Currently only instances of ScriptableObject are
		// supported since the resulting interface proxies should
		// be reused next time conversion is made and generic
		// Callable has no storage for it. Weak references can
		// address it but for now use this restriction.

		val key = Kit.makeHashKeyFromPair(COERCED_INTERFACE_KEY, type);
		val old = so.getAssociatedValue(key);
		if (old != null) {
			// Function was already wrapped
			return old;
		}
		// Store for later retrieval
        return so.associateValue(key, InterfaceAdapter.create(cx, type, so));
	}

	static Object reportConversionError(Object value, Class<?> type) {
		return reportConversionError(value, type, value);
	}

	static Object reportConversionError(Object value, Class<?> type, Object stringValue) {
		// It uses String.valueOf(value), not value.toString() since
		// value can be null, bug 282447.
		throw Context.reportRuntimeError2(
			"msg.conversion.not.allowed",
			String.valueOf(stringValue),
			ReflectsKit.javaSignature(type)
		);
	}
}
