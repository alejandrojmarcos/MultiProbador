package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu2541 {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;
    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());

    private Context context;
    private AlertDialog dialog;

    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.d(TAG, "❌ Timeout alcanzado, cancelando proceso (2541).");
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu2541(Context ctx) {
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

        dismissDialog();

        if (listener != null) {
            listener.onHguResult(false, null, 999);
        }
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
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
    public void scrap2541() {
        cancelado = false;

        // Crear WebView internamente
        webViewRef = new WebView(context);
        WebView webView = webViewRef;

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        // Mostrar WebView en diálogo
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("🧭 WebView Debug (HGU2541)");
            builder.setView(webView);
            builder.setPositiveButton("Cerrar", (d, w) -> d.dismiss());
            dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "No se pudo mostrar el WebView en diálogo: " + e.getMessage());
        }

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d(TAG, value ? "🍪 Todas las cookies eliminadas" : "⚠️ No se pudieron eliminar todas las cookies"));

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean isDeviceInfoExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.equals("http://192.168.1.1:8000/")) {
                        String loginScript = "try {" +
                                "var usuarioField = document.querySelector('input[name=\"user\"]');" +
                                "var passwordField = document.querySelector('input[name=\"pass\"]');" +
                                "var loginButton = document.querySelector('input[id=\"acceptLogin\"]');" +
                                "if(usuarioField && passwordField && loginButton) {" +
                                "usuarioField.value = 'Support';" +
                                "passwordField.value = 'Te2010An_2014Ma';" +
                                "loginButton.click();" +
                                "}" +
                                "} catch(e) { console.error(e); }";

                        view.evaluateJavascript(loginScript, v -> Log.d(TAG, "▶ Script login ejecutado"));
                        isLoggedIn = true;

                        webView.postDelayed(() ->
                                runIfNotCancelled(() ->
                                        webView.loadUrl("http://192.168.1.1:8000/deviceinfo.cgi")), 6000);
                    }

                    // 2. Extraer info deviceinfo.cgi
                    else if (url.contains("deviceinfo.cgi") && !isDeviceInfoExtracted) {
                        isDeviceInfoExtracted = true;

                        String extractScript =
                                "var data={};" +
                                        "try{" +
                                        "data.Firmware=document.evaluate('/html/body/div/div[5]/div[1]/div[4]/div[4]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Serial=document.evaluate('/html/body/div/div[5]/div[1]/div[4]/div[6]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Potencia=document.evaluate('/html/body/div/div[5]/div[3]/div[2]/div[3]/span[1]', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Ssid2=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[3]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Canal2=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[2]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Estado2=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[1]/label', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Ssid5=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[8]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Canal5=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[7]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Estado5=document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[6]/label', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "data.Voip=document.evaluate('/html/body/div/div[5]/div[4]/div[2]/div[1]/span', document, null, XPathResult.STRING_TYPE, null).stringValue||'No disponible';" +
                                        "JSON.stringify(data);" +
                                        "} catch(e){ console.error(e); JSON.stringify({Error:e.message}); }";

                        view.evaluateJavascript(extractScript, value -> runIfNotCancelled(() -> {
                            try {
                                String jsonString = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
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

                                webView.postDelayed(() ->
                                        runIfNotCancelled(() ->
                                                webView.loadUrl("http://192.168.1.1:8000/multipuesto.cgi")), 3000);

                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Error parseando JSON: " + e.getMessage());
                                if (listener != null && !cancelado) listener.onHguResult(false, null, 500);
                            }
                        }));
                    }

                    // 3. Extraer usuario multipuesto y finalizar
                    else if (url.contains("multipuesto.cgi")) {
                        String extractUserScript = "document.querySelector('input#pppUserName')?.value || 'N/A';";
                        view.evaluateJavascript(extractUserScript, value -> runIfNotCancelled(() -> {
                            resultado.setUsuario(filtrar(value.replace("\"", "")));
                            Log.d(TAG, "🏁 Resultado final HGU2541: " + resultado.toString());
                            if (listener != null && !cancelado) listener.onHguResult(true, resultado, 200);
                        }));
                    }
                });
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }
}
