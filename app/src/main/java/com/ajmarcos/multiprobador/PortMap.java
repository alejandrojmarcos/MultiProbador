package com.ajmarcos.multiprobador;

import android.util.Log;

public class PortMap {

    public interface PortMappingListener {
        void onComplete(boolean success, String[] salida);
    }

    // Lista de subinterfaces para cada puerto
    private final SubInterface[] subInterfaces = new SubInterface[]{
            new SubInterface("192.168.1.241", "eth1.0"), // puerto 1
            new SubInterface("192.168.1.241", "eth2.0"), // puerto 2
            new SubInterface("192.168.1.241", "eth3.0"), // puerto 3
            new SubInterface("192.168.1.242", "eth1.0"), // puerto 4
            new SubInterface("192.168.1.242", "eth2.0"), // puerto 5
            new SubInterface("192.168.1.242", "eth3.0"), // puerto 6
            //
            new SubInterface("192.168.1.243", "eth1.0"), // puerto 7
            new SubInterface("192.168.1.243", "eth2.0")  // puerto 8
    };

    public SubInterface[] getSubInterfaces() {
        return subInterfaces;
    }


    public void apagarTodasLasIPs(PortMappingListener listener) {
        apagarInterfacesPorIp("192.168.1.241", (success, salida) -> {
            Log.d("PORTMAP", "IP .241 apagada");
            apagarInterfacesPorIp("192.168.1.242", (s2, salida2) -> {
                Log.d("PORTMAP", "IP .242 apagada");
                if (listener != null) listener.onComplete(true, new String[]{"Todas las interfaces apagadas"});
            });
        });
    }


    public void apagarInterfacesPorIp(String ip, PortMappingListener listener) {
        String[] comandos = {
                "sh",
                "ip link set eth1.0 down",
                "ip link set eth2.0 down",
                "ip link set eth3.0 down",
                "exit"
        };

        ejecutarSSH(ip, comandos, listener);
    }


    public void levantarSubinterfaz(String ip, String subInterfaz, PortMappingListener listener) {
        String[] comandos = {
                "sh",
                "ip link set " + subInterfaz + " up",
                "exit"
        };

        ejecutarSSH(ip, comandos, listener);
    }

    private void ejecutarSSH(String ip, String[] comandos, PortMappingListener listener) {
        Ssh ssh = new Ssh(ip, "Support", "Te2010An_2014Ma");
        ssh.setCommands(comandos);
        ssh.setSshListener((success, message, code) -> {
            if (!success) Log.e("PORTMAP", "Error SSH en " + ip + " code=" + code);
            if (listener != null) listener.onComplete(success, message);
        });
        ssh.start();
    }
}
