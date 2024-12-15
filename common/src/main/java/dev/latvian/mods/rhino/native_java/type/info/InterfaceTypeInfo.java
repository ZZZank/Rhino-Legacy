package dev.latvian.mods.rhino.native_java.type.info;

import lombok.val;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class InterfaceTypeInfo extends ClassTypeInfo {
	static final Map<Class<?>, InterfaceTypeInfo> CACHE = new IdentityHashMap<>();
	private static final Lock WRITE_LOCK;
	private static final Lock READ_LOCK;

	static {
		val l = new ReentrantReadWriteLock();
		READ_LOCK = l.readLock();
		WRITE_LOCK = l.writeLock();
	}

	static InterfaceTypeInfo of(Class<?> c) {
		READ_LOCK.lock();
		var got = CACHE.get(c);
		READ_LOCK.unlock();
		if (got == null) {
			WRITE_LOCK.lock();
			got = CACHE.computeIfAbsent(c, InterfaceTypeInfo::new);
			WRITE_LOCK.unlock();
		}
		return got;
	}

	private Boolean functional;

	InterfaceTypeInfo(Class<?> type) {
		this(type, null);
	}

	InterfaceTypeInfo(Class<?> type, Boolean functional) {
		super(type);
		this.functional = functional;
	}

	@Override
	public boolean isFunctionalInterface() {
		if (functional == null) {
			functional = Boolean.FALSE;

			try {
				if (asClass().isAnnotationPresent(FunctionalInterface.class)) {
					functional = Boolean.TRUE;
				} else {
					int count = 0;

					for (var method : asClass().getMethods()) {
						if (!method.isDefault() && !method.isSynthetic() && !method.isBridge()) {
							count++;
						}

						if (count > 1) {
							break;
						}
					}

					if (count == 1) {
						functional = Boolean.TRUE;
					}
				}
			} catch (Throwable ex) {
				ex.printStackTrace();
			}
		}

		return functional;
	}

	@Override
	public boolean isInterface() {
		return true;
	}
}
