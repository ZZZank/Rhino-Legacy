package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ScriptRuntime;
import dev.latvian.mods.rhino.Scriptable;
import lombok.Getter;
import lombok.val;

@Getter
public final class FieldAndMethods extends NativeJavaMethod {
    final NativeJavaField field;
    Object javaObject;

    FieldAndMethods(Scriptable scope, NativeJavaMethod methods, NativeJavaField field) {
        super(methods.methods, methods.functionName);
        this.field = field;
        setParentScope(scope);
        setPrototype(getFunctionPrototype(scope));
    }

    @Override
    public Object getDefaultValue(Class<?> hint) {
        if (hint == ScriptRuntime.FunctionClass) {
            return this;
        }
        Object rval = field.get(javaObject);
        val type = field.type;
        val cx = Context.getContext();
        rval = cx.getWrapFactory().wrap(cx, this, rval, type);
        if (rval instanceof Scriptable) {
            rval = ((Scriptable) rval).getDefaultValue(hint);
        }
        return rval;
    }
}
