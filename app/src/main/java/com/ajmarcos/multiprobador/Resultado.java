package com.ajmarcos.multiprobador;

import java.util.LinkedHashMap;
import java.util.Map;

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

    // Campos nuevos
    private String modelo;
    private int puerto;
    private String estadoValidacion;
    private String mensajeValidacion;
    private int codigo;

    private String lote;
    private String catalogo;

    public String getLote() { return lote; }
    public void setLote(String lote) { this.lote = lote; }

    public String getCatalogo() { return catalogo; }
    public void setCatalogo(String catalogo) { this.catalogo = catalogo; }

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
                ", lote='" + lote + '\'' +
                ", catalogo='" + catalogo + '\'' +
                '}';
    }

    /** 🔹 Convierte el resultado en un mapa clave-valor para el JSON */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("modelo", modelo);
        map.put("serial", serial);
        map.put("firmware", firmware);
        map.put("potencia", potencia);
        map.put("ssid2", ssid2);
        map.put("estado2", estado2);
        map.put("canal2", canal2);
        map.put("ssid5", ssid5);
        map.put("estado5", estado5);
        map.put("canal5", canal5);
        map.put("usuario", usuario);
        map.put("voip", voip);
        map.put("puerto", puerto);
        map.put("estadoValidacion", estadoValidacion);
        map.put("mensajeValidacion", mensajeValidacion);
        map.put("codigo", codigo);
        map.put("lote", lote);
        map.put("catalogo", catalogo);
        return map;
    }
}
