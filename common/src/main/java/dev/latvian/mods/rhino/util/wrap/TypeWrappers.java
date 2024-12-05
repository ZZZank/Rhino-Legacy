package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class TypeWrappers {
	private final Map<Class<?>, TypeWrapper<?>> wrappers = new LinkedHashMap<>();

	public void removeAll() {
		wrappers.clear();
	}

	@Deprecated
	public <F, T> void register(String id, Class<F> from, Class<T> to, Function<F, T> factory) {
		// Keep old one for now so that it doesn't crash
	}

	public <T> void registerNew(Class<T> target, TypeWrapperValidator validator, TypeWrapperFactory<T> factory) {
		if (target == null || target == Object.class) {
			throw new IllegalArgumentException("target can't be Object.class!");
		} else if (target.isArray()) {
			throw new IllegalArgumentException("target can't be an array!");
		} else if (wrappers.containsKey(target)) {
			throw new IllegalArgumentException("Wrapper for class " + target.getName() + " already exists!");
		}
        wrappers.put(target, new TypeWrapper<>(target, validator, factory));
	}

	@SuppressWarnings("unchecked")
	@Deprecated
	public <T> void register(Class<T> target, Predicate<Object> validator, TypeWrapperFactory<T> factory) {
		if (target == null || target == Object.class) {
			throw new IllegalArgumentException("target can't be Object.class!");
		} else if (target.isArray()) {
			throw new IllegalArgumentException("target can't be an array!");
		} else if (wrappers.containsKey(target)) {
			throw new IllegalArgumentException("Wrapper for class " + target.getName() + " already exists!");
		}

        wrappers.put(target, new TypeWrapper<>(target, validator::test, factory));
	}

	public <T> void register(Class<T> target, TypeWrapperFactory<T> factory) {
		registerNew(target, TypeWrapperValidator.ALWAYS_VALID, factory);
	}

	@Nullable
	public TypeWrapperFactory<?> getWrapperFactory(@Nullable Object from, TypeInfo target) {
		if (target == TypeInfo.OBJECT) {
			return null;
		}

		val cl = target.asClass();
		val wrapper = wrappers.get(cl);

        if (wrapper == null || !wrapper.validator.test(from, target)) {
            return null;
        }
        return wrapper.factory;
    }

	@Nullable
	public TypeWrapperFactory<?> getWrapperFactory(Class<?> target, @Nullable Object from) {
		if (target == Object.class) {
			return null;
		}

		val wrapper = wrappers.get(target);

		if (wrapper != null && wrapper.validator.test(from)) {//explicit wrapper
			return wrapper.factory;
		} else if (target.isEnum()) {//enum wrapper
			return EnumTypeWrapper.get((Class<Enum>) target);
		}
		//else if (from != null && target.isArray() && !from.getClass().isArray() && target.getComponentType() == from.getClass() && !target.isPrimitive())
		//{
		//	return TypeWrapperFactory.OBJECT_TO_ARRAY;
		//}

		return null;
	}
}
