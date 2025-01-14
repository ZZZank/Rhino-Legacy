package dev.latvian.mods.rhino.native_java.type.info;

import java.lang.reflect.Array;

public abstract class TypeInfoBase implements TypeInfo {
	private TypeInfo asArray;
	private Object emptyArray;

	@Override
	public TypeInfo asArray() {
		if (asArray == null) {
			asArray = new ArrayTypeInfo(this);
		}

		return asArray;
	}

	@Override
	public Object newArray(int length) {
		if (length == 0) {
			if (emptyArray == null) {
				emptyArray = Array.newInstance(asClass(), 0);
			}

			return emptyArray;
		}

		return Array.newInstance(asClass(), length);
	}
}
