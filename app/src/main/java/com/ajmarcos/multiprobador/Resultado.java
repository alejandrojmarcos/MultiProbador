package com.ajmarcos.multiprobador;

import java.util.HashMap;
import java.util.Map;

public class Resultado {
    private String multiprobador;
    private String fecha;
    private String modelo; // <-- NUEVO CAMPO
    private String serial;
    private String firmware;
    private String potencia;
    private String ssid2;
    private String estado2;
    private String canal2;
    private String rssi2;
    private String ssid5;
    private String estado5;
    private String canal5;
    private String rssi5;
    private String usuario;
    private String voip;
    private String catalogo;
    private String falla;
    private String condicion;


    // Getters y Setters
    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

    public String getPotencia() { return potencia; }
    public void setPotencia(String potencia) { this.potencia = potencia; }

    public String getSsid2() { return ssid2; }
    public void setSsid2(String ssid2) { this.ssid2 = ssid2; }

    public String getEstado2() { return estado2; }
    public void setEstado2(String estado2) { this.estado2 = estado2; }

    public String getCanal2() { return canal2; }
    public void setCanal2(String canal2) { this.canal2 = canal2; }

    public String getRssi2() { return rssi2; }
    public void setRssi2(String rssi2) { this.rssi2 = rssi2; }

    public String getSsid5() { return ssid5; }
    public void setSsid5(String ssid5) { this.ssid5 = ssid5; }

    public String getEstado5() { return estado5; }
    public void setEstado5(String estado5) { this.estado5 = estado5; }

    public String getCanal5() { return canal5; }
    public void setCanal5(String canal5) { this.canal5 = canal5; }

    public String getRssi5() { return rssi5; }
    public void setRssi5(String rssi5) { this.rssi5 = rssi5; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getVoip() { return voip; }
    public void setVoip(String voip) { this.voip = voip; }

    public String getCatalogo() { return catalogo; }
    public void setCatalogo(String catalogo) { this.catalogo = catalogo; }

    public String getFalla() { return falla; }
    public void setFalla(String falla) { this.falla = falla; }

    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }

    public String getMultiprobador() { return multiprobador; }
    public void setMultiprobador(String multiprobador) { this.multiprobador = multiprobador; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    // Conversión a Map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("multiprobador", multiprobador);
        map.put("fecha", fecha);
        map.put("modelo", modelo); // agregado
        map.put("serial", serial);
        map.put("firmware", firmware);
        map.put("potencia", potencia);
        map.put("ssid2", ssid2);
        map.put("estado2", estado2);
        map.put("canal2", canal2);
        map.put("rssi2", rssi2);
        map.put("ssid5", ssid5);
        map.put("estado5", estado5);
        map.put("canal5", canal5);
        map.put("rssi5", rssi5);
        map.put("usuario", usuario);
        map.put("voip", voip);
        map.put("catalogo", catalogo);
        map.put("falla", falla);
        map.put("condicion", condicion);


        return map;
    }

    @Override
    public String toString() {
        return "Resultado{" +
                "multiprobador='" + multiprobador + '\'' +
                ", fecha='" + fecha + '\'' +
                ", modelo='" + modelo + '\'' +
                ", serial='" + serial + '\'' +
                ", firmware='" + firmware + '\'' +
                ", potencia='" + potencia + '\'' +
                ", ssid2='" + ssid2 + '\'' +
                ", estado2='" + estado2 + '\'' +
                ", canal2='" + canal2 + '\'' +
                ", rssi2='" + rssi2 + '\'' +
                ", ssid5='" + ssid5 + '\'' +
                ", estado5='" + estado5 + '\'' +
                ", canal5='" + canal5 + '\'' +
                ", rssi5='" + rssi5 + '\'' +
                ", usuario='" + usuario + '\'' +
                ", voip='" + voip + '\'' +
                ", catalogo='" + catalogo + '\'' +
                ", falla='" + falla + '\'' +
                ", condicion='" + condicion + '\'' +
                '}';
    }
}
