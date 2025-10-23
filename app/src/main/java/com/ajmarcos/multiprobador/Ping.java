package com.ajmarcos.multiprobador;

import java.io.IOException;

public class Ping extends Thread {

    private String ipAddress;
    private PingListener listener;
    private int maxRetries; // Número máximo de intentos fallidos permitidos

    public interface PingListener {
        void onPingResult(boolean success, String message);
    }

    public Ping(String ipAddress, int maxRetries) {
        this.ipAddress = ipAddress;
        this.maxRetries = maxRetries;
    }

    public void setPingListener(PingListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        int failedAttempts = 0; // Contador de intentos fallidos
        boolean success = false;

        for (int i = 1; i <= maxRetries; i++) {
            try {
                // Comando para hacer un solo ping
                Process process = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
                int returnCode = process.waitFor();

                if (returnCode == 0) {
                    success = true; // El ping fue exitoso
                    if (listener != null) {
                        listener.onPingResult(true, "Ping exitoso a " + ipAddress + " en el intento " + i);
                    }
                    break; // Salir del bucle al primer éxito
                } else {
                    failedAttempts++;
                }

            } catch (IOException | InterruptedException e) {
                failedAttempts++;
            }

            // Notificar progreso si es necesario
            if (listener != null) {
                //listener.onPingResult(false, "Ping fallido en el intento " + i + " a " + ipAddress);
            }

            // Esperar un momento entre intentos (opcional)
            try {
                Thread.sleep(1500); // 1 segundo entre intentos
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Notificar resultado final después de todos los intentos
        if (!success && failedAttempts >= maxRetries && listener != null) {
            listener.onPingResult(false, "No se pudo hacer ping a " + ipAddress + " después de " + maxRetries + " intentos.");
        }
    }
}

