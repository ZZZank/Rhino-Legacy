/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.native_java.original;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContinuationPending;
import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.ReflectsKit;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */
@Getter
public final class MemberBox implements Serializable {
	private static final long serialVersionUID = 6358550398665688245L;
	private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

	private transient Executable memberObject;
	transient final Class<?>[] argTypes;
	@Setter
	transient Object delegateTo;
	transient final boolean vararg;
	private MethodHandle handle = null;
	private boolean handleInit = false;

	public MemberBox(Method method) {
		this.memberObject = method;
		this.argTypes = method.getParameterTypes();
		this.vararg = method.isVarArgs();
	}

	public MemberBox(Constructor<?> constructor) {
		this.memberObject = constructor;
		this.argTypes = constructor.getParameterTypes();
		this.vararg = constructor.isVarArgs();
	}

	private MethodHandle refreshHandle() throws IllegalAccessException {
		if (handleInit) {
			return handle;
		}
		handleInit = true;
		if (memberObject instanceof Method m) {
			return handle = lookup.unreflect(m);
		} else if (memberObject instanceof Constructor<?> c) {
			return handle = lookup.unreflectConstructor(c);
		} else {
			throw new IllegalStateException("MemberBox#memberObject neither a Method nor a Constructor");
		}
	}

	public Method method() {
		return (Method) memberObject;
	}

	public Constructor<?> ctor() {
		return (Constructor<?>) memberObject;
	}

	public Member member() {
		return memberObject;
	}

	public boolean isMethod() {
		return memberObject instanceof Method;
	}

	public boolean isCtor() {
		return memberObject instanceof Constructor;
	}

	public boolean isStatic() {
		return Modifier.isStatic(memberObject.getModifiers());
	}

	public boolean isPublic() {
		return Modifier.isPublic(memberObject.getModifiers());
	}

	public String getName() {
		return memberObject.getName();
	}

	public Class<?> getDeclaringClass() {
		return memberObject.getDeclaringClass();
	}

	public String toJavaDeclaration() {
		StringBuilder sb = new StringBuilder();
		if (isMethod()) {
			Method method = method();
			sb.append(method.getReturnType());
			sb.append(' ');
			sb.append(method.getName());
		} else {
			Constructor<?> ctor = ctor();
			String name = ctor.getDeclaringClass().getName();
			int lastDot = name.lastIndexOf('.');
			if (lastDot >= 0) {
				name = name.substring(lastDot + 1);
			}
			sb.append(name);
		}
		sb.append(ReflectsKit.liveConnectSignature(getArgTypes()));
		return sb.toString();
	}

	@Override
	public String toString() {
		return memberObject.toString();
	}

	public Object invoke(Object target, Object[] args) {
		Method method = method();
		try {
			try {
				return method.invoke(target, args);
			} catch (IllegalAccessException ex) {
				handleInit = false;
				val accessible = searchAccessibleMethod(method, getArgTypes());
				if (accessible != null) {
					memberObject = accessible;
					method = accessible;
				} else if (!VMBridge.vm.tryToMakeAccessible(method)) {
                    throw Context.throwAsScriptRuntimeEx(ex);
                }
				// Retry after recovery
				return method.invoke(target, args);
			}
		} catch (InvocationTargetException ite) {
			// Must allow ContinuationPending exceptions to propagate unhindered
			Throwable e = ite.getTargetException();
            while (e instanceof InvocationTargetException invok) {
                e = invok.getTargetException();
            }
			if (e instanceof ContinuationPending pending) {
				throw pending;
			}
			throw Context.throwAsScriptRuntimeEx(e);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	public Object newInstance(Object[] args) {
		Constructor<?> ctor = ctor();
		try {
			try {
				return ctor.newInstance(args);
			} catch (IllegalAccessException ex) {
				if (!VMBridge.vm.tryToMakeAccessible(ctor)) {
					throw Context.throwAsScriptRuntimeEx(ex);
				}
			}
			return ctor.newInstance(args);
		} catch (Exception ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	public Object constructOrStaticInvoke(Object... args) {
		return this.isCtor() ? newInstance(args) : invoke(null, args);
	}

	private static Method searchAccessibleMethod(Method method, Class<?>[] params) {
		val modifiers = method.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)) {
            return null;
        }
        Class<?> c = method.getDeclaringClass();
        if (Modifier.isPublic(c.getModifiers())) {
            return null;
        }
        val name = method.getName();
		for (val intf : c.getInterfaces()) {
			if (Modifier.isPublic(intf.getModifiers())) {
				try {
					return intf.getMethod(name, params);
				} catch (NoSuchMethodException | SecurityException ignored) {
				}
			}
		}
        for (; ; ) {
            c = c.getSuperclass();
            if (c == null) {
                break;
            }
            if (Modifier.isPublic(c.getModifiers())) {
                try {
                    Method m = c.getMethod(name, params);
                    int mModifiers = m.getModifiers();
                    if (Modifier.isPublic(mModifiers) && !Modifier.isStatic(mModifiers)) {
                        return m;
                    }
                } catch (NoSuchMethodException | SecurityException ignored) {
                }
            }
        }
        return null;
	}

	private static final Class<?>[] primitives = {
		Boolean.TYPE, Byte.TYPE, Character.TYPE, Double.TYPE, Float.TYPE, Integer.TYPE, Long.TYPE, Short.TYPE, Void.TYPE
	};
}

