package com.ajmarcos.multiprobador;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import androidx.core.content.FileProvider;

public class Prueba {

    private final boolean[] puertosSeleccionados;
    private final Context context;
    private final TextView tvSalida;
    private final Handler mainHandler;
    private final PortMap portMap;
    private final Button btnComenzar;
    private final Button btnEnviar;
    private WebView webView;
    private String lote = "";
    private String catalogo = "";


    private int currentIndex = 0;  // Índice actual de puerto
    private final ArrayList<Resultado> resultados = new ArrayList<>();


    public void setLote(String lote) {
        this.lote = lote;
    }

    public void setCatalogo(String catalogo) {
        this.catalogo = catalogo;
    }


    public Prueba(boolean[] puertosSeleccionados, Context context, TextView tvSalida,
                  Button btnComenzar, Button btnEnviar, PortMap portMap) {
        this.puertosSeleccionados = puertosSeleccionados;
        this.context = context;
        this.tvSalida = tvSalida;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.portMap = portMap;
        this.btnComenzar = btnComenzar;
        this.btnEnviar = btnEnviar;

        btnComenzar.setOnClickListener(v -> {
            btnComenzar.setEnabled(false);
            btnEnviar.setEnabled(false);
            resultados.clear();
            currentIndex = 0;
            ejecutarSiguientePuerto();
        });

        btnEnviar.setOnClickListener(v -> prepararInternetYEnviar());

        mainHandler.post(() -> {
            webView = new WebView(context);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // scraping
                }
            });
        });
    }

    public void iniciar() {
        currentIndex = 0;
        ejecutarSiguientePuerto();
    }

    private void ejecutarSiguientePuerto() {
        while (currentIndex < puertosSeleccionados.length && !puertosSeleccionados[currentIndex]) {
            currentIndex++;
        }

        if (currentIndex >= puertosSeleccionados.length) {
            appendSalida("\n✅ Secuencia finalizada.\n");
            mainHandler.post(() -> btnEnviar.setEnabled(true));
            btnComenzar.setEnabled(true);
            return;
        }

        final int puerto = currentIndex;
        appendSalida("\n=== Configurando Puerto " + (puerto + 1) + " ===\n");

        SubInterface si = portMap.getSubInterfaces()[puerto];
        portMap.levantarSubinterfaz(si.getIp(), si.getNombre(), (success, salida) -> {
            if (!success) {
                appendSalida("❌ Error levantando interfaz " + si.getNombre() + " en " + si.getIp() + "\n");
                currentIndex++;
                ejecutarSiguientePuerto();
                return;
            }

            mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto), 2000);
        });
    }

    private void ejecutarDeviceModel(int puerto) {
        Ssh ssh = new Ssh("192.168.1.1", "Support", "Te2010An_2014Ma");
        String[] comandos = {"show device_model"};

        ssh.setCommands(comandos);
        ssh.setSshListener((success, message, code) -> mainHandler.post(() -> {
            appendSalida("--- Resultado device_model Puerto " + (puerto + 1) + " ---\n");
            String modelo = "Modelo desconocido";
            if (success && message != null && message.length > 2 && !message[2].trim().isEmpty()) {
                modelo = message[2].trim();
            }
            appendSalida(modelo + "\n");

            ejecutarScraperPorModelo(puerto, modelo);
        }));
        ssh.start();
    }

    private void ejecutarScraperPorModelo(int puerto, String modelo) {
        prepararWebView();
        Runnable continuar = () -> apagarYContinuar(puerto);

        if (modelo.contains("2541")) {
            Hgu2541 hgu = new Hgu2541(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap2541();
        } else if (modelo.contains("2741")) {
            Hgu2741 hgu = new Hgu2741(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap2741();
        } else if (modelo.contains("2742")) {
            Hgu2742 hgu = new Hgu2742(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap2742();
        } else if (modelo.contains("3505")) {
            Hgu3505 hgu = new Hgu3505(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap3505();
        } else if (modelo.contains("8115")) {
            Hgu8115 hgu = new Hgu8115(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap8115();
        } else if (modelo.contains("8225")) {
            Hgu8225 hgu = new Hgu8225(context);
            hgu.setHguListener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code));
            hgu.scrap8225();
        } else {
            appendSalida("⚠️ Modelo no soportado: " + modelo + "\n");
            continuar.run();
        }
    }

    private void procesarResultadoScrap(int puerto, String modelo, boolean success, Resultado resultado, int code) {
        if (resultado == null) resultado = new Resultado();

        if (success) {
            ValidadorResultado.ResultadoValidacion val = ValidadorResultado.validar(resultado);
            appendSalida("✅ Scrap OK [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            appendSalida("Validación: " + val.getMensaje() + "\n");
        } else {
            appendSalida("❌ Scrap falló [" + modelo + "] Puerto " + (puerto + 1) + "\n");
        }


        resultado.setLote(lote);
        resultado.setCatalogo(catalogo);


        resultados.add(resultado);
        apagarYContinuar(puerto);
    }

    private void apagarYContinuar(int puerto) {
        SubInterface si = portMap.getSubInterfaces()[puerto];
        portMap.apagarInterfacesPorIp(si.getIp(), (success, salida) -> {
            appendSalida("Interfaces de " + si.getIp() + " apagadas\n");
            currentIndex++;
            ejecutarSiguientePuerto();
        });
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

    private void appendSalida(String texto) {
        mainHandler.post(() -> {
            if (tvSalida != null) {
                tvSalida.append(texto + "\n");
                int scrollAmount = tvSalida.getLayout().getLineTop(tvSalida.getLineCount()) - tvSalida.getHeight();
                tvSalida.scrollTo(0, Math.max(scrollAmount, 0));
            }
            Log.d("PRUEBA", texto);
        });
    }

    public ArrayList<Resultado> getResultados() {
        return resultados;
    }

    // -------------------------------------------------
    // ENVIO DE RESULTADOS POR CORREO CON OUTLOOK
    // -------------------------------------------------
    public void prepararInternetYEnviar() {
        String ipInternet = "192.168.1.230";
        String subInterfaz = "eth3.0";

        appendSalida("🌐 Verificando conectividad en interfaz: " + ipInternet + " / " + subInterfaz + "...\n");

        mainHandler.postDelayed(() -> {
            if (tieneInternet()) {
                appendSalida("✅ Conexión a Internet OK. Abriendo Outlook...\n");
                enviarResultadosPorCorreo(); // abrir Outlook con el ZIP
            } else {
                appendSalida("❌ No hay conexión a Internet. Reintentar.\n");
            }
        }, 2000); // espera 2 segundos por si la interfaz recién se estabiliza
    }




    private boolean tieneInternet() {
        try {
            Process p = Runtime.getRuntime().exec("ping -c 1 8.8.8.8");
            int returnVal = p.waitFor();
            return (returnVal == 0);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void enviarResultadosPorCorreo() {
        try {
            // Generar ZIP de resultados
            File zipFile = new File(context.getFilesDir(), "resultados.zip");

            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
                Gson gson = new Gson();

                // Convertir cada Resultado en un Map (clave-valor)
                ArrayList<Map<String, Object>> listaMap = new ArrayList<>();
                for (Resultado r : resultados) {
                    listaMap.add(r.toMap());
                }

                // Generar JSON ordenado y legible
                String json = gson.toJson(listaMap);

                ZipEntry entry = new ZipEntry("resultados.json");
                zos.putNextEntry(entry);
                zos.write(json.getBytes());
                zos.closeEntry();
            }

            // URI usando FileProvider
            Uri uri = FileProvider.getUriForFile(
                    context,
                    "com.ajmarcos.multiprobador.fileprovider",
                    zipFile
            );

            // Intent para enviar a Outlook
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alejandro.marcos@tmoviles.com.ar"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Resultados de prueba");
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto los resultados de la prueba.");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage("com.microsoft.office.outlook");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);
            appendSalida("📧 Abriendo Outlook con el archivo adjunto...\n");

        } catch (Exception e) {
            Log.e("PRUEBA", "❌ Error preparando envío de correo", e);
            appendSalida("❌ Error preparando envío de correo\n");
        }
    }

}
