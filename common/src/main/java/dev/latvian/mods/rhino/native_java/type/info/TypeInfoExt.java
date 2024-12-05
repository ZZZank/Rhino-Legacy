package dev.latvian.mods.rhino.native_java.type.info;

import dev.latvian.mods.rhino.NativeIterator;
import dev.latvian.mods.rhino.Scriptable;

/**
 * @author ZZZank
 */
public interface TypeInfoExt {
    TypeInfo SCRIPTABLE = TypeInfo.of(Scriptable.class);
    TypeInfo WRAPPED_JAVA_ITERATOR = TypeInfo.of(NativeIterator.WrappedJavaIterator.class);
}
