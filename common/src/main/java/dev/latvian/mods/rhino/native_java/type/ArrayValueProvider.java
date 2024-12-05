package dev.latvian.mods.rhino.native_java.type;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.EvaluatorException;
import dev.latvian.mods.rhino.NativeArray;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.native_java.type.info.TypeInfo;
import lombok.val;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public interface ArrayValueProvider {
	ArrayValueProvider EMPTY = new ArrayValueProvider() {
		@Override
		public int getLength(Context cx) {
			return 0;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return null;
		}

		@Override
		public Object getErrorSource(Context cx) {
			return null;
		}
	};

	int getLength(Context cx);

	Object getArrayValue(Context cx, int index);

	Object getErrorSource(Context cx);

	default Object createArray(Context cx, TypeInfo target) {
		val len = getLength(cx);
		val arr = target.newArray(len);

		for (int i = 0; i < len; i++) {
			try {
				Array.set(arr, i, Context.jsToJava(cx, getArrayValue(cx, i), target));
			} catch (EvaluatorException ee) {
				return NativeJavaObject.reportConversionError(getErrorSource(cx), target);
			}
		}

		return arr;
	}

	default Object createList(Context cx, TypeInfo target) {
		val len = getLength(cx);

		if (len == 0) {
			return ImmutableList.of();
		} else if (len == 1) {
			try {
				return ImmutableList.of(Context.jsToJava(cx, getArrayValue(cx, 0), target));
			} catch (EvaluatorException ee) {
				return NativeJavaObject.reportConversionError(getErrorSource(cx), target);
			}
		}

		val list = new ArrayList<>(len);

		for (int i = 0; i < len; i++) {
			try {
				list.add(Context.jsToJava(cx, getArrayValue(cx, i), target));
			} catch (EvaluatorException ee) {
				return NativeJavaObject.reportConversionError(getErrorSource(cx), target);
			}
		}

		return list;
	}

	default Object createSet(Context cx, TypeInfo target) {
		int len = getLength(cx);

		if (len == 0) {
			return ImmutableSet.of();
		} else if (len == 1) {
			try {
				return ImmutableSet.of(Context.jsToJava(cx, getArrayValue(cx, 0), target));
			} catch (EvaluatorException ee) {
				return NativeJavaObject.reportConversionError(getErrorSource(cx), target);
			}
		}

		var set = new HashSet<>(len);

		for (int i = 0; i < len; i++) {
			try {
				set.add(Context.jsToJava(cx, getArrayValue(cx, i), target));
			} catch (EvaluatorException ee) {
				return NativeJavaObject.reportConversionError(getErrorSource(cx), target);
			}
		}

		return set;
	}

	@Desugar
	record FromObject(Object object) implements ArrayValueProvider {
		public static final FromObject FROM_NULL = new FromObject(null);

		@Override
		public int getLength(Context cx) {
			return 1;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return object;
		}

		@Override
		public Object getErrorSource(Context cx) {
			return object;
		}
	}

	static ArrayValueProvider fromNativeArray(NativeArray array) {
		return array.getLength() == 0 ? EMPTY : new FromNativeArray(array);
	}

	@Desugar
	record FromNativeArray(NativeArray array) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return (int) array.getLength();
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return array.get(index, array);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return array;
		}
	}

	@Desugar
	record FromJavaArray(Object array, int length) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return length;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return Array.get(array, index);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return array;
		}
	}

	@Desugar
	record FromPlainJavaArray(Object[] array) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return array.length;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return array[index];
		}

		@Override
		public Object getErrorSource(Context cx) {
			return array;
		}
	}

	static ArrayValueProvider fromJavaList(List<?> list, Object errorSource) {
		return list.isEmpty() ? EMPTY : new FromJavaList(list, errorSource);
	}

	@Desugar
	record FromJavaList(List<?> list, Object errorSource) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return list.size();
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return list.get(index);
		}

		@Override
		public Object getErrorSource(Context cx) {
			return errorSource;
		}
	}

	static ArrayValueProvider fromIterable(Iterable<?> iterable) {
		int len;

		if (iterable instanceof Collection<?> c) {
			len = c.size();
		} else {
			len = 0;

			for (var ignored : iterable) {
				len++;
			}
		}

		return len == 0 ? EMPTY : new FromIterator(len, iterable.iterator(), iterable);
	}

	@Desugar
	record FromIterator(int length, Iterator<?> iterator, Object errorSource) implements ArrayValueProvider {
		@Override
		public int getLength(Context cx) {
			return length;
		}

		@Override
		public Object getArrayValue(Context cx, int index) {
			return iterator.next();
		}

		@Override
		public Object getErrorSource(Context cx) {
			return errorSource;
		}
	}
}