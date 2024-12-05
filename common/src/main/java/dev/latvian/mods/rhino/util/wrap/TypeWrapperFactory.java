package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;

/**
 * @author LatvianModder
 */
@FunctionalInterface
public interface TypeWrapperFactory<T> {

	T wrap(Object o);

	default T wrap(Context cx, Object o, TypeInfo target) {
		return wrap(o);
	}

	interface New<T> extends TypeWrapperFactory<T> {
		@Override
		T wrap(Context cx, Object o, TypeInfo target);

		@Override
		@Deprecated
        default T wrap(Object o) {
			throw new IllegalStateException();
		}
	}
}
