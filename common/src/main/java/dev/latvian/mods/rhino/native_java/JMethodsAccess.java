package dev.latvian.mods.rhino.native_java;

import dev.latvian.mods.rhino.native_java.reflectasm.MethodAccess;
import dev.latvian.mods.rhino.util.remapper.RemapperManager;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.val;

/**
 * @author ZZZank
 */
public class JMethodsAccess {

    public final Class<?> raw;
    public final MethodAccess access;
    /**
     * native name -> method index
     * <p>
     * we assume that methods with different indexes always have different names
     */
    public final Object2IntOpenHashMap<String> nameIndex;

    public JMethodsAccess(Class<?> clazz) {
        val remapper = RemapperManager.getDefault();

        this.raw = clazz;
        this.access = MethodAccess.get(raw);
        val methods = clazz.getMethods();
        this.nameIndex = new Object2IntOpenHashMap<>(methods.length);
        for (val method : methods) {
            val remapped = remapper.remapMethod(raw, method);
            nameIndex.put(method.getName(), access.getIndex(method.getName()));
        }
    }
}
