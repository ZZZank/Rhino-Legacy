/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.*;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * not moving to {@link dev.latvian.mods.rhino.native_java} for KubeJS compat
 * <p>
 * This class reflects Java classes into the JavaScript environment, mainly
 * for constructors and static members.  We lazily reflect properties,
 * and currently do not guarantee that a single j.l.Class is only
 * reflected once into the JS environment, although we should.
 * The only known case where multiple reflections
 * are possible occurs when a j.l.Class is wrapped as part of a
 * method return or property access, rather than by walking the
 * Packages/java tree.
 *
 * @author Mike Shaver
 * @see NativeJavaArray
 * @see NativeJavaObject
 * @see NativeJavaPackage
 */
public class NativeJavaClass extends NativeJavaObject implements Function {
	private static final long serialVersionUID = -6460763940409461664L;

	// Special property for getting the underlying Java class object.
	static final String javaClassPropertyName = "__javaObject__";

	public NativeJavaClass(Context cx, Scriptable scope, Class<?> clazz, boolean isAdapter) {
		super(cx, scope, clazz, null, isAdapter);
	}

	public NativeJavaClass(Context cx, Scriptable scope, Class<?> clazz) {
		this(cx, scope, clazz, false);
	}

	@Deprecated
	public NativeJavaClass(Scriptable scope, Class<?> value) {
		this(Context.getContext(), scope, value);
	}

	@Override
	protected void initMembers(Context cx, Scriptable scope) {
		Class<?> cl = (Class<?>) javaObject;
		members = JavaMembers.lookupClass(cx, scope, cl, cl, isAdapter);
		staticFieldAndMethods = members.getFieldAndMethodsObjects(this, cl, true);
	}

	@Override
	public String getClassName() {
		return "JavaClass";
	}

	@Override
	public boolean has(String name, Scriptable start) {
		return members.has(name, true) || javaClassPropertyName.equals(name);
	}

	@Override
	public Object get(String name, Scriptable start) {
		// When used as a constructor, ScriptRuntime.newObject() asks
		// for our prototype to create an object of the correct type.
		// We don't really care what the object is, since we're returning
		// one constructed out of whole cloth, so we return null.
		if (name.equals("prototype")) {
			return null;
		}

		if (staticFieldAndMethods != null) {
			Object result = staticFieldAndMethods.get(name);
			if (result != null) {
				return result;
			}
		}
		if (members.has(name, true)) {
			return members.get(this, name, javaObject, true);
		}

		val cx = Context.getContext();
		val scope = ScriptableObject.getTopLevelScope(start);
		val wrapFactory = cx.getWrapFactory();

		if (javaClassPropertyName.equals(name)) {
			return wrapFactory.wrap(cx, scope, javaObject, TypeInfo.CLASS);
		}

		// experimental:  look for nested classes by appending $name to
		// current class' name.
		Class<?> nestedClass = findNestedClass(getClassObject(), name);
		if (nestedClass != null) {
			val nestedValue = wrapFactory.wrapJavaClass(cx, scope, nestedClass);
			nestedValue.setParentScope(this);
			return nestedValue;
		}

		throw members.reportMemberNotFound(name);
	}

	@Override
	public void put(String name, Scriptable start, Object value) {
		members.put(this, name, javaObject, value, true);
	}

	@Override
	public Object[] getIds() {
		return members.getIds(true);
	}

	public Class<?> getClassObject() {
		return (Class<?>) super.unwrap();
	}

	@Override
	public Object getDefaultValue(Class<?> hint) {
		if (hint == null || hint == ScriptRuntime.StringClass) {
			return this.toString();
		}
		if (hint == ScriptRuntime.BooleanClass) {
			return Boolean.TRUE;
		}
		if (hint == ScriptRuntime.NumberClass) {
			return ScriptRuntime.NaNobj;
		}
		return this;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		// If it looks like a "cast" of an object to this class type,
		// walk the prototype chain to see if there's a wrapper of a
		// object that's an instanceof this class.
		if (args.length == 1 && args[0] instanceof Scriptable p) {
			Class<?> c = getClassObject();
            do {
				if (p instanceof Wrapper wrapper) {
					if (c.isInstance(wrapper.unwrap())) {
						return p;
					}
				}
				p = p.getPrototype();
			} while (p != null);
		}
		return construct(cx, scope, args);
	}

	@Override
	public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
		Class<?> classObject = getClassObject();
		int modifiers = classObject.getModifiers();
		if (!(Modifier.isInterface(modifiers) || Modifier.isAbstract(modifiers))) {
			val ctors = members.ctors;
			int index = ctors.findCachedFunction(cx, args);
			if (index < 0) {
				String sig = NativeJavaMethod.scriptSignature(args);
				throw Context.reportRuntimeError2("msg.no.java.ctor", classObject.getName(), sig);
			}

			// Found the constructor, so try invoking it.
			return constructSpecific(cx, scope, args, ctors.methods[index]);
		}
		if (args.length == 0) {
			throw Context.reportRuntimeError0("msg.adapter.zero.args");
		}
		Scriptable topLevel = ScriptableObject.getTopLevelScope(this);
		String msg = "";
		try {
			// use JavaAdapter to construct a new class on the fly that
			// implements/extends this interface/abstract class.
			Object v = topLevel.get("JavaAdapter", topLevel);
			if (v != NOT_FOUND) {
				Function f = (Function) v;
				// Args are (interface, js object)
				Object[] adapterArgs = {this, args[0]};
				return f.construct(cx, topLevel, adapterArgs);
			}
		} catch (Exception ex) {
			// fall through to error
			String m = ex.getMessage();
			if (m != null) {
				msg = m;
			}
		}
		throw Context.reportRuntimeError2("msg.cant.instantiate", msg, classObject.getName());
	}

	public static Scriptable constructSpecific(Context cx, Scriptable scope, Object[] args, MemberBox ctor) {
		Object instance = constructInternal(cx, args, ctor);
		// we need to force this to be wrapped, because construct _has_
		// to return a scriptable
		Scriptable topLevel = ScriptableObject.getTopLevelScope(scope);
		return cx.getWrapFactory().wrapNewObject(cx, topLevel, instance, ctor.getReturnTypeInfo());
	}

	static Object constructInternal(Context cx, Object[] args, MemberBox ctor) {
        args = ctor.vararg
			? JavaArgWrapping.wrapVarArgs(cx, args, ctor.getArgTypeInfos())
			: JavaArgWrapping.wrapRegularArgs(cx, args, ctor.getArgTypeInfos());
		return ctor.newInstance(args);
	}

	@Override
	public String toString() {
		return "[JavaClass " + getClassObject().getName() + "]";
	}

	/**
	 * Determines if prototype is a wrapped Java object and performs
	 * a Java "instanceof".
	 * Exception: if value is an instance of NativeJavaClass, it isn't
	 * considered an instance of the Java class; this forestalls any
	 * name conflicts between java.lang.Class's methods and the
	 * static methods exposed by a JavaNativeClass.
	 */
	@Override
	public boolean hasInstance(Scriptable value) {

		if (value instanceof Wrapper && !(value instanceof NativeJavaClass)) {
			Object instance = ((Wrapper) value).unwrap();

			return getClassObject().isInstance(instance);
		}

		// value wasn't something we understand
		return false;
	}

	private static Class<?> findNestedClass(Class<?> parentClass, String name) {
		String nestedClassName = parentClass.getName() + '$' + name;
		ClassLoader loader = parentClass.getClassLoader();
		if (loader == null) {
			// ALERT: if loader is null, nested class should be loaded
			// via system class loader which can be different from the
			// loader that brought Rhino classes that Class.forName() would
			// use, but ClassLoader.getSystemClassLoader() is Java 2 only
			return Kit.classOrNull(nestedClassName);
		}
		return Kit.classOrNull(loader, nestedClassName);
	}

	private Map<String, FieldAndMethods> staticFieldAndMethods;
}
