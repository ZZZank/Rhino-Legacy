/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.native_java.original;

import dev.latvian.mods.rhino.*;
import dev.latvian.mods.rhino.native_java.ReflectsKit;
import dev.latvian.mods.rhino.util.HideFromJS;
import lombok.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * @author Mike Shaver
 * @author Norris Boyd
 * @see NativeJavaObject
 * @see NativeJavaClass
 */
public class JavaMembers {

    public static class FieldInfo {
        public final Field field;
        public String name = "";

        public FieldInfo(Field f) {
            field = f;
        }
    }

    public static class MethodInfo {
        public Method method;
        public String name = "";
        public boolean hidden = false;

        public MethodInfo(Method m) {
            method = m;
        }
    }

    /**
     * @deprecated use {@link ReflectsKit#javaSignature(Class)} instead
     */
    public static String javaSignature(@NotNull Class<?> type) {
        return ReflectsKit.javaSignature(type);
    }

    /**
     * @deprecated use {@link ReflectsKit#liveConnectSignature(Class[])} instead
     */
    public static String liveConnectSignature(Class<?>[] argTypes) {
        return ReflectsKit.liveConnectSignature(argTypes);
    }

    private static MemberBox findGetter(boolean isStatic, Map<String, Object> ht, String prefix, String propertyName) {
        val getterName = prefix.concat(propertyName);
        return ht.get(getterName) instanceof NativeJavaMethod njmGet
            ? extractGetMethod(njmGet.methods, isStatic)
            : null;
    }

    private static MemberBox extractGetMethod(MemberBox[] methods, boolean isStatic) {
        // Inspect the list of all MemberBox for the only one having no
        // parameters
        for (val method : methods) {
            // Does getter method have an empty parameter list with a return
            // value (eg. a getSomething() or isSomething())?
            if (method.getArgTypes().length == 0 && (!isStatic || method.isStatic())) {
                val type = method.method().getReturnType();
                if (type != Void.TYPE) {
                    return method;
                }
                break;
            }
        }
        return null;
    }

    private static MemberBox extractSetMethod(Class<?> type, MemberBox[] methods, boolean isStatic) {
        //
        // Note: it may be preferable to allow NativeJavaMethod.findFunction()
        //       to find the appropriate setter; unfortunately, it requires an
        //       instance of the target arg to determine that.
        //

        // Make two passes: one to find a method with direct type assignment,
        // and one to find a widening conversion.
        for (int pass = 1; pass <= 2; ++pass) {
            for (MemberBox method : methods) {
                if (isStatic && !method.isStatic()) {
                    continue;
                }
                Class<?>[] params = method.getArgTypes();
                if (params.length != 1) {
                    continue;
                }
                if (pass == 1) {
                    if (params[0] == type) {
                        return method;
                    }
                } else {
                    if (params[0].isAssignableFrom(type)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private static MemberBox extractSetMethod(MemberBox[] methods, boolean isStatic) {
        for (val method : methods) {
            if ((!isStatic || method.isStatic())
                && method.method().getReturnType() == Void.TYPE
                && method.getArgTypes().length == 1
            ) {
                return method;
            }
        }
        return null;
    }

    public static JavaMembers lookupClass(
        Context cx,
        Scriptable scope,
        Class<?> dynamicType,
        Class<?> staticType,
        boolean includeProtected
    ) {
        val cache = cx.classTable;

        Class<?> c = dynamicType;
        JavaMembers members;
        while (true) {
            members = cache.get(c);
            if (members != null) {
                if (c != dynamicType) {
                    // member lookup for the original class failed because of missing privileges, cache the result so we don't try again
                    cache.put(dynamicType, members);
                }
                return members;
            }
            try {
                members = new JavaMembers(c, includeProtected, cx, scope);
                break;
            } catch (SecurityException e) {
                // Reflection may fail for objects that are in a restricted
                // access package (e.g. sun.*).  If we get a security
                // exception, try again with the static type if it is interface.
                // Otherwise, try superclass
                if (staticType != null && staticType.isInterface()) {
                    c = staticType;
                    staticType = null; // try staticType only once
                } else {
                    Class<?> parent = c.getSuperclass();
                    if (parent == null) {
                        if (c.isInterface()) {
                            // last resort after failed staticType interface
                            parent = ScriptRuntime.ObjectClass;
                        } else {
                            throw e;
                        }
                    }
                    c = parent;
                }
            }
        }

        cache.put(c, members);
        if (c != dynamicType) {
            // member lookup for the original class failed because of missing privileges, cache the result, so we don't try again
            cache.put(dynamicType, members);
        }
        return members;
    }

    public static JavaMembers lookupClass(
        Scriptable scope,
        Class<?> dynamicType,
        Class<?> staticType,
        boolean includeProtected
    ) {
        return lookupClass(Context.getContext(), scope, dynamicType, staticType, includeProtected);
    }

    public final Context localContext;
    private final Class<?> clazz;
    private final Map<String, Object> members = new HashMap<>();
    private final Map<String, Object> staticMembers = new HashMap<>();
    public NativeJavaMethod ctors; // we use NativeJavaMethod for ctor overload resolution
    private final Map<String, FieldAndMethods> fieldAndMethods = new HashMap<>();
    private final Map<String, FieldAndMethods> staticFieldAndMethods = new HashMap<>();

    JavaMembers(Class<?> clazz, boolean includeProtected, Context cx, Scriptable scope) {
        this.localContext = cx;
        this.clazz = clazz;

        val shutter = cx.getClassShutter();
        if (shutter != null && !shutter.visibleToScripts(clazz.getName(), ClassShutter.TYPE_MEMBER)) {
            throw Context.reportRuntimeError1("msg.access.prohibited", clazz.getName());
        }

        reflect(cx, scope, includeProtected);
    }

    public boolean has(String name, boolean isStatic) {
        val ht = isStatic ? staticMembers : members;
        val obj = ht.get(name);
        if (obj != null) {
            return true;
        }
        return findExplicitFunction(name, isStatic) != null;
    }

    public Object get(Scriptable scope, String name, Object javaObject, boolean isStatic) {
        val cx = localContext;
        val ht = isStatic ? staticMembers : members;
        var member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
        if (member == null) {
            member = this.getExplicitFunction(scope, name, javaObject, isStatic);
            if (member == null) {
                return Scriptable.NOT_FOUND;
            }
        }
        if (member instanceof Scriptable) {
            return member;
        }
        Object rval;
        Class<?> type;
        try {
            if (member instanceof BeanProperty bp) {
                if (bp.getter == null) {
                    return Scriptable.NOT_FOUND;
                }
                rval = bp.getter.invoke(javaObject, ScriptRuntime.EMPTY_OBJECTS);
                type = bp.getter.method().getReturnType();
            } else {
                Field field = (Field) member;
                rval = field.get(isStatic ? null : javaObject);
                type = field.getType();
            }
        } catch (Exception ex) {
            throw Context.throwAsScriptRuntimeEx(ex);
        }
        // Need to wrap the object before we return it.
        scope = ScriptableObject.getTopLevelScope(scope);
        return cx.getWrapFactory().wrap(cx, scope, rval, type);
    }

    public void put(Scriptable scope, String name, Object javaObject, Object value, boolean isStatic) {
        val ht = isStatic ? staticMembers : members;
        Object member = ht.get(name);
        if (!isStatic && member == null) {
            // Try to get static member from instance (LC3)
            member = staticMembers.get(name);
        }
        if (member == null) {
            throw reportMemberNotFound(name);
        }
        if (member instanceof FieldAndMethods) {
            FieldAndMethods fam = (FieldAndMethods) ht.get(name);
            member = fam.field;
        }

        // Is this a bean property "set"?
        if (member instanceof BeanProperty bp) {
            if (bp.setter == null) {
                throw reportMemberNotFound(name);
            }
            // If there's only one setter or if the value is null, use the
            // main setter. Otherwise, let the NativeJavaMethod decide which
            // setter to use:
            if (bp.setters == null || value == null) {
                Class<?> setType = bp.setter.getArgTypes()[0];
                Object[] args = {Context.jsToJava(value, setType)};
                try {
                    bp.setter.invoke(javaObject, args);
                } catch (Exception ex) {
                    throw Context.throwAsScriptRuntimeEx(ex);
                }
            } else {
                Object[] args = {value};
                localContext.callSync(bp.setters, ScriptableObject.getTopLevelScope(scope), scope, args);
            }
        } else {
            if (!(member instanceof Field field)) {
                throw Context.reportRuntimeError1(
                    (member == null) ? "msg.java.internal.private" : "msg.java.method.assign",
                    name
                );
            }
            int fieldModifiers = field.getModifiers();

            if (Modifier.isFinal(fieldModifiers)) {
                // treat Java final the same as JavaScript [[READONLY]]
                throw Context.throwAsScriptRuntimeEx(new IllegalAccessException("Can't modify final field "
                    + field.getName()));
            }

            Object javaValue = Context.jsToJava(value, field.getType());
            try {
                field.set(javaObject, javaValue);
            } catch (IllegalAccessException accessEx) {
                throw Context.throwAsScriptRuntimeEx(accessEx);
            } catch (IllegalArgumentException argEx) {
                throw Context.reportRuntimeError3("msg.java.internal.field.type",
                    value.getClass().getName(),
                    field,
                    javaObject.getClass().getName()
                );
            }
        }
    }

    public Object[] getIds(boolean isStatic) {
        val map = isStatic ? staticMembers : members;
        return map.keySet().toArray(ScriptRuntime.EMPTY_OBJECTS);
    }

    private MemberBox findExplicitFunction(String name, boolean isStatic) {
        int sigStart = name.indexOf('(');
        if (sigStart < 0) {
            return null;
        }

        val ht = isStatic ? staticMembers : members;
        MemberBox[] methodsOrCtors = null;
        val isCtor = (isStatic && sigStart == 0);

        if (isCtor) {
            // Explicit request for an overloaded constructor
            methodsOrCtors = ctors.methods;
        } else {
            // Explicit request for an overloaded method
            val trueName = name.substring(0, sigStart);
            Object obj = ht.get(trueName);
            if (!isStatic && obj == null) {
                // Try to get static member from instance (LC3)
                obj = staticMembers.get(trueName);
            }
            if (obj instanceof NativeJavaMethod njm) {
                methodsOrCtors = njm.methods;
            }
        }

        if (methodsOrCtors != null) {
            for (MemberBox methodsOrCtor : methodsOrCtors) {
                Class<?>[] type = methodsOrCtor.getArgTypes();
                String sig = ReflectsKit.liveConnectSignature(type);
                if (sigStart + sig.length() == name.length() && name.regionMatches(sigStart, sig, 0, sig.length())) {
                    return methodsOrCtor;
                }
            }
        }

        return null;
    }

    private Object getExplicitFunction(Scriptable scope, String name, Object javaObject, boolean isStatic) {
        val ht = isStatic ? staticMembers : members;
        Object member = null;
        MemberBox methodOrCtor = findExplicitFunction(name, isStatic);

        if (methodOrCtor != null) {
            Scriptable prototype = ScriptableObject.getFunctionPrototype(scope);

            if (methodOrCtor.isCtor()) {
                val fun = new NativeJavaConstructor(methodOrCtor);
                fun.setPrototype(prototype);
                member = fun;
                ht.put(name, fun);
            } else {
                val trueName = methodOrCtor.getName();
                member = ht.get(trueName);

                if (member instanceof NativeJavaMethod njm && njm.methods.length > 1) {
                    val fun = new NativeJavaMethod(methodOrCtor, name);
                    fun.setPrototype(prototype);
                    ht.put(name, fun);
                    member = fun;
                }
            }
        }

        return member;
    }

    private void reflect(Context cx, Scriptable scope, boolean includeProtected) {
        if (clazz.isAnnotationPresent(HideFromJS.class)) {
            ctors = new NativeJavaMethod(new MemberBox[0], clazz.getSimpleName());
            return;
        }

        // We reflect methods first, because we want overloaded field/method
        // names to be allocated to the NativeJavaMethod before the field
        // gets in the way.

        for (val methodInfo : getAccessibleMethods(cx, includeProtected)) {
            val method = methodInfo.method;
            val mods = method.getModifiers();
            val isStatic = Modifier.isStatic(mods);
            val ht = isStatic ? staticMembers : members;
            val name = methodInfo.name;

            val value = ht.get(name);
            if (value == null) {
                ht.put(name, method);
            } else {
                List<Method> overloadedMethods;
                if (value instanceof List objArray) {
                    overloadedMethods = objArray;
                } else if (value instanceof Method m) {
                    // value should be an instance of Method as at this stage
                    // staticMembers and members can only contain methods
                    overloadedMethods = new ArrayList<>();
                    overloadedMethods.add(m);
                    ht.put(name, overloadedMethods);
                } else {
                    throw Kit.codeBug();
                }
                overloadedMethods.add(method);
            }
        }

        // replace Method instances by wrapped NativeJavaMethod objects
        // first in staticMembers and then in members
        for (int tableCursor = 0; tableCursor != 2; ++tableCursor) {
            val isStatic = (tableCursor == 0);
            val ht = isStatic ? staticMembers : members;
            for (val entry : ht.entrySet()) {
                MemberBox[] methodBoxes;
                val value = entry.getValue();
                if (value instanceof Method) {
                    methodBoxes = new MemberBox[1];
                    methodBoxes[0] = new MemberBox((Method) value);
                } else {
                    val overloadedMethods = (ArrayList<Method>) value;
                    val N = overloadedMethods.size();
                    if (N < 2) {
                        Kit.codeBug();
                    }
                    methodBoxes = new MemberBox[N];
                    for (int i = 0; i != N; ++i) {
                        methodBoxes[i] = new MemberBox(overloadedMethods.get(i));
                    }
                }
                NativeJavaMethod fun = new NativeJavaMethod(methodBoxes);
                if (scope != null) {
                    ScriptRuntime.setFunctionProtoAndParent(fun, scope);
                }
                ht.put(entry.getKey(), fun);
            }
        }

        // Reflect fields.
        for (FieldInfo fieldInfo : getAccessibleFields(cx, includeProtected)) {
            val field = fieldInfo.field;
            val name = fieldInfo.name;

            val mods = field.getModifiers();
            try {
                val isStatic = Modifier.isStatic(mods);
                val ht = isStatic ? staticMembers : members;
                val o = ht.get(name);
                if (o == null) {
                    ht.put(name, field);
                } else if (o instanceof NativeJavaMethod method) {
                    val fam = new FieldAndMethods(scope, method.methods, field);
                    val fmht = isStatic ? staticFieldAndMethods : fieldAndMethods;
                    fmht.put(name, fam);
                    ht.put(name, fam);
                } else if (o instanceof Field oldField) {// If this newly reflected field shadows an inherited field,
                    // then replace it. Otherwise, since access to the field
                    // would be ambiguous from Java, no field should be
                    // reflected.
                    // For now, the first field found wins, unless another field
                    // explicitly shadows it.
                    if (oldField.getDeclaringClass().isAssignableFrom(field.getDeclaringClass())) {
                        ht.put(name, field);
                    }
                } else {
                    Kit.codeBug();// "unknown member type"
                }
            } catch (SecurityException e) {
                // skip this field
                Context.reportWarning("Could not access field "
                    + name
                    + " of class "
                    + clazz.getName()
                    + " due to lack of privileges.");
            }
        }

        createBeaning();

        // Reflect constructors
        val constructors = getAccessibleConstructors();
        val ctorMembers = new MemberBox[constructors.size()];
        for (int i = 0; i != constructors.size(); ++i) {
            ctorMembers[i] = new MemberBox(constructors.get(i));
        }
        ctors = new NativeJavaMethod(ctorMembers, clazz.getSimpleName());
    }

    /**
     * Create bean properties from corresponding get/set methods first for
     * static members and then for instance members
     */
    private void createBeaning() {
        for (byte tableCursor = 0; tableCursor != 2; ++tableCursor) {
            val isStatic = (tableCursor == 0);
            val ht = isStatic ? staticMembers : members;

            Map<String, BeanProperty> toAdd = new HashMap<>();

            // Now, For each member, make "bean" properties.
            for (String name : ht.keySet()) {
                // Is this a getter?
                val memberIsGetMethod = name.startsWith("get");
                val memberIsSetMethod = name.startsWith("set");
                val memberIsIsMethod = name.startsWith("is");
                if (!memberIsGetMethod && !memberIsIsMethod && !memberIsSetMethod) {
                    continue;
                }
                // Double check name component.
                String nameComponent = name.substring(memberIsIsMethod ? 2 : 3);
                if (nameComponent.isEmpty()) {
                    continue;
                }

                // Make the bean property name.
                String beanPropertyName = nameComponent;
                char ch0 = nameComponent.charAt(0);
                if (Character.isUpperCase(ch0)) {
                    if (nameComponent.length() == 1) {
                        beanPropertyName = nameComponent.toLowerCase();
                    } else {
                        char ch1 = nameComponent.charAt(1);
                        if (!Character.isUpperCase(ch1)) {
                            beanPropertyName = Character.toLowerCase(ch0) + nameComponent.substring(1);
                        }
                    }
                }

                // If we already have a member by this name, don't do this
                // property.
                if (toAdd.containsKey(beanPropertyName)) {
                    continue;
                }
                Object v = ht.get(beanPropertyName);
                if (v != null) {
                    // A private field shouldn't mask a public getter/setter
                    continue;
                }

                // Find the getter method, or if there is none, the is-
                // method.
                MemberBox getter;
                getter = findGetter(isStatic, ht, "get", nameComponent);
                // If there was no valid getter, check for an is- method.
                if (getter == null) {
                    getter = findGetter(isStatic, ht, "is", nameComponent);
                }

                // setter
                MemberBox setter = null;
                NativeJavaMethod setters = null;
                val setterName = "set".concat(nameComponent);

                if (ht.get(setterName) instanceof NativeJavaMethod njmSetter) {
                    if (getter != null) {
                        // We have a getter. Now, do we have a matching setter?
                        val type = getter.method().getReturnType();
                        setter = extractSetMethod(type, njmSetter.methods, isStatic);
                    } else {
                        // No getter, find any set method
                        setter = extractSetMethod(njmSetter.methods, isStatic);
                    }
                    if (njmSetter.methods.length > 1) {
                        setters = njmSetter;
                    }
                }
                // Make the property.
                BeanProperty bp = new BeanProperty(getter, setter, setters);
                toAdd.put(beanPropertyName, bp);
            }

            // Add the new bean properties.
            ht.putAll(toAdd);
        }
    }

    public List<Constructor<?>> getAccessibleConstructors() {
        List<Constructor<?>> constructorsList = new ArrayList<>();

        for (Constructor<?> c : ReflectsKit.getConstructorsSafe(clazz)) {
            if (
                !c.isAnnotationPresent(HideFromJS.class)
                && Modifier.isPublic(c.getModifiers())
            ) {
                constructorsList.add(c);
            }
        }

        return constructorsList;
    }

    public Collection<FieldInfo> getAccessibleFields(Context cx, boolean includeProtected) {
        val fieldMap = new LinkedHashMap<String, FieldInfo>();
        val remapper = cx.getRemapper();

        try {
            Class<?> currentClass = clazz;

            while (currentClass != null) {
                // get all declared fields in this class, make them
                // accessible, and save

                for (val field : ReflectsKit.getDeclaredFieldsSafe(currentClass)) {
                    val mods = field.getModifiers();
                    if (Modifier.isTransient(mods)
                        || (!Modifier.isPublic(mods) && (!includeProtected || !Modifier.isProtected(mods)))
                        || field.isAnnotationPresent(HideFromJS.class)
                    ) {
                        continue;
                    }
                    try {
                        if (includeProtected && Modifier.isProtected(mods) && !field.isAccessible()) {
                            field.setAccessible(true);
                        }

                        val info = new FieldInfo(field);

                        if (info.name.isEmpty()) {
                            info.name = remapper.remapField(currentClass, field);
                        }
                        if (info.name.isEmpty()) {
                            info.name = field.getName();
                        }

                        fieldMap.putIfAbsent(info.name, info);
                    } catch (Exception ex) {
                        // ex.printStackTrace();
                    }
                }

                // walk up superclass chain.  no need to deal specially with
                // interfaces, since they can't have fields
                currentClass = currentClass.getSuperclass();
            }
        } catch (SecurityException e) {
            // fall through to !includePrivate case
        }

        return fieldMap.values();
    }

    public Collection<MethodInfo> getAccessibleMethods(Context cx, boolean includeProtected) {
        val methodMap = new LinkedHashMap<MethodSignature, MethodInfo>();
        val remapper = cx.getRemapper();
        val stack = new ArrayDeque<Class<?>>();
        stack.add(clazz);

        while (!stack.isEmpty()) {
            val currentClass = stack.pop();

            for (val method : ReflectsKit.getDeclaredMethodsSafe(currentClass)) {
                val mods = method.getModifiers();
                if (!Modifier.isPublic(mods) && !(includeProtected && Modifier.isProtected(mods))) {
                    continue;
                }
                val signature = new MethodSignature(method);

                var info = methodMap.get(signature);
                val hidden = method.isAnnotationPresent(HideFromJS.class);

                if (info == null) {
                    try {
                        if (!hidden && includeProtected && Modifier.isProtected(mods) && !method.isAccessible()) {
                            method.setAccessible(true);
                        }

                        info = new MethodInfo(method);
                        methodMap.put(signature, info);
                    } catch (Exception ex) {
                        // ex.printStackTrace();
                    }
                }

                if (info == null) {
                    continue;
                } else if (hidden) {
                    info.hidden = true;
                    continue;
                }

                if (info.name.isEmpty()) {
                    info.name = remapper.remapMethod(currentClass, method);
                }
                if (info.name.isEmpty()) {
                    info.name = method.getName();
                }
            }

            stack.addAll(Arrays.asList(currentClass.getInterfaces()));

            val parent = currentClass.getSuperclass();

            if (parent != null) {
                stack.add(parent);
            }
        }

        val list = new ArrayList<MethodInfo>(methodMap.size());

        for (val m : methodMap.values()) {
            if (!m.hidden) {
                if (m.name.isEmpty()) {
                    m.name = m.method.getName();
                }

                list.add(m);
            }
        }

        return list;
    }

    public Map<String, FieldAndMethods> getFieldAndMethodsObjects(Scriptable scope, Object javaObject, boolean isStatic) {
        Map<String, FieldAndMethods> ht = isStatic ? staticFieldAndMethods : fieldAndMethods;
        if (ht == null) {
            return null;
        }
        int len = ht.size();
        Map<String, FieldAndMethods> result = new HashMap<>(len);
        for (FieldAndMethods fam : ht.values()) {
            FieldAndMethods famNew = new FieldAndMethods(scope, fam.methods, fam.field);
            famNew.javaObject = javaObject;
            result.put(fam.field.getName(), famNew);
        }
        return result;
    }

    public RuntimeException reportMemberNotFound(String memberName) {
        return Context.reportRuntimeError2("msg.java.member.not.found", clazz.getName(), memberName);
    }
}
