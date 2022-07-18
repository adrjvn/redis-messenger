package me.adrjan.messenger.packet.listener;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
import me.adrjan.messenger.packet.Packet;
import org.redisson.api.listener.MessageListener;

import javax.xml.validation.Validator;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;

@AllArgsConstructor
public class PacketListenerWrapper implements MessageListener<Packet> {

    @Getter
    private final String channel;
    private final PacketListener instance;
    @Getter
    private final Class<? extends Packet> type;
    private final Method method;
    private final Consumer<Runnable> syncExecutor;

    @SneakyThrows
    @Override
    public void onMessage(CharSequence charSequence, Packet packet) {
        if (!packet.getClass().isAssignableFrom(this.type)) return;

        if (Objects.isNull(this.syncExecutor))
            this.method.invoke(this.instance, packet);
        else
            this.syncExecutor.accept(() -> {
                try {
                    this.method.invoke(this.instance, packet);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
    }
}