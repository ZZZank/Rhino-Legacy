package dev.latvian.mods.rhino.mod.util;

import com.google.gson.*;
import dev.latvian.mods.rhino.Undefined;
import dev.latvian.mods.rhino.annotations.typing.JSInfo;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.EncoderException;
import lombok.val;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author LatvianModder
 */
public class NBTUtils {

//	ValueUnwrapper VALUE_UNWRAPPER = (contextData, scope, value) -> value instanceof Tag tag ? fromTag(tag) : value;

	@Nullable
	public static Object fromTag(@Nullable Tag t) {
		if (t == null || t instanceof EndTag) {
			return null;
		} else if (t instanceof StringTag) {
			return t.getAsString();
		} else if (t instanceof NumericTag num) {
			return num.getAsNumber();
		}

		return t;
	}

	@Nullable
	public static Tag toTag(@Nullable Object o) {
		//already resolved
		if (o == null || o instanceof EndTag) {
			return null;
		} else if (o instanceof Tag tag) {
			return tag;
		} else if (o instanceof NBTSerializable s) {
			return s.toNBT();
		}
		//primitive
		else if (o instanceof CharSequence || o instanceof Character) {
			return StringTag.valueOf(o.toString());
		} else if (o instanceof Boolean b) {
			return ByteTag.valueOf(b);
		} else if (o instanceof Number number) {
			if (number instanceof Byte) {
				return ByteTag.valueOf(number.byteValue());
			} else if (number instanceof Short) {
				return ShortTag.valueOf(number.shortValue());
			} else if (number instanceof Integer) {
				return IntTag.valueOf(number.intValue());
			} else if (number instanceof Long) {
				return LongTag.valueOf(number.longValue());
			} else if (number instanceof Float) {
				return FloatTag.valueOf(number.floatValue());
			}
			return DoubleTag.valueOf(number.doubleValue());
		}
		//native json
		else if (o instanceof JsonPrimitive json) {
			if (json.isNumber()) {
				return toTag(json.getAsNumber());
			} else if (json.isBoolean()) {
				return ByteTag.valueOf(json.getAsBoolean());
			} else {
				return StringTag.valueOf(json.getAsString());
			}
		} else if (o instanceof JsonArray array) {
			val list = new ArrayList<Tag>(array.size());
			for (val element : array) {
				list.add(toTag(element));
			}
			return toTagCollection(list);
		} else if (o instanceof JsonObject json) {
			val tag = new OrderedCompoundTag();

			for (val entry : json.entrySet()) {
				val nbt1 = toTag(entry.getValue());
				if (nbt1 != null) {
					tag.put(entry.getKey(), nbt1);
				}
			}

			return tag;
		}
		//java collections
		else if (o instanceof Map<?, ?> map) {
			val tag = new OrderedCompoundTag();
			for (val entry : map.entrySet()) {
				val valueNbt = toTag(entry.getValue());
				if (valueNbt != null) {
					tag.put(String.valueOf(entry.getKey()), valueNbt);
				}
			}
			return tag;
		} else if (o instanceof Collection<?> c) {
			return toTagCollection(c);
		}

		return null;
	}

	@Deprecated
	@JSInfo("use `canBeTagCompound(...)` instead")
	public static boolean isTagCompound(Object o) {
		return canBeTagCompound(o);
	}

	public static boolean canBeTagCompound(Object o) {
		return o == null || Undefined.isUndefined(o)
			|| o instanceof CompoundTag
			|| o instanceof CharSequence || o instanceof Map
			|| o instanceof JsonElement;
	}

	@Nullable
	public static CompoundTag toTagCompound(@Nullable Object v) {
		if (v instanceof CompoundTag nbt) {
			return nbt;
		} else if (v instanceof CharSequence) {
			try {
				return TagParser.parseTag(v.toString());
			} catch (Exception ex) {
				return null;
			}
		} else if (v instanceof JsonPrimitive json) {
			try {
				return TagParser.parseTag(json.getAsString());
			} catch (Exception ex) {
				return null;
			}
		} else if (v instanceof JsonObject json) {
			try {
				return TagParser.parseTag(json.toString());
			} catch (Exception ex) {
				return null;
			}
		}

		return toTag(v) instanceof CompoundTag nbt ? nbt : null;
	}

	public static boolean isTagCollection(Object o) {
		return o == null || Undefined.isUndefined(o) || o instanceof CharSequence || o instanceof Collection<?> || o instanceof JsonArray;
	}

	@Nullable
	public static CollectionTag<?> toTagCollection(@Nullable Object v) {
		if (v instanceof CollectionTag<?> tag) {
			return tag;
		} else if (v instanceof CharSequence) {
			try {
				return (CollectionTag<?>) TagParser.parseTag("{a:" + v + "}").get("a");
			} catch (Exception ex) {
				return null;
			}
		} else if (v instanceof JsonArray array) {
			val list = new ArrayList<Tag>(array.size());

			for (val element : array) {
				list.add(toTag(element));
			}

			return toTagCollection(list);
		}

		return v == null ? null : toTagCollection((Collection<?>) v);
	}

	@Nullable
	public static ListTag toTagList(@Nullable Object list) {
		return (ListTag) toTagCollection(list);
	}

	public static CollectionTag<?> toTagCollection(Collection<?> c) {
		if (c.isEmpty()) {
			return new ListTag();
		}

		val values = new Tag[c.size()];
		int s = 0;
		byte commmonId = -1;

		for (Object o : c) {
			values[s] = toTag(o);

			if (values[s] != null) {
				if (commmonId == -1) {
					commmonId = values[s].getId();
				} else if (commmonId != values[s].getId()) {
					commmonId = 0;
				}

				s++;
			}
		}

		if (commmonId == NbtType.INT) {
			int[] array = new int[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsInt();
			}

			return new IntArrayTag(array);
		} else if (commmonId == NbtType.BYTE) {
			byte[] array = new byte[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsByte();
			}

			return new ByteArrayTag(array);
		} else if (commmonId == NbtType.LONG) {
			long[] array = new long[s];

			for (int i = 0; i < s; i++) {
				array[i] = ((NumericTag) values[i]).getAsLong();
			}

			return new LongArrayTag(array);
		} else if (commmonId == 0 || commmonId == -1) {
			return new ListTag();
		}

		ListTag nbt = new ListTag();

		for (Tag nbt1 : values) {
			if (nbt1 == null) {
				return nbt;
			}

			nbt.add(nbt1);
		}

		return nbt;
	}

	public static Tag compoundTag() {
		return new OrderedCompoundTag();
	}

	public static Tag compoundTag(Map<?, ?> map) {
		OrderedCompoundTag tag = new OrderedCompoundTag();

		for (var entry : map.entrySet()) {
			var tag1 = toTag(entry.getValue());

			if (tag1 != null) {
				tag.put(String.valueOf(entry.getKey()), tag1);
			}
		}

		return tag;
	}

	public static Tag listTag() {
		return new ListTag();
	}

	public static Tag listTag(List<?> list) {
		ListTag tag = new ListTag();

		for (Object v : list) {
			tag.add(toTag(v));
		}

		return tag;
	}

	public static Tag byteTag(byte v) {
		return ByteTag.valueOf(v);
	}

	public static Tag b(byte v) {
		return ByteTag.valueOf(v);
	}

	public static Tag shortTag(short v) {
		return ShortTag.valueOf(v);
	}

	public static Tag s(short v) {
		return ShortTag.valueOf(v);
	}

	public static Tag intTag(int v) {
		return IntTag.valueOf(v);
	}

	public static Tag i(int v) {
		return IntTag.valueOf(v);
	}

	public static Tag longTag(long v) {
		return LongTag.valueOf(v);
	}

	public static Tag l(long v) {
		return LongTag.valueOf(v);
	}

	public static Tag floatTag(float v) {
		return FloatTag.valueOf(v);
	}

	public static Tag f(float v) {
		return FloatTag.valueOf(v);
	}

	public static Tag doubleTag(double v) {
		return DoubleTag.valueOf(v);
	}

	public static Tag d(double v) {
		return DoubleTag.valueOf(v);
	}

	public static Tag stringTag(String v) {
		return StringTag.valueOf(v);
	}

	public static Tag intArrayTag(int[] v) {
		return new IntArrayTag(v);
	}

	public static Tag longArrayTag(long[] v) {
		return new LongArrayTag(v);
	}

	public static Tag byteArrayTag(byte[] v) {
		return new ByteArrayTag(v);
	}

	public static void quoteAndEscapeForJS(StringBuilder stringBuilder, String string) {
		int start = stringBuilder.length();
		stringBuilder.append(' ');
		char c = 0;

		for (int i = 0; i < string.length(); ++i) {
			char d = string.charAt(i);
			if (d == '\\') {
				stringBuilder.append('\\');
			} else if (d == '"' || d == '\'') {
				if (c == 0) {
					c = d == '\'' ? '"' : '\'';
				}

				if (c == d) {
					stringBuilder.append('\\');
				}
			}

			stringBuilder.append(d);
		}

		if (c == 0) {
			c = '\'';
		}

		stringBuilder.setCharAt(start, c);
		stringBuilder.append(c);
	}

	public static TagType<?> convertType(TagType<?> tagType) {
		return tagType == CompoundTag.TYPE ? OrderedCompoundTag.TYPE
			: tagType == ListTag.TYPE ? LIST_TYPE
				: tagType;
	}

	public static JsonElement toJson(@Nullable Tag t) {
		if (t == null || t instanceof EndTag) {
			return JsonNull.INSTANCE;
		} else if (t instanceof StringTag) {
			return new JsonPrimitive(t.getAsString());
		} else if (t instanceof NumericTag) {
			return new JsonPrimitive(((NumericTag) t).getAsNumber());
		} else if (t instanceof CollectionTag<?> l) {
			JsonArray array = new JsonArray();

			for (Tag tag : l) {
				array.add(toJson(tag));
			}

			return array;
		} else if (t instanceof CompoundTag c) {
			JsonObject object = new JsonObject();

			for (String key : c.getAllKeys()) {
				object.add(key, toJson(c.get(key)));
			}

			return object;
		}

		return JsonNull.INSTANCE;
	}

	@Nullable
	public static OrderedCompoundTag read(FriendlyByteBuf buf) {
		val i = buf.readerIndex();
		val b = buf.readByte();
		if (b == 0) {
			return null;
		}
        buf.readerIndex(i);

        try (val stream = new DataInputStream(new ByteBufInputStream(buf))) {
            val b1 = stream.readByte();
            if (b1 == 0) {
                return null;
            }
            stream.readUTF();
            val tagType = convertType(TagTypes.getType(b1));

            if (tagType != OrderedCompoundTag.TYPE) {
                return null;
            }

            return OrderedCompoundTag.TYPE.load(stream, 0, NbtAccounter.UNLIMITED);
        } catch (IOException var5) {
            throw new EncoderException(var5);
        }
    }

	public static Map<String, Tag> accessTagMap(CompoundTag tag) {
		return tag.tags;
	}

	public static final TagType<ListTag> LIST_TYPE = new TagType<>() {
		@Override
		public ListTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
			nbtAccounter.accountBits(8 * 37L);
			if (i > 512) {
				throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
			}
            val typeId = dataInput.readByte();
            val size = dataInput.readInt();
            if (typeId == 0 && size > 0) {
                throw new RuntimeException("Missing type on ListTag");
            }
            nbtAccounter.accountBits(8 * 4L * size);
            val valueType = convertType(TagTypes.getType(typeId));

            val list = new ListTag();
            for (int k = 0; k < size; ++k) {
                list.add(valueType.load(dataInput, i + 1, nbtAccounter));
            }
            return list;
        }

		@Override
		public String getName() {
			return "LIST";
		}

		@Override
		public String getPrettyName() {
			return "TAG_List";
		}
	};

	@Deprecated
	@Nullable
	@JSInfo("use `toTag(...) instead`")
    public static Tag toNBT(@Nullable Object o) {
		return toTag(o);
	}
}