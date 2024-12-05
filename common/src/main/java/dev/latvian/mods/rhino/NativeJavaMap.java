/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package dev.latvian.mods.rhino;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.util.Deletable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NativeJavaMap extends NativeJavaObject {

	public final Map<Object, Object> map;
	public final TypeInfo mapKeyType;
	public final TypeInfo mapValueType;

	public NativeJavaMap(Context cx, Scriptable scope, Map map, TypeInfo type) {
		super(cx, scope, map, type);
		this.map = map;
		this.mapKeyType = type.param(0);
		this.mapValueType = type.param(1);
	}

	@Override
	public String getClassName() {
		return "JavaMap";
	}

	@Override
	public boolean has(String name, Scriptable start) {
		if (map.containsKey(name)) {
			return true;
		}
		return super.has(name, start);
	}

	@Override
	public boolean has(int index, Scriptable start) {
		if (map.containsKey(index)) {
			return true;
		}
		return super.has(index, start);
	}

	@Override
	public Object get(String name, Scriptable start) {
		if (map.containsKey(name)) {
			Context cx = Context.getContext();
			Object obj = map.get(name);
			return cx.getWrapFactory().wrap(cx, this, obj, this.mapValueType);
		}
		return super.get(name, start);
	}

	@Override
	public Object get(int index, Scriptable start) {
		if (map.containsKey(index)) {
			Context cx = Context.getContext();
			Object obj = map.get(index);
			return cx.getWrapFactory().wrap(cx, this, obj, this.mapValueType);
		}
		return super.get(index, start);
	}

	@Override
	public void put(String name, Scriptable start, Object value) {
		map.put(name, Context.jsToJava(Context.getCurrentContext(), value, this.mapValueType));
	}

	@Override
	public void put(int index, Scriptable start, Object value) {
		map.put(index, Context.jsToJava(Context.getCurrentContext(), value, this.mapValueType));
	}

	@Override
	public Object[] getIds() {
		List<Object> ids = new ArrayList<>(map.size());
		for (Object key : map.keySet()) {
			if (key instanceof Integer) {
				ids.add(key);
			} else {
				ids.add(ScriptRuntime.toString(key));
			}
		}
		return ids.toArray();
	}

	@Override
	public void delete(String name) {
		Deletable.deleteObject(map.remove(name));
	}

	@Override
	public void delete(int index) {
		Deletable.deleteObject(map.remove(index));
	}
}
