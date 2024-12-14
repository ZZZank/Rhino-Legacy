/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.VMBridge;
import dev.latvian.mods.rhino.native_java.type.TypeConsolidator;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.Getter;
import lombok.Setter;
import lombok.val;

import java.io.Serializable;
import java.lang.reflect.*;

/**
 * Wrapper class for Method and Constructor instances to cache
 * getParameterTypes() results, recover from IllegalAccessException
 * in some cases and provide serialization support.
 *
 * @author Igor Bukanov
 */
public final class MemberBox implements Serializable {

	private final Class<?> from;
    private transient Executable memberObject;
	public transient final Class<?>[] argTypes;
	private TypeInfo[] argTypeInfos = null;
	private TypeInfo returnTypeInfo;
	@Setter
	@Getter
	private transient Object delegateTo;
	public transient final boolean vararg;

	public MemberBox(Method method) {
		this(method, method.getDeclaringClass());
	}

	public MemberBox(Method method, Class<?> from) {
		this.memberObject = method;
		this.from = from;
		this.argTypes = method.getParameterTypes();
		this.vararg = method.isVarArgs();
	}

    public MemberBox(Constructor<?> constructor) {
		this(constructor, constructor.getDeclaringClass());//limited type consolidating
	}

    public MemberBox(Constructor<?> constructor, Class<?> from) {
		this.memberObject = constructor;
		this.from = from;
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

	public String liveConnectSignature() {
		return ReflectsKit.liveConnectSignature(this.argTypes);
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
		sb.append(liveConnectSignature());
		return sb.toString();
	}

	@Override
	public String toString() {
		return memberObject.toString();
	}

	public Object invoke(Object instance, Object... args) {
		val method = method();
		try {
			try {
				return method.invoke(instance, args);
			} catch (IllegalAccessException e) {
				val accessible = searchAccessibleMethod(method, argTypes);
				if (accessible != null) {
					memberObject = accessible;
				} else if (!VMBridge.vm.tryToMakeAccessible(method)) {
					throw Context.throwAsScriptRuntimeEx(e);
				}
				// Retry after recovery
				return method().invoke(instance, args);
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

	public TypeInfo[] getArgTypeInfos() {
		if (this.argTypeInfos == null) {
			argTypeInfos = TypeInfo.ofArray(memberObject.getGenericParameterTypes());
			if (from != null) {
				argTypeInfos = TypeConsolidator.consolidateAll(argTypeInfos, TypeConsolidator.getMapping(from));
			}
		}
		return this.argTypeInfos;
	}

	public TypeInfo getReturnTypeInfo() {
		if (returnTypeInfo == null && isMethod()) {
			returnTypeInfo = TypeInfo.of(method().getReturnType());
			if (from != null) {
				returnTypeInfo = returnTypeInfo.consolidate(TypeConsolidator.getMapping(from));
			}
		}
		return returnTypeInfo;
	}
}

