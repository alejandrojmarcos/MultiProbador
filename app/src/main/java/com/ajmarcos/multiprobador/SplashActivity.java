package com.ajmarcos.multiprobador;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "Splash";
    private static final int MY_REQUEST_CODE = 101;
    // SplashActivity.java

    private Runnable timeoutRunnable;
    private final long TIMEOUT_MS = 5000; // 5 segundos
// Asegúrate de que tienes: private AppUpdateManager appUpdateManager;


    private AppUpdateManager appUpdateManager;
    private Handler mainHandler; // 👈 Manejador para volver al hilo UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mainHandler = new Handler(Looper.getMainLooper());
        appUpdateManager = AppUpdateManagerFactory.create(this);

        // 📢 Ejecutamos la lógica de chequeo en un hilo secundario
        startBackgroundCheck();
    }

    /**
     * Inicia el chequeo de red en un hilo de fondo.
     */
    private void startBackgroundCheck() {
        new Thread(() -> {
            // 1. Chequeo de red (I/O)
            boolean isConnected = isNetworkAvailable(SplashActivity.this);

            // 2. Volver al hilo principal para las actualizaciones de UI y el flujo
            mainHandler.post(() -> {
                if (isConnected) {
                    Log.d(TAG, "Internet OK. Chequeando actualizaciones...");
                    checkForAppUpdates();
                } else {
                    Log.w(TAG, "Sin Internet. Mostrando diálogo de conexión forzada.");
                    showNoInternetDialog();
                }
            });
        }).start();
    }


    // SplashActivity.java

// SplashActivity.java (Modificar el método isNetworkAvailable)

    private boolean isNetworkAvailable(Context context) {
        // 1. Verificar si hay conectividad local (API 28 y anteriores)
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = (cm != null) ? cm.getActiveNetworkInfo() : null;

        if (activeNetwork == null || !activeNetwork.isConnected()) {
            return false; // No hay conexión local
        }

        // 2. 📢 VERIFICACIÓN DE SALIDA EXTERNA (Ping)
        // El ping solo debe ejecutarse en el hilo de fondo (ya estás en un hilo secundario)
        try {
            // Ejecuta un ping a un servidor conocido (Google DNS) con un timeout estricto (e.g., 2 segundos)
            Process process = Runtime.getRuntime().exec("/system/bin/ping -c 1 -W 2 8.8.8.8");
            int exitCode = process.waitFor();

            // Retorna true si el ping fue exitoso (código de salida 0)
            return exitCode == 0;
        } catch (Exception e) {
            Log.e(TAG, "Ping falló: " + e.getMessage());
            return false;
        }
    }

    private void showNoInternetDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Conexión a Internet Requerida")
                .setMessage("La aplicación necesita conexión a Internet para verificar actualizaciones críticas de seguridad y funcionalidad.")
                .setCancelable(false)
                .setPositiveButton("Configurar Red", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                })
                .setNegativeButton("Reintentar", (dialog, which) -> {
                    // 📢 Al reintentar, volvemos a iniciar el chequeo en el HILO SECUNDARIO
                    startBackgroundCheck();
                })
                .show();
    }

    // ----------------------------------------------------
    // Lógica de Actualización (IMMEDIATE Flow)
    // ----------------------------------------------------

    // SplashActivity.java

    // SplashActivity.java

    private void checkForAppUpdates() {
        // 1. Definir la acción de timeout (continuar si pasan 5 segundos)
        timeoutRunnable = this::startMainActivity;
        mainHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    // 2. Si la respuesta llega, cancelar el timeout
                    mainHandler.removeCallbacks(timeoutRunnable);

                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.IMMEDIATE,
                                    this,
                                    MY_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error iniciando flujo inmediato", e);
                            startMainActivity();
                        }
                    } else {
                        // 3. Continuar si no se necesita actualización
                        startMainActivity();
                    }
                })
                .addOnFailureListener(e -> { // 📢 MANEJAR FALLO EXPLÍCITO
                    // Si el chequeo falla (ej. Play Services no disponible), cancelar timeout y continuar.
                    mainHandler.removeCallbacks(timeoutRunnable);
                    Log.e(TAG, "Play Core check failed: " + e.getMessage());
                    startMainActivity();
                });
    }

    private void startMainActivity() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1000);
    }

    // ----------------------------------------------------
    // Manejo del Ciclo de Vida para Actualizaciones Inmediatas
    // ----------------------------------------------------

// SplashActivity.java

    @Override
    protected void onResume() {
        super.onResume();

        // ... (rest of onResume logic) ...

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {

                    // Verifica si la actualización inmediata se interrumpió (valor 3)
                    if (appUpdateInfo.updateAvailability() == 3) {
                        try {
                            // La llamada correcta debe ser: (AppUpdateInfo, Type, Activity, Code)
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,         // 🟢 ARG 1: AppUpdateInfo (Objeto correcto)
                                    AppUpdateType.IMMEDIATE,
                                    this,                  // 🟢 ARG 3: La Activity
                                    MY_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error reanudando flujo inmediato", e);
                            startMainActivity();
                        }
                    }
                    // ... (resto de onResume)
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.e(TAG, "Actualización inmediata cancelada o fallida. Código: " + resultCode);
                // Si falla la actualización crítica, cerramos la app para forzar al usuario
                finishAndRemoveTask();
            } else {
                // Actualización inmediata fue exitosa
                startMainActivity();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


}