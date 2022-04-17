package me.adrjan.messagerexample.listener;

import me.adrjan.messagerexample.packet.ExampleAnnotationPacket;
import me.adrjan.messagerexample.packet.ExamplePacket;
import me.adrjan.messenger.packet.listener.PacketHandler;
import me.adrjan.messenger.packet.listener.PacketListener;

public class ExampleListener implements PacketListener {

    @PacketHandler(channel = "self")
    public void onExamplePacket(ExamplePacket packet) {
        System.out.println(packet.getAmount());
    }

    @PacketHandler
    public void onExampleAnnotationPacket(ExampleAnnotationPacket packet){
        System.out.println(packet.getMessage());
    }
}