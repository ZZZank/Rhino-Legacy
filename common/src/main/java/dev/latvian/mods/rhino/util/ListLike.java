package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaListLike;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import org.jetbrains.annotations.Nullable;

public interface ListLike<T> extends CustomJavaObjectWrapper.New {
	@Override
	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, TypeInfo target) {
		return new NativeJavaListLike(cx, scope, this, target);
	}

	@Nullable
	T getLL(int index);

	default void setLL(int index, T value) {
		throw new UnsupportedOperationException("Can't insert values in this list!");
	}

	int sizeLL();

	default void removeLL(int index) {
		throw new UnsupportedOperationException("Can't delete values from this list!");
	}

	default void clearLL() {
		throw new UnsupportedOperationException("Can't clear this list!");
	}
}
