/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

public class NativeJavaList extends NativeJavaObject implements Iterable<Object> {

	public final List<Object> list;
	public final TypeInfo listType;

	public NativeJavaList(Context cx, Scriptable scope, Object jo, List list, TypeInfo type) {
		super(cx, scope, jo, type);
		this.list = list;
		this.listType = type.param(0);
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
			return list.size();
		}
		return super.get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (isWithValidIndex(index)) {
			Context cx = Context.getContext();
			Object obj = list.get(index);
			return cx.getWrapFactory().wrap(cx, this, obj, this.listType);
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
			list.set(index, Context.jsToJava(Context.getContext(), value, TypeInfo.OBJECT));
			return;
		}
		super.put(index, start, value);
	}

	@Override
	public Object[] getIds() {
		val result = new Object[list.size()];
		int i = list.size();
		while (--i >= 0) {
			result[i] = i;
		}
		return result;
	}

	private boolean isWithValidIndex(int index) {
		return index >= 0 && index < list.size();
	}

	@Override
	public void delete(int index) {
		if (isWithValidIndex(index)) {
			Object obj = list.remove(index);
			Deletable.deleteObject(obj);
		}
	}

	@NotNull
	@Override
	public Iterator<Object> iterator() {
		return this.list.iterator();
	}
}
