package dev.latvian.mods.rhino.util.wrap;

import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author LatvianModder
 */
public class TypeWrappers {
	private final Map<Class<?>, TypeWrapper<?>> wrappers = new Reference2ObjectOpenHashMap<>();

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

	public boolean hasWrapper(Object from, TypeInfo target) {
		if (target instanceof TypeWrapperFactory<?>) {
			return true;
		}

		val wrapper = wrappers.get(target.asClass());
		return wrapper != null && wrapper.validator.test(from, target);
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
	@Deprecated
	public TypeWrapperFactory<?> getWrapperFactory(Class<?> target, @Nullable Object from) {
		return getWrapperFactory(from, TypeInfo.of(target));
	}
}
