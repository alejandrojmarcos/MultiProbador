package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu8115 implements Ssh8115.SshListener {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;

    private static final int TIMEOUT_MS = 30000; // 30s
    private final Handler timeoutHandler = new Handler();
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelar();
            Log.d(TAG, "Timeout alcanzado, cancelando proceso.");
        }
    };



    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public void cancelar() {
        cancelado = true;
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
        timeoutHandler.removeCallbacks(timeoutRunnable);
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
    public void scrap8115(WebView webView) {
        cancelado = false;
        webViewRef = webView;
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean summaryExtracted = false;
            private boolean wanExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;

                // Login
                if (!isLoggedIn && url.contains("login.asp")) {
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String loginScript =
                                "try {" +
                                        "document.getElementsByName('Username')[0].value='Support';" +
                                        "document.getElementsByName('Password')[0].value='Te2010An_2014Ma';" +
                                        "document.querySelector('input[type=\\'submit\\']').click();" +
                                        "} catch(e){ console.error(e); }";
                        view.evaluateJavascript(loginScript, v ->
                                view.postDelayed(() -> runIfNotCancelled(() -> webView.loadUrl("http://192.168.1.1:8000/summary.asp")), 2000)
                        );
                        isLoggedIn = true;
                    }), 3000);
                    return;
                }

                // Extraer firmware y serial
                if (isLoggedIn && !summaryExtracted && url.contains("summary.asp")) {
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String extractScript =
                                "try {" +
                                        "var firmware = document.evaluate('/html/body/table/tbody/tr[2]/td', document,null,XPathResult.STRING_TYPE,null).stringValue || 'No disponible';" +
                                        "var serial = document.evaluate('/html/body/table/tbody/tr[9]/td', document,null,XPathResult.STRING_TYPE,null).stringValue || 'No disponible';" +
                                        "JSON.stringify({Firmware: firmware, Serial: serial});" +
                                        "} catch(e){ JSON.stringify({Error:e.message}); }";
                        view.evaluateJavascript(extractScript, value -> {
                            if (cancelado) return;
                            try {
                                value = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
                                JSONObject json = new JSONObject(value);
                                resultado.firmware = filtrar(json.optString("Firmware","No disponible"));
                                resultado.serial = filtrar(json.optString("Serial","No disponible"));
                                summaryExtracted = true;
                                webView.loadUrl("http://192.168.1.1:8000/wanintf.asp");
                            } catch (JSONException e) {
                                Log.e(TAG, "Error JSON summary: " + e.getMessage());
                            }
                        });
                    }), 3000);
                    return;
                }

                // Extraer usuario PPP
                if (summaryExtracted && !wanExtracted && url.contains("wanintf.asp")) {
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String extractUser =
                                "try {" +
                                        "var input = document.evaluate('/html/body/fieldset[2]/div/form/fieldset[2]/fieldset[1]/div[1]/input', document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;" +
                                        "input ? JSON.stringify({Username: input.value}) : JSON.stringify({Error:'Campo no encontrado'});" +
                                        "} catch(e){ JSON.stringify({Error:e.message}); }";
                        view.evaluateJavascript(extractUser, value -> {
                            if (cancelado) return;
                            value = value.replace("\"Username\"", "").replace("\\\"", "\"");
                            resultado.usuario = filtrar(value);
                            wanExtracted = true;
                            startSsh();
                        });
                    }), 3000);
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/login.asp");
    }

    private void startSsh() {
        if (cancelado) return;
        Ssh8115 ssh = new Ssh8115();
        ssh.setSshListener(this);
        ssh.start();
    }

    @Override
    public void onSsh8115Result(boolean success, String[] message, int code) {
        if (cancelado) return;
        timeoutHandler.removeCallbacks(timeoutRunnable);

        if (success) {
            // mapear campos uniformes
            resultado.potencia = message[6];
            resultado.ssid2 = message[0];
            resultado.estado2 = message[2];
            resultado.canal2 = message[1];
            resultado.ssid5 = message[3];
            resultado.estado5 = message[5];
            resultado.canal5 = message[4];
            resultado.voip = message[7]; // agregar VOIP

            if (listener != null) listener.onHguResult(true, resultado, 200);
        } else {
            if (listener != null) listener.onHguResult(false, null, 201);
        }
    }
}
