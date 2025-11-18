package com.ajmarcos.multiprobador;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SshParser {

    /**
     * Busca un patrón de clave:valor y extrae el valor.
     * Ejemplo de patron: "Channel\s+:\s*(.*)"
     */
    private String extractValue(String output, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(output);
        if (matcher.find() && matcher.groupCount() > 0) {
            // El grupo 1 (.*) es el valor capturado
            return matcher.group(1).trim();
        }
        return "N/A"; // Valor por defecto si no se encuentra
    }

    /**
     * Parsea la salida SSH completa y llena un objeto Resultado.
     */
    public Resultado parseSshOutput(String fullOutput) {
        Resultado resultado = new Resultado();

        // ----------------------------------------------------
        // I. IDENTIFICACIÓN (Sección display version / show serial)
        // ----------------------------------------------------

        // FIRMWARE: main software version = V5R025C00S115
        String firmware = extractValue(fullOutput, "main software version\\s*=\\s*(.*)");
        resultado.setFirmware(firmware);

        // SERIAL: 48575443F96AD3AF (buscamos la línea que solo contiene el serial después de 'show serial')
        // Es más difícil de parsear por contexto, a menudo es la línea más corta después del comando.
        // Simplificación: si ya se extrajo el serial antes (en ejecutarDeviceModel), úsalo.
        String serial = extractValue(fullOutput, "show serial\\s*\\n\\s*(\\S+)");
        resultado.setSerial(serial.length() > 3 ? serial : "N/A");

        // ----------------------------------------------------
        // II. WI-FI: Bandas 2.4 GHz y 5 GHz
        // Nota: La salida muestra 4 bloques 2.4GHz (Index 1-4) y 4 bloques 5GHz (Index 5-8).
        // Usaremos el Index 1 para 2.4GHz y el Index 5 para 5GHz.
        // ----------------------------------------------------

        // Separamos la salida en bloques para manejar ambas bandas
        String wifiOutput = extractValue(fullOutput, "display wifi information\\s*\\n(.*)\\s*WAP>");

        // Bloque de 2.4 GHz (Asumimos el primer SSID activo)
        // Busca desde el inicio del bloque hasta el siguiente BSSID o el final del bloque
        String block2_4 = extractValue(wifiOutput, "(?s)SSID Index\\s+:\\s*1.*?---");
        if (!block2_4.equals("N/A")) {
            resultado.setSsid2(extractValue(block2_4, "SSID\\s+:\\s*(.*)"));
            resultado.setCanal2(extractValue(block2_4, "Channel\\s+:\\s*(.*)"));
            resultado.setEstado2(extractValue(block2_4, "Status\\s+:\\s*(.*)"));
            // RSSI no está en esta salida, se mantiene N/A
        }

        // Bloque de 5 GHz (Asumimos el SSID Index 5, que inicia la 5GHz en esta ONU)
        // Busca desde el SSID Index 5 hasta el final del bloque de WiFi
        String block5 = extractValue(wifiOutput, "(?s)SSID Index\\s+:\\s*5.*?---");
        if (!block5.equals("N/A")) {
            resultado.setSsid5(extractValue(block5, "SSID\\s+:\\s*(.*)"));
            resultado.setCanal5(extractValue(block5, "Channel\\s+:\\s*(.*)"));
            resultado.setEstado5(extractValue(block5, "Status\\s+:\\s*(.*)"));
            // RSSI no está en esta salida, se mantiene N/A
        }

        // ----------------------------------------------------
        // III. ÓPTICA Y VOIP
        // ----------------------------------------------------

        String diagOutput = extractValue(fullOutput, "show primary_diagnosis\\s*\\n(.*)\\s*success!");

        if (!diagOutput.equals("N/A")) {
            // POTENCIA ÓPTICA: received_optical power: -- dBm??
            String potencia = extractValue(diagOutput, "received_optical power:\\s*(.*?)?\\?\\?");
            resultado.setPotencia(potencia.trim());

            // VOIP STATUS: voip status: unconfigured??
            String voipStatus = extractValue(diagOutput, "voip status:\\s*(.*?)?\\?\\?");
            resultado.setVoip(voipStatus.trim());

            // SIP REGISTER STATUS: SIP client register: unregistered??
            String sipStatus = extractValue(diagOutput, "SIP client register:\\s*(.*?)?\\?\\?");
            // Puedes usar esto para determinar el estado de "falla" o "condicion"
            if (sipStatus.toLowerCase().contains("unregistered")) {
                resultado.setFalla("SIP Unregistered");
            }
        }

        // ----------------------------------------------------
        // IV. USUARIO (Sección display voip info)
        // ----------------------------------------------------

        // Hay una línea 'Current Tx-Power Level :100%' que se repite. No es la potencia óptica.

        // Usuario (el "Local Number" no está visible o registrado en "display last call log")
        // Si el usuario es el "Registered Phone Number" que normalmente vendría de otra fuente,
        // lo llenas por separado. Aquí lo dejamos N/A.

        return resultado;
    }
}