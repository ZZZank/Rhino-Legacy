/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.native_java.original;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.ReflectsKit;
import dev.latvian.mods.rhino.native_java.reflectasm.MethodAccess;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.io.Serializable;
import java.lang.reflect.*;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */
@Getter
public final class MemberBox implements Serializable {

	private static final Map<Class<?>, MethodAccess> ACCESSES = new IdentityHashMap<>();

	private transient Executable memberObject;
	transient final Class<?>[] argTypes;
	@Setter
	transient Object delegateTo;
	transient final boolean vararg;
	private int indexASM = -1;
	private MethodAccess accessASM = null;

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

	public String toJavaDeclaration(Context cx) {
		val sb = new StringBuilder();
		if (isMethod()) {
			val method = method();
			sb.append(method.getReturnType())
				.append(' ')
				.append(cx.getRemapper().remapMethodSafe(method.getDeclaringClass(), method));
		} else {
			val ctor = ctor();
			String name = ctor.getDeclaringClass().getName();
			val lastDot = name.lastIndexOf('.');
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

	private void ensureASM() {
		if (indexASM >= 0) {
			return;
		}
		val method = method();
		accessASM = ACCESSES.computeIfAbsent(method.getDeclaringClass(), MethodAccess::get);
		indexASM = accessASM.getIndex(method);
	}

	public Object invoke(Object target, Object... args) {
		val method = method();
		try {
			ensureASM(); //trigger init for index and accessASM
			try {
				return accessASM.invoke(target, indexASM, args);
			} catch (IllegalAccessError e) {
				val accessible = searchAccessibleMethod(method, getArgTypes());
				if (accessible != null) {
					memberObject = accessible;
					//refresh access
					indexASM = -1;
					ensureASM();
				} else if (!VMBridge.vm.tryToMakeAccessible(method)) {
					throw Context.throwAsScriptRuntimeEx(e);
				}
				// Retry after recovery
				return accessASM.invoke(target, indexASM, args);
			}
		} catch (Throwable ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
	}

	public Object newInstance(Object[] args) {
		val ctor = ctor();
		try {
			try {
				return ctor.newInstance(args);
			} catch (IllegalAccessException ex) {
				if (!VMBridge.vm.tryToMakeAccessible(ctor)) {
					throw Context.throwAsScriptRuntimeEx(ex);
				}
			}
			return ctor().newInstance(args);
		} catch (Throwable ex) {
			throw Context.throwAsScriptRuntimeEx(ex);
		}
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

