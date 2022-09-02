package me.adrjan.messagerexample;

import me.adrjan.messagerexample.packet.ExampleAnnotationPacket;
import me.adrjan.messagerexample.packet.ExamplePacket;
import me.adrjan.messenger.Messenger;
import me.adrjan.messenger.packet.listener.PacketHandler;
import me.adrjan.messenger.packet.listener.PacketListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExampleBootstrap {

    public static void main(String[] args) {
        new Example().enable();
    }

    public static class Example {

        private final Messenger messenger;

        public Example() {
            ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            this.messenger = new Messenger("test", "redis://127.0.0.1:6379")
                    .withCustomAsyncExecutor(executorService::execute);
        }

        public void enable() {
            this.messenger.registerListener(new ExampleListener());
            this.messenger.registerHandler(ExampleAnnotationPacket.class, exampleAnnotationPacket -> System.out.println(exampleAnnotationPacket.getMessage() + " OOOO"));

            this.messenger.publish(this.messenger.getClient(), false, new ExamplePacket(2137));
            this.messenger.publish(true, new ExampleAnnotationPacket("B)"));
            this.messenger.publish(new ExampleAnnotationPacket(":)"));

            //this.messenger.unregisterListenerByPacket(ExamplePacket.class);
            //this.messenger.unregisterListener(ExampleListener.class);
        }
    }

    public static class ExampleListener implements PacketListener {

        @PacketHandler(channel = "self", handleSync = true)
        public void onExamplePacket(ExamplePacket packet) {
            System.out.println(packet.getAmount());
        }

        @PacketHandler
        public void onExampleAnnotationPacket(ExampleAnnotationPacket packet) {
            System.out.println(packet.getMessage());
        }
    }

}