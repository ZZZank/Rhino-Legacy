package dev.latvian.mods.rhino.classdata;

import dev.latvian.mods.rhino.util.HideFromJS;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

public class ClassData {
	private static boolean isVoid(Class<?> c) {
		return c == void.class || c == Void.class;
	}

	private static boolean isBoolean(Class<?> c) {
		return c == boolean.class || c == Boolean.class;
	}

	public final ClassDataCache cache;
	public final Class<?> type;
	private ClassData parent;
	private Map<String, ClassMember> ownMembers;
	private Map<String, ClassMember> actualMembers;
	private Map<MethodSignature, Constructor<?>> constructors;

	ClassData(ClassDataCache c, Class<?> t) {
		cache = c;
		type = t;
	}

	@Nullable
	public ClassData getParent() {
		if (parent == null) {
			if (this == cache.objectClassData) {
				return null;
			}

			parent = cache.of(type.getSuperclass());
		}

		return parent;
	}

	private ClassMember make(String name) {
		ClassMember m = ownMembers.get(name);

		if (m == null) {
			m = new ClassMember(this, name);
			ownMembers.put(name, m);
		}

		return m;
	}

	private Map<String, ClassMember> getOwnMembers() {
		if (ownMembers == null) {
			if (type == Object.class) {
				ownMembers = new HashMap<>();
				return ownMembers;
			}

			ownMembers = new HashMap<>();

			for (Field field : type.getDeclaredFields()) {
				int m = field.getModifiers();

				if (Modifier.isPublic(m) && !Modifier.isTransient(m) && !field.isAnnotationPresent(HideFromJS.class)) {
					String n = cache.data.getRemapper().getMappedField(type, field);
					ClassMember cm = make(n);
					cm.field = field;
					cm.isFinal = Modifier.isFinal(m);
				}
			}

			for (Method method : type.getDeclaredMethods()) {
				int m = method.getModifiers();

				if (Modifier.isPublic(m) && !Modifier.isNative(m)) {
					String n = cache.data.getRemapper().getMappedMethod(type, method);
					ClassMember cm = make(n);

					if (cm.methods == null) {
						cm.methods = new HashMap<>();
					}

					MethodInfo mi = new MethodInfo();
					mi.method = method;
					mi.signature = MethodSignature.of(method.getParameterTypes());
					cm.methods.put(mi.signature, mi);

					mi.isHidden = method.isAnnotationPresent(HideFromJS.class);

					if (mi.isHidden) {
						continue;
					}

					if (mi.signature.types.length == 0 && n.length() >= 4 && !isVoid(method.getReturnType()) && Character.isUpperCase(n.charAt(3)) && n.startsWith("get")) {
						mi.bean = n.substring(3, 4).toLowerCase() + n.substring(4);
						make(mi.bean).beanGet = mi;
					} else if (mi.signature.types.length == 1 && n.length() >= 4 && Character.isUpperCase(n.charAt(3)) && n.startsWith("set")) {
						mi.bean = n.substring(3, 4).toLowerCase() + n.substring(4);
						make(mi.bean).beanSet = mi;
					} else if (mi.signature.types.length == 0 && n.length() >= 3 && isBoolean(method.getReturnType()) && Character.isUpperCase(n.charAt(2)) && n.startsWith("is")) {
						mi.bean = n.substring(2, 3).toLowerCase() + n.substring(3);
						make(mi.bean).beanGet = mi;
					}
				}
			}

			if (ownMembers.isEmpty()) {
				ownMembers = new HashMap<>();
			}
		}

		return ownMembers;
	}

	private Map<String, ClassMember> getActualMembers() {
		if (actualMembers == null) {
			Map<String, ClassMember> members = new HashMap<>();
			ArrayDeque<ClassData> stack = new ArrayDeque<ClassData>();
			stack.add(this);

			while (!stack.isEmpty()) {
				ClassData current = stack.pop();

				for (ClassMember member : current.getOwnMembers().values()) {
					ClassMember existing = members.get(member.name);

					if (existing == null) {
						existing = new ClassMember(this, member.name);
						members.put(member.name, existing);
					}

					existing.merge(member);
				}

				for (Class<?> iface : current.type.getInterfaces()) {
					stack.add(cache.of(iface));
				}

				ClassData parent = current.getParent();

				if (parent != null) {
					stack.add(parent);
				}
			}

			actualMembers = new HashMap<>(members.size());

			for (ClassMember member : members.values()) {
				actualMembers.put(member.name, member);
			}

			if (actualMembers.isEmpty()) {
				actualMembers = new HashMap<>();
			}
		}

		return actualMembers;
	}

	@Nullable
	public ClassMember getMember(String name) {
		return getActualMembers().get(name);
	}

	@Nullable
	public Constructor<?> getConstructor(MethodSignature sig) {
		if (constructors == null) {
			constructors = new HashMap<>();

			for (Constructor<?> c : type.getDeclaredConstructors()) {
				int m = c.getModifiers();

				if (Modifier.isPublic(m) && !c.isAnnotationPresent(HideFromJS.class)) {
					constructors.put(MethodSignature.of(c.getParameterTypes()), c);
				}
			}
		}

		return constructors.get(sig);
	}
}
