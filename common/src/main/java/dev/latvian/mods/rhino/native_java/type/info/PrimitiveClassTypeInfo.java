package dev.latvian.mods.rhino.native_java.type.info;

import org.jetbrains.annotations.Nullable;

public class PrimitiveClassTypeInfo extends ClassTypeInfo {
	private final Object defaultValue;

	public PrimitiveClassTypeInfo(Class<?> type, @Nullable Object defaultValue) {
		super(type);
		this.defaultValue = defaultValue;
	}

	@Override
	public boolean isPrimitive() {
		return true;
	}

	@Override
	@Nullable
	public Object createDefaultValue() {
		return defaultValue;
	}
}
