package com.ajmarcos.multiprobador;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Hgu8225 {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private boolean cancelado = false;
    private WebView webViewRef = null;
    private Resultado resultado = new Resultado();
    private hgu8225Listener listener;
    private Context context;

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


    public interface hgu8225Listener {
        void onHgu8225Result(boolean success, Resultado resultado, int code);
    }

    public Hgu8225(Context context) {
        this.context = context;
    }

    public void setHgu8225Listener(hgu8225Listener listener) {
        this.listener = listener;
    }

    public void cancelar() {
        cancelado = true;
        Log.d(TAG, CLASS + " → Proceso cancelado por el usuario.");
        detenerWebView();

        if (listener != null)
            listener.onHgu8225Result(false, resultado, 999);
    }

    private void detenerWebView() {
        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.removeCallbacks(null);
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
            } catch (Exception e) {
                Log.e(TAG, CLASS + " → Error deteniendo WebView: " + e.getMessage());
            }
        }
    }

    private void runIfNotCancelled(Runnable task) {
        if (!cancelado) task.run();
    }

    private String filtrar(String texto) {
        char[] validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789@.-_".toCharArray();
        StringBuilder filtered = new StringBuilder();
        for (char c : texto.toCharArray()) {
            for (char v : validChars) {
                if (c == v) {
                    filtered.append(c);
                    break;
                }
            }
        }
        return filtered.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap8225(WebView webView) {
        Log.d(TAG, CLASS + " → scrap8225 iniciado.");
        cancelado = false;
        webViewRef = webView;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;
            private boolean firmwareAndSerialExtracted = false;
            private boolean wanDetailsExtracted = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                super.onPageFinished(view, url);
                Log.d(TAG, CLASS + " → Página cargada: " + url);

                // LOGIN
                if (!isLoggedIn && url.contains("login.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String loginScript =
                                "try {" +
                                        "var u=document.getElementsByName('Username')[0];" +
                                        "var p=document.getElementsByName('Password')[0];" +
                                        "if(u&&p){u.value='Support';p.value='Te2010An_2014Ma';" +
                                        "document.querySelector('input[type=\\'submit\\']').click();}" +
                                        "}catch(e){console.error(e);}";
                        view.evaluateJavascript(loginScript, v -> {
                            Log.d(TAG, CLASS + " → Script de login ejecutado.");
                            view.postDelayed(() -> runIfNotCancelled(() ->
                                    webView.loadUrl("http://192.168.1.1:8000/summary.asp")), 2000);
                        });
                        isLoggedIn = true;
                    }), 3000);
                    return;
                }

                // EXTRAER FIRMWARE Y SERIAL
                if (isLoggedIn && !firmwareAndSerialExtracted && url.contains("summary.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String extractScript =
                                "try {" +
                                        "var fw=document.evaluate('/html/body/table/tbody/tr[2]/td',document,null,XPathResult.STRING_TYPE,null).stringValue||'';" +
                                        "var sn=document.evaluate('/html/body/table/tbody/tr[8]/td',document,null,XPathResult.STRING_TYPE,null).stringValue||'';" +
                                        "JSON.stringify({fw:fw,sn:sn});" +
                                        "}catch(e){JSON.stringify({error:e.message});}";
                        view.evaluateJavascript(extractScript, value -> {
                            if (cancelado) return;
                            Log.d(TAG, CLASS + " → Datos extraídos: " + value);
                            value = value.replace("\"", "").replace("{", "").replace("}", "");
                            String[] partes = value.split(",");
                            if (partes.length >= 2) {
                                resultado.setFirmware(filtrar(partes[0].split(":")[1]));
                                resultado.setSerial(filtrar(partes[1].split(":")[1]));
                            }
                            webView.loadUrl("http://192.168.1.1:8000/wanintf.asp");
                        });
                        firmwareAndSerialExtracted = true;
                    }), 3000);
                    return;
                }

                // NAVEGAR A DETALLE WAN
                if (firmwareAndSerialExtracted && !wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String clickScript =
                                "try{var l=document.evaluate('/html/body/fieldset[1]/table/tbody/tr[1]/td[2]/a',document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;if(l)l.click();}catch(e){console.error(e);}";
                        view.evaluateJavascript(clickScript, val -> Log.d(TAG, CLASS + " → Clic WAN ejecutado."));
                        wanDetailsExtracted = true;
                    }), 3000);
                    return;
                }

                // EXTRAER USUARIO
                if (wanDetailsExtracted && url.contains("wanintf.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String extractUserScript =
                                "try {" +
                                        "var i=document.evaluate('/html/body/fieldset[2]/div/form/fieldset[2]/fieldset[1]/div[1]/input',document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;" +
                                        "i?JSON.stringify({user:i.value}):JSON.stringify({error:'no encontrado'});" +
                                        "}catch(e){JSON.stringify({error:e.message});}";
                        view.evaluateJavascript(extractUserScript, value -> {
                            if (cancelado) return;
                            Log.d(TAG, CLASS + " → Username extraído: " + value);
                            value = value.replace("\"", "").replace("{", "").replace("}", "");
                            if (value.contains(":"))
                                resultado.setUsuario(filtrar(value.split(":")[1]));
                            iniciarSSH();
                        });
                    }), 3000);
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/login.asp");
    }

    private void iniciarSSH() {
        if (cancelado) return;
        Log.d(TAG, CLASS + " → Inicia SSH 8225.");

        Ssh8225 ssh = new Ssh8225();
        ssh.setSshListener((success, message, code) -> {
            if (cancelado) return;

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
                    Log.e(TAG, CLASS + " → Error mapeando salida SSH: " + e.getMessage());
                }

                Log.d(TAG, CLASS + " → SSH OK -> " + resultado.toString());
                if (listener != null) listener.onHgu8225Result(true, resultado, 200);
            } else {
                Log.d(TAG, CLASS + " → SSH falló.");
                if (listener != null) listener.onHgu8225Result(false, resultado, code);
            }
        });
        ssh.start();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void restoreDefault(WebView webView) {
        Log.d(TAG, CLASS + " → Reseteo a fábrica iniciado.");
        cancelado = false;
        webViewRef = webView;

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (cancelado) return;
                if (!isLoggedIn && url.contains("login.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String loginScript =
                                "try{" +
                                        "document.getElementsByName('Username')[0].value='Support';" +
                                        "document.getElementsByName('Password')[0].value='Te2010An_2014Ma';" +
                                        "document.querySelector('input[type=\\'submit\\']').click();" +
                                        "}catch(e){console.error(e);}";
                        view.evaluateJavascript(loginScript, v -> {
                            webView.postDelayed(() -> runIfNotCancelled(() ->
                                    webView.loadUrl("http://192.168.1.1:8000/restore_default.asp")), 3000);
                        });
                        isLoggedIn = true;
                    }), 3000);
                    return;
                }

                if (isLoggedIn && url.contains("restore_default.asp")) {
                    webView.postDelayed(() -> runIfNotCancelled(() -> {
                        String resetScript =
                                "try{var b=document.querySelector('input[type=\\'button\\']#btnRestore');if(b)b.click();}catch(e){console.error(e);}";
                        view.evaluateJavascript(resetScript, val -> Log.d(TAG, CLASS + " → Reseteo ejecutado."));
                    }), 3000);

                    if (listener != null && !cancelado)
                        listener.onHgu8225Result(true, resultado, 201);
                }
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/login.asp");
    }
}
