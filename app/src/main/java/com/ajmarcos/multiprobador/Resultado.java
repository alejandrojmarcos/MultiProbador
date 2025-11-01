package com.ajmarcos.multiprobador;

public class Resultado {

    // Campos existentes
    private String firmware = "";
    private String serial = "";
    private String potencia = "";
    private String ssid2 = "";
    private String estado2 = "";
    private String canal2 = "";
    private String ssid5 = "";
    private String estado5 = "";
    private String canal5 = "";
    private String usuario = "";
    private String voip = "";

    // Campos nuevos para seguimiento y validación
    private String modelo;
    private int puerto;
    private String estadoValidacion;
    private String mensajeValidacion;
    private int codigo;

    // --- Getters y Setters ---
    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getPotencia() { return potencia; }
    public void setPotencia(String potencia) { this.potencia = potencia; }
    public String getSsid2() { return ssid2; }
    public void setSsid2(String ssid2) { this.ssid2 = ssid2; }
    public String getEstado2() { return estado2; }
    public void setEstado2(String estado2) { this.estado2 = estado2; }
    public String getCanal2() { return canal2; }
    public void setCanal2(String canal2) { this.canal2 = canal2; }
    public String getSsid5() { return ssid5; }
    public void setSsid5(String ssid5) { this.ssid5 = ssid5; }
    public String getEstado5() { return estado5; }
    public void setEstado5(String estado5) { this.estado5 = estado5; }
    public String getCanal5() { return canal5; }
    public void setCanal5(String canal5) { this.canal5 = canal5; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getVoip() { return voip; }
    public void setVoip(String voip) { this.voip = voip; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public int getPuerto() { return puerto; }
    public void setPuerto(int puerto) { this.puerto = puerto; }
    public String getEstadoValidacion() { return estadoValidacion; }
    public void setEstadoValidacion(String estadoValidacion) { this.estadoValidacion = estadoValidacion; }
    public String getMensajeValidacion() { return mensajeValidacion; }
    public void setMensajeValidacion(String mensajeValidacion) { this.mensajeValidacion = mensajeValidacion; }
    public int getCodigo() { return codigo; }
    public void setCodigo(int codigo) { this.codigo = codigo; }

    @Override
    public String toString() {
        return "Resultado{" +
                "firmware='" + firmware + '\'' +
                ", serial='" + serial + '\'' +
                ", potencia='" + potencia + '\'' +
                ", ssid2='" + ssid2 + '\'' +
                ", estado2='" + estado2 + '\'' +
                ", canal2='" + canal2 + '\'' +
                ", ssid5='" + ssid5 + '\'' +
                ", estado5='" + estado5 + '\'' +
                ", canal5='" + canal5 + '\'' +
                ", usuario='" + usuario + '\'' +
                ", voip='" + voip + '\'' +
                ", modelo='" + modelo + '\'' +
                ", puerto=" + puerto +
                ", estadoValidacion='" + estadoValidacion + '\'' +
                ", mensajeValidacion='" + mensajeValidacion + '\'' +
                ", codigo=" + codigo +
                '}';
    }
}
