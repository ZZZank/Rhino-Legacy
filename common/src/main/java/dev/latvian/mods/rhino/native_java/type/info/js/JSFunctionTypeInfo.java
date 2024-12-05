package dev.latvian.mods.rhino.native_java.type.info.js;

import com.github.bsideup.jabel.Desugar;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeStringContext;

import java.util.Collection;
import java.util.List;

// (a: string) => void
@Desugar
public record JSFunctionTypeInfo(List<JSOptionalParam> params, TypeInfo returnType) implements TypeInfo {
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
		sb.append('(');

		boolean first = true;

		for (var param : params) {
			if (first) {
				first = false;
			} else {
				sb.append(',');
				ctx.appendSpace(sb);
			}

			param.append(ctx, sb);
		}

		sb.append(')');
		ctx.appendSpace(sb);
		sb.append('=');
		sb.append('>');
		ctx.appendSpace(sb);
		ctx.append(sb, returnType);
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		for (var param : params) {
			param.type().collectContainedComponentClasses(classes);
		}

		returnType.collectContainedComponentClasses(classes);
	}
}
