package dev.latvian.mods.rhino.native_java.type.info.js;

import com.google.common.collect.ImmutableSet;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeStringContext;

import java.util.Collection;
import java.util.Set;

// "abc"
public record JSStringConstantTypeInfo(String constant) implements TypeInfo {
	public static final JSStringConstantTypeInfo EMPTY = new JSStringConstantTypeInfo("");

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return ScriptRuntime.escapeAndWrapString(constant);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(ScriptRuntime.escapeAndWrapString(constant));
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return ImmutableSet.of();
	}
}
