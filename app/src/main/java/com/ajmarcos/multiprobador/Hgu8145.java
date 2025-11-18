package com.ajmarcos.multiprobador;

import android.util.Log;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Hgu8145 {

    // --- INTERFACE Y METADATOS ---
    public interface Hgu8145Listener {
        void onHgu8145Result(boolean success, Resultado res, int code);
    }

    private Hgu8145Listener listener;
    private String model = "";
    private String multi = "";
    private String catal = "";

    private final String TAG = "Hgu8145";

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

        // 1. Definir la secuencia de comandos
        // 📢 ESTRATEGIA: Diagnóstico PRIMERO, Configuración XML AL FINAL
        String[] scrapCommands = new String[]{
                "ssh -o StrictHostKeyChecking=no Support@192.168.1.1",
                // Comandos de Diagnóstico (Texto Plano - Esto ya funciona)
                "display wifi information",
                "display voip info",
                "display optic",
                "display version",
                "show serial",
                "show primary_diagnosis",
                // Comando de Configuración (XML - Para el Usuario PPPoE)
                "terminal length 0",
                "display current-configuration",
                "quit"
        };

        // 2. Ejecutar SSH
        SshInteractivo ssh = new SshInteractivo(routerIp, "Support", "Te2010An_2014Ma");
        ssh.setCommands(scrapCommands);

        ssh.setSshListener((success, message, code) -> {
            if (success && message != null) {
                // 3. CONCATENAR TODA LA SALIDA EN UN SOLO STRING GRANDE
                String fullSshOutput = String.join("\n", message);

                // 4. LLAMAR AL PARSER DE DATOS
                parseAndSetResults(fullSshOutput, resultadoSsh);

                // Finaliza exitosamente
                if (listener != null) listener.onHgu8145Result(true, resultadoSsh, 0);

            } else {
                Log.e(TAG, "Fallo total en SSH Scraping.");
                // Finaliza con error (manteniendo los metadatos)
                if (listener != null) listener.onHgu8145Result(false, resultadoSsh, code);
            }
        });
        ssh.start();
    }

    // --- IMPLEMENTACIÓN DE MÉTODOS AUXILIARES (Sin cambios, funcionan bien) ---

    private String extractFirstGroup(String output, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1).trim().replace("??", "");
        }
        return "N/A";
    }

    // 📢 NUEVO MÉTODO AUXILIAR PARA XML (Solo para el usuario PPPoE)
    private String extractXmlAttribute(String output, String tagAndPart, String attribute) {
        Pattern pattern = Pattern.compile("<" + tagAndPart + ".*?" + attribute + "=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(output);
        if (matcher.find() && matcher.groupCount() > 0) {
            return matcher.group(1).trim();
        }
        return "N/A";
    }

    private String extractByLineKey(String fullOutput, String key) {
        String[] lines = fullOutput.split("[\\n\\r]+");
        String searchKey = key.trim();
        for (String line : lines) {
            if (line.contains(searchKey)) {
                int index = line.indexOf(':');
                if (index == -1) index = line.indexOf('=');
                if (index != -1) {
                    return line.substring(index + 1).trim().replace("??", "");
                }
            }
        }
        return "N/A";
    }

    private String extractLineAfterKey(String fullOutput, String key) {
        String[] lines = fullOutput.split("[\\n\\r]+");
        boolean foundKey = false;
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (foundKey && !trimmedLine.isEmpty() && !trimmedLine.toLowerCase().contains("success")) {
                return trimmedLine;
            }
            if (trimmedLine.toLowerCase().contains(key.toLowerCase())) {
                foundKey = true;
            }
        }
        return "N/A";
    }

    private String extractBlock(String fullOutput, String startRegex, String endRegex) {
        try {
            Pattern startPattern = Pattern.compile(startRegex, Pattern.DOTALL);
            Matcher startMatcher = startPattern.matcher(fullOutput);
            if (startMatcher.find()) {
                String remaining = fullOutput.substring(startMatcher.start());
                Pattern endPattern = Pattern.compile(endRegex, Pattern.DOTALL);
                Matcher endMatcher = endPattern.matcher(remaining);
                if (endMatcher.find()) {
                    return remaining.substring(0, endMatcher.end());
                }
                return remaining;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error aislando bloque con regex.", e);
        }
        return "";
    }

    private String extractByBlockKey(String block, String key) {
        String searchKey = key.trim();
        String[] lines = block.split("[\\n\\r]+");
        for (String line : lines) {
            if (line.contains(searchKey)) {
                int index = line.indexOf(':');
                if (index != -1) {
                    return line.substring(index + 1).trim();
                }
            }
        }
        return "N/A";
    }

    // 📢 MÉTODO PRINCIPAL DE PARSING (MODIFICADO)
    private void parseAndSetResults(String fullOutput, Resultado r) {

        // ----------------------------------------------------
        // PARTE 1: DATOS DE DIAGNÓSTICO (TEXTO PLANO) - Esto ya funcionaba
        // ----------------------------------------------------

        // Serial y Firmware
        String serialLine = extractLineAfterKey(fullOutput, "show serial");
        r.setSerial(serialLine.length() > 3 ? serialLine : "N/A");
        r.setFirmware(extractByLineKey(fullOutput, "main software version"));

        // Potencia y VOIP
        r.setPotencia(extractByLineKey(fullOutput, "received_optical power"));
        r.setVoip(extractByLineKey(fullOutput, "voip status"));
        r.setUsuario(extractByLineKey(fullOutput, "registered phone number")); // Fallback temporal

        // Falla SIP
        String sipStatus = extractByLineKey(fullOutput, "SIP client register");
        if (sipStatus.toLowerCase().contains("unregistered")) {
            r.setFalla("SIP Unregistered");
        } else {
            r.setFalla("");
        }

        // Wi-Fi (Usando bloques de texto plano)
        // Nota: Como el XML viene AL FINAL, no interfiere con la búsqueda de estos bloques iniciales.
        String block2_4 = extractBlock(fullOutput, "SSID Index\\s+:\\s*1", "----------------------------------------------------");
        if (!block2_4.isEmpty()) {
            r.setSsid2(extractByBlockKey(block2_4, "SSID"));
            r.setCanal2(extractByBlockKey(block2_4, "Channel").replace("(auto)", "").trim());
            r.setEstado2(extractByBlockKey(block2_4, "Status"));
            r.setRssi2("N/A");
        } else {
            r.setSsid2("N/A");
        }

        String block5 = extractBlock(fullOutput, "SSID Index\\s+:\\s*5", "----------------------------------------------------");
        if (!block5.isEmpty()) {
            r.setSsid5(extractByBlockKey(block5, "SSID"));
            r.setCanal5(extractByBlockKey(block5, "Channel").replace("(auto)", "").trim());
            r.setEstado5(extractByBlockKey(block5, "Status"));
            r.setRssi5("N/A");
        } else {
            r.setSsid5("N/A");
        }

        // ----------------------------------------------------
        // PARTE 2: USUARIO PPPoE (XML) - Esto es lo nuevo
        // ----------------------------------------------------

        // Buscamos el bloque XML que contiene el usuario. Como está al final, usamos una búsqueda más amplia.
        // Buscamos la etiqueta específica directamente en todo el output.
        String pppoeUser = extractXmlAttribute(fullOutput, "WANPPPConnectionInstance", "Username");

        if (!pppoeUser.equals("N/A")) {
            r.setUsuario(pppoeUser); // Sobrescribe el fallback si encontramos el usuario real
        }
    }
}