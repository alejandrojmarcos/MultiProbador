package com.ajmarcos.multiprobador;

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

    public static ResultadoValidacion validar(Resultado r) {
        if (r == null) {
            return new ResultadoValidacion(EstadoValidacion.ERROR, "Sin datos");
        }


        if (r.getFirmware() == null || r.getFirmware().isEmpty()) {
            return new ResultadoValidacion(EstadoValidacion.ERROR, "Firmware no detectado");
        }


        try {
            double potencia = Double.parseDouble(r.getPotencia().replace("dBm", "").trim());
            if (potencia < -28) {
                return new ResultadoValidacion(EstadoValidacion.ERROR, "Potencia muy baja (" + potencia + " dBm)");
            } else if (potencia < -25) {
                return new ResultadoValidacion(EstadoValidacion.WARNING, "Potencia baja (" + potencia + " dBm)");
            }
        } catch (Exception ignored) {
        }


        if (r.getVoip() != null && r.getVoip().equalsIgnoreCase("No disponible")) {
            return new ResultadoValidacion(EstadoValidacion.WARNING, "VOIP no registrado");
        }

        return new ResultadoValidacion(EstadoValidacion.OK, "Datos correctos");
    }
}
