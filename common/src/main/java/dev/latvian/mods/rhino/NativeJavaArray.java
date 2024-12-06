/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.NativeJavaPackage;
import dev.latvian.mods.rhino.native_java.type.info.ArrayTypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Array;

/**
 * This class reflects Java arrays into the JavaScript environment.
 *
 * @author Mike Shaver
 * @see NativeJavaClass
 * @see NativeJavaObject
 * @see NativeJavaPackage
 */

public class NativeJavaArray extends NativeJavaObject implements SymbolScriptable {
	private static final long serialVersionUID = -924022554283675333L;

	Object array;
	int length;
	TypeInfo componentType;

	public NativeJavaArray(Context cx, Scriptable scope, Object array, ArrayTypeInfo type) {
		super(cx, scope, array, type);
		this.array = array;
		this.length = Array.getLength(array);
		this.componentType = type.componentType();
	}

	@Override
	public String getClassName() {
		return "JavaArray";
	}

	@Override
	public Object unwrap() {
		return array;
	}

	@Override
	public boolean has(String id, Scriptable start) {
		return id.equals("length") || super.has(id, start);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		return 0 <= index && index < length;
	}

	@Override
	public boolean has(Symbol key, Scriptable start) {
		return SymbolKey.IS_CONCAT_SPREADABLE.equals(key);
	}

	@Override
	public Object get(String id, Scriptable start) {
		if (id.equals("length")) {
			return length;
		}
		Object result = super.get(id, start);
		if (result == NOT_FOUND && !ScriptableObject.hasProperty(getPrototype(), id)) {
			throw Context.reportRuntimeError2("msg.java.member.not.found", array.getClass().getName(), id);
		}
		return result;
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (0 <= index && index < length) {
			val cx = Context.getContext();
			val obj = Array.get(array, index);
			return cx.getWrapFactory().wrap(cx, this, obj, componentType);
		}
		return Undefined.instance;
	}

	@Override
	public Object get(Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return Boolean.TRUE;
		}
		return NOT_FOUND;
	}

	@Override
	public void put(String id, Scriptable start, Object value) {
		// Ignore assignments to "length"--it's readonly.
		if (!id.equals("length")) {
			throw Context.reportRuntimeError1("msg.java.array.member.not.found", id);
		}
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
        if (0 > index || index >= length) {
            throw Context.reportRuntimeError2("msg.java.array.index.out.of.bounds", String.valueOf(index), String.valueOf(length - 1));
        }
        Array.set(array, index, Context.jsToJava(Context.getContext(), value, componentType));
    }

	@Override
	public void delete(Symbol key) {
		// All symbols are read-only
	}

	@Override
	public Object getDefaultValue(Class<?> hint) {
		if (hint == null || hint == ScriptRuntime.StringClass) {
			return array.toString();
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
	public Object[] getIds() {
		Object[] result = new Object[length];
		int i = length;
		while (--i >= 0) {
			result[i] = i;
		}
		return result;
	}

	@Override
	public boolean hasInstance(Scriptable value) {
        if (value instanceof Wrapper wrapper) {
            return componentType.asClass().isInstance(wrapper.unwrap());
        }
        return false;
    }

	@Override
	public Scriptable getPrototype() {
		if (prototype == null) {
			prototype = ScriptableObject.getArrayPrototype(this.getParentScope());
		}
		return prototype;
	}
}
