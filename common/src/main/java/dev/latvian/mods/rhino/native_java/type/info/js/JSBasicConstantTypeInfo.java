package dev.latvian.mods.rhino.native_java.type.info.js;

import com.github.bsideup.jabel.Desugar;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeStringContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

// null, undefined, true, false
@Desugar
public record JSBasicConstantTypeInfo(String value) implements TypeInfo {
	public static final JSBasicConstantTypeInfo NULL = new JSBasicConstantTypeInfo("null");
	public static final JSBasicConstantTypeInfo UNDEFINED = new JSBasicConstantTypeInfo("undefined");
	public static final JSBasicConstantTypeInfo TRUE = new JSBasicConstantTypeInfo("true");
	public static final JSBasicConstantTypeInfo FALSE = new JSBasicConstantTypeInfo("false");

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return value;
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append(value);
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return Collections.emptySet();
	}
}
