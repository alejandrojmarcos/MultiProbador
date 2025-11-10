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

import androidx.core.content.FileProvider;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

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
    private String TAG = "Deploy";
    private String MultiLocal = "Bella Vista 1";

    private final ArrayList<Resultado> resultados = new ArrayList<>();
    private int currentIndex = 0;

    public void setCatalogo(String catalogo) {
        this.catalogo = catalogo;
    }

    public Prueba(WebView webView,boolean[] puertosSeleccionados, Context context, TextView tvSalida,
                  Button btnComenzar, Button btnEnviar, PortMap portMap) {
        this.puertosSeleccionados = puertosSeleccionados;
        this.context = context;
        this.tvSalida = tvSalida;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.portMap = portMap;
        this.btnComenzar = btnComenzar;
        this.btnEnviar = btnEnviar;
        this.webView=webView;

        btnComenzar.setOnClickListener(v -> {
            btnComenzar.setEnabled(false);
            btnEnviar.setEnabled(false);
            resultados.clear();
            currentIndex = 0;
            // arranca la secuencia secuencial
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

            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    // scraping (si lo usan los HGU)
                }
            });
        });
    }

    public void iniciar() {
        currentIndex = 0;
        ejecutarSiguientePuerto();
    }

    /**
     * Busca el siguiente puerto seleccionado y lo procesa. No continúa hasta
     * que la cadena levantar->ssh->scrap->apagar complete y llame a onDone.
     */
    private void ejecutarSiguientePuerto() {
        // buscar siguiente puerto seleccionado
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

        // levantar subinterfaz correspondiente
        SubInterface si = portMap.getSubInterfaces()[puerto];
        portMap.levantarSubinterfaz(si.getIp(), si.getNombre(), (success, salida) -> {
            if (!success) {
                appendSalida("❌ Error levantando interfaz " + si.getNombre() + " en " + si.getIp() + "\n");
                // avanzar al siguiente puerto
                currentIndex++;
                ejecutarSiguientePuerto();
                return;
            }

            // esperar un poco y llamar a device_model (todo en secuencia)
            mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto, () -> {
                // llamado cuando termina todo para este puerto (incluye apagar interfaces)
                currentIndex++;
                ejecutarSiguientePuerto();
            }), 1500);
        });
    }

    /**
     * Ejecuta "show device_model" vía SSH al equipo de gestión (192.168.1.1).
     * Al finalizar (callback onDone) ejecuta el scraper asociado.
     * Firma del listener Ssh original: onSshResult(boolean success, String[] message, int code)
     */
    private void ejecutarDeviceModel(int puerto, Runnable onDone) {
        Ssh ssh = new Ssh("192.168.1.1", "Support", "Te2010An_2014Ma");
        ssh.setCommands(new String[]{"show device_model"});
        ssh.setSshListener((success, message, code) -> mainHandler.post(() -> {
            appendSalida("--- Resultado device_model Puerto " + (puerto + 1) + " ---\n");

            String modelo = "Modelo desconocido";
            if (success && message != null && message.length > 2) {
                String linea = message[2];
                if (linea != null) {
                    linea = linea.trim();
                    if (!linea.isEmpty()) modelo = linea;
                }
            }
            appendSalida("📡 " + modelo + "\n");

            // Ejecutar scraper asociado; cuando el scraper termine, se debe llamar a apagarYContinuar(puerto,onDone)
            ejecutarScraperPorModelo(puerto, modelo, onDone);
        }));
        ssh.start();
    }

    /**
     * Ejecuta el scraper según modelo. Llama a apagarYContinuar(puerto,onDone) cuando termina.
     */
    private void ejecutarScraperPorModelo(int puerto, String modelo, Runnable onDone) {
        prepararWebView();

        // cada HguXXXX debe llamar a su listener que finalmente invoque procesarResultadoScrap(..., onDone)
        if (modelo.contains("2541")) {
            Hgu2541 hgu = new Hgu2541();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu2541Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap2541(this.webView);

        } else if (modelo.contains("2741")) {
            Hgu2741 hgu = new Hgu2741();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu2741Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone)
            );
            hgu.scrap2741(this.webView); // o su propio WebView interno

        } else if (modelo.contains("2742")) {
            Hgu2742 hgu = new Hgu2742();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu2742Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap2742(this.webView);
        } else if (modelo.contains("3505")) {
            Hgu3505 hgu = new Hgu3505(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu3505Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap3505(this.webView);
        } else if (modelo.contains("8115")) {
            Hgu8115 hgu = new Hgu8115(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu8115Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap8115(this.webView);
        } else if (modelo.contains("8225")) {
            Hgu8225 hgu = new Hgu8225(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            hgu.setHgu8225Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap8225(this.webView);
        } else {
            appendSalida("⚠️ Modelo no soportado: " + modelo + "\n");
            // llama onDone para continuar la secuencia
            onDone.run();
        }
    }

    private void procesarResultadoScrap(int puerto, String modelo, boolean success, Resultado resultado, int code, Runnable onDone) {
        if (resultado == null) resultado = new Resultado();

        if (success) {
            ValidadorResultado.ResultadoValidacion val = ValidadorResultado.validar(resultado);
            appendSalida("✅ Scrap OK [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            appendSalida("Validación: " + val.getMensaje() + "\n");

            // Imprimir todos los valores de resultado
            appendSalida("📊 Resultado completo:");
            appendSalida("Serial: " + resultado.getSerial());
            appendSalida("Firmware: " + resultado.getFirmware());
            appendSalida("Potencia: " + resultado.getPotencia());
            appendSalida("SSID2: " + resultado.getSsid2() + ", Canal2: " + resultado.getCanal2() + ", Estado2: " + resultado.getEstado2());
            appendSalida("SSID5: " + resultado.getSsid5() + ", Canal5: " + resultado.getCanal5() + ", Estado5: " + resultado.getEstado5());
            appendSalida("Usuario: " + resultado.getUsuario());
            appendSalida("VoIP: " + resultado.getVoip());
            appendSalida("Catalogo: " + resultado.getCatalogo());
            appendSalida("Falla: " + resultado.getFalla());
            appendSalida("Condicion: " + resultado.getCondicion());
            appendSalida("Multiprobador: " + resultado.getMultiprobador());
        } else {
            appendSalida("❌ Scrap falló [" + modelo + "] Puerto " + (puerto + 1) + "\n");
        }

        resultados.add(resultado);
        apagarYContinuar(puerto, onDone);
    }


    /**
     * Apaga todas las subinterfaces del IP asociado al puerto y cuando termina ejecuta onDone.
     */
    private void apagarYContinuar(int puerto, Runnable onDone) {
        SubInterface si = portMap.getSubInterfaces()[puerto];
        portMap.apagarInterfacesPorIp(si.getIp(), (success, salida) -> mainHandler.post(() -> {
            appendSalida("Interfaces de " + si.getIp() + " apagadas\n");
            // ahora podemos continuar con el siguiente puerto
            onDone.run();
        }));
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
                try {
                    int scrollAmount = tvSalida.getLayout().getLineTop(tvSalida.getLineCount()) - tvSalida.getHeight();
                    tvSalida.scrollTo(0, Math.max(scrollAmount, 0));
                } catch (Exception ignored) { }
            }
            Log.d(TAG, texto);
        });
    }

    public ArrayList<Resultado> getResultados() {
        return resultados;
    }

    // --- Envío por correo (igual que tenías) ---
    public void prepararInternetYEnviar() throws InterruptedException {
        String ipInternet = "192.168.1.230";
        String subInterfaz = "eth3.0";

        appendSalida("🌐 Levantando interfaz " + ipInternet + " / " + subInterfaz + "...\n");
        Thread.sleep(1000);
        portMap.levantarSubinterfaz(ipInternet, subInterfaz, (success, salida) -> {
            if (!success) {
                appendSalida("❌ No se pudo levantar la interfaz " + subInterfaz + " en " + ipInternet + "\n");
                return;
            }

            appendSalida("✅ Interfaz levantada. Verificando conexión a Internet...\n");
            mainHandler.postDelayed(() -> {
                if (tieneInternet()) {
                    appendSalida("✅ Conexión a Internet OK. Abriendo Outlook...\n");
                    enviarResultadosPorCorreo();
                } else {
                    appendSalida("❌ No hay conexión a Internet. Reintentar.\n");
                }
            }, 1000);
        });
    }

    private boolean tieneInternet() {
        try {
            // Envía 10 paquetes ICMP al host
            Process p = Runtime.getRuntime().exec("ping -c 10 192.168.1.1");
            int returnVal = p.waitFor(); // espera a que termine el ping
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
                String[] headers = {
                        "fecha","multiprobador","modelo", "serial", "firmware", "potencia",
                        "ssid2", "estado2", "canal2","rssi2",
                        "ssid5", "estado5", "canal5","rssi5",
                        "usuario", "voip",
                        "catalogo", "falla", "condicion"
                };
                writer.write(String.join(",", headers));
                writer.newLine();

                for (Resultado r : resultados) {
                    String[] values = {
                            r.getFecha(), r.getMultiprobador(), r.getModelo(), r.getSerial(), r.getFirmware(), r.getPotencia(),
                            r.getSsid2(), r.getEstado2(), r.getCanal2(), r.getRssi2(),
                            r.getSsid5(), r.getEstado5(), r.getCanal5(), r.getRssi5(),
                            r.getUsuario(), r.getVoip(),
                            r.getCatalogo(), r.getFalla(), r.getCondicion()
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

            // 2️⃣ ZIP con contraseña
            File zipFile = new File(context.getFilesDir(), "Resultados.zip");
            ZipFile zip = new ZipFile(zipFile, "abcdef12345".toCharArray());

            ZipParameters parameters = new ZipParameters();
            parameters.setEncryptFiles(true);
            parameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
            parameters.setFileNameInZip("Resultados.csv");

            zip.addFile(csvFile, parameters);

            // 3️⃣ Intent Outlook
            Uri uri = FileProvider.getUriForFile(
                    context,
                    "com.ajmarcos.multiprobador.fileprovider",
                    zipFile
            );

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"alejandro.marcos@tmoviles.com.ar"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Resultados de prueba");
            intent.putExtra(Intent.EXTRA_TEXT, "Adjunto los resultados de la prueba en CSV comprimido.");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setPackage("com.microsoft.office.outlook");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(intent);
            appendSalida("📧 Abriendo Outlook con el ZIP protegido...\n");

            csvFile.delete();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error preparando envío de correo", e);
            appendSalida("❌ Error preparando envío de correo\n");
        }
    }
}
