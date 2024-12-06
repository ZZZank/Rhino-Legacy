/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.ReflectsKit;
import dev.latvian.mods.rhino.native_java.FieldAndMethods;
import dev.latvian.mods.rhino.native_java.JavaMembers;
import dev.latvian.mods.rhino.native_java.NativeJavaPackage;
import dev.latvian.mods.rhino.native_java.type.Converter;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;
import lombok.Getter;
import lombok.val;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

	protected transient final Object javaObject;
	protected transient final TypeInfo typeInfo;

	@Getter
	protected transient JavaMembers members;
	private transient Map<String, FieldAndMethods> fieldAndMethods;
	protected transient final boolean isAdapter;

	@Deprecated
	public NativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType) {
		this(Context.getContext(), scope, javaObject, TypeInfo.of(staticType));
	}

	public NativeJavaObject(Context cx, Scriptable scope, Object javaObject, TypeInfo typeInfo) {
		this(cx, scope, javaObject, typeInfo, false);
	}

	public NativeJavaObject(Context cx, Scriptable scope, Object javaObject, TypeInfo typeInfo, boolean isAdapter) {
		this.parent = scope;
		this.javaObject = javaObject;
		this.typeInfo = typeInfo;
		this.isAdapter = isAdapter;
		initMembers(cx, scope);
	}

	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> dynamicType = javaObject != null
			? javaObject.getClass()
			: typeInfo.asClass();
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
			} else if (javaObject instanceof Number) {
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
	@Deprecated
	public static boolean canConvert(Context cx, Object fromObj, Class<?> to) {
		return Converter.getConversionWeight(cx, fromObj, TypeInfo.of(to)) < Converter.CONVERSION_NONE;
	}

	/**
	 * Derive a ranking based on how "natural" the conversion is.
	 * The special value CONVERSION_NONE means no conversion is possible,
	 * and CONVERSION_NONTRIVIAL signals that more type conformance testing
	 * is required.
	 * Based on
	 * <a href="http://www.mozilla.org/js/liveconnect/lc3_method_overloading.html">
	 * "preferred method conversions" from Live Connect 3</a>
	 */
	@Deprecated
	public static int getConversionWeight(Context cx, Object fromObj, Class<?> to) {
        return Converter.getConversionWeight(cx, fromObj, TypeInfo.of(to));
	}

	@Deprecated
	public static int getConversionWeight(Context cx, Object from, TypeInfo target) {
		return Converter.getConversionWeight(cx, from, target);
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
			return Converter.CONVERSION_NONE;
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
