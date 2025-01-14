package com.nukkitx.protocol.bedrock.v431;

import com.nukkitx.nbt.NBTInputStream;
import com.nukkitx.nbt.NBTOutputStream;
import com.nukkitx.nbt.NbtMap;
import com.nukkitx.network.VarInts;
import com.nukkitx.protocol.bedrock.BedrockSession;
import com.nukkitx.protocol.bedrock.data.LevelEventType;
import com.nukkitx.protocol.bedrock.data.SoundEvent;
import com.nukkitx.protocol.bedrock.data.inventory.InventoryActionData;
import com.nukkitx.protocol.bedrock.data.inventory.InventorySource;
import com.nukkitx.protocol.bedrock.data.inventory.ItemData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.CraftResultsDeprecatedStackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionData;
import com.nukkitx.protocol.bedrock.data.inventory.stackrequestactions.StackRequestActionType;
import com.nukkitx.protocol.bedrock.util.LittleEndianByteBufInputStream;
import com.nukkitx.protocol.bedrock.util.LittleEndianByteBufOutputStream;
import com.nukkitx.protocol.bedrock.v428.BedrockPacketHelper_v428;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;

import java.io.IOException;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class BedrockPacketHelper_v431 extends BedrockPacketHelper_v428 {
    public static final BedrockPacketHelper_v431 INSTANCE = new BedrockPacketHelper_v431();

    @Override
    protected void registerLevelEvents() {
        super.registerLevelEvents();

        this.addLevelEvent(1064, LevelEventType.SOUND_POINTED_DRIPSTONE_LAND);
        this.addLevelEvent(1065, LevelEventType.SOUND_DYE_USED);
        this.addLevelEvent(1066, LevelEventType.SOUND_INK_SACE_USED);

        this.addLevelEvent(2028, LevelEventType.PARTICLE_DRIPSTONE_DRIP);
        this.addLevelEvent(2029, LevelEventType.PARTICLE_FIZZ_EFFECT);
        this.addLevelEvent(2030, LevelEventType.PARTICLE_WAX_ON);
        this.addLevelEvent(2031, LevelEventType.PARTICLE_WAX_OFF);
        this.addLevelEvent(2032, LevelEventType.PARTICLE_SCRAPE);
        this.addLevelEvent(2033, LevelEventType.PARTICLE_ELECTRIC_SPARK);
    }

    @Override
    protected void registerSoundEvents() {
        super.registerSoundEvents();

        this.addSoundEvent(332, SoundEvent.POINTED_DRIPSTONE_CAULDRON_DRIP_LAVA);
        this.addSoundEvent(333, SoundEvent.POINTED_DRIPSTONE_CAULDRON_DRIP_WATER);
        this.addSoundEvent(334, SoundEvent.POINTED_DRIPSTONE_DRIP_LAVA);
        this.addSoundEvent(335, SoundEvent.POINTED_DRIPSTONE_DRIP_WATER);
        this.addSoundEvent(336, SoundEvent.CAVE_VINES_PICK_BERRIES);
        this.addSoundEvent(337, SoundEvent.BIG_DRIPLEAF_TILT_DOWN);
        this.addSoundEvent(338, SoundEvent.BIG_DRIPLEAF_TILT_UP);
        this.addSoundEvent(339, SoundEvent.UNDEFINED);
    }

    @Override
    public ItemData readItemInstance(ByteBuf buffer, BedrockSession session) {
        int id = VarInts.readInt(buffer);
        if (id == 0) {
            // We don't need to read anything extra.
            return ItemData.AIR;
        }
        int count = buffer.readUnsignedShortLE();
        int damage = VarInts.readUnsignedInt(buffer);
        int blockRuntimeId = VarInts.readInt(buffer);

        NbtMap compoundTag = null;
        long blockingTicks = 0;
        String[] canPlace;
        String[] canBreak;

        ByteBuf buf = buffer.readSlice(VarInts.readUnsignedInt(buffer));
        try (LittleEndianByteBufInputStream stream = new LittleEndianByteBufInputStream(buf);
             NBTInputStream nbtStream = new NBTInputStream(stream)) {
            int nbtSize = stream.readShort();

            if (nbtSize > 0) {
                compoundTag = (NbtMap) nbtStream.readTag();
            } else if (nbtSize == -1) {
                int tagCount = stream.readUnsignedByte();
                if (tagCount != 1) throw new IllegalArgumentException("Expected 1 tag but got " + tagCount);
                compoundTag = (NbtMap) nbtStream.readTag();
            }

            canPlace = new String[stream.readInt()];
            for (int i = 0; i < canPlace.length; i++) {
                canPlace[i] = stream.readUTF();
            }
            canBreak = new String[stream.readInt()];
            for (int i = 0; i < canBreak.length; i++) {
                canBreak[i] = stream.readUTF();
            }

            if (this.isBlockingItem(id, session.getHardcodedBlockingId().get())) {
                blockingTicks = stream.readLong();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read item user data", e);
        }

        if (buf.isReadable()) {
            log.info("Item user data has {} readable bytes left\n{}", buf.readableBytes(), ByteBufUtil.prettyHexDump(buf.readerIndex(0)));
        }

        return ItemData.builder()
                .id(id)
                .damage(damage)
                .count(count)
                .tag(compoundTag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockingTicks(blockingTicks)
                .blockRuntimeId(blockRuntimeId)
                .build();
    }

    @Override
    public ItemData readItem(ByteBuf buffer, BedrockSession session) {
        int id = VarInts.readInt(buffer);
        if (id == 0) {
            // We don't need to read anything extra.
            return ItemData.AIR;
        }
        int count = buffer.readUnsignedShortLE();
        int damage = VarInts.readUnsignedInt(buffer);

        boolean hasNetId = buffer.readBoolean();
        int netId = 0;
        if (hasNetId) {
            netId = VarInts.readInt(buffer);
        }

        int blockRuntimeId = VarInts.readInt(buffer);

        NbtMap compoundTag = null;
        long blockingTicks = 0;
        String[] canPlace;
        String[] canBreak;

        ByteBuf buf = buffer.readSlice(VarInts.readUnsignedInt(buffer));
        try (LittleEndianByteBufInputStream stream = new LittleEndianByteBufInputStream(buf);
             NBTInputStream nbtStream = new NBTInputStream(stream)) {
            int nbtSize = stream.readShort();

            if (nbtSize > 0) {
                compoundTag = (NbtMap) nbtStream.readTag();
            } else if (nbtSize == -1) {
                int tagCount = stream.readUnsignedByte();
                if (tagCount != 1) throw new IllegalArgumentException("Expected 1 tag but got " + tagCount);
                compoundTag = (NbtMap) nbtStream.readTag();
            }

            canPlace = new String[stream.readInt()];
            for (int i = 0; i < canPlace.length; i++) {
                canPlace[i] = stream.readUTF();
            }
            canBreak = new String[stream.readInt()];
            for (int i = 0; i < canBreak.length; i++) {
                canBreak[i] = stream.readUTF();
            }

            if (this.isBlockingItem(id, session.getHardcodedBlockingId().get())) {
                blockingTicks = stream.readLong();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read item user data", e);
        }

        if (buf.isReadable()) {
            log.info("Item user data has {} readable bytes left\n{}", buf.readableBytes(), ByteBufUtil.prettyHexDump(buf.readerIndex(0)));
        }

        return ItemData.builder()
                .id(id)
                .damage(damage)
                .count(count)
                .tag(compoundTag)
                .canPlace(canPlace)
                .canBreak(canBreak)
                .blockingTicks(blockingTicks)
                .blockRuntimeId(blockRuntimeId)
                .usingNetId(hasNetId)
                .netId(netId)
                .build();
    }

    @Override
    public ItemData readNetItem(ByteBuf buffer, BedrockSession session) {
        return this.readItem(buffer, session);
    }

    @Override
    public void writeItemInstance(ByteBuf buffer, ItemData item, BedrockSession session) {
        requireNonNull(item, "item is null!");

        // Write id
        int id = item.getId();
        if (id == 0) {
            // We don't need to write anything extra.
            buffer.writeByte(0);
            return;
        }
        VarInts.writeInt(buffer, id);

        // Write damage and count
        buffer.writeShortLE(item.getCount());
        VarInts.writeUnsignedInt(buffer, item.getDamage());
        VarInts.writeInt(buffer, item.getBlockRuntimeId());

        ByteBuf userDataBuf = ByteBufAllocator.DEFAULT.ioBuffer();
        try (LittleEndianByteBufOutputStream stream = new LittleEndianByteBufOutputStream(userDataBuf);
             NBTOutputStream nbtStream = new NBTOutputStream(stream)) {
            if (item.getTag() != null) {
                stream.writeShort(-1);
                stream.writeByte(1); // Hardcoded in current version
                nbtStream.writeTag(item.getTag());
            } else {
                userDataBuf.writeShortLE(0);
            }

            String[] canPlace = item.getCanPlace();
            stream.writeInt(canPlace.length);
            for (String aCanPlace : canPlace) {
                stream.writeUTF(aCanPlace);
            }

            String[] canBreak = item.getCanBreak();
            stream.writeInt(canBreak.length);
            for (String aCanBreak : canBreak) {
                stream.writeUTF(aCanBreak);
            }

            if (this.isBlockingItem(id, session.getHardcodedBlockingId().get())) {
                stream.writeLong(item.getBlockingTicks());
            }

            VarInts.writeUnsignedInt(buffer, userDataBuf.readableBytes());
            buffer.writeBytes(userDataBuf);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write item user data", e);
        } finally {
            userDataBuf.release();
        }
    }

    @Override
    public void writeItem(ByteBuf buffer, ItemData item, BedrockSession session) {
        requireNonNull(item, "item is null!");

        // Write id
        int id = item.getId();
        if (id == 0) {
            // We don't need to write anything extra.
            buffer.writeByte(0);
            return;
        }
        VarInts.writeInt(buffer, id);

        // Write damage and count
        buffer.writeShortLE(item.getCount());
        VarInts.writeUnsignedInt(buffer, item.getDamage());

        buffer.writeBoolean(item.isUsingNetId());
        if (item.isUsingNetId()) {
            VarInts.writeInt(buffer, item.getNetId());
        }

        VarInts.writeInt(buffer, item.getBlockRuntimeId());

        ByteBuf userDataBuf = ByteBufAllocator.DEFAULT.ioBuffer();
        try (LittleEndianByteBufOutputStream stream = new LittleEndianByteBufOutputStream(userDataBuf);
             NBTOutputStream nbtStream = new NBTOutputStream(stream)) {
            if (item.getTag() != null) {
                stream.writeShort(-1);
                stream.writeByte(1); // Hardcoded in current version
                nbtStream.writeTag(item.getTag());
            } else {
                userDataBuf.writeShortLE(0);
            }

            String[] canPlace = item.getCanPlace();
            stream.writeInt(canPlace.length);
            for (String aCanPlace : canPlace) {
                stream.writeUTF(aCanPlace);
            }

            String[] canBreak = item.getCanBreak();
            stream.writeInt(canBreak.length);
            for (String aCanBreak : canBreak) {
                stream.writeUTF(aCanBreak);
            }

            if (this.isBlockingItem(id, session.getHardcodedBlockingId().get())) {
                stream.writeLong(item.getBlockingTicks());
            }

            VarInts.writeUnsignedInt(buffer, userDataBuf.readableBytes());
            buffer.writeBytes(userDataBuf);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write item user data", e);
        } finally {
            userDataBuf.release();
        }
    }

    @Override
    public void writeNetItem(ByteBuf buffer, ItemData item, BedrockSession session) {
        this.writeItem(buffer, item, session);
    }

    @Override
    public boolean readInventoryActions(ByteBuf buffer, BedrockSession session, List<InventoryActionData> actions) {
        this.readArray(buffer, actions, session, (buf, helper, aSession) -> {
            InventorySource source = helper.readSource(buf);
            int slot = VarInts.readUnsignedInt(buf);
            ItemData fromItem = helper.readItem(buf, aSession);
            ItemData toItem = helper.readItem(buf, aSession);

            return new InventoryActionData(source, slot, fromItem, toItem);
        });
        return false;
    }

    @Override
    public void writeInventoryActions(ByteBuf buffer, BedrockSession session, List<InventoryActionData> actions,
                                      boolean hasNetworkIds) {
        this.writeArray(buffer, actions, session, (buf, helper, aSession, action) -> {
            helper.writeSource(buf, action.getSource());
            VarInts.writeUnsignedInt(buf, action.getSlot());
            helper.writeItem(buf, action.getFromItem(), aSession);
            helper.writeItem(buf, action.getToItem(), aSession);
        });
    }

    @Override
    protected StackRequestActionData readRequestActionData(ByteBuf byteBuf, StackRequestActionType type, BedrockSession session) {
        if (type == StackRequestActionType.CRAFT_RESULTS_DEPRECATED) {
            return new CraftResultsDeprecatedStackRequestActionData(
                    this.readArray(byteBuf, new ItemData[0], buf -> this.readItemInstance(buf, session)),
                    byteBuf.readByte()
            );
        } else {
            return super.readRequestActionData(byteBuf, type, session);
        }
    }

    @Override
    protected void writeRequestActionData(ByteBuf byteBuf, StackRequestActionData action, BedrockSession session) {
        if (action.getType() == StackRequestActionType.CRAFT_RESULTS_DEPRECATED) {
            this.writeArray(byteBuf, ((CraftResultsDeprecatedStackRequestActionData) action).getResultItems(), (buf2, item) -> this.writeItemInstance(buf2, item, session));
            byteBuf.writeByte(((CraftResultsDeprecatedStackRequestActionData) action).getTimesCrafted());
        } else {
            super.writeRequestActionData(byteBuf, action, session);
        }
    }
}
