package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu2742 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private AlertDialog dialog;
    private hguListener listener;
    private Context context;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.e(TAG, "❌ Timeout alcanzado, cancelando proceso (2742).");
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu2742(Context ctx) {
        this.context = ctx;
    }

    public void cancelar() {
        if (cancelado) return;
        cancelado = true;

        timeoutHandler.removeCallbacks(timeoutRunnable);

        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
                webViewRef = null;
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo WebView: " + e.getMessage());
            }
        }

        if (dialog != null && dialog.isShowing()) dialog.dismiss();

        if (listener != null) listener.onHguResult(false, null, 999);
    }

    private void runIfNotCancelled(Runnable task) {
        if (!cancelado) task.run();
    }

    private String filtrar(String texto) {
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) if (c == v) { filtered.append(c); break; }
        }
        return filtered.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2742() {
        cancelado = false;

        // Crear WebView internamente
        webViewRef = new WebView(context);

        // Mostrar WebView en diálogo
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("🧭 WebView Debug (HGU2742)");
            builder.setView(webViewRef);
            builder.setPositiveButton("Cerrar", (d, w) -> d.dismiss());
            dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "⚠️ No se pudo mostrar WebView en diálogo: " + e.getMessage());
        }

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        webViewRef.getSettings().setJavaScriptEnabled(true);
        webViewRef.getSettings().setDomStorageEnabled(true);
        webViewRef.clearCache(true);
        webViewRef.clearHistory();
        webViewRef.clearFormData();

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d(TAG, value ? "🍪 Todas las cookies eliminadas" : "⚠️ No se pudieron eliminar cookies"));

        webViewRef.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean isDeviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                Log.d(TAG, "🌐 Página cargada: " + url);

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.contains("login_advance.cgi")) {
                        Log.d(TAG, "🟡 Detectada pantalla de login. Intentando iniciar sesión...");
                        String loginScript = "try {" +
                                "document.getElementsByName('Loginuser')[0].value = 'Support';" +
                                "document.getElementById('LoginPassword').value = 'Te2010An_2014Ma';" +
                                "document.querySelector('input[type=\\'submit\\']').click();" +
                                "} catch(e){ console.error(e); }";

                        view.evaluateJavascript(loginScript, v -> Log.d(TAG, "▶ Script login ejecutado."));
                        isLoggedIn = true;

                        webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                webViewRef.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi")), 6000);
                    }

                    // 2. Extraer info del dispositivo
                    else if (url.contains("deviceinfo.cgi") && !isDeviceInfoExtracted) {
                        Log.d(TAG, "📄 deviceinfo.cgi detectada. Extrayendo info del dispositivo...");
                        String extractDeviceInfoScript =
                                "var data = {};" +
                                        "try {" +
                                        "data.Firmware = document.querySelector('.span_deviceinfo#swversion')?.innerText || 'No disponible';" +
                                        "data.Serial = document.querySelector('.span_deviceinfo#gsn')?.innerText || 'No disponible';" +
                                        "data.Potencia = document.querySelector('.span_deviceinfo#opticalRX')?.innerText || 'No disponible';" +
                                        "data.Ssid2 = document.getElementById('ssidname')?.innerText || 'No disponible';" +
                                        "data.Canal2 = document.querySelector('.span_deviceinfo#cur_wifi_channel')?.innerText || 'No disponible';" +
                                        "data.Estado2 = document.getElementById('wlStatus')?.innerText || 'No disponible';" +
                                        "data.Ssid5 = document.getElementById('ssidname_5g')?.innerText || 'No disponible';" +
                                        "data.Canal5 = document.querySelector('.span_deviceinfo#cur_wifi_channel_5g')?.innerText || 'No disponible';" +
                                        "data.Estado5 = document.querySelector('.span_deviceinfo#wlStatus_5g')?.innerText || 'No disponible';" +
                                        "data.Voip = document.querySelector('.span_deviceinfo#linen_umber')?.innerText || 'No disponible';" +
                                        "JSON.stringify(data);" +
                                        "} catch(e){ console.error(e); JSON.stringify({Error: e.message}); }";

                        view.evaluateJavascript(extractDeviceInfoScript, value -> runIfNotCancelled(() -> {
                            try {
                                String jsonString = value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
                                jsonString = jsonString.replace("\\\"", "\"");
                                JSONObject jsonObject = new JSONObject(jsonString);

                                resultado.setFirmware(filtrar(jsonObject.optString("Firmware", "No disponible")));
                                resultado.setSerial(filtrar(jsonObject.optString("Serial", "No disponible")));
                                resultado.setPotencia(filtrar(jsonObject.optString("Potencia", "No disponible")));
                                resultado.setSsid2(filtrar(jsonObject.optString("Ssid2", "No disponible")));
                                resultado.setCanal2(filtrar(jsonObject.optString("Canal2", "No disponible")));
                                resultado.setEstado2(filtrar(jsonObject.optString("Estado2", "No disponible")));
                                resultado.setSsid5(filtrar(jsonObject.optString("Ssid5", "No disponible")));
                                resultado.setCanal5(filtrar(jsonObject.optString("Canal5", "No disponible")));
                                resultado.setEstado5(filtrar(jsonObject.optString("Estado5", "No disponible")));
                                resultado.setVoip(filtrar(jsonObject.optString("Voip", "No disponible")));

                                Log.d(TAG, "✅ Info dispositivo extraída: " + resultado.toString());
                                isDeviceInfoExtracted = true;

                                webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                        webViewRef.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi")), 3000);

                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) {
                                    listener.onHguResult(false, null, 500);
                                }
                            }
                        }));
                    }

                    // 3. Extraer usuario multipuesto
                    else if (url.contains("multipuesto.cgi")) {
                        Log.d(TAG, "📶 multipuesto.cgi detectada. Extrayendo usuario...");
                        String extractPppUserNameScript =
                                "document.querySelector('input#pppUserName')?.value || 'N/A';";

                        view.evaluateJavascript(extractPppUserNameScript, value -> runIfNotCancelled(() -> {
                            resultado.setUsuario(filtrar(value.replace("\"", "")));
                            Log.d(TAG, "🏁 Resultado final HGU2742: " + resultado.toString());
                            if (listener != null && !cancelado) {
                                listener.onHguResult(true, resultado, 200);
                            }
                        }));
                    }
                });
            }
        });

        webViewRef.setWebChromeClient(new WebChromeClient());
        Log.d(TAG, "🌍 Cargando página inicial HGU2742...");
        webViewRef.loadUrl("http://192.168.1.1:8000/");
    }
}
