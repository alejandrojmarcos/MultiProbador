package com.ajmarcos.multiprobador;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class RedesDisponibles {

    private RedesListener listener;
    private WifiManager wifiManager;
    private Context context;
    private ArrayList<RedWiFi> redesAcumuladas;

    public RedesDisponibles(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.redesAcumuladas = new ArrayList<>();
    }

    public interface RedesListener {
        void onRedesResult(boolean success, String message, int code, ArrayList<RedWiFi> redes);
    }

    public void setRedesListener(RedesListener listener) {
        this.listener = listener;
    }

    public void escanearRedes() {
        if (wifiManager == null) {
            if (listener != null) {
                listener.onRedesResult(false, "Error: WifiManager no inicializado.", 1, null);
            }
            return;
        }

        boolean scanStarted = wifiManager.startScan();
        if (!scanStarted) {
            if (listener != null) {
                listener.onRedesResult(false, "Error al iniciar el escaneo de redes Wi-Fi.", 3, null);
            }
            return;
        }

        // Obtener los resultados del escaneo
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        List<ScanResult> redes = wifiManager.getScanResults();
        if (redes == null || redes.isEmpty()) {
            if (listener != null) {
                listener.onRedesResult(false, "No se encontraron redes disponibles.", 4, null);
            }
        } else {
            if (listener != null) {
                listener.onRedesResult(true, "Redes disponibles obtenidas con éxito.", 0,redesProcesadas(redes));
            }
        }



    }
    public ArrayList<RedWiFi> redesProcesadas(List<ScanResult> redesObtenidas){

        for (ScanResult red : redesObtenidas) {

            int frecuencia = 0;
            if(red.frequency>3000){
                frecuencia = 5;
            }else{
                frecuencia=2;
            }

            RedWiFi nueva = new RedWiFi(red.SSID,frecuencia,red.frequency,red.level);

            if(!redesAcumuladas.contains(nueva)){
                redesAcumuladas.add(nueva);
            }else{
                int ind = redesAcumuladas.indexOf(nueva);
                RedWiFi redAlmacenada = redesAcumuladas.get(ind);
                int levelViejo = redAlmacenada.getRSSI();

                if(levelViejo > red.level){
                    redesAcumuladas.get(ind).setRSSI(red.level);
                }
            }
        }
        return redesAcumuladas;
    }
}
