package com.ajmarcos.multiprobador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hgu8115 {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private Context context;

    private static final int TIMEOUT_MS = 30000; // 30 segundos
    private final Handler timeoutHandler = new Handler();

    private hgu8115Listener listener;

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


    public interface hgu8115Listener {
        void onHgu8115Result(boolean success, Resultado resultado, int code);
    }

    public void setHgu8115Listener(hgu8115Listener listener) {
        this.listener = listener;
    }

    public Hgu8115(Context context) {
        this.context = context;
    }

    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelado = true;
            Log.d(TAG, CLASS + ": Timeout alcanzado, cancelando proceso.");
            if (listener != null) listener.onHgu8115Result(false, resultado, 408);
        }
    };

    private String filtrar(String texto) {
        if (texto == null) return "";
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars)
                if (c == v) {
                    filtered.append(c);
                    break;
                }
        }
        return filtered.toString();
    }

    private boolean isCharValid(char c, char[] validChars) {
        for (char v : validChars) if (c == v) return true;
        return false;
    }

    @SuppressLint("SetJavaScriptEnabled")
    public Resultado scrap8115(WebView webView) {
        Log.d(TAG, CLASS + ": scrap8115 iniciado");
        cancelado = false;
        resultado = new Resultado();

        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        this.webViewRef = webView;
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean firmwareAndSerialExtracted = false;
            private boolean wanDetailsExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                super.onPageFinished(view, url);
                Log.d(TAG, CLASS + ": Página cargada -> " + url);

                // 1️⃣ Login automático
                if (!isLoggedIn && url.contains("login.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String loginScript =
                                "try {" +
                                        "var usernameField = document.getElementsByName('Username')[0];" +
                                        "var passwordField = document.getElementsByName('Password')[0];" +
                                        "if (usernameField && passwordField) {" +
                                        "usernameField.value = 'Support';" +
                                        "passwordField.value = 'Te2010An_2014Ma';" +
                                        "document.querySelector('input[type=\\'submit\\']').click();" +
                                        "}" +
                                        "} catch (e) { console.error(e); }";
                        view.evaluateJavascript(loginScript, value -> {
                            Log.d(TAG, CLASS + ": Script de login ejecutado.");
                            view.postDelayed(() -> {
                                if (!cancelado) webView.loadUrl("http://192.168.1.1:8000/summary.asp");
                            }, 3000);
                        });
                        isLoggedIn = true;
                    }, 3000);
                    return;
                }

                // 2️⃣ Extraer firmware y serial
                if (isLoggedIn && !firmwareAndSerialExtracted && url.contains("summary.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String extractScript =
                                "try {" +
                                        "var firmware = document.evaluate('/html/body/table/tbody/tr[2]/td', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                        "var serial = document.evaluate('/html/body/table/tbody/tr[9]/td', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                        "JSON.stringify({Firmware: firmware, Serial: serial});" +
                                        "} catch (e) { JSON.stringify({Error: e.message}); }";
                        view.evaluateJavascript(extractScript, value -> {
                            if (cancelado) return;
                            Log.d(TAG, CLASS + ": Datos extraídos -> " + value);

                            String v = value.replace("Firmware", "").replace("Serial", "").replace("ASKY", "41534B59");
                            String[] nuevo = v.split(",");

                            char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
                            StringBuilder filtered = new StringBuilder();
                            if (nuevo.length > 0) {
                                for (char c : nuevo[0].toCharArray())
                                    if (isCharValid(c, validChars)) filtered.append(c);
                                resultado.setFirmware(filtered.toString());
                            }
                            filtered.setLength(0);
                            if (nuevo.length > 1) {
                                for (char c : nuevo[1].toCharArray())
                                    if (isCharValid(c, validChars)) filtered.append(c);
                                resultado.setSerial(filtered.toString());
                            }

                            Log.d(TAG, CLASS + ": Firmware -> " + resultado.getFirmware() + " ; Serial -> " + resultado.getSerial());
                            firmwareAndSerialExtracted = true;

                            webView.loadUrl("http://192.168.1.1:8000/wanintf.asp");
                        });
                    }, 3000);
                    return;
                }

                // 3️⃣ Click para abrir detalles WAN
                if (firmwareAndSerialExtracted && !wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String clickScript =
                                "try {" +
                                        "var link = document.evaluate('/html/body/fieldset[1]/table/tbody/tr[1]/td[2]/a', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;" +
                                        "if (link) link.click();" +
                                        "} catch (e) { console.error(e); }";
                        view.evaluateJavascript(clickScript, val ->
                                Log.d(TAG, CLASS + ": Clic en enlace WAN realizado."));
                        wanDetailsExtracted = true;
                    }, 3000);
                }

                // 4️⃣ Extraer usuario PPP y arrancar SSH
                if (wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String extractUserScript =
                                "try {" +
                                        "var inputField = document.evaluate('/html/body/fieldset[2]/div/form/fieldset[2]/fieldset[1]/div[1]/input', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;" +
                                        "inputField ? JSON.stringify({Username: inputField.value}) : JSON.stringify({Error: 'Campo no encontrado'});" +
                                        "} catch (e) { JSON.stringify({Error: e.message}); }";
                        view.evaluateJavascript(extractUserScript, value -> {
                            if (cancelado) return;
                            Log.d(TAG, CLASS + ": Username raw -> " + value);

                            StringBuilder filtered2 = new StringBuilder();
                            char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
                            for (char c : value.toCharArray()) {
                                if (isCharValid(c, validChars)) filtered2.append(c);
                            }

                            resultado.setUsuario(filtrar(filtered2.toString()).replace("Username",""));
                            Log.d(TAG, CLASS + ": Usuario PPP -> " + resultado.getUsuario());
                            iniciarSSH();
                        });
                    }, 3000);
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/login.asp");
        return resultado;
    }

    private void iniciarSSH() {
        if (cancelado) return;
        timeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);

        resultado.setUsuario(filtrar(resultado.getUsuario()));
        Log.d(TAG, CLASS + ": Iniciando SSH 8225 desde Hgu8115");

        Ssh8115 ssh = new Ssh8115();
        ssh.setSshListener((success, message, code) -> {
            if (cancelado) return;
            timeoutHandler.removeCallbacks(timeoutRunnable);

            if (success && message != null) {
                try {
                    resultado.setFecha(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    resultado.setCatalogo(catal);
                    resultado.setModelo(model);
                    resultado.setMultiprobador(multi);
                    if (message.length > 0) resultado.setSsid2(filtrar(message[0]));
                    if (message.length > 1) resultado.setCanal2(filtrar(message[1]));
                    if (message.length > 2) resultado.setEstado2(filtrar(message[2]));
                    if (message.length > 3) resultado.setSsid5(filtrar(message[3]));
                    if (message.length > 4) resultado.setCanal5(filtrar(message[4]));
                    if (message.length > 5) resultado.setEstado5(filtrar(message[5]));
                    if (message.length > 6) resultado.setPotencia(filtrar(message[6]));
                } catch (Exception e) {
                    Log.e(TAG, CLASS + ": Error mapeando salida SSH -> " + e.getMessage());
                }

                Log.d(TAG, CLASS + ": SSH ok. Resultado final -> " + resultado);
                if (listener != null) listener.onHgu8115Result(true, resultado, 200);
            } else {
                Log.d(TAG, CLASS + ": SSH falló o mensaje nulo");
                if (listener != null) listener.onHgu8115Result(false, resultado, code);
            }
        });

        ssh.start();
    }

    public void cancelar() {
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
                Log.e(TAG, CLASS + ": Error deteniendo WebView -> " + e.getMessage());
            }
        }
    }
}
