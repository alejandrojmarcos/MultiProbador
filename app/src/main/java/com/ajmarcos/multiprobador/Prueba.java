package com.ajmarcos.multiprobador;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class Prueba {

    private final boolean[] puertosSeleccionados;
    private final Context context;
    private final TextView tvSalida;
    private final Handler mainHandler;
    private final PortMap portMap;
    private final Button[] botones;
    private WebView webView;

    private int currentIndex = 0;  // Índice actual de puerto

    public Prueba(boolean[] puertosSeleccionados, Context context, TextView tvSalida, Button[] botones, PortMap portMap) {
        this.puertosSeleccionados = puertosSeleccionados;
        this.context = context;
        this.tvSalida = tvSalida;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.portMap = portMap;
        this.botones = botones;

        // Crear WebView en el hilo principal
            mainHandler.post(() -> {
            webView = new WebView(context);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // hacer scraping
                }
            });
        });

    }

    public void probar() {
        currentIndex = 0;
        ejecutarSiguientePuerto();
    }

    private void ejecutarSiguientePuerto() {
        // Avanzar hasta el próximo puerto seleccionado
        while (currentIndex < puertosSeleccionados.length && !puertosSeleccionados[currentIndex]) {
            currentIndex++;
        }

        if (currentIndex >= puertosSeleccionados.length) {
            appendSalida("\n✅ Secuencia finalizada.\n");
            return;
        }

        final int puerto = currentIndex;
        appendSalida("\n=== Configurando Puerto " + (puerto + 1) + " ===\n");

        // Obtenemos la subinterfaz y IP correctas del puerto actual
        SubInterface si = portMap.getSubInterfaces()[puerto];
        String ip = si.getIp();
        String subInterfaz = si.getNombre();

        portMap.levantarSubinterfaz(ip, subInterfaz, (success, salida) -> {
            if (!success) {
                appendSalida("❌ Error levantando interfaz " + subInterfaz + " en " + ip + "\n");
                currentIndex++;
                ejecutarSiguientePuerto();
                return;
            }

            // Esperamos 2 segundos para estabilizar la conexión antes de SSH
            mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto), 2000);
        });
    }


    private void ejecutarDeviceModel(int puerto) {
        Ssh ssh = new Ssh("192.168.1.1", "Support", "Te2010An_2014Ma");
        String[] comandos = {"show device_model"};

        ssh.setCommands(comandos);
        ssh.setSshListener((success, message, code) -> {
            mainHandler.post(() -> {
                appendSalida("--- Resultado device_model Puerto " + (puerto + 1) + " ---\n");

                if (success && message != null && message.length > 0) {
                    for (String linea : message) appendSalida(linea + "\n");

                    String modelo = (message.length > 2 && !message[2].trim().isEmpty()) ? message[2].trim() : "Modelo desconocido";

                    if (puerto < botones.length && botones[puerto] != null) {
                        botones[puerto].setText(modelo);
                    }

                    // Ejecutar scraping según modelo
                    ejecutarScraperPorModelo(puerto, modelo);

                } else {
                    appendSalida("❌ Error ejecutando device_model en puerto " + (puerto + 1) + "\n");
                    apagarYContinuar(puerto);
                }
            });
        });

        ssh.start();
    }



    private void appendSalida(String texto) {
        mainHandler.post(() -> {
            if (tvSalida != null) {
                tvSalida.append(texto);
                final int scrollAmount = tvSalida.getLayout().getLineTop(tvSalida.getLineCount()) - tvSalida.getHeight();
                if (scrollAmount > 0)
                    tvSalida.scrollTo(0, scrollAmount);
                else
                    tvSalida.scrollTo(0, 0);
            }
            Log.d("PRUEBA", texto);
        });
    }
    private void ejecutarScraperPorModelo(int puerto, String modelo) {
        switch (modelo) {
            case "2541": {
                prepararWebView();
                Hgu2541 hgu = new Hgu2541();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap2541(webView);
                break;
            }
            case "2741": {
                prepararWebView();
                Hgu2741 hgu = new Hgu2741();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap2741(webView);
                break;
            }
            case "2742": {
                prepararWebView();
                Hgu2742 hgu = new Hgu2742();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap2742(webView);
                break;
            }
            case "3505": {
                prepararWebView();
                Hgu3505 hgu = new Hgu3505();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap3505(webView);
                break;
            }
            case "8115": {
                prepararWebView();
                Hgu8115 hgu = new Hgu8115();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap8115(webView);
                break;
            }
            case "8225": {
                prepararWebView();
                Hgu8225 hgu = new Hgu8225();
                hgu.setHguListener((success, resultado, code) ->
                        procesarResultadoScrap(puerto, modelo, success, resultado, code));
                hgu.scrap8225(webView);
                break;
            }
            default:
                appendSalida("⚠️ Modelo no soportado: " + modelo + "\n");
                apagarYContinuar(puerto);
                break;
        }
    }


    private void procesarResultadoScrap(int puerto, String modelo, boolean success, Resultado resultado, int code) {
        if (success) {
            ValidadorResultado.ResultadoValidacion val = ValidadorResultado.validar(resultado);

            appendSalida("✅ Scrap OK [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            appendSalida("Validación: " + val.getMensaje() + "\n");

            cambiarColorBoton(puerto, val.getEstado());

        } else {
            appendSalida("❌ Scrap falló [" + modelo + "] Puerto " + (puerto + 1) + " (code " + code + ")\n");
            cambiarColorBoton(puerto, ValidadorResultado.EstadoValidacion.ERROR);
        }

        apagarYContinuar(puerto);
    }

    private void prepararWebView() {
        if (webView != null) {
            webView.stopLoading();
            webView.clearHistory();
            webView.clearCache(true);
            webView.loadUrl("about:blank");
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
        }
    }



    private void apagarYContinuar(int puerto) {
        String apagarIp = (puerto < 3) ? "192.168.1.241" : "192.168.1.242";
        portMap.apagarInterfacesPorIp(apagarIp, (f, s) -> {
            appendSalida("Interfaces de " + apagarIp + " apagadas\n");
            currentIndex++;
            ejecutarSiguientePuerto();
        });
    }

    private void cambiarColorBoton(int puerto, ValidadorResultado.EstadoValidacion estado) {
        if (puerto >= botones.length || botones[puerto] == null) return;

        int color;
        switch (estado) {
            case OK:
                color = 0xFF4CAF50; // Verde
                break;
            case WARNING:
                color = 0xFFFFC107; // Amarillo
                break;
            case ERROR:
            default:
                color = 0xFFF44336; // Rojo
                break;
        }

        final int finalColor = color;
        mainHandler.post(() -> botones[puerto].setBackgroundColor(finalColor));
    }




}
