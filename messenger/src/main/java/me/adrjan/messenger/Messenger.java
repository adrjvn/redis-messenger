package me.adrjan.messenger;

import lombok.Getter;
import me.adrjan.messenger.packet.Packet;
import me.adrjan.messenger.packet.PacketChannel;
import me.adrjan.messenger.packet.listener.PacketHandler;
import me.adrjan.messenger.packet.listener.PacketListener;
import me.adrjan.messenger.packet.listener.PacketListenerWrapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Messenger {

    @Getter
    private final String client;
    private final RedissonClient redissonClient;

    protected final Map<Class<? extends PacketListener>, Set<PacketListenerWrapper>> packetListenerCache = new ConcurrentHashMap<>();

    public Messenger(String client, String redisURL) {
        this.client = client;

        Config config = new Config();
        config.useSingleServer().setAddress(redisURL);
        this.redissonClient = Redisson.create(config);
    }

    public Messenger(String client, Config redisConfig) {
        this.client = client;
        this.redissonClient = Redisson.create(redisConfig);
    }

    //Packet class must contains PacketChannel annotation
    public void publish(Packet packet) {
        this.publish(false, packet);
    }

    //Packet class must contains PacketChannel annotation
    public void publish(boolean async, Packet packet) {
        if (packet.getClass().isAnnotationPresent(PacketChannel.class))
            throw new RuntimeException("Packet class " + packet.getClass().getSimpleName() + " does not contains PacketChannel annotation!");
        this.publish(packet.getClass().getAnnotation(PacketChannel.class).channel(), async, packet);
    }

    public void publish(String channel, Packet packet) {
        this.publish(channel, false, packet);
    }

    public void publish(String channel, boolean async, Packet packet) {
        packet.setClientSender(this.client);
        channel = channel.equalsIgnoreCase("self") ? this.client : channel;
        if (async) this.redissonClient.getTopic(channel).publishAsync(packet);
        else this.redissonClient.getTopic(channel).publish(packet);
    }

    public void registerListener(PacketListener packetListener) {
        Arrays.stream(packetListener.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(PacketHandler.class))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> Packet.class.isAssignableFrom(method.getParameterTypes()[0]))
                .forEach(method -> {
                    Class<? extends Packet> packetParameter = (Class<? extends Packet>) method.getParameterTypes()[0];
                    Optional<String> optionalChannel = resolveListenerChannel(method, packetParameter);
                    if (optionalChannel.isEmpty()) return;
                    String channel = optionalChannel.get().equalsIgnoreCase("self") ? this.client : optionalChannel.get();
                    PacketListenerWrapper packetListenerWrapper = new PacketListenerWrapper(channel, packetListener, packetParameter, method);
                    this.redissonClient.getTopic(channel).addListener(Packet.class,
                            packetListenerWrapper);
                    this.packetListenerCache.computeIfAbsent(packetListener.getClass(), set -> new HashSet<>()).add(packetListenerWrapper);
                    //System.out.println("Registered PacketListener -> " + packetListener.getClass().getSimpleName() + " -> " + method.getName() + " -> Channel: " + channel + " Packet: " + packetParameter.getSimpleName());
                });
    }

    public void unregisterListener(Class<? extends PacketListener> packetListenerClazz) {
        this.packetListenerCache.get(packetListenerClazz).forEach(packetListenerWrapper -> {
            this.redissonClient.getTopic(packetListenerWrapper.getChannel()).removeListener(packetListenerWrapper);
            //System.out.println("Unregistered PacketListener -> " + packetListenerClazz.getSimpleName() + " -> Channel: " + packetListenerWrapper.getChannel() + " Packet: " + packetListenerWrapper.getType().getSimpleName());
        });
        this.packetListenerCache.remove(packetListenerClazz);
    }

    public void unregisterListenerByPacket(Class<? extends Packet> packetClazz) {
        List<PacketListenerWrapper> toRemove = new ArrayList<>();
        this.packetListenerCache.values().forEach(set -> {
            set.stream()
                    .filter(handler -> handler.getType().isAssignableFrom(packetClazz))
                    .forEach(packetListenerWrapper -> {
                        this.redissonClient.getTopic(packetListenerWrapper.getChannel()).removeListener(packetListenerWrapper);
                        toRemove.add(packetListenerWrapper);
                        //System.out.println("Unregistered PacketListener -> Channel: " + packetListenerWrapper.getChannel() + " Packet: " + packetListenerWrapper.getType().getSimpleName());
                    });
            toRemove.forEach(set::remove);
        });
    }

    protected Optional<String> resolveListenerChannel(Method method, Class<? extends Packet> parameter) {
        PacketHandler destiny = method.getAnnotation(PacketHandler.class);
        if (!destiny.channel().isEmpty())
            return Optional.of(destiny.channel());
        if (parameter.isAnnotationPresent(PacketChannel.class))
            return Optional.of(parameter.getAnnotation(PacketChannel.class).channel());
        return Optional.empty();
    }
}