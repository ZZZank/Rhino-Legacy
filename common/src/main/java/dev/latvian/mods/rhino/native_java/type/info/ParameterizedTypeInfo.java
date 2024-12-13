package dev.latvian.mods.rhino.native_java.type.info;

import lombok.val;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ParameterizedTypeInfo extends TypeInfoBase {
	private final TypeInfo rawType;
	private final TypeInfo[] params;
	private int hashCode;

	ParameterizedTypeInfo(TypeInfo rawType, TypeInfo[] params) {
		this.rawType = rawType;
		this.params = params;
	}

	@Override
	public Class<?> asClass() {
		return rawType.asClass();
	}

	@Override
	public boolean is(TypeInfo info) {
		return rawType.is(info);
	}

	@Override
	public TypeInfo param(int index) {
		return index >= 0 && index < params.length && params[index] != TypeInfo.OBJECT ? params[index] : TypeInfo.NONE;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = Objects.hash(rawType, Arrays.hashCode(params));

			if (hashCode == 0) {
				hashCode = 1;
			}
		}

		return hashCode;
	}

	@Override
	public boolean equals(Object object) {
		return this == object || object instanceof ParameterizedTypeInfo that && params.length == that.params.length && rawType.equals(that.rawType) && Arrays.deepEquals(params, that.params);
	}

	@Override
	public String toString() {
		return TypeStringContext.DEFAULT.toString(this);
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		ctx.append(sb, rawType);
		sb.append('<');

		for (int i = 0; i < params.length; i++) {
			if (i > 0) {
				sb.append(',');
				ctx.appendSpace(sb);
			}

			ctx.append(sb, params[i]);
		}

		sb.append('>');
	}

	@Override
	public String signature() {
		return rawType.signature();
	}

	public TypeInfo rawType() {
		return rawType;
	}

	public TypeInfo[] params() {
		return params;
	}

	@Override
	public Object newArray(int length) {
		return rawType.newArray(length);
	}

	@Override
	public TypeInfo withParams(TypeInfo... params) {
		return rawType.withParams(params);
	}

	@Override
	public boolean isFunctionalInterface() {
		return rawType.isFunctionalInterface();
	}

	@Override
	public List<Object> enumConstants() {
		return rawType.enumConstants();
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
		rawType.collectContainedComponentClasses(classes);

		for (var param : params) {
			param.collectContainedComponentClasses(classes);
		}
	}

	@Override
	public boolean isInterface() {
		return this.rawType.isInterface();
	}

	@Override
	public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
		TypeInfo[] params = null;
        for (int i = 0, len = this.params.length; i < len; i++) {
            val param = this.params[i];
            val consolidated = param.consolidate(mapping);
            if (param != consolidated) {
				if (params == null) {
					params = new TypeInfo[len];
					System.arraycopy(this.params, 0, params, 0, i);
				}
				params[i] = consolidated;
            } else if (params != null) {
				params[i] = consolidated;
			}
		}
		return params == null
			? this
			: new ParameterizedTypeInfo(rawType, params);
	}
}
