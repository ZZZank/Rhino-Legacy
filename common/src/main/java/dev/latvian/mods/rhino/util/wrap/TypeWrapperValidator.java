package dev.latvian.mods.rhino.util.wrap;


import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;

import java.util.function.Predicate;

@FunctionalInterface
public interface TypeWrapperValidator extends Predicate<Object> {
	TypeWrapperValidator.New ALWAYS_VALID = (from, target) -> true;

	default boolean test(Object from, TypeInfo target) {
		return test(from);
	}

	@FunctionalInterface
	interface New extends TypeWrapperValidator {
		@Override
        default boolean test(Object o) {
			throw new IllegalStateException();
		}

		@Override
		boolean test(Object from, TypeInfo target);
	}
}