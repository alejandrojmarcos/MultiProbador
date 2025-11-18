package com.ajmarcos.multiprobador;

import android.util.Log;

public class PortMap {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

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

    private String ultimaInterfazLevantada = null;
    private String ultimaIPLevanta = null;

    public SubInterface[] getSubInterfaces() {
        return subInterfaces;
    }

    // ---------------------------------------------------------
    // Apagar todas las IPs de forma secuencial
    // ---------------------------------------------------------
    public void apagarTodasLasIPs(PortMappingListener listener) {
        Log.d(TAG, CLASS + " → Iniciando apagado de *todas* las IPs...");

        String[] routers = {"192.168.1.240", "192.168.1.241", "192.168.1.242"};
        apagarRoutersSecuencial(routers, 0, listener);
    }

    private void apagarRoutersSecuencial(String[] routers, int index, PortMappingListener listener) {
        if (index >= routers.length) {
            if (listener != null) listener.onComplete(true, new String[]{"Todas las interfaces apagadas"});
            return;
        }

        String ip = routers[index];
        if ("192.168.1.230".equals(ip)) {
            Log.d(TAG, "\n══════════ ROUTER " + ip + " → APAGAR eth3.0 ══════════");
            apagarSubinterfaz(ip, "eth3.0", (success, salida) -> {
                Log.d(TAG, CLASS + " → Finalizado apagado en " + ip);
                apagarRoutersSecuencial(routers, index + 1, listener);
            });
        } else {
            Log.d(TAG, "\n══════════ ROUTER " + ip + " → APAGAR TODAS LAS INTERFACES ══════════");
            apagarInterfacesPorIp(ip, (success, salida) -> {
                Log.d(TAG, CLASS + " → Finalizado apagado en " + ip);
                apagarRoutersSecuencial(routers, index + 1, listener);
            });
        }
    }

    // ---------------------------------------------------------
    // Apagar subinterfaces completas
    // ---------------------------------------------------------
    public void apagarInterfacesPorIp(String ip, PortMappingListener listener) {
        Log.d(TAG, CLASS + " → Preparando comandos de apagado para " + ip);
        String[] comandos = {
                "ip link set eth1.0 down",
                "ip link set eth2.0 down",
                "ip link set eth3.0 down"
        };
        ejecutarSSHInteractivo(ip, comandos, listener);
    }

    // ---------------------------------------------------------
    // Apagar única subinterfaz
    // ---------------------------------------------------------
    public void apagarSubinterfaz(String ip, String subInterfaz, PortMappingListener listener) {
        Log.d(TAG, CLASS + " → Apagando subinterfaz " + subInterfaz + " en " + ip);
        String[] comandos = {"ip link set " + subInterfaz + " down"};
        ejecutarSSHInteractivo(ip, comandos, listener);
    }

    // ---------------------------------------------------------
    // Levantar subinterfaz (Versión corregida: solo levanta la interfaz)
    // ---------------------------------------------------------
    public void levantarSubinterfaz(String routerIP, String subInterfaz, PortMappingListener listener) {
        Log.d(TAG, CLASS + " → Levantando SOLAMENTE subinterfaz " + subInterfaz + " en " + routerIP);
        ultimaInterfazLevantada = subInterfaz;
        ultimaIPLevanta = routerIP;

        // Paso 1: levantar subinterfaz en router
        String[] comandosLevantar = {"ip link set " + subInterfaz + " up; ip neigh flush all"};
        ejecutarSSHInteractivo(routerIP, comandosLevantar, listener);
    }

    // ---------------------------------------------------------
    // APAGAR ÚLTIMA INTERFAZ
    // ---------------------------------------------------------
    public void apagarUltimaInterfaz(PortMappingListener listener) {
        if (ultimaInterfazLevantada == null || ultimaIPLevanta == null) {
            Log.d(TAG, CLASS + " → No hay última interfaz registrada para apagar.");
            if (listener != null) listener.onComplete(true, new String[]{"Nada que apagar"});
            return;
        }
        String ip = ultimaIPLevanta;
        String subif = ultimaInterfazLevantada;
        ultimaInterfazLevantada = null;
        ultimaIPLevanta = null;

        Log.d(TAG, CLASS + " → Apagando ULTIMA interfaz usada: " + subif + " en " + ip);
        String[] comandos = {"ip link set " + subif + " down"};
        ejecutarSSHInteractivo(ip, comandos, listener);
    }

    // ---------------------------------------------------------
    // Ejecuta comandos usando SshInteractivo
    // ---------------------------------------------------------
    private void ejecutarSSHInteractivo(String ip, String[] comandos, PortMappingListener listener) {
        SshInteractivo ssh = new SshInteractivo(ip, "Support", "Te2010An_2014Ma");
        ssh.setCommands(comandos);
        ssh.setSshListener((success, salida, code) -> {
            if (listener != null) listener.onComplete(success, salida);
        });
        ssh.start();
    }
}