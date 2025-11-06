package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu2741 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;
    private Context context;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.e(TAG, "❌ Timeout alcanzado, cancelando proceso (2741).");
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu2741(Context ctx) {
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

        if (listener != null) listener.onHguResult(false, null, 999);
    }

    private void runIfNotCancelled(Runnable task) {
        if (!cancelado) task.run();
    }

    private String filtrar(String texto) {
        if (texto == null) return "";
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_ ".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) if (c == v) { filtered.append(c); break; }
        }
        return filtered.toString().trim();
    }

    /**
     * Versión recomendada: recibe un WebView ya existente (y preferiblemente añadido a la UI)
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2741(WebView webView) {
        cancelado = false;
        this.webViewRef = webView;

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        webViewRef.getSettings().setJavaScriptEnabled(true);
        webViewRef.getSettings().setDomStorageEnabled(true);
        webViewRef.clearCache(true);
        webViewRef.clearHistory();
        webViewRef.clearFormData();

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d(TAG, value ? "🍪 Todas las cookies eliminadas" : "⚠️ No se pudieron eliminar cookies"));

        // Aseguramos tener un WebChromeClient (ayuda con confirm/alert y logs)
        webViewRef.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "JSConfirm:" + message);
                result.confirm();
                return true;
            }
        });

        webViewRef.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean deviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                Log.d(TAG, "🌐 Página cargada: " + url);

                runIfNotCancelled(() -> {
                    String urlLower = url == null ? "" : url.toLowerCase();

                    // Detectar login de forma más tolerante
                    if (!isLoggedIn && (urlLower.contains("login") || urlLower.contains("login_main") || urlLower.contains("login.asp") || urlLower.contains("logIn_main.cgi"))) {
                        Log.d(TAG, "🟡 Detectada pantalla de login. Intentando iniciar sesión...");
                        String loginScript =
                                "try{" +
                                        "var u=document.querySelector('input[name=\"username\"]')||document.querySelector('input[name=\"Username\"]');" +
                                        "var p=document.querySelector('input[name=\"syspasswd_1\"]')||document.querySelector('input[name=\"Password\"]')||document.querySelector('input[type=password]');" +
                                        "var b=document.querySelector('input[id=\"Submit\"]')||document.querySelector('input[type=submit]')||document.querySelector('button[type=submit]');" +
                                        "if(u&&p){u.value='Support';p.value='Te2010An_2014Ma'; if(b) b.click(); else { var f = u.form || p.form; if(f) f.submit(); }} else {console.log('Campos login no encontrados');}" +
                                        "}catch(e){console.error('login err', e);}";
                        view.evaluateJavascript(loginScript, v -> Log.d(TAG, "▶ Script login ejecutado: " + v));
                        isLoggedIn = true;

                        // esperar y navegar a deviceinfo.cgi
                        webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                webViewRef.loadUrl("http://192.168.1.1:8000/cgi-bin/deviceinfo.cgi")), 6000);

                        return;
                    }

                    // Extraer deviceinfo
                    if (!deviceInfoExtracted && urlLower.contains("deviceinfo.cgi")) {
                        deviceInfoExtracted = true;
                        Log.d(TAG, "📄 Extrayendo información del dispositivo...");
                        String extractScript =
                                " (function(){ var data={}; try{ " +
                                        "var get = function(sel){ var e=document.querySelector(sel); if(!e) return ''; return (e.innerText||e.value||'').trim(); };" +
                                        "data.Firmware = get('#swversion') || get('.span_deviceinfo#swversion') || get('.swversion');" +
                                        "data.Serial = get('#gsn') || get('.span_deviceinfo#gsn');" +
                                        "data.Potencia = get('#opticalRX') || get('.span_deviceinfo#opticalRX');" +
                                        "data.Ssid2 = (document.getElementById('ssidname') && document.getElementById('ssidname').innerText) || '';" +
                                        "data.Canal2 = get('#cur_wifi_channel') || get('.span_deviceinfo#cur_wifi_channel');" +
                                        "data.Estado2 = (document.getElementById('wlStatus') && document.getElementById('wlStatus').innerText) || '';" +
                                        "data.Ssid5 = (document.getElementById('ssidname_5g') && document.getElementById('ssidname_5g').innerText) || '';" +
                                        "data.Canal5 = get('#cur_wifi_channel_5g') || get('.span_deviceinfo#cur_wifi_channel_5g');" +
                                        "data.Estado5 = get('#wlStatus_5g') || '';" +
                                        "data.Voip = get('#linen_umber') || get('.span_deviceinfo#linen_umber') || '';" +
                                        "return JSON.stringify(data);" +
                                        "}catch(e){return JSON.stringify({Error: String(e)});} })();";
                        view.evaluateJavascript(extractScript, value -> runIfNotCancelled(() -> {
                            try {
                                Log.d(TAG, "Raw deviceinfo JS value: " + value);
                                // Normalizar: value viene con comillas dobles por evaluateJavascript
                                String jsonString = value;
                                if (jsonString == null || jsonString.trim().isEmpty() || jsonString.equals("null")) {
                                    throw new JSONException("JS returned empty/null");
                                }
                                // quitar comillas exteriores si las tuviera
                                if (jsonString.startsWith("\"") && jsonString.endsWith("\"")) {
                                    jsonString = jsonString.substring(1, jsonString.length() - 1)
                                            .replace("\\\\", "\\")
                                            .replace("\\\"", "\"");
                                }
                                JSONObject json = new JSONObject(jsonString);

                                resultado.setFirmware(filtrar(json.optString("Firmware", "No disponible")));
                                resultado.setSerial(filtrar(json.optString("Serial", "No disponible")));
                                resultado.setPotencia(filtrar(json.optString("Potencia", "No disponible")));
                                resultado.setSsid2(filtrar(json.optString("Ssid2", "No disponible")));
                                resultado.setCanal2(filtrar(json.optString("Canal2", "No disponible")));
                                resultado.setEstado2(filtrar(json.optString("Estado2", "No disponible")));
                                resultado.setSsid5(filtrar(json.optString("Ssid5", "No disponible")));
                                resultado.setCanal5(filtrar(json.optString("Canal5", "No disponible")));
                                resultado.setEstado5(filtrar(json.optString("Estado5", "No disponible")));
                                resultado.setVoip(filtrar(json.optString("Voip", "No disponible")));

                                Log.d(TAG, "✅ Info dispositivo extraída: " + resultado.toString());

                                webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                        webViewRef.loadUrl("http://192.168.1.1:8000/cgi-bin/multipuesto.cgi")), 3000);

                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) listener.onHguResult(false, null, 500);
                            }
                        }));
                        return;
                    }

                    // extraer usuario PPP
                    if (urlLower.contains("multipuesto.cgi")) {
                        Log.d(TAG, "📄 Extrayendo usuario PPP...");
                        String extractUser = "(function(){ try{ var v=document.querySelector('input#pppUserName')||document.querySelector('input[name=pppUserName]'); return (v && (v.value||v.innerText))? (v.value||v.innerText) : 'N/A'; }catch(e){return 'N/A';}})();";
                        view.evaluateJavascript(extractUser, value -> runIfNotCancelled(() -> {
                            String usuario = "N/A";
                            if (value != null) usuario = value.replace("\"", "");
                            resultado.setUsuario(filtrar(usuario));
                            Log.d(TAG, "🏁 Resultado final HGU2741: " + resultado.toString());
                            if (listener != null && !cancelado) listener.onHguResult(true, resultado, 200);
                        }));
                    }
                });
            }
        });

        Log.d(TAG, "🌍 Cargando página inicial HGU2741...");
        webViewRef.loadUrl("http://192.168.1.1:8000/");
    }
}
