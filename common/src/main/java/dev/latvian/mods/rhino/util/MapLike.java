package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaMapLike;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

public interface MapLike<K, T> extends CustomJavaObjectWrapper.New {

	@Override
    default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, TypeInfo target) {
		return new NativeJavaMapLike(cx, scope, this, target);
	}

	@Nullable
	T getML(K key);

	default boolean containsKeyML(K key) {
		return getML(key) != null;
	}

	default void putML(K key, T value) {
		throw new UnsupportedOperationException("Can't insert values in this map!");
	}

	default Collection<K> keysML() {
		return Collections.emptySet();
	}

	default void removeML(K key) {
		throw new UnsupportedOperationException("Can't delete values from this map!");
	}

	default void clearML() {
		throw new UnsupportedOperationException("Can't clear this map!");
	}
}
