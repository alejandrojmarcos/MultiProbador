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

public class Hgu3505 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.d(TAG, "Timeout alcanzado, cancelando proceso (3505).");
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
    public void scrap3505(WebView webView) {
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
            private boolean isPPPExtracted = false;
            private boolean isTEInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.equals("http://192.168.1.1:8000/")) {
                        String loginScript = "try {" +
                                "   var frame = document.getElementsByName('loginfrm')[0];" +
                                "   var frameDoc = frame.contentDocument || frame.contentWindow.document;" +
                                "   frameDoc.getElementsByName('Username')[0].value = 'Support';" +
                                "   frameDoc.getElementsByName('Password')[0].value = 'Te2010An_2014Ma';" +
                                "   frameDoc.querySelector('input[type=\\'submit\\']').click();" +
                                "} catch (e) { console.error('Error login:', e); }";

                        view.evaluateJavascript(loginScript, null);
                        isLoggedIn = true;

                        webView.postDelayed(() -> runIfNotCancelled(() ->
                                webView.loadUrl("http://192.168.1.1:8000/wanL3Edit.cmd?serviceId=1&wanIfName=ppp0.1&ntwkPrtcl=0")), 4000);
                    }

                    // 2. Extraer PPP User
                    else if (url.contains("wanL3Edit.cmd") && !isPPPExtracted) {
                        String extractPPPUserScript =
                                "document.querySelector('input[name=\\'pppUserName\\']')?.value || 'No disponible';";

                        view.evaluateJavascript(extractPPPUserScript, value -> runIfNotCancelled(() -> {
                            resultado.usuario = filtrar((value != null) ? value.replace("\"", "") : "No disponible");
                            isPPPExtracted = true;

                            webView.postDelayed(() -> runIfNotCancelled(() ->
                                    webView.loadUrl("http://192.168.1.1:8000/te_info.html")), 3000);
                        }));
                    }

                    // 3. Extraer datos de te_info.html
                    else if (url.contains("te_info.html") && !isTEInfoExtracted) {
                        String extractTEInfoScript =
                                "var data = {};" +
                                        "try {" +
                                        "data.Firmware = document.getElementById('tdSw')?.innerText || 'No disponible';" +
                                        "data.Serial = document.getElementById('tdId')?.innerText || 'No disponible';" +
                                        "data.Potencia = document.getElementById('tdRx')?.innerText || 'No disponible';" +
                                        "data.Ssid2 = document.getElementById('tdSsid0')?.innerText || 'No disponible';" +
                                        "data.Canal2 = document.getElementById('tdCh0')?.innerText || 'No disponible';" +
                                        "data.Estado2 = document.getElementById('tdWl0')?.innerText || 'No disponible';" +
                                        "data.Ssid5 = document.getElementById('tdSsid1')?.innerText || 'No disponible';" +
                                        "data.Canal5 = document.getElementById('tdCh1')?.innerText || 'No disponible';" +
                                        "data.Estado5 = document.getElementById('tdWl1')?.innerText || 'No disponible';" +
                                        "data.Voip = document.getElementById('tdNum')?.innerText || 'No disponible';" +
                                        "JSON.stringify(data);" +
                                        "} catch(e){ console.error(e); JSON.stringify({Error: e.message}); }";

                        view.evaluateJavascript(extractTEInfoScript, value -> runIfNotCancelled(() -> {
                            try {
                                String jsonString = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
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

                                isTEInfoExtracted = true;
                                if (listener != null && !cancelado) {
                                    listener.onHguResult(true, resultado, 200);
                                }

                            } catch (JSONException e) {
                                Log.e("JSONError3505", "Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) {
                                    listener.onHguResult(false, null, 500);
                                }
                            }
                        }));
                    }
                });
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");

    }
}
