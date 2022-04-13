package me.adrjan.messenger.packet.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import me.adrjan.messenger.packet.Packet;
import org.redisson.api.listener.MessageListener;

import java.lang.reflect.Method;

@AllArgsConstructor
public class PacketListenerWrapper implements MessageListener<Packet> {

    @Getter
    private final String channel;
    private final PacketListener instance;
    @Getter
    private final Class<? extends Packet> type;
    private final Method method;

    @SneakyThrows
    @Override
    public void onMessage(CharSequence charSequence, Packet packet) {
        if (packet.getClass().isAssignableFrom(this.type))
            this.method.invoke(this.instance, packet);
    }
}