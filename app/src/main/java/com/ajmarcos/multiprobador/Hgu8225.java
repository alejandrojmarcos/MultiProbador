package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hgu8225 implements Ssh8225.SshListener {

    private boolean sshStarted = false;
    private boolean usuarioExtracted = false;

    private String usuario = "";
    private String firmware = "";
    private String serial = "";
    private String ipPrueba="192.168.1.2";

    private boolean cancelado = false;
    private WebView webViewRef = null;
    private hguListener listener;
    private Resultado resultado = new Resultado();

    private Context context;
    private AlertDialog dialog;

    private static final int TIMEOUT_MS = 60000; // 60s
    private final Handler timeoutHandler = new Handler();
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelado = true;
            Log.e(TAG, "❌ Timeout alcanzado, cancelando proceso.");
            dismissDialog();
            if (listener != null) listener.onHguResult(false, null, 408);
        }
    };

    public interface hguListener {
        void onHguResult(boolean success, Resultado resultado, int code);
    }

    public void setHguListener(hguListener listener) {
        this.listener = listener;
    }

    public Hgu8225(Context ctx) {
        this.context = ctx;
    }

    public void cancelar() {
        cancelado = true;
        Log.w(TAG, "⚠️ Proceso cancelado por el usuario.");
        dismissDialog();
        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
            } catch (Exception e) {
                Log.e(TAG, "Error al detener WebView: " + e.getMessage());
            }
        }
        timeoutHandler.removeCallbacks(timeoutRunnable);
        if (listener != null) listener.onHguResult(false, null, 999);
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
        if (texto == null) return "";
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) {
                if (c == v) { filtered.append(c); break; }
            }
        }
        return filtered.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap8225() {
        cancelado = false;

        // Activar timeout
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        // Crear WebView internamente
        webViewRef = new WebView(context);
        webViewRef.getSettings().setJavaScriptEnabled(true);
        webViewRef.getSettings().setDomStorageEnabled(true);
        webViewRef.clearCache(true);
        webViewRef.clearHistory();

        // Mostrar en diálogo
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("🧭 WebView Debug (HGU8225)");
            builder.setView(webViewRef);
            builder.setPositiveButton("Cerrar", (d, w) -> d.dismiss());
            dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "⚠️ No se pudo mostrar el WebView en diálogo: " + e.getMessage());
        }

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d(TAG, value ? "🍪 Cookies eliminadas correctamente" : "⚠️ No se pudieron eliminar cookies"));

        webViewRef.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean summaryExtracted = false;
            private boolean wanExtracted = false;

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d(TAG, "🔄 Cargando página: " + url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "➡️ Redirección detectada hacia: " + url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                super.onPageFinished(view, url);
                Log.d(TAG, "🌐 Página cargada: " + url);

                // Login
                if (!isLoggedIn && url.contains("login.asp")) {
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String loginScript =
                                "try {" +
                                        "var u = document.getElementsByName('Username')[0];" +
                                        "var p = document.getElementsByName('Password')[0];" +
                                        "if(u && p){ u.value='Support'; p.value='Te2010An_2014Ma';" +
                                        "var btn = document.querySelector('input[type=\\'submit\\']');" +
                                        "if(btn){ btn.click(); } else { var form=document.querySelector('form'); if(form) form.submit(); } }" +
                                        "} catch(e){ console.error(e); }";
                        view.evaluateJavascript(loginScript, v -> view.postDelayed(() ->
                                runIfNotCancelled(() -> webViewRef.loadUrl("http://"+ipPrueba+":8000/summary.asp")), 3000));
                        isLoggedIn = true;
                    }), 3000);
                    return;
                }

                // Extraer summary
                if (isLoggedIn && !summaryExtracted && url.contains("summary.asp")) {
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String extractScript =
                                "try {" +
                                        "var firmware = document.evaluate('/html/body/table/tbody/tr[2]/td', document,null,XPathResult.STRING_TYPE,null).stringValue || 'No disponible';" +
                                        "var serial = document.evaluate('/html/body/table/tbody/tr[8]/td', document,null,XPathResult.STRING_TYPE,null).stringValue || 'No disponible';" +
                                        "JSON.stringify({Firmware: firmware, Serial: serial});" +
                                        "} catch(e){ JSON.stringify({Error:e.message}); }";
                        view.evaluateJavascript(extractScript, value -> {
                            if (cancelado) return;
                            try {
                                String clean = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
                                String fw = "No disponible";
                                String sn = "No disponible";
                                if (clean.startsWith("{") && clean.contains("Firmware")) {
                                    int fIdx = clean.indexOf("Firmware");
                                    int sIdx = clean.indexOf("Serial");
                                    if (fIdx >= 0) {
                                        int colon = clean.indexOf(":", fIdx);
                                        int comma = clean.indexOf(",", fIdx);
                                        if (colon >= 0 && comma >= 0) {
                                            fw = clean.substring(colon + 1, comma).replace("\"", "").trim();
                                        }
                                    }
                                    if (sIdx >= 0) {
                                        int colon = clean.indexOf(":", sIdx);
                                        int end = clean.indexOf("}", sIdx);
                                        if (colon >= 0 && end >= 0) {
                                            sn = clean.substring(colon + 1, end).replace("\"", "").trim();
                                        }
                                    }
                                }
                                firmware = filtrar(fw);
                                serial = filtrar(sn);
                                Log.d(TAG, "✅ Firmware: " + firmware + " | Serial: " + serial);
                                summaryExtracted = true;
                                webViewRef.loadUrl("http://"+ipPrueba+":8000/wanintf.asp");
                            } catch (Exception e) {
                                Log.e(TAG, "❌ Error parseando summary: " + e.getMessage());
                            }
                        });
                    }), 4000);
                    return;
                }

                // Extraer usuario PPP
                if (summaryExtracted && !wanExtracted && url.contains("wanintf.asp")) {
                    wanExtracted = true; // <- marcar al inicio para evitar repeticiones
                    view.postDelayed(() -> runIfNotCancelled(() -> {
                        String clickAndExtractUser =
                                "try {" +
                                        "console.log('🔍 Buscando enlace WAN...');" +
                                        "var enlace = document.evaluate('/html/body/fieldset[1]/table/tbody/tr[1]/td[2]/a', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;" +
                                        "if (enlace) {" +
                                        "   console.log('✅ Enlace WAN encontrado, haciendo clic...');" +
                                        "   enlace.click();" +
                                        "   setTimeout(function() {" +
                                        "       console.log('🔍 Buscando campo username tras clic...');" +
                                        "       var input = document.querySelector('input[name=username]');" +
                                        "       if (input) {" +
                                        "           console.log('✅ Campo username encontrado:', input.outerHTML);" +
                                        "           window._usernameFound = JSON.stringify({Found:true, Username: input.value});" +
                                        "       } else {" +
                                        "           console.log('❌ No se encontró campo username tras clic.');" +
                                        "           window._usernameFound = JSON.stringify({Found:false});" +
                                        "       }" +
                                        "   }, 3000);" +
                                        "} else {" +
                                        "   console.log('❌ Enlace WAN no encontrado.');" +
                                        "   window._usernameFound = JSON.stringify({Found:false, Error:'No link found'});" +
                                        "}" +
                                        "} catch(e){ window._usernameFound = JSON.stringify({Error:e.message}); }";

                        view.evaluateJavascript(clickAndExtractUser, v -> {
                            view.postDelayed(() -> view.evaluateJavascript("window._usernameFound;", value -> {
                                if (cancelado) return;
                                try {
                                    String clean = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
                                    String extracted = "No disponible";
                                    if (clean.contains("Username")) {
                                        int idx = clean.indexOf("Username");
                                        int colon = clean.indexOf(":", idx);
                                        int end = clean.indexOf("}", idx);
                                        if (colon >= 0 && end >= 0)
                                            extracted = clean.substring(colon + 1, end).replace("\"", "").trim();
                                    }
                                    if (!usuarioExtracted) {
                                        usuario = filtrar(extracted);
                                        Log.d(TAG, "✅ Usuario PPP extraído tras clic: " + usuario);
                                        usuarioExtracted = true;

                                        if (!sshStarted) {
                                            sshStarted = true;
                                            Log.d(TAG, "🔐 Iniciando conexión SSH para obtener datos adicionales...");
                                            startSsh();
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "❌ Error parseando usuario PPP tras clic: " + e.getMessage());
                                }
                            }), 4000);
                        });
                    }), 4000);
                }

            }
        });

        webViewRef.setWebChromeClient(new WebChromeClient());
        Log.d(TAG, "🌍 Cargando página de login inicial...");
        webViewRef.loadUrl("http://"+ipPrueba+":8000/login.asp");
    }

    private void startSsh() {
        if (cancelado) return;
        dismissDialog();
        Log.d(TAG, "🔐 Iniciando conexión SSH para obtener datos adicionales...");
        Ssh8225 ssh = new Ssh8225();
        ssh.setSshListener(this);
        ssh.start();
    }

    @Override
    public void onSsh8225Result(boolean success, String[] message, int code) {

        for(String m : message){
            Log.d(TAG, "✅ Resultado SSH recibido: " + m);
        }


        if (cancelado) return;
        timeoutHandler.removeCallbacks(timeoutRunnable);

        Log.d(TAG, "📡 Resultado SSH recibido: success=" + success + " | code=" + code);

        if (success) {
            resultado.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            resultado.setFirmware(firmware);
            resultado.setSerial(serial);
            resultado.setPotencia(message.length > 6 ? message[6] : "");
            resultado.setSsid2(message.length > 0 ? message[0] : "");
            resultado.setEstado2(message.length > 2 ? message[2] : "");
            resultado.setCanal2(message.length > 1 ? message[1] : "");
            resultado.setRssi2("-50");
            resultado.setSsid5(message.length > 3 ? message[3] : "");
            resultado.setEstado5(message.length > 5 ? message[5] : "");
            resultado.setCanal5(message.length > 4 ? message[4] : "");
            resultado.setRssi5("-50");
            resultado.setUsuario(usuario);
            resultado.setVoip(message.length > 7 ? message[7] : "");
            resultado.setFalla("sin falla");
            resultado.setCondicion("ok");
            resultado.setVoip("-");

            Log.d(TAG, "🏁 Resultado final HGU8225: " + resultado.toString());
            if (listener != null) listener.onHguResult(true, resultado, 200);
        } else {
            Log.e(TAG, "❌ SSH falló con código " + code);
            if (listener != null) listener.onHguResult(false, null, 201);
        }
    }
}
