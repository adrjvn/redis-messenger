package me.adrjan.messagerexample.packet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.adrjan.messenger.packet.Packet;

@Getter
@AllArgsConstructor
public class ExamplePacket extends Packet {

    private final int amount;
}
