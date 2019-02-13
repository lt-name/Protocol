package com.nukkitx.protocol.bedrock.v332;

import com.flowpowered.math.vector.Vector2f;
import com.flowpowered.math.vector.Vector3f;
import com.flowpowered.math.vector.Vector3i;
import com.nukkitx.nbt.NbtUtils;
import com.nukkitx.nbt.stream.NBTInputStream;
import com.nukkitx.nbt.stream.NBTOutputStream;
import com.nukkitx.nbt.tag.CompoundTag;
import com.nukkitx.nbt.tag.Tag;
import com.nukkitx.network.VarInts;
import com.nukkitx.network.util.Preconditions;
import com.nukkitx.protocol.bedrock.data.*;
import com.nukkitx.protocol.bedrock.packet.ResourcePackStackPacket;
import com.nukkitx.protocol.bedrock.packet.ResourcePacksInfoPacket;
import com.nukkitx.protocol.bedrock.util.LittleEndianByteBufInputStream;
import com.nukkitx.protocol.bedrock.v332.serializer.GameRulesChangedSerializer_v332;
import com.nukkitx.protocol.util.TIntHashBiMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.util.AsciiString;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static com.nukkitx.protocol.bedrock.data.Metadata.*;

@UtilityClass
public final class BedrockUtils {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(BedrockUtils.class);
    private static final TIntHashBiMap<Metadata> METADATAS = new TIntHashBiMap<>();
    private static final TIntHashBiMap<Metadata.Flag> METADATA_FLAGS = new TIntHashBiMap<>();
    private static final TIntHashBiMap<Metadata.Type> METADATA_TYPES = new TIntHashBiMap<>(9);

    static {
        METADATAS.put(0, FLAGS);
        METADATAS.put(1, HEALTH);
        METADATAS.put(2, VARIANT);
        METADATAS.put(3, COLOR);
        METADATAS.put(4, NAMETAG);
        METADATAS.put(5, OWNER_EID);
        METADATAS.put(6, TARGET_EID);
        METADATAS.put(7, AIR);
        METADATAS.put(8, POTION_COLOR);
        METADATAS.put(9, POTION_AMBIENT);
        METADATAS.put(10, JUMP_DURATION);
        METADATAS.put(11, HURT_TIME);
        METADATAS.put(12, HURT_DIRECTION);
        METADATAS.put(13, PADDLE_TIME_LEFT);
        METADATAS.put(14, PADDLE_TIME_RIGHT);
        METADATAS.put(15, EXPERIENCE_VALUE);
        METADATAS.put(16, DISPLAY_ITEM);
        METADATAS.put(17, DISPLAY_OFFSET);
        METADATAS.put(18, HAS_DISPLAY);
        METADATAS.put(22, CHARGED);
        METADATAS.put(23, ENDERMAN_HELD_ITEM_ID);
        METADATAS.put(24, ENTITY_AGE);
        //METADATAS.put(25, WITCH_UNKNOWN);
        METADATAS.put(26, CAN_START_SLEEP);
        METADATAS.put(27, PLAYER_INDEX);
        METADATAS.put(28, BED_RESPAWN_POS);
        METADATAS.put(29, FIREBALL_POWER_X);
        METADATAS.put(30, FIREBALL_POWER_Y);
        METADATAS.put(31, FIREBALL_POWER_Z);
        METADATAS.put(36, POTION_AUX_VALUE);
        METADATAS.put(37, LEAD_HOLDER_EID);
        METADATAS.put(38, SCALE);
        METADATAS.put(39, INTERACTIVE_TAG);
        METADATAS.put(40, NPC_SKIN_ID);
        METADATAS.put(41, URL_TAG);
        METADATAS.put(42, MAX_AIR);
        METADATAS.put(43, MARK_VARIANT);
        METADATAS.put(44, CONTAINER_TYPE);
        METADATAS.put(45, CONTAINER_BASE_SIZE);
        METADATAS.put(46, CONTAINER_EXTRA_SLOTS_PER_STRENGTH);
        METADATAS.put(47, BLOCK_TARGET);
        METADATAS.put(48, WITHER_INVULNERABLE_TICKS);
        METADATAS.put(49, WITHER_TARGET_1);
        METADATAS.put(50, WITHER_TARGET_2);
        METADATAS.put(51, WITHER_TARGET_3);
        METADATAS.put(53, BOUNDING_BOX_WIDTH);
        METADATAS.put(54, BOUNDING_BOX_HEIGHT);
        METADATAS.put(55, FUSE_LENGTH);
        METADATAS.put(56, RIDER_SEAT_POSITION);
        METADATAS.put(57, RIDER_ROTATION_LOCKED);
        METADATAS.put(58, RIDER_MAX_ROTATION);
        METADATAS.put(59, RIDER_MIN_ROTATION);
        METADATAS.put(60, AREA_EFFECT_CLOUD_RADIUS);
        METADATAS.put(61, AREA_EFFECT_CLOUD_WAITING);
        METADATAS.put(62, AREA_EFFECT_CLOUD_PARTICLE_ID);
        METADATAS.put(64, SHULKER_ATTACH_FACE);
        METADATAS.put(66, SHULKER_ATTACH_POS);
        METADATAS.put(67, TRADING_PLAYER_EID);
        METADATAS.put(69, COMMAND_BLOCK_ENABLED); // Unsure
        METADATAS.put(70, COMMAND_BLOCK_COMMAND);
        METADATAS.put(71, COMMAND_BLOCK_LAST_OUTPUT);
        METADATAS.put(72, COMMAND_BLOCK_TRACK_OUTPUT);
        METADATAS.put(73, CONTROLLING_RIDER_SEAT_NUMBER);
        METADATAS.put(74, STRENGTH);
        METADATAS.put(75, MAX_STRENGTH);
        METADATAS.put(76, EVOKER_SPELL_COLOR);
        METADATAS.put(77, LIMITED_LIFE);
        METADATAS.put(78, ARMOR_STAND_POSE_INDEX);
        METADATAS.put(79, ENDER_CRYSTAL_TIME_OFFSET);
        METADATAS.put(80, ALWAYS_SHOW_NAMETAG);
        METADATAS.put(81, COLOR_2);
        METADATAS.put(83, SCORE_TAG);
        METADATAS.put(84, BALLOON_ATTACHED_ENTITY);
        METADATAS.put(85, PUFFERFISH_SIZE);
        METADATAS.put(87, AGENT_ID);

        METADATA_FLAGS.put(0, Flag.ON_FIRE);
        METADATA_FLAGS.put(1, Flag.SNEAKING);
        METADATA_FLAGS.put(2, Flag.RIDING);
        METADATA_FLAGS.put(3, Flag.SPRINTING);
        METADATA_FLAGS.put(4, Flag.USING_ITEM);
        METADATA_FLAGS.put(5, Flag.INVISIBLE);
        METADATA_FLAGS.put(6, Flag.TEMPTED);
        METADATA_FLAGS.put(7, Flag.IN_LOVE);
        METADATA_FLAGS.put(8, Flag.SADDLED);
        METADATA_FLAGS.put(9, Flag.POWERED);
        METADATA_FLAGS.put(10, Flag.IGNITED);
        METADATA_FLAGS.put(11, Flag.BABY);
        METADATA_FLAGS.put(12, Flag.CONVERTING);
        METADATA_FLAGS.put(13, Flag.CRITICAL);
        METADATA_FLAGS.put(14, Flag.CAN_SHOW_NAME);
        METADATA_FLAGS.put(15, Flag.ALWAYS_SHOW_NAME);
        METADATA_FLAGS.put(16, Flag.NO_AI);
        METADATA_FLAGS.put(17, Flag.SILENT);
        METADATA_FLAGS.put(18, Flag.WALL_CLIMBING);
        METADATA_FLAGS.put(19, Flag.CAN_CLIMB);
        METADATA_FLAGS.put(20, Flag.CAN_SWIM);
        METADATA_FLAGS.put(21, Flag.CAN_FLY);
        METADATA_FLAGS.put(22, Flag.CAN_WALK);
        METADATA_FLAGS.put(23, Flag.RESTING);
        METADATA_FLAGS.put(24, Flag.SITTING);
        METADATA_FLAGS.put(25, Flag.ANGRY);
        METADATA_FLAGS.put(26, Flag.INTERESTED);
        METADATA_FLAGS.put(27, Flag.CHARGED);
        METADATA_FLAGS.put(28, Flag.TAMED);
        METADATA_FLAGS.put(29, Flag.ORPHANED);
        METADATA_FLAGS.put(30, Flag.LEASHED);
        METADATA_FLAGS.put(31, Flag.SHEARED);
        METADATA_FLAGS.put(32, Flag.GLIDING);
        METADATA_FLAGS.put(33, Flag.ELDER);
        METADATA_FLAGS.put(34, Flag.MOVING);
        METADATA_FLAGS.put(35, Flag.BREATHING);
        METADATA_FLAGS.put(36, Flag.CHESTED);
        METADATA_FLAGS.put(37, Flag.STACKABLE);
        METADATA_FLAGS.put(38, Flag.SHOW_BOTTOM);
        METADATA_FLAGS.put(39, Flag.STANDING);
        METADATA_FLAGS.put(40, Flag.SHAKING);
        METADATA_FLAGS.put(41, Flag.IDLING);
        METADATA_FLAGS.put(42, Flag.CASTING);
        METADATA_FLAGS.put(43, Flag.CHARGING);
        METADATA_FLAGS.put(44, Flag.WASD_CONTROLLED);
        METADATA_FLAGS.put(45, Flag.CAN_POWER_JUMP);
        METADATA_FLAGS.put(46, Flag.LINGERING);
        METADATA_FLAGS.put(47, Flag.HAS_COLLISION);
        METADATA_FLAGS.put(48, Flag.HAS_GRAVITY);
        METADATA_FLAGS.put(49, Flag.FIRE_IMMUNE);
        METADATA_FLAGS.put(50, Flag.DANCING);
        METADATA_FLAGS.put(51, Flag.ENCHANTED);
        METADATA_FLAGS.put(52, Flag.RETURN_TRIDENT);
        METADATA_FLAGS.put(53, Flag.CONTAINER_IS_PRIVATE);
        METADATA_FLAGS.put(55, Flag.DAMAGE_NEARBY_MOBS);
        METADATA_FLAGS.put(56, Flag.SWIMMING);
        METADATA_FLAGS.put(57, Flag.BRIBED);
        METADATA_FLAGS.put(58, Flag.IS_PREGNANT);
        METADATA_FLAGS.put(59, Flag.LAYING_EGG);

        METADATA_TYPES.put(7, Type.FLAGS);
        METADATA_TYPES.put(0, Type.BYTE);
        METADATA_TYPES.put(1, Type.SHORT);
        METADATA_TYPES.put(2, Type.INT);
        METADATA_TYPES.put(3, Type.FLOAT);
        METADATA_TYPES.put(4, Type.STRING);
        METADATA_TYPES.put(5, Type.ITEM);
        METADATA_TYPES.put(6, Type.VECTOR3I);
        METADATA_TYPES.put(7, Type.LONG);
        METADATA_TYPES.put(8, Type.VECTOR3F);
    }

    public static byte[] readByteArray(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        byte[] bytes = new byte[VarInts.readUnsignedInt(buffer)];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static void writeByteArray(ByteBuf buffer, byte[] bytes) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(bytes, "bytes");
        VarInts.writeUnsignedInt(buffer, bytes.length);
        buffer.writeBytes(bytes);
    }

    public static String readString(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        return new String(readByteArray(buffer), StandardCharsets.UTF_8);
    }

    public static void writeString(ByteBuf buffer, String string) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(string, "string");
        writeByteArray(buffer, string.getBytes(StandardCharsets.UTF_8));
    }

    public static AsciiString readLEAsciiString(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        int length = buffer.readIntLE();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new AsciiString(bytes);
    }

    public static void writeLEAsciiString(ByteBuf buffer, AsciiString string) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(string, "string");
        buffer.writeIntLE(string.length());
        buffer.writeBytes(string.toByteArray());
    }

    public static AsciiString readVarIntAsciiString(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        int length = VarInts.readUnsignedInt(buffer);
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new AsciiString(bytes);
    }

    public static void writeVarIntAsciiString(ByteBuf buffer, AsciiString string) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(string, "string");
        VarInts.writeUnsignedInt(buffer, string.length());
        buffer.writeBytes(string.toByteArray());
    }

    public static UUID readUuid(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        return new UUID(buffer.readLong(), buffer.readLong());
    }

    public static void writeUuid(ByteBuf buffer, UUID uuid) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(uuid, "uuid");
        buffer.writeLong(uuid.getMostSignificantBits());
        buffer.writeLong(uuid.getLeastSignificantBits());
    }

    public static Vector3f readVector3f(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        float x = buffer.readFloatLE();
        float y = buffer.readFloatLE();
        float z = buffer.readFloatLE();
        return new Vector3f(x, y, z);
    }

    public static void writeVector3f(ByteBuf buffer, Vector3f vector3f) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(vector3f, "vector3f");
        buffer.writeFloatLE(vector3f.getX());
        buffer.writeFloatLE(vector3f.getY());
        buffer.writeFloatLE(vector3f.getZ());
    }

    public static Vector2f readVector2f(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        float x = buffer.readFloatLE();
        float y = buffer.readFloatLE();
        return new Vector2f(x, y);
    }

    public static void writeVector2f(ByteBuf buffer, Vector2f vector2f) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(vector2f, "vector2f");
        buffer.writeFloatLE(vector2f.getX());
        buffer.writeFloatLE(vector2f.getY());
    }


    public static Vector3i readVector3i(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        int x = VarInts.readInt(buffer);
        int y = VarInts.readInt(buffer);
        int z = VarInts.readInt(buffer);

        return new Vector3i(x, y, z);
    }

    public static void writeVector3i(ByteBuf buffer, Vector3i vector3i) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(vector3i, "vector3i");
        VarInts.writeInt(buffer, vector3i.getX());
        VarInts.writeInt(buffer, vector3i.getY());
        VarInts.writeInt(buffer, vector3i.getZ());
    }

    public static Vector3i readBlockPosition(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        int x = VarInts.readInt(buffer);
        int y = VarInts.readUnsignedInt(buffer);
        int z = VarInts.readInt(buffer);

        return new Vector3i(x, y, z);
    }

    public static void writeBlockPosition(ByteBuf buffer, Vector3i blockPosition) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(blockPosition, "blockPosition");
        VarInts.writeInt(buffer, blockPosition.getX());
        VarInts.writeUnsignedInt(buffer, blockPosition.getY());
        VarInts.writeInt(buffer, blockPosition.getZ());
    }

    public static Vector3f readByteRotation(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        float pitch = readByteAngle(buffer);
        float yaw = readByteAngle(buffer);
        float roll = readByteAngle(buffer);
        return new Vector3f(pitch, yaw, roll);
    }

    public static void writeByteRotation(ByteBuf buffer, Vector3f rotation) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(rotation, "rotation");
        writeByteAngle(buffer, rotation.getX());
        writeByteAngle(buffer, rotation.getY());
        writeByteAngle(buffer, rotation.getZ());
    }

    public static float readByteAngle(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        return buffer.readByte() / 255f * 360f;
    }

    public static void writeByteAngle(ByteBuf buffer, float angle) {
        Preconditions.checkNotNull(buffer, "buffer");
        buffer.writeByte((byte) Math.ceil(angle / 360 * 255));
    }

    public static Attribute readEntityAttribute(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        String name = BedrockUtils.readString(buffer);
        float min = buffer.readFloatLE();
        float max = buffer.readFloatLE();
        float val = buffer.readFloatLE();

        return new Attribute(name, min, max, val, max);
    }

    public static void writeEntityAttribute(ByteBuf buffer, Attribute attribute) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(attribute, "attribute");

        BedrockUtils.writeString(buffer, attribute.getName());
        buffer.writeFloatLE(attribute.getMinimum());
        buffer.writeFloatLE(attribute.getMaximum());
        buffer.writeFloatLE(attribute.getValue());
    }

    public static Attribute readPlayerAttribute(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        float min = buffer.readFloatLE();
        float max = buffer.readFloatLE();
        float val = buffer.readFloatLE();
        float def = buffer.readFloatLE();
        String name = BedrockUtils.readString(buffer);

        return new Attribute(name, min, max, val, def);
    }

    public static void writePlayerAttribute(ByteBuf buffer, Attribute attribute) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(attribute, "attribute");

        buffer.writeFloatLE(attribute.getMinimum());
        buffer.writeFloatLE(attribute.getMaximum());
        buffer.writeFloatLE(attribute.getValue());
        buffer.writeFloatLE(attribute.getDefaultValue());
        BedrockUtils.writeString(buffer, attribute.getName());
    }

    public static EntityLink readEntityLink(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        long from = VarInts.readLong(buffer);
        long to = VarInts.readLong(buffer);
        int type = buffer.readUnsignedByte();
        boolean immediate = buffer.readBoolean();

        return new EntityLink(from, to, EntityLink.Type.values()[type], immediate);
    }

    public static void writeEntityLink(ByteBuf buffer, EntityLink entityLink) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(entityLink, "entityLink");

        VarInts.writeLong(buffer, entityLink.getFrom());
        VarInts.writeLong(buffer, entityLink.getTo());
        buffer.writeByte(entityLink.getType().ordinal());
        buffer.writeBoolean(entityLink.isImmediate());
    }

    public static ItemData readItemData(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        int id = VarInts.readInt(buffer);
        if (id == 0) {
            // We don't need to read anything extra.
            return ItemData.AIR;
        }
        int aux = VarInts.readInt(buffer);
        short damage = (short) (aux >> 8);
        if (damage == Short.MAX_VALUE) damage = -1;
        int count = aux & 0xff;
        int nbtSize = buffer.readShortLE();

        CompoundTag compoundTag = null;
        if (nbtSize > 0) {
            try (NBTInputStream reader = new NBTInputStream(new LittleEndianByteBufInputStream(buffer.readSlice(nbtSize)))) {
                Tag<?> tag = reader.readTag();
                if (tag instanceof CompoundTag) {
                    compoundTag = (CompoundTag) tag;
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load NBT data", e);
            }
        } else if (nbtSize == -1) {
            try (NBTInputStream reader = NbtUtils.createNetworkReader(new ByteBufInputStream(buffer))) {
                int nbtTagCount = VarInts.readUnsignedInt(buffer);
                if (nbtTagCount == 1) {
                    Tag<?> tag = reader.readTag();
                    if (tag instanceof CompoundTag) {
                        compoundTag = (CompoundTag) tag;
                    }
                } else {
                    throw new IllegalArgumentException("Expected 1 tag but got " + nbtTagCount);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load NBT data", e);
            }
        }

        String[] canPlace = new String[VarInts.readInt(buffer)];
        for (int i = 0; i < canPlace.length; i++) {
            canPlace[i] = readString(buffer);
        }

        String[] canBreak = new String[VarInts.readInt(buffer)];
        for (int i = 0; i < canBreak.length; i++) {
            canBreak[i] = readString(buffer);
        }

        return ItemData.of(id, damage, count, compoundTag, canPlace, canBreak);
    }

    public static void writeItemData(ByteBuf buffer, ItemData item) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(item, "item");

        // Write id
        int id = item.getId();
        if (id == 0) {
            // We don't need to write anything extra.
            buffer.writeByte(0);
            return;
        }
        VarInts.writeInt(buffer, id);

        // Write damage and count
        short damage = item.getDamage();
        if (damage == -1) damage = Short.MAX_VALUE;
        VarInts.writeInt(buffer, (damage << 8) | (item.getCount() & 0xff));

        buffer.writeShort(-1);
        VarInts.writeUnsignedInt(buffer, 1); // Hardcoded in current version

        try (NBTOutputStream stream = NbtUtils.createNetworkWriter(new ByteBufOutputStream(buffer))) {
            stream.write(item.getTag());
        } catch (IOException e) {
            // This shouldn't happen (as this is backed by a Netty ByteBuf), but okay...
            throw new IllegalStateException("Unable to save NBT data", e);
        }

        String[] canPlace = item.getCanPlace();
        VarInts.writeInt(buffer, canPlace.length);
        for (String aCanPlace : canPlace) {
            BedrockUtils.writeString(buffer, aCanPlace);
        }

        String[] canBreak = item.getCanBreak();
        VarInts.writeInt(buffer, canBreak.length);
        for (String aCanBreak : canBreak) {
            BedrockUtils.writeString(buffer, aCanBreak);
        }
    }

    public static CommandOriginData readCommandOriginData(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        CommandOriginData.Origin origin = CommandOriginData.Origin.values()[VarInts.readUnsignedInt(buffer)];
        UUID uuid = readUuid(buffer);
        String requestId = readString(buffer);
        long varLong = -1;
        if (origin == CommandOriginData.Origin.DEV_CONSOLE || origin == CommandOriginData.Origin.TEST) {
            varLong = VarInts.readLong(buffer);
        }
        return new CommandOriginData(origin, uuid, requestId, varLong);
    }

    public static void writeCommandOriginData(ByteBuf buffer, CommandOriginData originData) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(originData, "commandOriginData");
        VarInts.writeUnsignedInt(buffer, originData.getOrigin().ordinal());
        writeUuid(buffer, originData.getUuid());
        writeString(buffer, originData.getRequestId());
        if (originData.getOrigin() == CommandOriginData.Origin.DEV_CONSOLE || originData.getOrigin() == CommandOriginData.Origin.TEST) {
            VarInts.writeLong(buffer, originData.getEvent());
        }
    }

    public static CommandOutputMessage readCommandOutputMessage(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");
        boolean internal = buffer.readBoolean();
        String messageId = readString(buffer);
        String[] parameters = new String[VarInts.readUnsignedInt(buffer)];
        for (int i = 0; i < parameters.length; i++) {
            parameters[i] = BedrockUtils.readString(buffer);
        }
        return new CommandOutputMessage(internal, messageId, parameters);
    }

    public static void writeCommandOutputMessage(ByteBuf buffer, CommandOutputMessage outputMessage) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(outputMessage, "outputMessage");
        buffer.writeBoolean(outputMessage.isInternal());
        writeString(buffer, outputMessage.getMessageId());
        for (String parameter : outputMessage.getParameters()) {
            writeString(buffer, parameter);
        }
    }

    public static List<ResourcePacksInfoPacket.Entry> readPacksInfoEntries(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        List<ResourcePacksInfoPacket.Entry> entries = new ArrayList<>();
        int length = buffer.readUnsignedShortLE();
        for (int i = 0; i < length; i++) {
            UUID packId = UUID.fromString(readString(buffer));
            String packVersion = readString(buffer);
            long packSize = buffer.readLongLE();
            String encryptionKey = readString(buffer);
            String subpackName = readString(buffer);
            String contentId = readString(buffer);
            boolean unknownBool = buffer.readBoolean();

            entries.add(new ResourcePacksInfoPacket.Entry(packId, packVersion, packSize, encryptionKey, subpackName, contentId, unknownBool));
        }
        return entries;
    }

    public static void writePacksInfoEntries(ByteBuf buffer, Collection<ResourcePacksInfoPacket.Entry> packInfoEntries) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(packInfoEntries, "packInfoEntries");
        buffer.writeShortLE(packInfoEntries.size());
        for (ResourcePacksInfoPacket.Entry packInfoEntry : packInfoEntries) {
            writeString(buffer, packInfoEntry.getPackId().toString());
            writeString(buffer, packInfoEntry.getPackVersion());
            buffer.writeLongLE(packInfoEntry.getPackSize());
            writeString(buffer, packInfoEntry.getEncryptionKey());
            writeString(buffer, packInfoEntry.getSubpackName());
            writeString(buffer, packInfoEntry.getContentId());
            buffer.writeBoolean(packInfoEntry.isScripting());
        }
    }

    public static ResourcePackStackPacket.Entry readPackInstanceEntry(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        UUID packId = UUID.fromString(readString(buffer));
        String packVersion = readString(buffer);
        String subpackName = readString(buffer);
        return new ResourcePackStackPacket.Entry(packId, packVersion, subpackName);
    }

    public static void writePackInstanceEntry(ByteBuf buffer, ResourcePackStackPacket.Entry packInstanceEntry) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(packInstanceEntry, "packInstanceEntry");

        writeString(buffer, packInstanceEntry.getPackId().toString());
        writeString(buffer, packInstanceEntry.getPackVersion());
        writeString(buffer, packInstanceEntry.getSubpackName());
    }

    public static <T> void readArray(ByteBuf buffer, Collection<T> array, Function<ByteBuf, T> function) {
        int length = VarInts.readUnsignedInt(buffer);


        for (int i = 0; i < length; i++) {
            array.add(function.apply(buffer));
        }
    }

    public static <T> void writeArray(ByteBuf buffer, Collection<T> array, BiConsumer<ByteBuf, T> biConsumer) {
        VarInts.writeUnsignedInt(buffer, array.size());
        for (T val : array) {
            biConsumer.accept(buffer, val);
        }
    }

    public static InventoryAction readInventoryAction(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        InventorySource source = readInventorySource(buffer);

        int slot = VarInts.readUnsignedInt(buffer);
        ItemData fromItem = readItemData(buffer);
        ItemData toItem = readItemData(buffer);

        return new InventoryAction(source, slot, fromItem, toItem);
    }

    public static void writeInventoryAction(ByteBuf buffer, InventoryAction action) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(action, "action");

        writeInventorySource(buffer, action.getSource());

        VarInts.writeUnsignedInt(buffer, action.getSlot());
        writeItemData(buffer, action.getFromItem());
        writeItemData(buffer, action.getToItem());
    }

    public static InventorySource readInventorySource(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        InventorySource.Type type = InventorySource.Type.byId(VarInts.readUnsignedInt(buffer));

        switch (type) {
            case CONTAINER:
                ContainerId containerId = ContainerId.byId(VarInts.readInt(buffer));
                return InventorySource.fromContainerWindowId(containerId);
            case GLOBAL:
                return InventorySource.fromGlobalInventory();
            case WORLD_INTERACTION:
                InventorySource.Flag flag = InventorySource.Flag.values()[VarInts.readUnsignedInt(buffer)];
                return InventorySource.fromWorldInteraction(flag);
            case CREATIVE:
                return InventorySource.fromCreativeInventory();
            case UNTRACKED_INTERACTION_UI:
                containerId = ContainerId.byId(VarInts.readInt(buffer));
                return InventorySource.fromUntrackedInteractionUI(containerId);
            case NON_IMPLEMENTED_TODO:
                containerId = ContainerId.byId(VarInts.readInt(buffer));
                return InventorySource.fromNonImplementedTodo(containerId);
            default:
                return InventorySource.fromInvalid();
        }
    }

    public static void writeInventorySource(ByteBuf buffer, InventorySource inventorySource) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(inventorySource, "inventorySource");

        VarInts.writeUnsignedInt(buffer, inventorySource.getType().id());

        switch (inventorySource.getType()) {
            case CONTAINER:
            case UNTRACKED_INTERACTION_UI:
            case NON_IMPLEMENTED_TODO:
                VarInts.writeInt(buffer, inventorySource.getContainerId().id());
                break;
            case WORLD_INTERACTION:
                VarInts.writeUnsignedInt(buffer, inventorySource.getFlag().ordinal());
                break;
        }
    }

    public static GameRule readGameRule(ByteBuf buffer) {
        Preconditions.checkNotNull(buffer, "buffer");

        String name = BedrockUtils.readString(buffer);
        int type = VarInts.readUnsignedInt(buffer);

        switch (type) {
            case 1:
                return new GameRule<>(name, buffer.readBoolean());
            case 2:
                return new GameRule<>(name, VarInts.readUnsignedInt(buffer));
            case 3:
                return new GameRule<>(name, buffer.readFloatLE());
        }
        throw new IllegalStateException("Invalid gamerule type received");
    }

    public static void writeGameRule(ByteBuf buffer, GameRule gameRule) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(gameRule, "gameRule");

        Object value = gameRule.getValue();
        int type = GameRulesChangedSerializer_v332.RULE_TYPES.get(value.getClass());

        BedrockUtils.writeString(buffer, gameRule.getName());
        VarInts.writeUnsignedInt(buffer, type);

        switch (type) {
            case 1:
                buffer.writeBoolean((boolean) value);
                break;
            case 2:
                VarInts.writeUnsignedInt(buffer, (int) value);
                break;
            case 3:
                buffer.writeFloatLE((float) value);
                break;
        }
    }

    public static void readMetadata(ByteBuf buffer, MetadataDictionary metadataDictionary) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(metadataDictionary, "metadataDictionary");

        int length = VarInts.readUnsignedInt(buffer);

        for (int i = 0; i < length; i++) {
            int metadataInt = VarInts.readUnsignedInt(buffer);
            Metadata metadata = METADATAS.get(metadataInt);
            Metadata.Type type = METADATA_TYPES.get(VarInts.readUnsignedInt(buffer));

            Object object;
            switch (type) {
                case BYTE:
                    object = buffer.readByte();
                    break;
                case SHORT:
                    object = buffer.readShortLE();
                    break;
                case INT:
                    object = VarInts.readInt(buffer);
                    break;
                case FLOAT:
                    object = buffer.readFloatLE();
                    break;
                case STRING:
                    object = BedrockUtils.readString(buffer);
                    break;
                case ITEM:
                    object = BedrockUtils.readItemData(buffer);
                    break;
                case VECTOR3I:
                    object = BedrockUtils.readVector3i(buffer);
                    break;
                case FLAGS:
                    object = MetadataFlags.create(VarInts.readLong(buffer), 0, METADATA_FLAGS);
                    break;
                case LONG:
                    object = VarInts.readLong(buffer);
                    break;
                case VECTOR3F:
                    object = BedrockUtils.readVector3f(buffer);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown metadata type received");
            }
            if (metadata != null) {
                metadataDictionary.put(metadata, object);
            } else {
                log.debug("Unknown metadata: {} type {} value {}", metadataInt, type, object);
            }
        }
    }

    public static void writeMetadata(ByteBuf buffer, MetadataDictionary metadataDictionary) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(metadataDictionary, "metadataDictionary");

        VarInts.writeUnsignedInt(buffer, metadataDictionary.size());

        for (Map.Entry<Metadata, Object> entry : metadataDictionary.entrySet()) {
            int index = buffer.writerIndex();
            VarInts.writeUnsignedInt(buffer, METADATAS.get(entry.getKey()));
            Object object = entry.getValue();
            Metadata.Type type = MetadataDictionary.getType(object);
            VarInts.writeUnsignedInt(buffer, METADATA_TYPES.get(type));

            switch (type) {
                case BYTE:
                    buffer.writeByte((byte) object);
                    break;
                case SHORT:
                    buffer.writeShortLE((short) object);
                    break;
                case INT:
                    VarInts.writeInt(buffer, (int) object);
                    break;
                case FLOAT:
                    buffer.writeFloatLE((float) object);
                    break;
                case STRING:
                    BedrockUtils.writeString(buffer, (String) object);
                    break;
                case ITEM:
                    BedrockUtils.writeItemData(buffer, (ItemData) object);
                    break;
                case VECTOR3I:
                    BedrockUtils.writeVector3i(buffer, (Vector3i) object);
                    break;
                case FLAGS:
                    object = ((MetadataFlags) object).get(0, METADATA_FLAGS);
                case LONG:
                    VarInts.writeLong(buffer, (long) object);
                    break;
                case VECTOR3F:
                    BedrockUtils.writeVector3f(buffer, (Vector3f) object);
                    break;
                default:
                    buffer.writerIndex(index);
                    break;
            }
        }
    }

    public static CommandEnumData readCommandEnumData(ByteBuf buffer, boolean soft) {
        Preconditions.checkNotNull(buffer, "buffer");

        String name = BedrockUtils.readString(buffer);

        String[] values = new String[VarInts.readUnsignedInt(buffer)];
        for (int i = 0; i < values.length; i++) {
            values[i] = BedrockUtils.readString(buffer);
        }
        return new CommandEnumData(name, values, soft);
    }

    public static void writeCommandEnumData(ByteBuf buffer, CommandEnumData enumData) {
        Preconditions.checkNotNull(buffer, "buffer");
        Preconditions.checkNotNull(enumData, "enumData");

        BedrockUtils.writeString(buffer, enumData.getName());

        String[] values = enumData.getValues();
        VarInts.writeUnsignedInt(buffer, values.length);
        for (String value : values) {
            BedrockUtils.writeString(buffer, value);
        }
    }
}
