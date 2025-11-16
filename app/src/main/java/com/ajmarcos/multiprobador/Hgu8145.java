package com.ajmarcos.multiprobador;

import android.util.Log;
// Necesitamos importar el Listener y el resultado

public class Hgu8145 {

    public interface Hgu8145Listener {
        void onHgu8145Result(boolean success, Resultado res, int code);
    }

    private String TAG = "Hgu8145";
    private Hgu8145Listener listener;
    private String model = "";
    private String multi = "";
    private String catal = "";

    public void setHgu8145Listener(Hgu8145Listener listener) { this.listener = listener; }
    public void setModel(String model) { this.model = model; }
    public void setMulti(String multi) { this.multi = multi; }
    public void setCatal(String catal) { this.catal = catal; }

    /**
     * Realiza el scraping vía SSH anidado para el HGU 8145.
     */
    public void scrap8145Ssh(String routerIp) {

        Resultado resultadoSsh = new Resultado();
        resultadoSsh.setModelo(model);
        resultadoSsh.setMultiprobador(multi);
        resultadoSsh.setCatalogo(catal);

        // 1. Definir la secuencia de comandos real
        String[] scrapCommands = new String[]{
                "ssh -o StrictHostKeyChecking=no Support@192.168.1.1",
                "show ont info",
                "show wlan config",
                "show tx power", // Ejemplo de 5 comandos
                "show rx power",
                "exit"
        };

        // 2. Ejecutar SSH
        SshInteractivo ssh = new SshInteractivo(routerIp, "Support", "Te2010An_2014Ma");
        ssh.setCommands(scrapCommands);

        ssh.setSshListener((success, message, code) -> {
            if (success && message != null) {
                // Lógica de Extracción de datos del array 'message'
                for (String line : message) {
                    if (line == null) continue;

                    if (line.contains("Serial Number:")) {
                        // Ejemplo de parsing (necesitarás una función auxiliar para extraer el valor)
                        resultadoSsh.setSerial(extractValue(line, "Serial Number:"));
                    } else if (line.contains("Firmware Version:")) {
                        resultadoSsh.setFirmware(extractValue(line, "Firmware Version:"));
                    } else if (line.contains("Tx Power:")) {
                        resultadoSsh.setPotencia(extractValue(line, "Tx Power:"));
                    }
                    // Implementar lógica para SSID2, SSID5, Canal, etc.

                }

                // Finaliza exitosamente
                if (listener != null) listener.onHgu8145Result(true, resultadoSsh, 0);

            } else {
                Log.e(TAG, "Fallo total en SSH Scraping.");
                // Finaliza con error
                if (listener != null) listener.onHgu8145Result(false, resultadoSsh, code);
            }
        });
        ssh.start();
    }

    // Función utilitaria para simplificar la extracción de valores clave:valor
    private String extractValue(String line, String key) {
        try {
            return line.substring(line.indexOf(key) + key.length()).trim().split("\\s+")[0];
        } catch (Exception e) {
            return "N/A";
        }
    }
}
