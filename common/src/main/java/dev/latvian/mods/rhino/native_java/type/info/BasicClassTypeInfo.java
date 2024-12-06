package dev.latvian.mods.rhino.native_java.type.info;

import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;

public class BasicClassTypeInfo extends ClassTypeInfo {
	static final Map<Class<?>, BasicClassTypeInfo> CACHE = new IdentityHashMap<>();

	static @NotNull BasicClassTypeInfo of(Class<?> c) {
		var got = CACHE.get(c);
		if (got == null) {
			synchronized (CACHE) {
				got = new BasicClassTypeInfo(c);
				CACHE.put(c, got);
			}
		}
		return got;
	}

	BasicClassTypeInfo(Class<?> type) {
		super(type);
	}
}
