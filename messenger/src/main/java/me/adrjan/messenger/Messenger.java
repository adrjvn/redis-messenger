package me.adrjan.messenger;

import lombok.Getter;
import me.adrjan.messenger.packet.Packet;
import me.adrjan.messenger.packet.PacketInfo;
import me.adrjan.messenger.packet.listener.PacketHandler;
import me.adrjan.messenger.packet.listener.PacketListener;
import me.adrjan.messenger.packet.listener.PacketListenerWrapper;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Messenger {

    @Getter
    private final String client;
    private final RedissonClient redissonClient;

    protected final Map<Class<? extends PacketListener>, Set<PacketListenerWrapper>> packetListenerCache = new ConcurrentHashMap<>();

    private Consumer<Runnable> asyncExercutor;
    private Consumer<Runnable> syncExecutor;

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

    public Messenger withCustomAsyncExecutor(Consumer<Runnable> executor) {
        this.asyncExercutor = executor;
        return this;
    }

    public Messenger withSyncExecutor(Consumer<Runnable> executor) {
        this.syncExecutor = executor;
        return this;
    }

    //Packet class must contains PacketInfo annotation
    public void publish(Packet packet) {
        if (!packet.getClass().isAnnotationPresent(PacketInfo.class))
            throw new RuntimeException("Packet class " + packet.getClass().getSimpleName() + " does not contains PacketInfo annotation!");
        PacketInfo packetInfo = packet.getClass().getAnnotation(PacketInfo.class);
        this.publish(packetInfo.channel(), packetInfo.async(), packet);
    }

    //Packet class must contains PacketInfo annotatio
    public void publish(boolean async, Packet packet) {
        if (!packet.getClass().isAnnotationPresent(PacketInfo.class))
            throw new RuntimeException("Packet class " + packet.getClass().getSimpleName() + " does not contains PacketInfo annotation!");
        PacketInfo packetInfo = packet.getClass().getAnnotation(PacketInfo.class);
        this.publish(packetInfo.channel(), async, packet);
    }

    public void publish(String channel, boolean async, Packet packet) {
        packet.setClientSender(this.client);
        final String ch = channel.equalsIgnoreCase("self") ? this.client : channel;
        if (!async) {
            this.redissonClient.getTopic(ch).publish(packet);
            return;
        }
        if (asyncExercutor == null) this.redissonClient.getTopic(ch).publishAsync(packet);
        else this.asyncExercutor.accept(() -> this.redissonClient.getTopic(ch).publish(packet));
    }

    public void registerListeners(PacketListener... listeners) {
        Arrays.stream(listeners).forEach(this::registerListener);
    }

    public void registerListener(PacketListener packetListener) {
        Arrays.stream(packetListener.getClass().getMethods())
                .filter(method -> method.isAnnotationPresent(PacketHandler.class))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> Packet.class.isAssignableFrom(method.getParameterTypes()[0]))
                .forEach(method -> {
                    PacketHandler packetHandler = method.getAnnotation(PacketHandler.class);
                    Class<? extends Packet> packetParameter = (Class<? extends Packet>) method.getParameterTypes()[0];
                    Optional<String> optionalChannel = resolveListenerChannel(packetHandler, packetParameter);
                    if (optionalChannel.isEmpty()) return;
                    String channel = optionalChannel.get().equalsIgnoreCase("self") ? this.client : optionalChannel.get();
                    PacketListenerWrapper packetListenerWrapper = new PacketListenerWrapper(channel, packetListener, packetParameter, method, packetHandler.handleSync() ? this.syncExecutor : null);
                    this.redissonClient.getTopic(channel).addListener(Packet.class, packetListenerWrapper);
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

    public void shutdown() {
        this.shoutdown(0, 2, TimeUnit.SECONDS);
    }

    public void shoutdown(long quietPeriod, long timeout, TimeUnit unit) {
        this.packetListenerCache.clear();
        this.redissonClient.shutdown(quietPeriod, timeout, unit);
    }

    protected Optional<String> resolveListenerChannel(PacketHandler destiny, Class<? extends Packet> parameter) {
        if (!destiny.channel().isEmpty())
            return Optional.of(destiny.channel());
        if (parameter.isAnnotationPresent(PacketInfo.class))
            return Optional.of(parameter.getAnnotation(PacketInfo.class).channel());
        return Optional.empty();
    }
}