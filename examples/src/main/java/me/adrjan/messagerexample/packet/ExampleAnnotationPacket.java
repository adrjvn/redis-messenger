package me.adrjan.messagerexample.packet;


import lombok.AllArgsConstructor;
import lombok.Getter;
import me.adrjan.messenger.packet.Packet;
import me.adrjan.messenger.packet.PacketChannel;

@Getter
@AllArgsConstructor
@PacketChannel(channel = "cj")
public class ExampleAnnotationPacket extends Packet {

    private final String message;
}