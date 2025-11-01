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

public class Hgu3505 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;
    private AlertDialog dialog;
    private Context context;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.e(TAG, "❌ Timeout alcanzado, cancelando proceso (3505).");
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu3505(Context ctx) {
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

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }

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
    public void scrap3505() {
        cancelado = false;

        // Crear WebView internamente
        webViewRef = new WebView(context);

        // Mostrar WebView en diálogo
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("🧭 WebView Debug (HGU3505)");
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
            private boolean isPPPExtracted = false;
            private boolean isTEInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                Log.d(TAG, "🌐 Página cargada: " + url);

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.equals("http://192.168.1.1:8000/")) {
                        Log.d(TAG, "🟡 Detectada pantalla de login. Intentando iniciar sesión...");
                        String loginScript = "try {" +
                                "   var frame = document.getElementsByName('loginfrm')[0];" +
                                "   var frameDoc = frame.contentDocument || frame.contentWindow.document;" +
                                "   frameDoc.getElementsByName('Username')[0].value = 'Support';" +
                                "   frameDoc.getElementsByName('Password')[0].value = 'Te2010An_2014Ma';" +
                                "   frameDoc.querySelector('input[type=\\'submit\\']').click();" +
                                "} catch (e) { console.error('Error login:', e); }";

                        view.evaluateJavascript(loginScript, v -> Log.d(TAG, "▶ Script login ejecutado."));
                        isLoggedIn = true;

                        webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                webViewRef.loadUrl("http://192.168.1.1:8000/wanL3Edit.cmd?serviceId=1&wanIfName=ppp0.1&ntwkPrtcl=0")), 4000);
                    }

                    // 2. Extraer PPP User
                    else if (url.contains("wanL3Edit.cmd") && !isPPPExtracted) {
                        Log.d(TAG, "📄 Extrayendo PPP User...");
                        String extractPPPUserScript =
                                "document.querySelector('input[name=\\'pppUserName\\']')?.value || 'No disponible';";

                        view.evaluateJavascript(extractPPPUserScript, value -> runIfNotCancelled(() -> {
                            resultado.setUsuario(filtrar((value != null) ? value.replace("\"", "") : "No disponible"));
                            Log.d(TAG, "✅ Usuario PPP extraído: " + resultado.getUsuario());
                            isPPPExtracted = true;

                            webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                    webViewRef.loadUrl("http://192.168.1.1:8000/te_info.html")), 3000);
                        }));
                    }

                    // 3. Extraer datos de te_info.html
                    else if (url.contains("te_info.html") && !isTEInfoExtracted) {
                        Log.d(TAG, "📄 Extrayendo info de TE...");
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

                                Log.d(TAG, "🏁 Resultado final HGU3505: " + resultado.toString());
                                isTEInfoExtracted = true;

                                if (listener != null && !cancelado) {
                                    listener.onHguResult(true, resultado, 200);
                                }
                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) {
                                    listener.onHguResult(false, null, 500);
                                }
                            }
                        }));
                    }
                });
            }
        });

        webViewRef.setWebChromeClient(new WebChromeClient());
        Log.d(TAG, "🌍 Cargando página inicial HGU3505...");
        webViewRef.loadUrl("http://192.168.1.1:8000/");
    }
}
