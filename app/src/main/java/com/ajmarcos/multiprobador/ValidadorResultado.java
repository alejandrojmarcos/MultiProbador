package com.ajmarcos.multiprobador;

import java.util.Set; // Importar la interfaz Set de Java

public class ValidadorResultado {

    public enum EstadoValidacion {
        OK,
        WARNING,
        ERROR
    }

    public static class ResultadoValidacion {
        private final EstadoValidacion estado;
        private final String mensaje;

        public ResultadoValidacion(EstadoValidacion estado, String mensaje) {
            this.estado = estado;
            this.mensaje = mensaje;
        }

        public EstadoValidacion getEstado() { return estado; }
        public String getMensaje() { return mensaje; }
    }

    // CLASE AGREGADA: Contiene la validación y los datos modificados
    public static class ResultadoCompleto {
        private final ResultadoValidacion validacion;
        private final Resultado datosModificados;

        public ResultadoCompleto(ResultadoValidacion validacion, Resultado datosModificados) {
            this.validacion = validacion;
            this.datosModificados = datosModificados;
        }

        public ResultadoValidacion getValidacion() { return validacion; }
        public Resultado getDatosModificados() { return datosModificados; }
    }

    /**
     * Valida el objeto Resultado contra múltiples listas de control.
     * @param r El objeto Resultado extraído.
     * @param serialesInvalidos Lista de seriales bloqueados (BLACKLIST).
     * @param firmwaresActuales Lista de versiones de firmware recomendadas.
     * @param firmwaresCriticos Lista de versiones que causan ERROR.
     * @param firmwaresObsoletos Lista de versiones que causan WARNING.
     * @return ResultadoCompleto con el estado final (OK/WARNING/ERROR) y el mensaje.
     */
    public static ResultadoCompleto validar(Resultado r,
                                            Set<String> serialesInvalidos,
                                            Set<String> firmwaresActuales,
                                            Set<String> firmwaresCriticos,
                                            Set<String> firmwaresObsoletos,
                                            Set<String> redesObservadas){ // 👈 PARÁMETRO DE REDES
        ResultadoValidacion rv = null; // Inicializamos a null para registrar el primer error/warning

        if (r == null) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Sin datos");
            return new ResultadoCompleto(rv, null);
        }

        // --- Preparación ---
        // Asumimos que r.getSerial() y r.getFirmware() devuelven cadenas limpias o "N/A".
        String serialLimpio = r.getSerial() != null ? r.getSerial().trim().toUpperCase() : "";
        String firmwareLimpio = r.getFirmware() != null ? r.getFirmware().trim().toUpperCase() : "";
        String ssid2Limpio = r.getSsid2() != null ? r.getSsid2().trim().toUpperCase() : "";
        String ssid5Limpio = r.getSsid5() != null ? r.getSsid5().trim().toUpperCase() : "";


        // --- VALIDACIÓN 1: SERIALES INVÁLIDOS (BLACKLIST) ---
        if (serialLimpio.isEmpty() || serialLimpio.equals("N/A")) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Serial No Encontrado");
        }
        if (serialesInvalidos.contains(serialLimpio)) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Serial en lista de bloqueo (BLACKLIST)");
            return new ResultadoCompleto(rv, r); // Error fatal, salimos inmediatamente
        }

        // --- VALIDACIÓN 2: FIRMWARES CRÍTICOS (ERROR) ---
        if (firmwaresCriticos.contains(firmwareLimpio)) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Firmware Crítico (Requiere Actualización Urgente)");
            return new ResultadoCompleto(rv, r); // Error fatal, salimos inmediatamente
        }

        // --- VALIDACIÓN 3: FIRMWARES OBSOLETOS (WARNING) ---
        if (firmwaresObsoletos.contains(firmwareLimpio)) {
            rv = new ResultadoValidacion(EstadoValidacion.WARNING, "Firmware Obsoleto (Recomendado Actualizar)");
        }

        // --- VALIDACIÓN 4: FIRMWARES NO CONOCIDOS (WARNING) ---
        if (!firmwaresActuales.contains(firmwareLimpio) && !firmwaresCriticos.contains(firmwareLimpio) && !firmwaresObsoletos.contains(firmwareLimpio)) {
            if (rv == null || rv.getEstado() != EstadoValidacion.ERROR) {
                rv = new ResultadoValidacion(EstadoValidacion.WARNING, "Firmware no catalogado. Revisar.");
            }
        }

        // --- VALIDACIÓN 5: VISIBILIDAD DE REDES (SSID) [NUEVO] ---
        // Solo validamos si no hay un ERROR fatal previo.
        if (rv == null || rv.getEstado() != EstadoValidacion.ERROR) {

            // Chequeo 2.4 GHz
            if (!ssid2Limpio.isEmpty() && !ssid2Limpio.equals("N/A") && !redesObservadas.contains(ssid2Limpio)) {
                rv = new ResultadoValidacion(EstadoValidacion.WARNING, "SSID 2.4G configurado pero NO VISIBLE en el escaneo.");
            }

            // Chequeo 5 GHz (Solo si el 2.4G no generó un WARNING que queremos mantener, o si no hubo error)
            else if (!ssid5Limpio.isEmpty() && !ssid5Limpio.equals("N/A") && !redesObservadas.contains(ssid5Limpio)) {
                // No sobrescribimos un ERROR existente, pero sí un WARNING de firmware o VOIP anterior.
                rv = new ResultadoValidacion(EstadoValidacion.WARNING, "SSID 5G configurado pero NO VISIBLE en el escaneo.");
            }
        }


        // --- VALIDACIÓN 6: POTENCIA ÓPTICA ---
        try {
            double potencia = Double.parseDouble(r.getPotencia().replace("dBm", "").trim());

            if (potencia < -28) {
                rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Potencia muy baja (" + potencia + " dBm)");
                return new ResultadoCompleto(rv, r); // Error fatal
            } else if (potencia < -25) {
                // Si rv es nulo (no hay errores/warnings previos), lo establecemos como WARNING.
                if (rv == null) {
                    rv = new ResultadoValidacion(EstadoValidacion.WARNING, "Potencia baja (" + potencia + " dBm)");
                }
            }
        } catch (Exception ignored) {
            // Ignoramos si no se puede parsear
        }

        // --- VALIDACIÓN 7: VOIP STATUS ---
        if (r.getVoip() != null && r.getVoip().equalsIgnoreCase("No disponible")) {
            // Si rv no tiene un ERROR, registramos este WARNING.
            if (rv == null || rv.getEstado() != EstadoValidacion.ERROR) {
                rv = new ResultadoValidacion(EstadoValidacion.WARNING, "VOIP no registrado");
            }
        }

        // Si rv es nulo después de todas las comprobaciones, significa OK.
        if (rv == null) {
            rv = new ResultadoValidacion(EstadoValidacion.OK, "Datos correctos");
        }

        return new ResultadoCompleto(rv, r);
    }
}