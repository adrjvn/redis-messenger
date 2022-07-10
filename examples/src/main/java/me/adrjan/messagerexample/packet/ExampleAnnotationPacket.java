package me.adrjan.messagerexample.packet;


import lombok.AllArgsConstructor;
import lombok.Getter;
import me.adrjan.messenger.packet.Packet;
import me.adrjan.messenger.packet.PacketInfo;

@Getter
@AllArgsConstructor
@PacketInfo(channel = "cj", async = true)
public class ExampleAnnotationPacket extends Packet {

    private final String message;
}