# Redis Messenger

A simple and efficient Redis-based messenger library for inter-application communication. This library provides a straightforward way to send and receive packet-based messages across different services using Redis Pub/Sub.

## Features

*   **Redis Pub/Sub**: Leverages Redis for high-performance message broadcasting.
*   **Packet-based Communication**: Define your own `Packet` classes for structured data exchange.
*   **Annotation-driven Listeners**: Easily register and handle incoming packets using annotations.
*   **Asynchronous & Synchronous Handling**: Control how packets are processed, either asynchronously or synchronously.
*   **Custom Executors**: Integrate with your existing thread management by providing custom executors for packet handling.

## Usage

### 1. Initialize the Messenger

First, create an instance of the `Messenger` class, providing a unique client identifier and your Redis URL or configuration.

```java
import me.adrjan.messenger.Messenger;
import org.redisson.config.Config;

public class MyService {

    private Messenger messenger;

    public MyService() {
        // Using a Redis URL
        messenger = new Messenger("my-service-client-id", "redis://127.0.0.1:6379");

        // Or using a Redisson Config object
        // Config config = new Config();
        // config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        // messenger = new Messenger("my-service-client-id", config);

        // Optional: Configure custom executors for async/sync tasks
        messenger.withCustomAsyncExecutor(runnable -> new Thread(runnable).start()); // Example: new thread for async
        messenger.withSyncExecutor(Runnable::run); // Example: run sync tasks directly
    }
}
```

### 2. Define a Packet

Create a class that extends `Packet` and annotate it with `@PacketInfo` to specify its default channel and whether it should be handled asynchronously by default.

```java
import me.adrjan.messenger.packet.Packet;
import me.adrjan.messenger.packet.PacketInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@PacketInfo(channel = "chat-channel", async = true) // Default channel and async handling
public class ChatMessagePacket extends Packet {
    private String sender;
    private String message;
}
```

### 3. Create a Packet Listener

Implement the `PacketListener` interface and define methods annotated with `@PacketHandler`. The method should take a single `Packet` type as an argument. You can specify a custom channel or force synchronous handling.

```java
import me.adrjan.messenger.packet.listener.PacketHandler;
import me.adrjan.messenger.packet.listener.PacketListener;

public class MyPacketListener implements PacketListener {

    @PacketHandler // Uses default channel and async setting from ChatMessagePacket's @PacketInfo
    public void onChatMessage(ChatMessagePacket packet) {
        System.out.println("Received chat message from " + packet.getClientSender() + " (" + packet.getSender() + "): " + packet.getMessage());
    }

    @PacketHandler(channel = "admin-channel", handleSync = true) // Custom channel and force synchronous handling
    public void onAdminMessage(AdminMessagePacket packet) {
        System.out.println("Received admin message on 'admin-channel' from " + packet.getClientSender() + ": " + packet.getCommand());
    }
}
```

### 4. Register and Unregister Listeners

Register your listener with the `Messenger` instance.

```java
// In your MyService class or similar
public void setupListeners() {
    messenger.registerListener(new MyPacketListener());
    // You can register multiple listeners at once
    // messenger.registerListeners(new MyPacketListener(), new AnotherListener());
}

public void shutdown() {
    // Unregister a specific listener class
    messenger.unregisterListener(MyPacketListener.class);
    // Or unregister all listeners for a specific packet type
    // messenger.unregisterListenerByPacket(ChatMessagePacket.class);

    messenger.shutdown(); // Shuts down the Redisson client
}
```

### 5. Publish Packets

Send packets to other services.

```java
// In your MyService class or similar
public void sendChatMessage(String sender, String message) {
    ChatMessagePacket packet = new ChatMessagePacket(sender, message);
    messenger.publish(packet); // Publishes to "chat-channel" as async (default from @PacketInfo)
}

public void sendAdminCommand(String command) {
    AdminMessagePacket packet = new AdminMessagePacket(command);
    // You can override the default async setting
    messenger.publish(false, packet); // Publishes to "admin-channel" as sync
    // Or specify channel and async explicitly
    // messenger.publish("admin-channel", false, packet);
}
```

### 6. Registering Handlers (Functional Approach)

For simpler cases, you can register a `Consumer` directly for a channel and packet type.

```java
// In your MyService class or similar
public void registerSimpleHandler() {
    messenger.registerHandler("status-updates", StatusPacket.class, packet -> {
        System.out.println("Received status update from " + packet.getClientSender() + ": " + packet.getStatus());
    });

    // Or if the packet has @PacketInfo, you can omit the channel
    messenger.registerHandler(AnotherPacket.class, packet -> {
        System.out.println("Received AnotherPacket from " + packet.getClientSender());
    });
}
```

## License

```
MIT License

Copyright (c) 2022 adrjvn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```