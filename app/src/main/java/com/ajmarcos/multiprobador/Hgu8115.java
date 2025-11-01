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

public class Hgu8115 implements Ssh8115.SshListener {

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
            Log.e(TAG, "❌ Timeout alcanzado, cancelando proceso (8115).");
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu8115(Context ctx) {
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
    public void scrap8115() {
        cancelado = false;

        // Crear WebView internamente
        webViewRef = new WebView(context);

        // Mostrar WebView en diálogo
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("🧭 WebView Debug (HGU8115)");
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

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d(TAG, value ? "🍪 Cookies eliminadas correctamente" : "⚠️ No se pudieron eliminar cookies"));

        webViewRef.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean summaryExtracted = false;
            private boolean wanExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                Log.d(TAG, "🌐 Página cargada: " + url);

                runIfNotCancelled(() -> {
                    // 1. Login
                    if (!isLoggedIn && url.contains("login.asp")) {
                        Log.d(TAG, "🟡 Detectada pantalla de login. Intentando iniciar sesión...");
                        String loginScript =
                                "try {" +
                                        "document.getElementsByName('Username')[0].value='Support';" +
                                        "document.getElementsByName('Password')[0].value='Te2010An_2014Ma';" +
                                        "document.querySelector('input[type=\\'submit\\']').click();" +
                                        "} catch(e){ console.error(e); }";
                        view.evaluateJavascript(loginScript, v -> Log.d(TAG, "▶ Script login ejecutado."));
                        isLoggedIn = true;

                        webViewRef.postDelayed(() -> runIfNotCancelled(() ->
                                webViewRef.loadUrl("http://192.168.1.1:8000/summary.asp")), 2000);
                    }

                    // 2. Extraer firmware y serial
                    else if (isLoggedIn && !summaryExtracted && url.contains("summary.asp")) {
                        Log.d(TAG, "📄 Extrayendo Firmware y Serial...");
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
                                resultado.setFirmware(filtrar(json.optString("Firmware", "No disponible")));
                                resultado.setSerial(filtrar(json.optString("Serial", "No disponible")));
                                summaryExtracted = true;
                                Log.d(TAG, "✅ Firmware: " + resultado.getFirmware() + " | Serial: " + resultado.getSerial());
                                webViewRef.loadUrl("http://192.168.1.1:8000/wanintf.asp");
                            } catch (JSONException e) {
                                Log.e(TAG, "❌ Error JSON summary: " + e.getMessage());
                            }
                        });
                    }

                    // 3. Extraer usuario PPP
                    else if (summaryExtracted && !wanExtracted && url.contains("wanintf.asp")) {
                        Log.d(TAG, "📄 Extrayendo usuario PPP...");
                        String extractUser =
                                "try {" +
                                        "var input = document.evaluate('/html/body/fieldset[2]/div/form/fieldset[2]/fieldset[1]/div[1]/input', document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;" +
                                        "input ? JSON.stringify({Username: input.value}) : JSON.stringify({Error:'Campo no encontrado'});" +
                                        "} catch(e){ JSON.stringify({Error:e.message}); }";
                        view.evaluateJavascript(extractUser, value -> {
                            if (cancelado) return;
                            value = value.replace("\"Username\"", "").replace("\\\"", "\"");
                            resultado.setUsuario(filtrar(value));
                            wanExtracted = true;
                            Log.d(TAG, "✅ Usuario PPP: " + resultado.getUsuario());
                            startSsh();
                        });
                    }
                });
            }
        });

        webViewRef.setWebChromeClient(new WebChromeClient()); // soporte JS completo
        Log.d(TAG, "🌍 Cargando página inicial HGU8115...");
        webViewRef.loadUrl("http://192.168.1.1:8000/login.asp");
    }

    private void startSsh() {
        if (cancelado) return;
        Log.d(TAG, "🔑 Iniciando SSH...");
        Ssh8115 ssh = new Ssh8115();
        ssh.setSshListener(this);
        ssh.start();
    }

    @Override
    public void onSsh8115Result(boolean success, String[] message, int code) {
        if (cancelado) return;
        timeoutHandler.removeCallbacks(timeoutRunnable);

        if (success) {
            Log.d(TAG, "🏁 SSH completado. Mapeando campos...");
            resultado.setPotencia(message.length > 6 ? message[6] : "");
            resultado.setSsid2(message.length > 0 ? message[0] : "");
            resultado.setEstado2(message.length > 2 ? message[2] : "");
            resultado.setCanal2(message.length > 1 ? message[1] : "");
            resultado.setSsid5(message.length > 3 ? message[3] : "");
            resultado.setEstado5(message.length > 5 ? message[5] : "");
            resultado.setCanal5(message.length > 4 ? message[4] : "");
            resultado.setVoip(message.length > 7 ? message[7] : "");

            Log.d(TAG, "🏁 Resultado final HGU8115: " + resultado.toString());
            if (listener != null) listener.onHguResult(true, resultado, 200);
        } else {
            Log.e(TAG, "❌ SSH falló con código: " + code);
            if (listener != null) listener.onHguResult(false, null, 201);
        }
    }
}
