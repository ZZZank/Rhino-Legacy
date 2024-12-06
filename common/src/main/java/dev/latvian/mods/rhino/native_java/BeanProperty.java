package dev.latvian.mods.rhino.native_java;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public final class BeanProperty {
    public final MemberBox getter;
    public final MemberBox setter;
    public final NativeJavaMethod setters;
}
