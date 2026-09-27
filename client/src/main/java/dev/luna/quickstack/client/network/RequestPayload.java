package dev.luna.quickstack.client.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import dev.luna.quickstack.client.Action;

/** Asks the server to run an action on the player inventory slots set in the mask. */
public record RequestPayload(Action action, long slotMask) implements CustomPacketPayload {
	public static final Type<RequestPayload> TYPE = new Type<>(Protocol.REQUEST);
	public static final StreamCodec<FriendlyByteBuf, RequestPayload> CODEC =
			CustomPacketPayload.codec(RequestPayload::write, RequestPayload::read);

	private static RequestPayload read(FriendlyByteBuf buffer) {
		buffer.readByte();
		return new RequestPayload(Action.byId(buffer.readByte()), buffer.readLong());
	}

	private void write(FriendlyByteBuf buffer) {
		buffer.writeByte(Protocol.VERSION);
		buffer.writeByte(action.id());
		buffer.writeLong(slotMask);
	}

	@Override
	public Type<RequestPayload> type() {
		return TYPE;
	}
}
