package com.ajmarcos.multiprobador;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import androidx.core.app.ActivityCompat; // Se mantiene por checkSelfPermission
import java.util.List;
import android.Manifest; // 👈 Importación requerida para usar Manifest.permission

public class RedesDisponibles {

    private final Context context;
    private final WifiManager wifiManager;
    private RedesListener listener;
    private WifiScanReceiver wifiReceiver;
    private final String TAG = "RedesDisponibles";

    public RedesDisponibles(Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        this.wifiReceiver = new WifiScanReceiver();

        // Registrar el BroadcastReceiver (se usa el contexto original de la Activity/Application)
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(wifiReceiver, intentFilter);
    }

    public interface RedesListener {
        void onRedesResult(boolean success, String message, int code, List<ScanResult> redes);
    }

    public void setRedesListener(RedesListener listener) {
        this.listener = listener;
    }

    // 📢 LÓGICA DE PERMISOS MOVIDA AQUÍ
    public void escanearRedes() {
        if (wifiManager == null) {
            if (listener != null) {
                listener.onRedesResult(false, "Error: WifiManager no inicializado.", 1, null);
            }
            return;
        }

        // 🚨 VERIFICACIÓN CRÍTICA DEL PERMISO DE UBICACIÓN FINA 🚨
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (listener != null) {
                // Notificamos a MainActivity que falta el permiso y la Activity debe solicitarlo.
                listener.onRedesResult(false, "Permiso de UBICACIÓN FINA (ACCESS_FINE_LOCATION) Requerido.", 5, null);
            }
            return;
        }

        boolean scanStarted = wifiManager.startScan();
        if (!scanStarted) {
            if (listener != null) {
                listener.onRedesResult(false, "Error al iniciar el escaneo (probablemente ya hay uno en curso).", 3, null);
            }
        } else {
            if (listener != null) {
                listener.onRedesResult(true, "Escaneo iniciado. Esperando resultados...", 2, null);
            }
        }
    }

    // ----------------------------------------------------
    // --- RECEPTOR ASÍNCRONO (LIMPIO DE LÓGICA DE PERMISOS) ---
    // ----------------------------------------------------

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {

            // 📢 CORRECCIÓN: SEGUNDA VERIFICACIÓN OBLIGATORIA DEL PERMISO
            List<ScanResult> redes = null;

            // El compilador OBLIGA a verificar FINE_LOCATION justo antes de getScanResults()
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                redes = wifiManager.getScanResults();
            } else {
                // Si llegamos aquí, el permiso fue REVOCADO o falló.
                // Logueamos un error y notificamos al listener con un código de error de permiso (Code 5).
                Log.e(TAG, "ERROR CRÍTICO: Permiso FINE_LOCATION faltante en onReceive.");
                if (listener != null) {
                    listener.onRedesResult(false, "Permiso Fino de Ubicación REVOCADO.", 5, null);
                    return;
                }
            }

            // --- 2. LOG DE DEPURACIÓN CRÍTICA ---
            if (redes != null) {
                Log.d(TAG, "DEBUG: Resultados brutos recibidos. Total: " + redes.size());
                Log.d(TAG, "DEBUG: Resultados brutos recibidos. Total: " + redes.toString());
                // ... (resto de tu lógica de logging) ...
            } else {
                Log.e(TAG, "DEBUG: getScanResults devolvió NULL (o permiso denegado).");
            }
            // ------------------------------------------

            if (listener == null) return;

            if (redes == null || redes.isEmpty()) {
                listener.onRedesResult(false, "No se encontraron redes disponibles.", 4, null);
            } else {
                listener.onRedesResult(true, "Redes disponibles obtenidas con éxito.", 0, redes);
            }
        }
    }

    // ----------------------------------------------------
    // --- MÉTODO DE LIMPIEZA ---
    // ----------------------------------------------------

    public void unregisterReceiver() {
        try {
            context.unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            // Ignorar si el receptor ya estaba desregistrado
        }
    }
}