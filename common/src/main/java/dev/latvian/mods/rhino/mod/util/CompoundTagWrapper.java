package dev.latvian.mods.rhino.mod.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.latvian.mods.rhino.util.MapLike;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class CompoundTagWrapper implements NBTSerializable, MapLike<String, Object>, JsonSerializable, ChangeListener<Tag> {
	public static Object unwrap(@Nullable Tag t, @Nullable ChangeListener<Tag> l) {
        if (t == null) {
            return null;
        } else if (t instanceof EndTag) {
            return null;
        } else if (t instanceof StringTag) {
            return t.getAsString();
        } else if (t instanceof NumericTag numeric) {
            return numeric.getAsNumber();
        } else if (t instanceof CompoundTag compound) {
            return new CompoundTagWrapper(compound).withListener(l);
        } else if (t instanceof CollectionTag<?> collection) {
            return new CollectionTagWrapper<>(collection).withListener(l);
        }
        return t;
    }

	public static Tag wrap(@Nullable Object o) {
        return NBTUtils.toTag(o);
    }

	public final CompoundTag minecraftTag;
	public ChangeListener<Tag> listener;

	public CompoundTagWrapper(CompoundTag t) {
		minecraftTag = t;
	}

	@Override
	public Object getML(String key) {
		return unwrap(minecraftTag.get(key), this);
	}

	@Override
	public void putML(String key, Object value) {
		Tag t = wrap(value);

		if (t != null) {
			minecraftTag.put(key, t);

			if (listener != null) {
				listener.onChanged(minecraftTag);
			}
		}
	}

	@Override
	public boolean containsKeyML(String key) {
		return minecraftTag.contains(key);
	}

	@Override
	public Collection<String> keysML() {
		return minecraftTag.getAllKeys();
	}

	@Override
	public JsonObject toJson() {
		JsonObject json = new JsonObject();

		for (String key : minecraftTag.getAllKeys()) {
			JsonElement e = JsonUtils.of(unwrap(minecraftTag.get(key), this));

			if (!e.isJsonNull()) {
				json.add(key, e);
			}
		}

		return json;
	}

	@Override
	public void onChanged(Tag o) {
		if (listener != null) {
			listener.onChanged(minecraftTag);
		}
	}

	public CompoundTagWrapper withListener(ChangeListener<Tag> listener) {
		this.listener = listener;
		return this;
	}

	@Override
	public Tag toNBT() {
		return this.minecraftTag;
	}
}
