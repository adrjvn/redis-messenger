package me.adrjan.messagerexample;

import me.adrjan.messagerexample.listener.ExampleListener;
import me.adrjan.messagerexample.packet.ExampleAnnotationPacket;
import me.adrjan.messagerexample.packet.ExamplePacket;
import me.adrjan.messenger.Messenger;

public class Example {

    private final Messenger messenger;

    public Example() {
        this.messenger = new Messenger("test", "redis://127.0.0.1:6379");
    }

    public void enable() {
        this.messenger.registerListener(new ExampleListener());

        this.messenger.publish(this.messenger.getClient(), new ExamplePacket(2137));
        this.messenger.publish("cj", new ExampleAnnotationPacket("example"));

        this.messenger.unregisterListenerByPacket(ExamplePacket.class);
    }
}