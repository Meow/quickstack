package dev.luna.quickstack.client.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.luna.quickstack.client.Action;

/** Outcome of a request: how many items were moved and how many containers took part. */
public record ResultPayload(Action action, byte status, int items, int containers) implements CustomPacketPayload {
	public static final Type<ResultPayload> TYPE = new Type<>(Protocol.RESULT);
	public static final StreamCodec<FriendlyByteBuf, ResultPayload> CODEC =
			CustomPacketPayload.codec(ResultPayload::write, ResultPayload::read);

	private static ResultPayload read(FriendlyByteBuf buffer) {
		ResultPayload result = new ResultPayload(
				Action.byId(buffer.readByte()), buffer.readByte(), buffer.readInt(), buffer.readInt());
		// Tolerate fields added by newer servers.
		buffer.skipBytes(buffer.readableBytes());
		return result;
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeByte(action.id());
		buffer.writeByte(status);
		buffer.writeInt(items);
		buffer.writeInt(containers);
	}

	@Override
	public Type<ResultPayload> type() {
		return TYPE;
	}
}
