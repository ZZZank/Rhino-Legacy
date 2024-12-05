/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import lombok.AllArgsConstructor;
import lombok.val;

import java.lang.reflect.*;

public class VMBridge {

	public static final VMBridge vm = makeVMInstance();
	private static final ThreadLocal<Object[]> contextLocal = new ThreadLocal<>();

	private static VMBridge makeVMInstance() {
		/*
		String[] classNames = {"dev.latvian.mods.rhino.VMBridge_custom", "dev.latvian.mods.rhino.VMBridge.VMBridge_jdk18",};
		for (int i = 0; i != classNames.length; ++i) {
			String className = classNames[i];
			Class<?> cl = Kit.classOrNull(className);
			if (cl != null) {
				VMBridge bridge = (VMBridge) Kit.newInstanceOrNull(cl);
				if (bridge != null) {
					return bridge;
				}
			}
		}
		throw new IllegalStateException("Failed to create VMBridge instance");
		 */

		return new VMBridge();
	}

	protected Object getThreadContextHelper() {
		// To make subsequent batch calls to getContext/setContext faster
		// associate permanently one element array with contextLocal
		// so getContext/setContext would need just to read/write the first
		// array element.
		// Note that it is necessary to use Object[], not Context[] to allow
		// garbage collection of Rhino classes. For details see comments
		// by Attila Szegedi in
		// https://bugzilla.mozilla.org/show_bug.cgi?id=281067#c5

		Object[] storage = VMBridge.contextLocal.get();
		if (storage == null) {
			storage = new Object[1];
			VMBridge.contextLocal.set(storage);
		}
		return storage;
	}

	protected Context getContext(Object contextHelper) {
		val storage = (Object[]) contextHelper;
		return (Context) storage[0];
	}

	protected void setContext(Object contextHelper, Context cx) {
		val storage = (Object[]) contextHelper;
		storage[0] = cx;
	}

	public boolean tryToMakeAccessible(AccessibleObject accessible) {
		if (accessible.isAccessible()) {
			return true;
		}

		try {
			accessible.setAccessible(true);
		} catch (Exception ignored) {
		}

		return accessible.isAccessible();
	}

	protected Object getInterfaceProxyHelper(ContextFactory cf, Class<?>[] interfaces) {
		// XXX: How to handle interfaces array withclasses from different
		// class loaders? Using cf.getApplicationClassLoader() ?
		val loader = interfaces[0].getClassLoader();
		val cl = Proxy.getProxyClass(loader, interfaces);
		Constructor<?> c;
		try {
			c = cl.getConstructor(InvocationHandler.class);
		} catch (NoSuchMethodException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		}
		return c;
	}

	protected Object newInterfaceProxy(
		Object proxyHelper,
		final ContextFactory cf,
		final InterfaceAdapter adapter,
		final Object target,
		final Scriptable topScope
	) {
		val c = (Constructor<?>) proxyHelper;

        try {
            return c.newInstance(new DefaultInvocationHandler(target, adapter, cf, topScope));
		} catch (InvocationTargetException ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		} catch (IllegalAccessException | InstantiationException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		}
    }

	@AllArgsConstructor
	private static final class DefaultInvocationHandler implements InvocationHandler {
		private final Object target;
		private final InterfaceAdapter adapter;
		private final ContextFactory cf;
		private final Scriptable topScope;

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// In addition to methods declared in the interface, proxies
			// also route some java.lang.Object methods through the
			// invocation handler.
			if (method.getDeclaringClass() == Object.class) {
				val methodName = method.getName();
				switch (methodName) {
					case "equals":
						// Note: we could compare a proxy and its wrapped function
						// as equal here but that would break symmetry of equal().
						// The reason == suffices here is that proxies are cached
						// in ScriptableObject (see NativeJavaObject.coerceType())
						return proxy == args[0];
					case "hashCode":
						return target.hashCode();
					case "toString":
						return "Proxy[" + target.toString() + "]";
				}
			}
			return adapter.invoke(cf, target, topScope, proxy, method, args);
		}
	}
}
