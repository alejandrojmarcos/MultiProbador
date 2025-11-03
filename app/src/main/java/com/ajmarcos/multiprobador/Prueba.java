package com.ajmarcos.multiprobador;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;


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
    private String catalogo = "";
    private AlertDialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;
    private AlertDialog dialogEnviando;



    private int currentIndex = 0;  // Índice actual de puerto
    private final ArrayList<Resultado> resultados = new ArrayList<>();

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

        btnEnviar.setOnClickListener(v -> {
            try {
                prepararInternetYEnviar();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

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
        Ssh ssh = new Ssh("192.168.1.2", "Support", "Te2010An_2014Ma");
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
    public void prepararInternetYEnviar() throws InterruptedException {
        String ipInternet = "192.168.1.230";
        String subInterfaz = "eth3.0";

        appendSalida("🌐 Levantando interfaz " + ipInternet + " / " + subInterfaz + "...\n");
        Thread.sleep(2000);
        // Levantar la subinterfaz antes de verificar Internet
        portMap.levantarSubinterfaz(ipInternet, subInterfaz, (success, salida) -> {
            if (!success) {
                appendSalida("❌ No se pudo levantar la interfaz " + subInterfaz + " en " + ipInternet + "\n");
                return;
            }

            appendSalida("✅ Interfaz levantada. Verificando conexión a Internet...\n");

            // Esperamos un par de segundos para que la interfaz se estabilice
            mainHandler.postDelayed(() -> {
                if (tieneInternet()) {
                    appendSalida("✅ Conexión a Internet OK. Abriendo Outlook...\n");
                    enviarResultadosPorCorreo(); // abrir Outlook con el ZIP
                } else {
                    appendSalida("❌ No hay conexión a Internet. Reintentar.\n");
                }
            }, 2000);
        });
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
            // 1️⃣ Generar CSV
            String csvFileName = "Resultados_" + System.currentTimeMillis() + ".csv";
            File csvFile = new File(context.getFilesDir(), csvFileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                // Cabeceras
                String[] headers = {
                        "fecha", "serial", "firmware", "potencia",
                        "ssid2", "estado2", "canal2","rssi2",
                        "ssid5", "estado5", "canal5","rssi5",
                        "usuario", "voip",
                        "catalogo", "falla", "condicion", "multiprobador"
                };
                writer.write(String.join(",", headers));
                writer.newLine();

                // Filas de resultados
                for (Resultado r : resultados) {
                    String[] values = {
                            r.getFecha(), r.getSerial(), r.getFirmware(), r.getPotencia(),
                            r.getSsid2(), r.getEstado2(), r.getCanal2(), r.getRssi2(),
                            r.getSsid5(), r.getEstado5(), r.getCanal5(), r.getRssi5(),
                            r.getUsuario(), r.getVoip(),
                            r.getCatalogo(), r.getFalla(), r.getCondicion(), r.getMultiprobador()
                    };

                    for (int i = 0; i < values.length; i++) {
                        if (values[i] != null) {
                            values[i] = values[i].replace("\"", "\"\"");
                            if (values[i].contains(",") || values[i].contains("\"") || values[i].contains("\n")) {
                                values[i] = "\"" + values[i] + "\"";
                            }
                        } else {
                            values[i] = "";
                        }
                    }

                    writer.write(String.join(",", values));
                    writer.newLine();
                }
            }

            // 2️⃣ Comprimir CSV en ZIP con contraseña
            File zipFile = new File(context.getFilesDir(), "Resultados.zip");
            ZipFile zip = new ZipFile(zipFile, "abcdef12345".toCharArray());

            ZipParameters parameters = new ZipParameters();
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

            // Solo el nombre del CSV dentro del ZIP, sin carpeta
            parameters.setFileNameInZip("Resultados.csv");

            zip.addFile(csvFile, parameters);

            // 3️⃣ Preparar Intent para enviar por Outlook
            Uri uri = FileProvider.getUriForFile(
                    context,
                    "com.ajmarcos.multiprobador.fileprovider",
                    zipFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/zip");
            // mail andres "andresm.fernandez@tmoviles.com.ar"
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alejandro.marcos@tmoviles.com.ar",});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Resultados de prueba");
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto los resultados de la prueba en CSV comprimido.");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage("com.microsoft.office.outlook");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);
            appendSalida("📧 Abriendo Outlook con el ZIP protegido...\n");

            // Opcional: borrar el CSV temporal después de crear el ZIP
            csvFile.delete();

        } catch (Exception e) {
            Log.e("PRUEBA", "❌ Error preparando envío de correo", e);
            appendSalida("❌ Error preparando envío de correo\n");
        }
    }


}
