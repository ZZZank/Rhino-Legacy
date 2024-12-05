package dev.latvian.mods.rhino.util.wrap;

import lombok.AllArgsConstructor;

/**
 * @author LatvianModder
 */
@AllArgsConstructor
public class TypeWrapper<T> {
	public final Class<T> target;
	public final TypeWrapperValidator validator;
	public final TypeWrapperFactory<T> factory;
}
