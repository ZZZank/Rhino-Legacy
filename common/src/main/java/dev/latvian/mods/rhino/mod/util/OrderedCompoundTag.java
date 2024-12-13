package dev.latvian.mods.rhino.mod.util;

import lombok.val;
import net.minecraft.nbt.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class OrderedCompoundTag extends CompoundTag {

	public static final TagType<OrderedCompoundTag> TYPE;

    public final Map<String, Tag> tagMap;

	public OrderedCompoundTag(Map<String, Tag> map) {
		super(map);
		tagMap = map;
	}

	public OrderedCompoundTag() {
		this(new LinkedHashMap<>());
	}

	@Override
	public void write(DataOutput dataOutput) throws IOException {
		for (Map.Entry<String, Tag> entry : tagMap.entrySet()) {
			Tag tag = entry.getValue();
			dataOutput.writeByte(tag.getId());

			if (tag.getId() != 0) {
				dataOutput.writeUTF(entry.getKey());
				tag.write(dataOutput);
			}
		}

		dataOutput.writeByte(0);
	}

    static {
		TYPE = new TagType<>() {
			@Override
			public OrderedCompoundTag load(DataInput dataInput, int i, NbtAccounter nbtAccounter) throws IOException {
				nbtAccounter.accountBits(8 * 48L);
				if (i > 512) {
					throw new RuntimeException("Tried to read NBT tag with too high complexity, depth > 512");
				}
				Map<String, Tag> map = new LinkedHashMap<>();

				byte typeId;
				while ((typeId = dataInput.readByte()) != 0) {
					val key = dataInput.readUTF();
					nbtAccounter.accountBits(8 * (28L + 2L * key.length()));
					val valueType = NBTUtils.convertType(TagTypes.getType(typeId));
					val value = valueType.load(dataInput, i + 1, nbtAccounter);

					if (map.put(key, value) != null) {
						nbtAccounter.accountBits(8 * 36L);
					}
				}

				return new OrderedCompoundTag(map);
			}

			@Override
			public String getName() {
				return "COMPOUND";
			}

			@Override
			public String getPrettyName() {
				return "TAG_Compound";
			}
		};
	}
}
