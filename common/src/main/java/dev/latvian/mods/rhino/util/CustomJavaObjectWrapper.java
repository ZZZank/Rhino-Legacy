package dev.latvian.mods.rhino.util;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;

@FunctionalInterface
public interface CustomJavaObjectWrapper {
	Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType);

	default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, TypeInfo target) {
		return wrapAsJavaObject(cx, scope, target.asClass());
	}

	interface New extends CustomJavaObjectWrapper {
		@Override
		Scriptable wrapAsJavaObject(Context cx, Scriptable scope, TypeInfo target);

		@Override
		default Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Class<?> staticType) {
			return wrapAsJavaObject(cx, scope, TypeInfo.of(staticType));
		}
	}
}
