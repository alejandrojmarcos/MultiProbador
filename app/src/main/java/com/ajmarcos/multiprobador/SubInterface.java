package com.ajmarcos.multiprobador;

public class SubInterface {
    private final String ip;
    private final String nombre; // eth1.0, eth2.0, etc.

    public SubInterface(String ip, String nombre) {
        this.ip = ip;
        this.nombre = nombre;
    }

    public String getIp() {
        return ip;
    }

    public String getNombre() {
        return nombre;
    }
}
