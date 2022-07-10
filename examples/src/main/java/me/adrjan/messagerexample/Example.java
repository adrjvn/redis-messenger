package me.adrjan.messagerexample;

import me.adrjan.messagerexample.listener.ExampleListener;
import me.adrjan.messagerexample.packet.ExampleAnnotationPacket;
import me.adrjan.messagerexample.packet.ExamplePacket;
import me.adrjan.messenger.Messenger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Example {

    private final Messenger messenger;

    public Example() {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.messenger = new Messenger("test", "redis://127.0.0.1:6379")
                .withCustomAsyncExecutor(executorService::execute);
    }

    public void enable() {
        this.messenger.registerListener(new ExampleListener());

        this.messenger.publish(this.messenger.getClient(), false, new ExamplePacket(2137));
        this.messenger.publish("cj", new ExampleAnnotationPacket("example"));
        this.messenger.publish(true, new ExampleAnnotationPacket("B)"));
        this.messenger.publish(new ExampleAnnotationPacket("B)"));

        this.messenger.unregisterListenerByPacket(ExamplePacket.class);
        this.messenger.unregisterListener(ExampleListener.class);
    }
}