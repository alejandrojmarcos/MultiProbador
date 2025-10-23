package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu2742 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.d(TAG, "Timeout alcanzado, cancelando proceso (2742).");
        }
    };



    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
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

        if (listener != null) {
            listener.onHguResult(false, null, 999);
        }
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
    public void scrap2742(WebView webView) {
        cancelado = false;
        webViewRef = webView;

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d("Cookies", value ? "Todas las cookies eliminadas" : "No se pudieron eliminar todas las cookies"));

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean isDeviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.contains("login_advance.cgi")) {
                        String loginScript = "try {" +
                                "document.getElementsByName('Loginuser')[0].value = 'Support';" +
                                "document.getElementById('LoginPassword').value = 'Te2010An_2014Ma';" +
                                "document.querySelector('input[type=\\'submit\\']').click();" +
                                "} catch(e){ console.error(e); }";

                        view.evaluateJavascript(loginScript, null);
                        isLoggedIn = true;

                        webView.postDelayed(() -> runIfNotCancelled(() ->
                                webView.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi")), 6000);
                    }

                    // 2. Extraer info del dispositivo
                    else if (url.contains("deviceinfo.cgi") && !isDeviceInfoExtracted) {
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

                                resultado.firmware = filtrar(jsonObject.optString("Firmware", "No disponible"));
                                resultado.serial = filtrar(jsonObject.optString("Serial", "No disponible"));
                                resultado.potencia = filtrar(jsonObject.optString("Potencia", "No disponible"));
                                resultado.ssid2 = filtrar(jsonObject.optString("Ssid2", "No disponible"));
                                resultado.canal2 = filtrar(jsonObject.optString("Canal2", "No disponible"));
                                resultado.estado2 = filtrar(jsonObject.optString("Estado2", "No disponible"));
                                resultado.ssid5 = filtrar(jsonObject.optString("Ssid5", "No disponible"));
                                resultado.canal5 = filtrar(jsonObject.optString("Canal5", "No disponible"));
                                resultado.estado5 = filtrar(jsonObject.optString("Estado5", "No disponible"));
                                resultado.voip = filtrar(jsonObject.optString("Voip", "No disponible"));

                                isDeviceInfoExtracted = true;

                                webView.postDelayed(() -> runIfNotCancelled(() ->
                                        webView.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi")), 3000);

                            } catch (JSONException e) {
                                Log.e("JSONError2742", "Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) {
                                    listener.onHguResult(false, null, 500);
                                }
                            }
                        }));
                    }

                    // 3. Extraer usuario multipuesto
                    else if (url.contains("multipuesto.cgi")) {
                        String extractPppUserNameScript =
                                "document.querySelector('input#pppUserName')?.value || 'N/A';";

                        view.evaluateJavascript(extractPppUserNameScript, value -> runIfNotCancelled(() -> {
                            resultado.usuario = filtrar(value.replace("\"", ""));
                            if (listener != null && !cancelado) {
                                listener.onHguResult(true, resultado, 200);
                            }
                        }));
                    }
                });
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }
}
