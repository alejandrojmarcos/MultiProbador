package com.ajmarcos.multiprobador;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu3505_ {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private Context context;
    private hgu3505Listener listener;

    public Hgu3505_(Context context) {
        this.context = context;
    }

    public interface hgu3505Listener {
        void onHgu3505Result(boolean success, Resultado resultado, int code);
    }

    public void setHgu3505Listener(hgu3505Listener listener) {
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
    public void scrap3505(WebView webView) {
        this.webViewRef = webView;
        cancelado = false;

        Log.d("scrap", "scrap 3505");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookies(value -> Log.d("Cookies", value ? "Todas las cookies eliminadas" : "No se pudieron eliminar cookies"));

        webView.setWebViewClient(new WebViewClient() {
            private boolean isPPPExtracted = false;
            private boolean isTEInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                super.onPageFinished(view, url);
                Log.d("🌍", "Página cargada: " + url);

                // --- LOGIN ---
                if (url.equals("http://192.168.1.1:8000/") && !isPPPExtracted) {
                    String loginScript = "try {" +
                            "var frame = document.getElementsByName('loginfrm')[0];" +
                            "var frameDoc = frame.contentDocument || frame.contentWindow.document;" +
                            "frameDoc.getElementsByName('Username')[0].value = 'Support';" +
                            "frameDoc.getElementsByName('Password')[0].value = 'Te2010An_2014Ma';" +
                            "frameDoc.querySelector('input[type=\\'submit\\']').click();" +
                            "} catch (e) { console.error(e); }";

                    view.evaluateJavascript(loginScript, v -> Log.d("Login", "Script de login ejecutado"));

                    view.postDelayed(() ->
                            webView.loadUrl("http://192.168.1.1:8000/wanL3Edit.cmd?serviceId=1&wanIfName=ppp0.1&ntwkPrtcl=0"), 3000);
                }

                // --- EXTRAER USUARIO PPP ---
                else if (url.equals("http://192.168.1.1:8000/wanL3Edit.cmd?serviceId=1&wanIfName=ppp0.1&ntwkPrtcl=0") && !isPPPExtracted) {
                    String extractPPPUserScript = "try { var input = document.querySelector('input[name=\\'pppUserName\\']'); input ? input.value : 'Campo no encontrado'; } catch (e) { 'Error: ' + e.message; }";
                    view.evaluateJavascript(extractPPPUserScript, value -> {
                        resultado.setUsuario(filtrar(value.replace("\"", "")));
                        Log.d("PPPUserExtraction", "Usuario PPP: " + resultado.getUsuario());
                        isPPPExtracted = true;
                        view.postDelayed(() -> webView.loadUrl("http://192.168.1.1:8000/te_info.html"), 3000);
                    });
                }

                // --- EXTRAER DATOS te_info.html ---
                else if (url.equals("http://192.168.1.1:8000/te_info.html") && !isTEInfoExtracted) {
                    String extractTEInfoScript =
                            "try {" +
                                    "var data = {" +
                                    "Firmware: document.getElementById('tdSw')?.innerText || 'No disponible'," +
                                    "Serial: document.getElementById('tdId')?.innerText || 'No disponible'," +
                                    "Potencia: document.getElementById('tdRx')?.innerText || 'No disponible'," +
                                    "Ssid2: document.getElementById('tdSsid0')?.innerText || 'No disponible'," +
                                    "Canal2: document.getElementById('tdCh0')?.innerText || 'No disponible'," +
                                    "Estado2: document.getElementById('tdWl0')?.innerText || 'No disponible'," +
                                    "Ssid5: document.getElementById('tdSsid1')?.innerText || 'No disponible'," +
                                    "Canal5: document.getElementById('tdCh1')?.innerText || 'No disponible'," +
                                    "Estado5: document.getElementById('tdWl1')?.innerText || 'No disponible'," +
                                    "Voip: document.getElementById('tdNum')?.innerText || 'No disponible'" +
                                    "}; JSON.stringify(data);" +
                                    "} catch(e){ 'Error al extraer datos: ' + e.message; }";

                    view.evaluateJavascript(extractTEInfoScript, value -> {
                        try {
                            String cleaned = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
                            JSONObject json = new JSONObject(cleaned);

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

                            Log.d("✅ Resultado 3505", resultado.toString());
                            isTEInfoExtracted = true;
                            finalizar(true, resultado, 200);

                        } catch (JSONException e) {
                            Log.e("TEInfoExtraction", "Error JSON: " + e.getMessage());
                            finalizar(false, null, 500);
                        }
                    });
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }

    public void cancelar() {
        cancelado = true;
        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
            } catch (Exception ignored) {}
        }
    }

    private void finalizar(boolean exito, Resultado resultado, int codigo) {
        cancelado = true;
        if (listener != null) listener.onHgu3505Result(exito, resultado, codigo);
    }
}
