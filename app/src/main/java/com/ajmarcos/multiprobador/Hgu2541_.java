package com.ajmarcos.multiprobador;



import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

public class Hgu2541_ {

    private String[] valoresEnviar = new String[11];
    private boolean cancelado = false;
    private WebView webViewRef = null;

    private hgu2541Listener listener;

    public interface hgu2541Listener {
        void onHgu2541Result(boolean success, Resultado resultado, int code);
    }

    public Hgu2541_() {}

    public void setHgu2541Listener(hgu2541Listener listener) {
        this.listener = listener;
    }

    public void cancelar() {
        cancelado = true;
        Log.d(TAG, "Proceso cancelado por el usuario.");

        if (webViewRef != null) {
            try {
                webViewRef.stopLoading();
                webViewRef.removeCallbacks(null);
                webViewRef.loadUrl("about:blank");
                webViewRef.clearHistory();
                webViewRef.clearCache(true);
            } catch (Exception e) {
                Log.e(TAG, "Error deteniendo WebView: " + e.getMessage());
            }
        }

        if (listener != null) {
            listener.onHgu2541Result(false, null, 999);
        }
    }

    private void runIfNotCancelled(Runnable task) {
        if (!cancelado) task.run();
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void scrap2541(WebView webView) {
        cancelado = false;
        webViewRef = webView; // referencia para cancelar

        // Hacemos visible el WebView si estaba invisible
        webView.setVisibility(View.VISIBLE);

        // Habilitamos debugging para ver consola de JS desde logcat o Chrome
        WebView.setWebContentsDebuggingEnabled(true);

        Log.d("scrap", "scrap 2541 visible");
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.clearCache(true);
        webView.clearHistory();
        webView.clearFormData();

        CookieManager.getInstance().removeAllCookies(value ->
                Log.d("Cookies", value ? "Todas las cookies eliminadas" : "No se pudieron eliminar todas las cookies"));

        webView.setWebViewClient(new WebViewClient() {
            private boolean isLoggedIn = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (cancelado) return;

                Log.d("PageFinished", "Página cargada: " + url);

                runIfNotCancelled(() -> {
                    // Login
                    // Login flexible
                    if (!isLoggedIn && (url.endsWith("8000/") || url.endsWith("main.html"))) {
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
                        view.evaluateJavascript(loginScript, null);
                        isLoggedIn = true;

                        view.postDelayed(() -> runIfNotCancelled(() ->
                                webView.loadUrl("http://192.168.1.1:8000/deviceinfo.cgi")), 3000);
                    }

                    // Extraer info del dispositivo
                    else if (url.contains("deviceinfo.cgi")) {
                        String extractDeviceInfoScript = "var data = {};" +
                                "try {" +
                                "data.Firmware = document.evaluate('/html/body/div/div[5]/div[1]/div[4]/div[4]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Serial = document.evaluate('/html/body/div/div[5]/div[1]/div[4]/div[6]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Potencia = document.evaluate('/html/body/div/div[5]/div[3]/div[2]/div[3]/span[1]', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Ssid2 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[3]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Canal2 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[2]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Estado2 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[1]/label', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Ssid5 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[8]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Canal5 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[7]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Estado5 = document.evaluate('/html/body/div/div[5]/div[2]/div[2]/div[6]/label', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "data.Voip = document.evaluate('/html/body/div/div[5]/div[4]/div[2]/div[1]/span', document, null, XPathResult.STRING_TYPE, null).stringValue || 'No disponible';" +
                                "JSON.stringify(data);" +
                                "} catch(e) { console.error(e); JSON.stringify({Error: e.message}); }";

                        view.evaluateJavascript(extractDeviceInfoScript, value -> runIfNotCancelled(() -> {
                            try {
                                String jsonString = value.replace("\\\"", "\"").replace("\"{", "{").replace("}\"", "}");
                                JSONObject jsonObject = new JSONObject(jsonString);

                                valoresEnviar = new String[]{
                                        jsonObject.optString("Firmware", "No disponible"),
                                        jsonObject.optString("Serial", "No disponible"),
                                        jsonObject.optString("Potencia", "No disponible"),
                                        jsonObject.optString("Ssid2", "No disponible"),
                                        jsonObject.optString("Canal2", "No disponible"),
                                        jsonObject.optString("Estado2", "No disponible"),
                                        jsonObject.optString("Ssid5", "No disponible"),
                                        jsonObject.optString("Canal5", "No disponible"),
                                        jsonObject.optString("Estado5", "No disponible"),
                                        jsonObject.optString("Voip", "No disponible"),
                                        "usuario"
                                };

                                webView.loadUrl("http://192.168.1.1:8000/multipuesto.cgi");

                            } catch (JSONException e) {
                                Log.e("JSONError", "Error parseando JSON: " + e.getMessage());
                            }
                        }));
                    }

                    // Extraer usuario multipuesto
                    else if (url.contains("multipuesto.cgi")) {
                        String extractPppUserNameScript = "document.querySelector('input#pppUserName')?.value || 'N/A';";
                        view.evaluateJavascript(extractPppUserNameScript, value -> runIfNotCancelled(() -> {
                            valoresEnviar[10] = value.replace("\"", "");

                            // Construimos objeto Resultado
                            Resultado resultado = new Resultado();
                            resultado.setFirmware(valoresEnviar[0]);
                            resultado.setSerial(valoresEnviar[1]);
                            resultado.setPotencia(valoresEnviar[2]);
                            resultado.setSsid2(valoresEnviar[3]);
                            resultado.setCanal2(valoresEnviar[4]);
                            resultado.setEstado2(valoresEnviar[5]);
                            resultado.setSsid5(valoresEnviar[6]);
                            resultado.setCanal5(valoresEnviar[7]);
                            resultado.setEstado5(valoresEnviar[8]);
                            resultado.setVoip(valoresEnviar[9]);
                            resultado.setUsuario(valoresEnviar[10]);

                            if (listener != null) listener.onHgu2541Result(true, resultado, 200);
                        }));
                    }
                });
            }
        });

        webView.loadUrl("http://192.168.1.1:8000/");
    }
}
