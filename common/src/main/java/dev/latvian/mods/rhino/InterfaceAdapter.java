/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Adapter to use JS function as implementation of Java interfaces with
 * single method or multiple methods with the same signature.
 */
public class InterfaceAdapter {
	private final Object proxyHelper;

	/**
	 * Make glue object implementing interface cl that will
	 * call the supplied JS function when called.
	 * Only interfaces were all methods have the same signature is supported.
	 *
	 * @return The glue object or null if <code>cl</code> is not interface or
	 * has methods with different signatures.
	 */
	public static Object create(Context cx, Class<?> cl, ScriptableObject object) {
		if (!cl.isInterface()) {
			throw new IllegalArgumentException();
		}

		val topScope = ScriptRuntime.getTopCallScope(cx);
		val cache = ClassCache.get(topScope);
		InterfaceAdapter adapter;
		adapter = (InterfaceAdapter) cache.getInterfaceAdapter(cl);
		val cf = cx.getFactory();
		if (adapter == null) {
			if (object instanceof Callable) {
				// Check if interface can be implemented by a single function.
				// We allow this if the interface has only one method or multiple
				// methods with the same name (in which case they'd result in
				// the same function to be invoked anyway).
				val methods = cl.getMethods();
				val length = methods.length;
				if (length == 0) {
					throw Context.reportRuntimeError1("msg.no.empty.interface.conversion", cl.getName());
				}
				if (length > 1) {
					String methodName = null;
					for (Method method : methods) {
						// there are multiple methods in the interface we inspect
						// only abstract ones, they must all have the same name.
						if (isFunctionalMethodCandidate(method)) {
							if (methodName == null) {
								methodName = method.getName();
							} else if (!methodName.equals(method.getName())) {
								throw Context.reportRuntimeError1("msg.no.function.interface.conversion", cl.getName());
							}
						}
					}
				}
			}
			adapter = new InterfaceAdapter(cf, cl);
			cache.cacheInterfaceAdapter(cl, adapter);
		}
		return VMBridge.vm.newInterfaceProxy(adapter.proxyHelper, cf, adapter, object, topScope);
	}

	/**
	 * We have to ignore java8 default methods and methods like 'equals', 'hashCode'
	 * and 'toString' as it occurs for example in the Comparator interface.
	 *
	 * @return true, if the function
	 */
	private static boolean isFunctionalMethodCandidate(Method method) {
		if (method.getName().equals("equals") || method.getName().equals("hashCode") || method.getName().equals("toString")) {
			// it should be safe to ignore them as there is also a special
			// case for these methods in VMBridge_jdk18.newInterfaceProxy
			return false;
		} else {
			return Modifier.isAbstract(method.getModifiers());
		}
	}

	private InterfaceAdapter(ContextFactory cf, Class<?> cl) {
		this.proxyHelper = VMBridge.vm.getInterfaceProxyHelper(cf, new Class[]{cl});
	}

	public Object invoke(ContextFactory cf, final Object target, final Scriptable topScope, final Object thisObject, final Method method, final Object[] args) {
		return cf.call(cx -> invokeImpl(cx, target, topScope, thisObject, method, args));
	}

	Object invokeImpl(Context cx, Object target, Scriptable topScope, Object thisObject, Method method, Object[] args) {
		Callable function;
		if (target instanceof Callable) {
			function = (Callable) target;
		} else {
			val s = (Scriptable) target;
			val methodName = method.getName();
			val value = ScriptableObject.getProperty(s, methodName);
			if (value == Scriptable.NOT_FOUND) {
				// We really should throw an error here, but for the sake of
				// compatibility with JavaAdapter we silently ignore undefined
				// methods.
				Context.reportWarning(ScriptRuntime.getMessage1("msg.undefined.function.interface", methodName));
                if (method.getReturnType() == Void.TYPE) {
					return null;
				}
				return Context.jsToJava(cx, null, TypeInfo.of(method.getGenericReturnType()));
			}
			if (!(value instanceof Callable)) {
				throw Context.reportRuntimeError1("msg.not.function.interface", methodName);
			}
			function = (Callable) value;
		}
		val wf = cx.getWrapFactory();
		if (args == null) {
			args = ScriptRuntime.emptyArgs;
		} else {
			for (int i = 0, N = args.length; i != N; ++i) {
				Object arg = args[i];
				// neutralize wrap factory java primitive wrap feature
				if (!(arg instanceof String || arg instanceof Number || arg instanceof Boolean)) {
					args[i] = wf.wrap(cx, topScope, arg, TypeInfo.NONE);
				}
			}
		}
		val thisObj = wf.wrapAsJavaObject(cx, topScope, thisObject, TypeInfo.NONE);

		val result = function.call(cx, topScope, thisObj, args);
		if (method.getReturnType() == Void.TYPE) {
            return null;
        }
        return Context.jsToJava(cx, result, TypeInfo.of(method.getGenericReturnType()));
    }
}
