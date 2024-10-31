package dev.latvian.mods.rhino.util.remapper;


import lombok.val;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public interface Remapper {

	/**
	 * used as the return value of Remapper when the Remapper does not remap the input
	 */
	String NOT_REMAPPED = "";

	/**
	 * @return a string holding remapped class name, or an empty string if not remapped
	 */
	default String remapClass(Class<?> from) {
		return NOT_REMAPPED;
	}

	/**
	 * @param from name of remapped class
	 * @return the original class name
	 */
	default String unmapClass(String from) {
		return NOT_REMAPPED;
	}

	/**
	 * @return a string holding remapped field name, or an empty string if not remapped
	 */
	default String remapField(Class<?> from, Field field) {
		return NOT_REMAPPED;
	}

	/**
	 * @return a string holding remapped method name, or an empty string if not remapped
	 */
	default String remapMethod(Class<?> from, Method method) {
		return NOT_REMAPPED;
	}

	/**
	 * @return a string holding remapped class name, or {@link Class#getName()} if not remapped
	 */
	default String remapClassSafe(Class<?> from) {
		val remapped = remapClass(from);
		return remapped.isEmpty() ? from.getName() : remapped;
	}

	/**
	 * @param from name of remapped class
	 * @return the original class name, {@code from} itself if no mapping associated to it
	 */
	default String unmapClassSafe(String from) {
		val remapped = unmapClass(from);
		return remapped.isEmpty() ? from : remapped;
	}

	/**
	 * @return a string holding remapped field name, or {@link Field#getName()} if not remapped
	 */
	default String remapFieldSafe(Class<?> from, Field field) {
		val remapped = remapField(from, field);
		return remapped.isEmpty() ? field.getName() : remapped;
	}

	/**
	 * @return a string holding remapped method name, or {@link Method#getName()} if not remapped
	 */
	default String remapMethodSafe(Class<?> from, Method method) {
		val remapped = remapMethod(from, method);
		return remapped.isEmpty() ? method.getName() : remapped;
	}
}

