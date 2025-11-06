package com.ajmarcos.multiprobador;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;


public class Hgu2742_ {

    private volatile boolean detener = false;
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private Hgu2742Listener listener;

    public interface Hgu2742Listener {
        void onHgu2742Result(boolean success, Resultado resultado, int code);
    }

    public void setHgu2742Listener(Hgu2742Listener listener) {
        this.listener = listener;
    }

    private String filtrar(String texto) {
        if (texto == null) return "";
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) if (c == v) { filtered.append(c); break; }
        }
        return filtered.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2742(WebView webView) {
        if (detener) return;

        webViewRef = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        CookieManager.getInstance().removeAllCookies(value -> {});
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        final Resultado resultado = new Resultado();

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean deviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (detener || cancelado) return;

                // 1️⃣ Login
                if (!isLoggedIn && url.contains("login_advance.cgi")) {
                    String loginScript = "document.getElementsByName('Loginuser')[0].value = 'Support';" +
                            "document.getElementById('LoginPassword').value = 'Te2010An_2014Ma';" +
                            "document.querySelector('input[type=\\'submit\\']').click();";
                    view.evaluateJavascript(loginScript, null);
                    isLoggedIn = true;
                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            webView.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi"), 6000);
                }

                // 2️⃣ Extraer deviceinfo
                else if (url.contains("deviceinfo.cgi") && !deviceInfoExtracted) {
                    deviceInfoExtracted = true;
                    String extractDeviceInfoScript =
                            "var data = {};" +
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
                                    "JSON.stringify(data);";

                    view.evaluateJavascript(extractDeviceInfoScript, value -> {
                        try {
                            String jsonString = value.startsWith("\"") && value.endsWith("\"") ?
                                    value.substring(1, value.length() - 1) : value;
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

                        } catch (JSONException e) {
                            if (listener != null) listener.onHgu2742Result(false, null, 500);
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                webView.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi"), 3000);
                    });
                }

                // 3️⃣ Extraer usuario multipuesto
                else if (url.contains("multipuesto.cgi")) {
                    String extractUserScript = "document.querySelector('input#pppUserName')?.value || 'N/A';";
                    view.evaluateJavascript(extractUserScript, value -> {
                        resultado.setUsuario(filtrar(value.replace("\"", "")));
                        if (listener != null) listener.onHgu2742Result(true, resultado, 200);
                    });
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }
}
