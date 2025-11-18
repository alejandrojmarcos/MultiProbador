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

    // 📢 CLASE AGREGADA: Contiene la validación y los datos modificados
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

    // 📢 MÉTODO VALIDAR MODIFICADO: Devuelve ResultadoCompleto
    public static ResultadoCompleto validar(Resultado r) {
        ResultadoValidacion rv;

        if (r == null) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Sin datos");
            return new ResultadoCompleto(rv, null);
        }

        // --- Lógica de Validación (Igual que la anterior) ---

        if (r.getFirmware() == null || r.getFirmware().isEmpty()) {
            rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Firmware no detectado");
            return new ResultadoCompleto(rv, r);
        }

        try {
            double potencia = Double.parseDouble(r.getPotencia().replace("dBm", "").trim());
            if (potencia < -28) {
                rv = new ResultadoValidacion(EstadoValidacion.ERROR, "Potencia muy baja (" + potencia + " dBm)");
                return new ResultadoCompleto(rv, r);
            } else if (potencia < -25) {
                rv = new ResultadoValidacion(EstadoValidacion.WARNING, "Potencia baja (" + potencia + " dBm)");
                return new ResultadoCompleto(rv, r);
            }
        } catch (Exception ignored) {
        }

        if (r.getVoip() != null && r.getVoip().equalsIgnoreCase("No disponible")) {
            rv = new ResultadoValidacion(EstadoValidacion.WARNING, "VOIP no registrado");
            return new ResultadoCompleto(rv, r);
        }

        // Si todo está OK
        rv = new ResultadoValidacion(EstadoValidacion.OK, "Datos correctos");
        return new ResultadoCompleto(rv, r);
    }
}