package dev.latvian.mods.rhino.native_java.type.info.js;

import com.google.common.collect.ImmutableList;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeStringContext;

import java.util.Collection;
import java.util.List;

// {a: string, b?: number}
public record JSObjectTypeInfo(List<JSOptionalParam> fields) implements TypeInfo {
	public static JSObjectTypeInfo of(JSOptionalParam field) {
		return new JSObjectTypeInfo(ImmutableList.of(field));
	}

	public static JSObjectTypeInfo of(JSOptionalParam field1, JSOptionalParam field2) {
		return new JSObjectTypeInfo(ImmutableList.of(field1, field2));
	}

	public static JSObjectTypeInfo of(JSOptionalParam... fields) {
		return new JSObjectTypeInfo(ImmutableList.copyOf(fields));
	}

	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append('{');

		boolean first = true;

		for (var field : fields) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
				ctx.appendSpace(sb);
			}

			field.append(ctx, sb);
		}

		sb.append('}');
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		for (var field : fields) {
			field.type().collectContainedComponentClasses(classes);
		}
	}
}
