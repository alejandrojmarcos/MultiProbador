package com.ajmarcos.multiprobador;

import java.util.Objects;

public class RedWiFi {

    private String SSID;
    private int Frecuencia;
    private int RSSI;
    private int frec;

    public RedWiFi(){
        this.SSID = "";
        this.Frecuencia = 0;
        this.RSSI = 0;
        this.frec=0;
    }
    public RedWiFi(String SSID, int frec, int Frecuencia, int RSSI){
        this.SSID = SSID;
        this.Frecuencia = Frecuencia;
        this.RSSI = RSSI;
        this.frec=frec;
    }

    public String getSSID() {
        return SSID;
    }

    public void setSSID(String SSID) {
        this.SSID = SSID;
    }
    public int getFrec(){
        return frec;
    }

    public int getFrecuencia() {
        return Frecuencia;
    }

    public void setFrecuencia(int frecuencia) {
        Frecuencia = frecuencia;
    }

    public int getRSSI() {
        return RSSI;
    }

    @Override
    public String toString() {
        return "RedWiFi{" +
                "SSID='" + SSID + '\'' +
                ", Frecuencia=" + Frecuencia +
                ", RSSI=" + RSSI +
                '}';
    }

    public void setRSSI(int RSSI) {
        this.RSSI = RSSI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedWiFi redWiFi = (RedWiFi) o;
        return Objects.equals(SSID, redWiFi.SSID) && Objects.equals(Frecuencia, redWiFi.Frecuencia) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(SSID, Frecuencia, RSSI);
    }


}
