package com.ajmarcos.multiprobador;

import android.util.Log;
import java.io.IOException;

public class Ping extends Thread {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private String ipAddress;
    private PingListener listener;
    private int maxRetries; // Número máximo de intentos fallidos permitidos

    public interface PingListener {
        void onPingResult(boolean success, String message);
    }

    public Ping(String ipAddress, int maxRetries) {
        this.ipAddress = ipAddress;
        this.maxRetries = maxRetries;
        Log.d(TAG, CLASS + " → Ping creado para IP: " + ipAddress + " con máximo de intentos: " + maxRetries);
    }

    public void setPingListener(PingListener listener) {
        this.listener = listener;
        Log.d(TAG, CLASS + " → Listener configurado.");
    }

    @Override
    public void run() {
        Log.d(TAG, CLASS + " → Inicio de ping a " + ipAddress);
        int failedAttempts = 0;
        boolean success = false;

        for (int i = 1; i <= maxRetries; i++) {
            Log.d(TAG, CLASS + " → Intento #" + i + " de ping a " + ipAddress);

            try {
                Process process = Runtime.getRuntime().exec("ping -c 1 " + ipAddress);
                int returnCode = process.waitFor();

                if (returnCode == 0) {
                    success = true;
                    Log.d(TAG, CLASS + " → Ping exitoso en intento " + i);

                    if (listener != null) {
                        listener.onPingResult(true, "Ping exitoso a " + ipAddress + " en el intento " + i);
                    }
                    break;
                } else {
                    failedAttempts++;
                    Log.d(TAG, CLASS + " → Ping fallido en intento " + i + " (código retorno: " + returnCode + ")");
                }

            } catch (IOException | InterruptedException e) {
                failedAttempts++;
                Log.e(TAG, CLASS + " → Error durante el ping en intento " + i + ": " + e.getMessage());
            }

            // Esperar un momento entre intentos
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.e(TAG, CLASS + " → Hilo interrumpido durante la espera entre intentos.");
                break;
            }
        }

        if (!success && failedAttempts >= maxRetries) {
            Log.d(TAG, CLASS + " → No se pudo hacer ping a " + ipAddress + " después de " + maxRetries + " intentos.");
            if (listener != null) {
                listener.onPingResult(false, "No se pudo hacer ping a " + ipAddress + " después de " + maxRetries + " intentos.");
            }
        } else {
            Log.d(TAG, CLASS + " → Finalizado con éxito: " + success);
        }
    }
}

