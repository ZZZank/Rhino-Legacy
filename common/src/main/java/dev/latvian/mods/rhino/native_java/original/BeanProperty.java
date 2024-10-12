package dev.latvian.mods.rhino.native_java.original;

public class BeanProperty {
    public final MemberBox getter;
    public final MemberBox setter;
    public final NativeJavaMethod setters;

    public BeanProperty(MemberBox getter, MemberBox setter, NativeJavaMethod setters) {
        this.getter = getter;
        this.setter = setter;
        this.setters = setters;
    }
}
