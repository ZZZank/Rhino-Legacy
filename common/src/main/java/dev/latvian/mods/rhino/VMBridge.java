/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// API class

package dev.latvian.mods.rhino;

import com.github.bsideup.jabel.Desugar;
import lombok.val;

import java.lang.reflect.*;

public class VMBridge {

	public static final VMBridge vm = new VMBridge();
	/**
     * To make subsequent batch calls to getContext/setContext faster,
     * associate permanently one element array with contextLocal,
     * so getContext/setContext would need just to read/write the first
     * array element.
	 * <p>
     * Note that it is necessary to use Object[], not Context[] to allow
     * garbage collection of Rhino classes. For details see comments
     * by Attila Szegedi in
     * <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=281067#c5">bugzilla</a>
     */
	private static final ThreadLocal<Object[]> contextLocal = ThreadLocal.withInitial(() -> new Object[1]);

	protected Object getThreadContextHelper() {
		return contextLocal.get();
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

	protected Object getInterfaceProxyHelper(ContextFactory factory, Class<?>... interfaces) {
		// XXX: How to handle interfaces array withclasses from different
		// class loaders? Using cf.getApplicationClassLoader() ?
		val loader = interfaces[0].getClassLoader();
		val cl = Proxy.getProxyClass(loader, interfaces);
        try {
            return cl.getConstructor(InvocationHandler.class);
		} catch (NoSuchMethodException ex) {
			throw new IllegalStateException(ex);// Should not happen
		}
    }

	protected Object newInterfaceProxy(
		Object proxyHelper,
		final Context cx,
		final InterfaceAdapter adapter,
		final Object target,
		final Scriptable topScope
	) {
		val c = (Constructor<?>) proxyHelper;

        try {
            return c.newInstance(new DefaultInvocationHandler(cx, topScope, target, adapter));
		} catch (InvocationTargetException ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		} catch (IllegalAccessException | InstantiationException ex) {
			// Should not happen
			throw new IllegalStateException(ex);
		}
    }

	@Desugar
	private record DefaultInvocationHandler(
		Context cx,
		Scriptable topScope,
		Object target,
		InterfaceAdapter adapter
	) implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// In addition to methods declared in the interface, proxies
			// also route some java.lang.Object methods through the
			// invocation handler.
			if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
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
			if (Context.getCurrentContext() != null) {
				return adapter.invoke(cx, target, topScope, proxy, method, args);
			}
			//ugly hack to make some (almost all) methods relying on Context.getContext() work
			cx.getFactory().enterContext(cx);
			try {
				return adapter.invoke(cx, target, topScope, proxy, method, args);
			} finally {
				Context.exit();
			}
		}
	}
}
