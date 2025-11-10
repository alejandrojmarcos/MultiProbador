package com.ajmarcos.multiprobador;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hgu2741 {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private WebView webViewRef = null;
    private boolean cancelado = false;
    private hgu2741Listener listener;
    private Resultado resultado = new Resultado();
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


    public interface hgu2741Listener {
        void onHgu2741Result(boolean success, Resultado resultado, int code);
    }

    public void setHgu2741Listener(hgu2741Listener listener) {
        this.listener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2741(WebView webView) {
        cancelado = false;
        webViewRef = webView;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(value ->
                Log.d(TAG, CLASS + (value
                        ? " → Todas las cookies eliminadas"
                        : " → No se pudieron eliminar todas las cookies")));

        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        Log.d(TAG, CLASS + " → Iniciando scrap2741");

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean deviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                Log.d(TAG, CLASS + " → Página cargada: " + url);

                // === LOGIN ===
                if (!isLoggedIn && url.contains("logIn_main.cgi")) {
                    Log.d(TAG, CLASS + " → Ejecutando login...");
                    String loginScript =
                            "var u=document.querySelector('input[name=\"username\"]');" +
                                    "var p=document.querySelector('input[name=\"syspasswd_1\"]');" +
                                    "var b=document.querySelector('input[id=\"Submit\"]');" +
                                    "if(u&&p&&b){u.value='Support';p.value='Te2010An_2014Ma';b.click();}";
                    view.evaluateJavascript(loginScript, value ->
                            Log.d(TAG, CLASS + " → Login ejecutado"));
                    isLoggedIn = true;

                    new android.os.Handler().postDelayed(() ->
                            webView.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi"), 6000);
                }

                // === EXTRAER DEVICE INFO ===
                else if (url.contains("deviceinfo.cgi") && !deviceInfoExtracted) {
                    Log.d(TAG, CLASS + " → Extrayendo información del dispositivo...");
                    deviceInfoExtracted = true;
                    String extractDeviceInfoScript =
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

                    view.evaluateJavascript(extractDeviceInfoScript, value -> {
                        try {
                            String jsonString = value.startsWith("\"") && value.endsWith("\"")
                                    ? value.substring(1, value.length() - 1).replace("\\\"", "\"")
                                    : value;

                            JSONObject json = new JSONObject(jsonString);

                            resultado.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                            resultado.setCatalogo(catal);
                            resultado.setModelo(model.replace("device_model ",""));
                            resultado.setMultiprobador(multi);
                            resultado.setFirmware(json.optString("Firmware", "No disponible"));
                            resultado.setSerial(json.optString("Serial", "No disponible"));
                            resultado.setPotencia(json.optString("Potencia", "No disponible"));
                            resultado.setSsid2(json.optString("Ssid2", "No disponible"));
                            resultado.setCanal2(json.optString("Canal2", "No disponible"));
                            resultado.setEstado2(json.optString("Estado2", "No disponible"));
                            resultado.setSsid5(json.optString("Ssid5", "No disponible"));
                            resultado.setCanal5(json.optString("Canal5", "No disponible"));
                            resultado.setEstado5(json.optString("Estado5", "No disponible"));
                            resultado.setVoip(json.optString("Voip", "No disponible"));

                            Log.d(TAG, CLASS + " → Datos extraídos correctamente");

                        } catch (JSONException e) {
                            Log.e(TAG, CLASS + " → Error parseando JSON: " + e.getMessage());
                        }

                        Log.d(TAG, CLASS + " → Cargando multipuesto.cgi...");
                        webView.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi");
                    });
                }

                // === EXTRAER USUARIO PPP ===
                else if (url.contains("multipuesto.cgi")) {
                    Log.d(TAG, CLASS + " → Extrayendo usuario PPP...");
                    String extractPppUserNameScript = "document.querySelector('input#pppUserName')?.value || 'N/A';";
                    view.evaluateJavascript(extractPppUserNameScript, value -> {
                        resultado.setUsuario(value.replace("\"", ""));
                        Log.d(TAG, CLASS + " → Usuario PPP: " + resultado.getUsuario());
                        if (listener != null && !cancelado) {
                            listener.onHgu2741Result(true, resultado, 200);
                            Log.d(TAG, CLASS + " → Proceso completado correctamente");
                        }
                    });
                }
            }
        });

        Log.d(TAG, CLASS + " → Cargando página inicial...");
        webView.loadUrl("http://192.168.1.1:8000/");
    }

    public void cancelar() {
        cancelado = true;
        Log.d(TAG, CLASS + " → Proceso cancelado por el usuario.");
        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
            } catch (Exception e) {
                Log.e(TAG, CLASS + " → Error deteniendo WebView: " + e.getMessage());
            }
        }
    }
}
