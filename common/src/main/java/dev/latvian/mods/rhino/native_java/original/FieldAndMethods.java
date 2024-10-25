package dev.latvian.mods.rhino.native_java.original;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public final class FieldAndMethods extends NativeJavaMethod {
    private static final long serialVersionUID = -9222428244284796755L;
    final Field field;
    Object javaObject;

    FieldAndMethods(Scriptable scope, MemberBox[] methods, Field field) {
        super(methods);
        this.field = field;
        setParentScope(scope);
        setPrototype(getFunctionPrototype(scope));
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == ScriptRuntime.FunctionClass) {
            return this;
        }
        Object rval;
        Class<?> type;
        try {
            rval = field.get(javaObject);
            type = field.getType();
        } catch (IllegalAccessException accEx) {
            throw Context.reportRuntimeError1("msg.java.internal.private", field.getName());
        }
        Context cx = Context.getContext();
        rval = cx.getWrapFactory().wrap(cx, this, rval, type);
        if (rval instanceof Scriptable) {
            rval = ((Scriptable) rval).getDefaultValue(hint);
        }
        return rval;
    }
}
