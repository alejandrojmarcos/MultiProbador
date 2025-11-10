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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hgu2742 {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private WebView webViewRef = null;
    private boolean cancelado = false;
    private boolean detenido = false;
    private Hgu2742Listener listener;
    private String model  = "";
    private String multi ="";

    private String catal = "";

    public void setCatal(String catal) {
        this.catal = catal;
    }

    public void setModel(String model){
        this.model = model;
    }
    public void setMulti(String multi){
        this.multi = multi;
    }


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
            for (char v : validChars) {
                if (c == v) {
                    filtered.append(c);
                    break;
                }
            }
        }
        return filtered.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2742(WebView webView) {
        if (detenido) return;

        cancelado = false;
        webViewRef = webView;
        final Resultado resultado = new Resultado();

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(value -> Log.d(TAG, CLASS + ": Cookies limpiadas -> " + value));

        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean infoExtraida = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (detenido || cancelado) return;
                Log.d(TAG, CLASS + ": Página cargada -> " + url);

                // 1️⃣ LOGIN
                if (!isLoggedIn && url.contains("login_advance.cgi")) {
                    String loginScript =
                            "var u=document.getElementsByName('Loginuser')[0];" +
                                    "var p=document.getElementById('LoginPassword');" +
                                    "var b=document.querySelector('input[type=\\'submit\\']');" +
                                    "if(u&&p&&b){u.value='Support';p.value='Te2010An_2014Ma';b.click();}";
                    view.evaluateJavascript(loginScript, value -> Log.d(TAG, CLASS + ": Login ejecutado"));
                    isLoggedIn = true;

                    new Handler(Looper.getMainLooper()).postDelayed(() ->
                            webView.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi"), 6000);
                }

                // 2️⃣ DEVICE INFO
                else if (url.contains("deviceinfo.cgi") && !infoExtraida) {
                    infoExtraida = true;
                    Log.d(TAG, CLASS + ": Extrayendo información de dispositivo...");

                    String script =
                            "var data = {};" +
                                    "data.Firmware=document.querySelector('.span_deviceinfo#swversion')?.innerText||'No disponible';" +
                                    "data.Serial=document.querySelector('.span_deviceinfo#gsn')?.innerText||'No disponible';" +
                                    "data.Potencia=document.querySelector('.span_deviceinfo#opticalRX')?.innerText||'No disponible';" +
                                    "data.Ssid2=document.getElementById('ssidname')?.innerText||'No disponible';" +
                                    "data.Canal2=document.querySelector('.span_deviceinfo#cur_wifi_channel')?.innerText||'No disponible';" +
                                    "data.Estado2=document.getElementById('wlStatus')?.innerText||'No disponible';" +
                                    "data.Ssid5=document.getElementById('ssidname_5g')?.innerText||'No disponible';" +
                                    "data.Canal5=document.querySelector('.span_deviceinfo#cur_wifi_channel_5g')?.innerText||'No disponible';" +
                                    "data.Estado5=document.querySelector('.span_deviceinfo#wlStatus_5g')?.innerText||'No disponible';" +
                                    "data.Voip=document.querySelector('.span_deviceinfo#linen_umber')?.innerText||'No disponible';" +
                                    "JSON.stringify(data);";

                    view.evaluateJavascript(script, value -> {
                        try {
                            String jsonString = value.startsWith("\"") && value.endsWith("\"")
                                    ? value.substring(1, value.length() - 1).replace("\\\"", "\"")
                                    : value;

                            JSONObject json = new JSONObject(jsonString);

                            resultado.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            resultado.setCatalogo(catal);
                            resultado.setModelo(model);
                            resultado.setMultiprobador(multi);
                            resultado.setFirmware(filtrar(json.optString("Firmware")));
                            resultado.setSerial(filtrar(json.optString("Serial")));
                            resultado.setPotencia(filtrar(json.optString("Potencia")));
                            resultado.setSsid2(filtrar(json.optString("Ssid2")));
                            resultado.setCanal2(filtrar(json.optString("Canal2")));
                            resultado.setEstado2(filtrar(json.optString("Estado2")));
                            resultado.setSsid5(filtrar(json.optString("Ssid5")));
                            resultado.setCanal5(filtrar(json.optString("Canal5")));
                            resultado.setEstado5(filtrar(json.optString("Estado5")));
                            resultado.setVoip(filtrar(json.optString("Voip")));

                        } catch (JSONException e) {
                            Log.e(TAG, CLASS + ": Error parseando JSON -> " + e.getMessage());
                            if (listener != null) listener.onHgu2742Result(false, null, 500);
                            return;
                        }

                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                webView.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi"), 3000);
                    });
                }

                // 3️⃣ USUARIO PPP
                else if (url.contains("multipuesto.cgi")) {
                    String extractUserScript = "document.querySelector('input#pppUserName')?.value || 'N/A';";
                    view.evaluateJavascript(extractUserScript, value -> {
                        resultado.setUsuario(filtrar(value.replace("\"", "")));
                        if (listener != null && !cancelado)
                            listener.onHgu2742Result(true, resultado, 200);
                    });
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }

    public void cancelar() {
        cancelado = true;
        detenido = true;
        if (webViewRef != null) {
            webViewRef.stopLoading();
            webViewRef.loadUrl("about:blank");
            webViewRef.clearHistory();
            webViewRef.clearCache(true);
        }
        Log.d(TAG, CLASS + ": Proceso cancelado.");
    }
}
