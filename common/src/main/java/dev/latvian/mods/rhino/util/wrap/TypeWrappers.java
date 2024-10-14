package dev.latvian.mods.rhino.util.wrap;

import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
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

	@SuppressWarnings("unchecked")
	public <T> void register(Class<T> target, Predicate<Object> validator, TypeWrapperFactory<T> factory) {
		if (target == null || target == Object.class) {
			throw new IllegalArgumentException("target can't be Object.class!");
		} else if (target.isArray()) {
			throw new IllegalArgumentException("target can't be an array!");
		} else if (wrappers.containsKey(target)) {
			throw new IllegalArgumentException("Wrapper for class " + target.getName() + " already exists!");
		}

		TypeWrapper<T> typeWrapper0 = new TypeWrapper<>(target, validator, factory);
		wrappers.put(target, typeWrapper0);

		// I know this looks like cancer, but it's actually pretty simple - grab T[].class, register ArrayTypeWrapperFactory
		// You may say that it would be better to just implement N-sized array checking directly in java parser, but this is way more efficient

		// 1D
		val arr1D = (Class<T[]>) Array.newInstance(target, 0).getClass();
		val wrapper1D = new TypeWrapper<>(arr1D, validator, new ArrayTypeWrapperFactory<>(typeWrapper0, target, arr1D));
		wrappers.put(arr1D, wrapper1D);
		// 2D
		val arr2D = (Class<T[][]>) Array.newInstance(arr1D, 0).getClass();
		val wrapper2D = new TypeWrapper<>(arr2D, validator, new ArrayTypeWrapperFactory<>(wrapper1D, arr1D, arr2D));
		wrappers.put(arr2D, wrapper2D);
		// 3D
		val arr3D = (Class<T[][][]>) Array.newInstance(arr2D, 0).getClass();
		val wrapper3D = new TypeWrapper<>(arr3D, validator, new ArrayTypeWrapperFactory<>(wrapper2D, arr2D, arr3D));
		wrappers.put(arr3D, wrapper3D);
		// 4D... yeah no. 3D already is an overkill
	}

	public <T> void register(Class<T> target, TypeWrapperFactory<T> factory) {
		register(target, TypeWrapper.ALWAYS_VALID, factory);
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
