package com.ajmarcos.multiprobador;

import android.util.Log;

public class PortMap {

    public interface PortMappingListener {
        void onComplete(boolean success, String[] salida);
    }

    
    private final SubInterface[] subInterfaces = new SubInterface[]{
            new SubInterface("192.168.1.240", "eth1.0"), // puerto 1
            new SubInterface("192.168.1.240", "eth2.0"), // puerto 2
            new SubInterface("192.168.1.240", "eth3.0"), // puerto 3
            new SubInterface("192.168.1.241", "eth1.0"), // puerto 4
            new SubInterface("192.168.1.241", "eth2.0"), // puerto 5
            new SubInterface("192.168.1.241", "eth3.0"), // puerto 6
            new SubInterface("192.168.1.242", "eth1.0"), // puerto 7
            new SubInterface("192.168.1.242", "eth2.0")  // puerto 8
    };

    public SubInterface[] getSubInterfaces() {
        return subInterfaces;
    }

    /**
     * Apaga todas las interfaces de los tres routers (.240, .241, .242)
     */
    public void apagarTodasLasIPs(PortMappingListener listener) {
        String[] routers = {"192.168.1.240", "192.168.1.241", "192.168.1.242", "192.168.1.230"};
        final int[] completados = {0};

        for (String ip : routers) {
            if ("192.168.1.230".equals(ip)) {
                // Solo apagamos la subinterfaz eth3.0
                apagarSubinterfaz(ip, "eth3.0", (success, salida) -> {
                    Log.d("PORTMAP", "Subinterfaz eth3.0 de " + ip + " apagada");
                    synchronized (completados) {
                        completados[0]++;
                        if (completados[0] == routers.length && listener != null) {
                            listener.onComplete(true, new String[]{"Todas las interfaces apagadas"});
                        }
                    }
                });
            } else {
                // Para las demás IPs apagamos todas las subinterfaces
                apagarInterfacesPorIp(ip, (success, salida) -> {
                    Log.d("PORTMAP", "Interfaces de " + ip + " apagadas");
                    synchronized (completados) {
                        completados[0]++;
                        if (completados[0] == routers.length && listener != null) {
                            listener.onComplete(true, new String[]{"Todas las interfaces apagadas"});
                        }
                    }
                });
            }
        }
    }


    /**
     * Apaga todas las subinterfaces de una IP (eth1.0, eth2.0, eth3.0)
     */
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

    /**
     * Apaga una sola subinterfaz, por ejemplo eth2.0
     */
    public void apagarSubinterfaz(String ip, String subInterfaz, PortMappingListener listener) {
        String[] comandos = {
                "sh",
                "ip link set " + subInterfaz + " down",
                "exit"
        };
        ejecutarSSH(ip, comandos, listener);
    }

    /**
     * Levanta una subinterfaz específica
     */
    public void levantarSubinterfaz(String ip, String subInterfaz, PortMappingListener listener) {
        String[] comandos = {
                "sh",
                "ip link set " + subInterfaz + " up",
                "exit"
        };
        ejecutarSSH(ip, comandos, listener);
    }

    /**
     * Ejecución SSH genérica
     */
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
