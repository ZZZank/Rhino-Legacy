/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.util.Deletable;
import dev.latvian.mods.rhino.util.ListLike;

import java.util.List;

public class NativeJavaListLike extends NativeJavaObject {
	private final ListLike<Object> list;

	@SuppressWarnings("unchecked")
	public NativeJavaListLike(Scriptable scope, Object list) {
		super(scope, list, list.getClass());
		assert list instanceof ListLike;
		this.list = (ListLike<Object>) list;
	}

	@Override
	public String getClassName() {
		return "JavaList";
	}

	@Override
	public boolean has(String name, Scriptable start) {
		if (name.equals("length")) {
			return true;
		}
		return super.has(name, start);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public boolean has(Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return true;
		}
		return super.has(key, start);
	}

	@Override
	public Object get(String name, Scriptable start) {
		if ("length".equals(name)) {
			return list.sizeLL();
		}
		return super.get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			Context cx = Context.getContext();
			Object obj = list.getLL(index);
			return cx.getWrapFactory().wrap(cx, this, obj, obj.getClass());
		}
		return Undefined.instance;
	}

	@Override
	public Object get(Symbol key, Scriptable start) {
		if (SymbolKey.IS_CONCAT_SPREADABLE.equals(key)) {
			return Boolean.TRUE;
		}
		return super.get(key, start);
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		if (isWithValidIndex(index)) {
			list.setLL(index, Context.jsToJava(value, Object.class));
			return;
		}
		super.put(index, start, value);
	}

	@Override
	public Object[] getIds() {
		List<?> list = (List<?>) javaObject;
		Object[] result = new Object[list.size()];
		int i = list.size();
		while (--i >= 0) {
			result[i] = i;
		}
		return result;
	}

	private boolean isWithValidIndex(int index) {
		return index >= 0 && index < list.sizeLL();
	}

	@Override
	public void delete(int index) {
		if (isWithValidIndex(index)) {
			Object obj = list.getLL(index);
			list.removeLL(index);
			Deletable.deleteObject(obj);
		}
	}
}
