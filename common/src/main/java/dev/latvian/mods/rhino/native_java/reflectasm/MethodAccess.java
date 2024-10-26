/**
 * Copyright (c) 2008, Nathan Sweet
 *  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *  3. Neither the name of Esoteric Software nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package dev.latvian.mods.rhino.native_java.reflectasm;

import static org.objectweb.asm.Opcodes.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import dev.latvian.mods.rhino.native_java.ReflectsKit;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.val;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

@SuppressWarnings("rawtypes")
@Getter
public abstract class MethodAccess {
	public static final String CLASS_INTERNAL_NAME = MethodAccess.class.getName().replace('.', '/');

	private Method[] methods;
	private String[] methodNames;
	private Class[][] parameterTypes;
	private Class[] returnTypes;

	abstract public Object invoke (Object object, int methodIndex, Object... args);

	/** Invokes the method with the specified name and the specified param types. */
	public Object invoke (Object object, String methodName, Class[] paramTypes, Object... args) {
		return invoke(object, getIndex(methodName, paramTypes), args);
	}

	/** Invokes the first method with the specified name and the specified number of arguments. */
	public Object invoke (Object object, String methodName, Object... args) {
		return invoke(object, getIndex(methodName, args == null ? 0 : args.length), args);
	}

	public int getIndex(Method method) {
        for (int i = 0; i < methods.length; i++) {
            if (methods[i].equals(method)) {
				return i;
            }
        }
		throw new IllegalArgumentException("Unable to find non-private method: " + method);
	}

	/** Returns the index of the first method with the specified name. */
	public int getIndex (String methodName) {
		for (int i = 0, n = methodNames.length; i < n; i++)
			if (methodNames[i].equals(methodName)) return i;
		throw new IllegalArgumentException("Unable to find non-private method: " + methodName);
	}

	/** Returns the index of the first method with the specified name and param types. */
	public int getIndex (String methodName, Class... paramTypes) {
		for (int i = 0, n = methodNames.length; i < n; i++)
			if (methodNames[i].equals(methodName) && Arrays.equals(paramTypes, parameterTypes[i])) return i;
		throw new IllegalArgumentException("Unable to find non-private method: " + methodName + " " + Arrays.toString(paramTypes));
	}

	/** Returns the index of the first method with the specified name and the specified number of arguments. */
	public int getIndex (String methodName, int paramsCount) {
		for (int i = 0, n = methodNames.length; i < n; i++)
			if (methodNames[i].equals(methodName) && parameterTypes[i].length == paramsCount) return i;
		throw new IllegalArgumentException(
			"Unable to find non-private method: " + methodName + " with " + paramsCount + " params.");
	}

	/** Creates a new MethodAccess for the specified type.
	 * @param type Must not be a primitive type, or void. */
	static public MethodAccess get (Class type) {
		val isInterface = type.isInterface();
		if (!isInterface && type.getSuperclass() == null && type != Object.class) {
			throw new IllegalArgumentException("The type must not be an interface, a primitive type, or void.");
		}

		val methodsRaw = ReflectsKit.getMethodsSafe(type);
		val methods = Arrays.asList(methodsRaw);

		val n = methods.size();
		val methodNames = new String[n];
		val parameterTypes = new Class[n][];
		val returnTypes = new Class[n];
		for (int i = 0; i < n; i++) {
			val method = methods.get(i);
			methodNames[i] = method.getName();
			parameterTypes[i] = method.getParameterTypes();
			returnTypes[i] = method.getReturnType();
		}

		val className = type.getName();
		val accessClassName = "reflectasm." + className + "MethodAccess";

		Class accessClass;
		val loader = AccessClassLoader.get(type);
		synchronized (loader) {
			accessClass = loader.loadAccessClass(accessClassName);
			if (accessClass == null) {
				String accessClassNameInternal = accessClassName.replace('.', '/');
				String classNameInternal = className.replace('.', '/');

				ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				MethodVisitor mv;
				cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, accessClassNameInternal, null, CLASS_INTERNAL_NAME, null);
				{
					mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
					mv.visitCode();
					mv.visitVarInsn(ALOAD, 0);
					mv.visitMethodInsn(INVOKESPECIAL, CLASS_INTERNAL_NAME, "<init>", "()V");
					mv.visitInsn(RETURN);
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				}
				{
					mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "invoke",
						"(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
					mv.visitCode();

					if (!methods.isEmpty()) {
						mv.visitVarInsn(ALOAD, 1);
						mv.visitTypeInsn(CHECKCAST, classNameInternal);
						mv.visitVarInsn(ASTORE, 4);

						mv.visitVarInsn(ILOAD, 2);
						Label[] labels = new Label[n];
						for (int i = 0; i < n; i++)
							labels[i] = new Label();
						Label defaultLabel = new Label();
						mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

						StringBuilder buffer = new StringBuilder(128);
						for (int i = 0; i < n; i++) {
							mv.visitLabel(labels[i]);
							if (i == 0)
								mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] {classNameInternal}, 0, null);
							else
								mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
							mv.visitVarInsn(ALOAD, 4);

							buffer.setLength(0);
							buffer.append('(');

							Class[] paramTypes = parameterTypes[i];
							Class returnType = returnTypes[i];
							for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
								mv.visitVarInsn(ALOAD, 3);
								mv.visitIntInsn(BIPUSH, paramIndex);
								mv.visitInsn(AALOAD);
								Type paramType = Type.getType(paramTypes[paramIndex]);
								switch (paramType.getSort()) {
								case Type.BOOLEAN:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z");
									break;
								case Type.BYTE:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Byte");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B");
									break;
								case Type.CHAR:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C");
									break;
								case Type.SHORT:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Short");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S");
									break;
								case Type.INT:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Integer");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I");
									break;
								case Type.FLOAT:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Float");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F");
									break;
								case Type.LONG:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Long");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J");
									break;
								case Type.DOUBLE:
									mv.visitTypeInsn(CHECKCAST, "java/lang/Double");
									mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D");
									break;
								case Type.ARRAY:
									mv.visitTypeInsn(CHECKCAST, paramType.getDescriptor());
									break;
								case Type.OBJECT:
									mv.visitTypeInsn(CHECKCAST, paramType.getInternalName());
									break;
								}
								buffer.append(paramType.getDescriptor());
							}

							buffer.append(')');
							buffer.append(Type.getDescriptor(returnType));
							int invoke;
							if (isInterface)
								invoke = INVOKEINTERFACE;
							else if (Modifier.isStatic(methods.get(i).getModifiers()))
								invoke = INVOKESTATIC;
							else
								invoke = INVOKEVIRTUAL;
							mv.visitMethodInsn(invoke, classNameInternal, methodNames[i], buffer.toString());

							switch (Type.getType(returnType).getSort()) {
							case Type.VOID:
								mv.visitInsn(ACONST_NULL);
								break;
							case Type.BOOLEAN:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
								break;
							case Type.BYTE:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
								break;
							case Type.CHAR:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
								break;
							case Type.SHORT:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
								break;
							case Type.INT:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
								break;
							case Type.FLOAT:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
								break;
							case Type.LONG:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
								break;
							case Type.DOUBLE:
								mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
								break;
							}

							mv.visitInsn(ARETURN);
						}

						mv.visitLabel(defaultLabel);
						mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
					}
					mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
					mv.visitInsn(DUP);
					mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
					mv.visitInsn(DUP);
					mv.visitLdcInsn("Method not found: ");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
					mv.visitVarInsn(ILOAD, 2);
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;");
					mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
					mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V");
					mv.visitInsn(ATHROW);
					mv.visitMaxs(0, 0);
					mv.visitEnd();
				}
				cw.visitEnd();
                accessClass = loader.defineAccessClass(accessClassName, cw.toByteArray());
			}
		}
		try {
			val access = (MethodAccess)accessClass.newInstance();
			access.methodNames = methodNames;
			access.parameterTypes = parameterTypes;
			access.returnTypes = returnTypes;
			access.methods = methodsRaw;
			return access;
		} catch (Throwable t) {
			throw new RuntimeException("Error constructing method access class: " + accessClassName, t);
		}
	}
}
