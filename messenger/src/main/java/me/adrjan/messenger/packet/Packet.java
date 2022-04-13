package me.adrjan.messenger.packet;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public abstract class Packet implements Serializable {

    private String clientSender;
}