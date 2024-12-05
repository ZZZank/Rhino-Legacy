package dev.latvian.mods.rhino.native_java.type.info.js;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import dev.latvian.mods.rhino.native_java.type.info.TypeStringContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// string | number
@Desugar
public record JSOrTypeInfo(List<TypeInfo> types) implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return TypeInfo.class;
	}

	@Override
	public TypeInfo or(TypeInfo info) {
		if (info instanceof JSOrTypeInfo or) {
			var list = new ArrayList<TypeInfo>(types.size() + or.types.size());
			list.addAll(types);
			list.addAll(or.types);
			return new JSOrTypeInfo(ImmutableList.copyOf(list));
		} else {
			var list = new ArrayList<TypeInfo>(types.size() + 1);
			list.addAll(types);
			list.add(info);
			return new JSOrTypeInfo(ImmutableList.copyOf(list));
		}
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		for (int i = 0; i < types.size(); i++) {
			if (i != 0) {
				ctx.appendSpace(sb);
				sb.append('|');
				ctx.appendSpace(sb);
			}

			ctx.append(sb, types.get(i));
		}
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		for (var type : types) {
			type.collectContainedComponentClasses(classes);
		}
	}
}
