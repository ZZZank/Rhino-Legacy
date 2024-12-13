package dev.latvian.mods.rhino.native_java.type.info;

import lombok.val;

import java.lang.reflect.TypeVariable;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author ZZZank
 */
public class VariableTypeInfo extends TypeInfoBase {
    private static final Map<TypeVariable<?>, VariableTypeInfo> CACHE = new IdentityHashMap<>();
    private static final Lock READ;
    private static final Lock WRITE;

    static {
        val l = new ReentrantReadWriteLock();
        READ = l.readLock();
        WRITE = l.writeLock();
    }

    private Object bound = null;

    /**
     * we don't need type name to match a TypeVariable, it's designed to be unique
     */
    static TypeInfo of(TypeVariable<?> t) {
        READ.lock();
        var got = CACHE.get(t);
        READ.unlock();
        if (got == null) {
            WRITE.lock();
            if (got == null) { // for concurrency
                CACHE.put(t, got = new VariableTypeInfo());
            }
            WRITE.unlock();
        }
        return got;
    }

    public TypeInfo getBound() {
        if (bound instanceof TypeVariable<?> t) {
            // a variable type can have multiple bounds, but we only resolves the first one, since type wrapper cannot
            // magically find or create a class that meets multiple bounds
            val bound = t.getBounds()[0];
            if (bound == Object.class) {
                this.bound = TypeInfo.NONE;
            } else {
                this.bound = TypeInfo.of(bound);
            }
        }
        return (TypeInfo) bound;
    }

    @Override
    public Class<?> asClass() {
        return getBound().asClass();
    }

    @Override
    public TypeInfo consolidate(Map<VariableTypeInfo, TypeInfo> mapping) {
        return mapping.getOrDefault(this, this);
    }
}
