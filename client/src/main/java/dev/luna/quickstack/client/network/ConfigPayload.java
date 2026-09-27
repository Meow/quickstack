package dev.luna.quickstack.client.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Sent by the server once it notices the mod, tells us what is allowed for this player. */
public record ConfigPayload(int version, boolean quickStack, boolean restock, int radius) implements CustomPacketPayload {
	public static final Type<ConfigPayload> TYPE = new Type<>(Protocol.CONFIG);
	public static final StreamCodec<FriendlyByteBuf, ConfigPayload> CODEC =
			CustomPacketPayload.codec(ConfigPayload::write, ConfigPayload::read);

	private static ConfigPayload read(FriendlyByteBuf buffer) {
		int version = buffer.readInt();
		if (version != Protocol.VERSION) {
			// Unknown layout, all we can tell is that the versions differ.
			buffer.skipBytes(buffer.readableBytes());
			return new ConfigPayload(version, false, false, 0);
		}
		return new ConfigPayload(version, buffer.readBoolean(), buffer.readBoolean(), buffer.readInt());
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeInt(version);
		buffer.writeBoolean(quickStack);
		buffer.writeBoolean(restock);
		buffer.writeInt(radius);
	}

	@Override
	public Type<ConfigPayload> type() {
		return TYPE;
	}
}
