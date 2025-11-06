package com.ajmarcos.multiprobador;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu8115_ {

    private Resultado resultado = new Resultado();
    private boolean cancelado = false;
    private WebView webViewRef = null;
    private Context context;

    private static final int TIMEOUT_MS = 30000; // 30 segundos
    private final Handler timeoutHandler = new Handler();

    private hgu8115Listener listener;

    public interface hgu8115Listener {
        void onHgu8115Result(boolean success, Resultado resultado, int code);
    }

    public void setHgu8115Listener(hgu8115Listener listener) {
        this.listener = listener;
    }

    public Hgu8115_(Context context) {
        this.context = context;
    }
    private final Runnable timeoutRunnable = () -> {
        if (!cancelado) {
            cancelado = true;
            Log.d(TAG, "Timeout alcanzado, cancelando proceso.");
            if (listener != null) listener.onHgu8115Result(false, resultado, 408);
        }
    };

    private String filtrar(String texto) {
        if (texto == null) return "";
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) if (c == v) { filtered.append(c); break; }
        }
        return filtered.toString();
    }

    private boolean isCharValid(char c, char[] validChars) {
        for (char v : validChars) if (c == v) return true;
        return false;
    }

    /**
     * Inicia el scraping. Devuelve el objeto Resultado (que se va completando),
     * y al finalizar notifica al listener con el Resultado completo.
     */
    @SuppressLint("SetJavaScriptEnabled")
    public Resultado scrap8115(WebView webView) {
        Log.d("scrap", "scrap 8115 iniciado");
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
                Log.d("PageFinished", "Página cargada: " + url);

                // 1) Login automático (igual que en tu versión)
                if (!isLoggedIn && url.contains("login.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String loginScript =
                                "try {" +
                                        "   var usernameField = document.getElementsByName('Username')[0];" +
                                        "   var passwordField = document.getElementsByName('Password')[0];" +
                                        "   if (usernameField && passwordField) {" +
                                        "       usernameField.value = 'Support';" +
                                        "       passwordField.value = 'Te2010An_2014Ma';" +
                                        "       document.querySelector('input[type=\\'submit\\']').click();" +
                                        "   }" +
                                        "} catch (e) { console.error(e); }";
                        view.evaluateJavascript(loginScript, value -> {
                            Log.d("Login", "Script de login ejecutado.");
                            view.postDelayed(() -> {
                                if (!cancelado) webView.loadUrl("http://192.168.1.1:8000/summary.asp");
                            }, 3000);
                        });
                        isLoggedIn = true;
                    }, 3000);
                    return;
                }

                // 2) Extraer firmware y serial desde summary.asp
                if (isLoggedIn && !firmwareAndSerialExtracted && url.contains("summary.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String extractScript =
                                "try {" +
                                        "   var firmware = document.evaluate('/html/body/table/tbody/tr[2]/td', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                        "   var serial = document.evaluate('/html/body/table/tbody/tr[9]/td', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                        "   JSON.stringify({Firmware: firmware, Serial: serial});" +
                                        "} catch (e) {" +
                                        "   JSON.stringify({Error: e.message});" +
                                        "}";
                        view.evaluateJavascript(extractScript, value -> {
                            if (cancelado) return;
                            Log.d("FirmwareAndSerial", "Datos extraídos: " + value);

                            // el value puede venir con comillas y braces; limpiamos pero sin modificar la lógica original
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

                            Log.d(TAG, "Firmware -> " + resultado.getFirmware() + " ; Serial -> " + resultado.getSerial());
                            firmwareAndSerialExtracted = true;

                            // cargar página WAN
                            webView.loadUrl("http://192.168.1.1:8000/wanintf.asp");
                        });
                    }, 3000);
                    return;
                }

                // 3) Click para abrir detalles WAN (igual que tu versión)
                if (firmwareAndSerialExtracted && !wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String clickScript =
                                "try {" +
                                        "   var link = document.evaluate('/html/body/fieldset[1]/table/tbody/tr[1]/td[2]/a', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;" +
                                        "   if (link) link.click();" +
                                        "} catch (e) { console.error(e); }";
                        view.evaluateJavascript(clickScript, val -> Log.d("WANClick", "Clic realizado."));
                        wanDetailsExtracted = true;
                    }, 3000);
                }

                // 4) Extraer usuario PPP y arrancar SSH
                if (wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> {
                        if (cancelado) return;
                        String extractUserScript =
                                "try {" +
                                        "   var inputField = document.evaluate('/html/body/fieldset[2]/div/form/fieldset[2]/fieldset[1]/div[1]/input', document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;" +
                                        "   inputField ? JSON.stringify({Username: inputField.value}) : JSON.stringify({Error: 'Campo no encontrado'});" +
                                        "} catch (e) {" +
                                        "   JSON.stringify({Error: e.message});" +
                                        "}";
                        view.evaluateJavascript(extractUserScript, value -> {
                            if (cancelado) return;
                            Log.d("Username", "Username raw: " + value);

                            // filtramos caracteres como hacés en otras clases
                            StringBuilder filtered2 = new StringBuilder();
                            char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
                            for (char c : value.toCharArray()) {
                                if (isCharValid(c, validChars)) filtered2.append(c);
                            }
                            // setUsuario sin comillas sobrantes
                            resultado.setUsuario(filtrar(filtered2.toString()));
                            Log.d(TAG, "Usuario PPP -> " + resultado.getUsuario());

                            // iniciar SSH (sin implements: pasamos un callback lambda)
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

        resultado.setUsuario(filtrar(resultado.getUsuario())); // asegurar formato
        Log.d(TAG, "Iniciando SSH 8225 desde Hgu8115_");

        Ssh8225_8115 ssh = new Ssh8225_8115();
        // no usamos implements; registramos listener lambda
        ssh.setSshListener((success, message, code) -> {
            if (cancelado) return;
            timeoutHandler.removeCallbacks(timeoutRunnable);

            if (success && message != null) {
                // llenamos el resto de campos del resultado a partir del array SSH (mapeo como en tu versión original)
                // mensaje: message[6], message[0], message[1], message[2], message[3], message[4], message[5]
                try {
                    if (message.length > 0) resultado.setSsid2(filtrar(message[0]));
                    if (message.length > 1) resultado.setCanal2(filtrar(message[1]));
                    if (message.length > 2) resultado.setEstado2(filtrar(message[2]));
                    if (message.length > 3) resultado.setSsid5(filtrar(message[3]));
                    if (message.length > 4) resultado.setCanal5(filtrar(message[4]));
                    if (message.length > 5) resultado.setEstado5(filtrar(message[5]));
                    if (message.length > 6) resultado.setPotencia(filtrar(message[6]));

                    // rssi2/rssi5/voip quedan si ssh provee (no asumimos índices)
                } catch (Exception e) {
                    Log.e(TAG, "Error mapeando salida SSH: " + e.getMessage());
                }

                Log.d(TAG, "SSH ok. Resultado final: " + resultado.toString());
                if (listener != null) listener.onHgu8115Result(true, resultado, 200);
            } else {
                Log.d(TAG, "SSH falló o mensaje nulo");
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
                Log.e(TAG, "Error deteniendo WebView: " + e.getMessage());
            }
        }
    }
}
