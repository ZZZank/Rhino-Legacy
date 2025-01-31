package dev.latvian.mods.rhino.native_java.type.info;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;

final class NoTypeInfo implements TypeInfo {
	@Override
	public Class<?> asClass() {
		return Object.class;
	}

	@Override
	public boolean shouldConvert() {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "?";
	}

	@Override
	public void append(TypeStringContext ctx, StringBuilder sb) {
		sb.append('?');
	}

	@Override
	public TypeInfo asArray() {
		return this;
	}

	@Override
	public TypeInfo withParams(TypeInfo... params) {
		return this;
	}

	@Override
	public void collectContainedComponentClasses(Collection<Class<?>> classes) {
	}

	@Override
	public Set<Class<?>> getContainedComponentClasses() {
		return ImmutableSet.of();
	}
}
