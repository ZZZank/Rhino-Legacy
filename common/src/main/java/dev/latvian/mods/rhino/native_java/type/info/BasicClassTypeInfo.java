package dev.latvian.mods.rhino.native_java.type.info;

import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BasicClassTypeInfo extends ClassTypeInfo {
	static final Map<Class<?>, BasicClassTypeInfo> CACHE = new IdentityHashMap<>();
	private static final Lock WRITE_LOCK;
	private static final Lock READ_LOCK;

	static {
		val l = new ReentrantReadWriteLock();
		READ_LOCK = l.readLock();
		WRITE_LOCK = l.writeLock();
	}

	static @NotNull BasicClassTypeInfo of(Class<?> c) {
		READ_LOCK.lock();
		var got = CACHE.get(c);
		READ_LOCK.unlock();
		if (got == null) {
			WRITE_LOCK.lock();
			got = new BasicClassTypeInfo(c);
			CACHE.put(c, got);
			WRITE_LOCK.unlock();
		}
		return got;
	}

	BasicClassTypeInfo(Class<?> type) {
		super(type);
	}
}
