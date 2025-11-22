package com.ajmarcos.multiprobador;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.app.Activity;

// Nota: Se asume que las clases Hgu8145, Hgu2541, ValidadorResultado, SubInterface, y SshInteractivo existen.

public class Prueba {

    private AppUpdateManager appUpdateManager;
    private static final int MY_UPDATE_REQUEST_CODE = 101;

    // --- VARIABLES DE CLASE ---
    private final boolean[] puertosSeleccionados;
    private final Context context;
    private final TextView tvSalida;
    private final Handler mainHandler;
    private final PortMap portMap;
    private final Button btnComenzar;
    private final Button btnEnviar;
    private final PruebaResultadoListener resultadoListener;
    private final ProgressBar progressBar;

    // 📢 CAMPOS DE VALIDACIÓN INYECTADA
    private final Set<String> serialesInvalidos;
    private final Set<String> firmwaresActuales;
    private final Set<String> firmwaresCriticos;
    private final Set<String> firmwaresObsoletos;
    private final Set<String> redesObservadas = new HashSet<>(); // Lista acumulada de redes visibles

    private WebView webView;
    private String catalogo = "";
    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();
    private String MultiLocal = "Bella Vista 1";
    private final ArrayList<Resultado> resultados = new ArrayList<>();
    private int currentIndex = 0;
    private boolean probando;


    // --- CONSTRUCTOR ---
    public Prueba(WebView webView,
                  boolean[] puertosSeleccionados,
                  Context context,
                  TextView tvSalida,
                  Button btnComenzar,
                  Button btnEnviar,
                  PortMap portMap,
                  PruebaResultadoListener resultadoListener,
                  ProgressBar progressBar,
                  // 📢 ARGUMENTOS INYECTADOS
                  Set<String> serialesInvalidos,
                  Set<String> firmwaresActuales,
                  Set<String> firmwaresCriticos,
                  Set<String> firmwaresObsoletos) {

        this.puertosSeleccionados = puertosSeleccionados;
        this.context = context;
        this.tvSalida = tvSalida;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.portMap = portMap;
        this.btnComenzar = btnComenzar;
        this.btnEnviar = btnEnviar;
        this.webView = webView;
        this.resultadoListener = resultadoListener;
        this.progressBar = progressBar;
        this.probando = false;

        // 📢 INICIALIZACIÓN DE LOS SETS
        this.serialesInvalidos = serialesInvalidos;
        this.firmwaresActuales = firmwaresActuales;
        this.firmwaresCriticos = firmwaresCriticos;
        this.firmwaresObsoletos = firmwaresObsoletos;

        // --- Configuración Inicial de la Barra de Progreso ---
        mainHandler.post(() -> {
            this.progressBar.setMax(100);
            this.progressBar.setProgress(0);
        });

        // Log de Verificación (para debugging)
        Log.d(TAG, CLASS + ": serialesInvalidos loaded. Total: " + serialesInvalidos.size());

        // Listeners del control principal (Comenzar/Enviar)
        btnComenzar.setOnClickListener(v -> {
            this.probando=true;
            btnComenzar.setEnabled(false);
            btnEnviar.setEnabled(false);
            resultados.clear();
            currentIndex = 0;
            iniciar();
        });

        btnEnviar.setOnClickListener(v -> {

            Log.d(TAG,"button enviar presionado");

            //enviar();
            enviarResultadosPorCorreo();
        });

        mainHandler.post(() -> {
            if (this.webView != null) {
                this.webView.getSettings().setJavaScriptEnabled(true);
                this.webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        // Lógica de scraping (si lo usan los HGU web)
                    }
                });
            }
        });
    }

    // --- Getters y Setters ---

    public void setCatalogo(String catalogo) {
        this.catalogo = catalogo;
    }
    public String getCatalogo() { return this.catalogo; }

    public Set<String> getRedesObservadas() {
        return redesObservadas;
    }

    // --- MÉTODOS DE PROGRESO INDETERMINADO ---

    private void startIndeterminateProgress() {
        mainHandler.post(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(true);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }
        });
    }

    private void stopIndeterminateProgress(boolean success) {
        mainHandler.post(() -> {
            if (progressBar != null) {
                progressBar.setIndeterminate(false);
                progressBar.setMax(100);
                progressBar.setProgress(100);

                int color = success ? Color.GREEN : Color.RED;
                progressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                // También aplicar a indeterminateDrawable por si acaso, aunque ya se detuvo
                progressBar.getIndeterminateDrawable().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        });
    }

    public void iniciar() {
        currentIndex = 0;
        startIndeterminateProgress();
        ejecutarSiguientePuerto();
    }

    public void agregarRedesObservadas(List<ScanResult> resultadosEscaneo) {
        if (resultadosEscaneo != null) {

            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("--- Redes Observadas Acumuladas ---\n");

            for (ScanResult result : resultadosEscaneo) {
                if (result.SSID != null && !result.SSID.isEmpty()) {
                    String ssidLimpio = result.SSID.trim().toUpperCase();
                    redesObservadas.add(ssidLimpio);
                }
            }

            logBuilder.append("Total actual: ").append(redesObservadas.size()).append(" SSIDs.\n");
            // ... (log de listado de SSIDs, omitido por brevedad) ...
            Log.d(TAG, logBuilder.toString());
        }
    }


    private void ejecutarSiguientePuerto() {
        // 1. Buscar siguiente puerto seleccionado
        while (currentIndex < puertosSeleccionados.length && !puertosSeleccionados[currentIndex]) {
            currentIndex++;
        }

        if (currentIndex >= puertosSeleccionados.length) {
            appendSalida("\n✅ Secuencia finalizada.\n");
            mainHandler.post(() -> {
                stopIndeterminateProgress(true);
                btnEnviar.setEnabled(true);
            });
            btnComenzar.setEnabled(true);
            return;
        }

        startIndeterminateProgress();

        final int puerto = currentIndex;

        appendSalida("⏳ Esperando estabilidad del sistema (3s)...");

        mainHandler.postDelayed(() -> {
            appendSalida("\n=== Configurando Puerto " + (puerto + 1) + " ===\n");

            SubInterface si = portMap.getSubInterfaces()[puerto];

            portMap.levantarSubinterfaz(si.getIp(), si.getNombre(), (success, salida) -> {
                if (!success) {
                    appendSalida("❌ Error levantando interfaz " + si.getNombre() + " en " + si.getIp() + "\n");
                    stopIndeterminateProgress(false);
                    currentIndex++;
                    ejecutarSiguientePuerto();
                    return;
                }

                mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto, () -> {
                    // Nota: stopIndeterminateProgress(true) se llama dentro de procesarResultadoScrap
                    currentIndex++;
                    ejecutarSiguientePuerto();
                }), 2000);
            });

        }, 3000);
    }

    /**
     * Ejecuta "show device_model" vía SSH anidado (Identifica el modelo).
     */
    private void ejecutarDeviceModel(int puerto, Runnable onDone) {
        SubInterface si = portMap.getSubInterfaces()[puerto];
        String routerIp = si.getIp();

        SshInteractivo ssh = new SshInteractivo(routerIp, "Support", "Te2010An_2014Ma");

        // Comandos para la doble conexión:
        ssh.setCommands(new String[]{
                "ssh -o StrictHostKeyChecking=no Support@192.168.1.1",
                "show device_model",
                "exit",
                "quit"
        });

        ssh.setSshListener((success, message, code) -> mainHandler.post(() -> {
            appendSalida("--- Resultado device_model Puerto " + (puerto + 1) + " ---\n");

            String modelo = "Modelo desconocido";

            if (success && message != null) {
                int showModelIndex = -1;

                // 1. Encontrar dónde se ejecutó el comando 'show device_model'
                for (int i = 0; i < message.length; i++) {
                    if (message[i] != null && message[i].trim().contains("show device_model")) {
                        showModelIndex = i;
                        break;
                    }
                }

                // 2. Si lo encontramos, asumimos que el modelo es la siguiente línea limpia
                if (showModelIndex != -1 && showModelIndex + 1 < message.length) {
                    String posibleModelo = message[showModelIndex + 1].trim();

                    if (!posibleModelo.isEmpty() &&
                            !posibleModelo.startsWith(">") &&
                            !posibleModelo.startsWith("#") &&
                            !posibleModelo.toLowerCase().contains("connection to")) {
                        modelo = posibleModelo;
                    }
                }
            }

            if (modelo.equals("Modelo desconocido")) {
                appendSalida("⚠️ No se pudo obtener el modelo.\n");
            }

            appendSalida("📡 " + modelo + "\n");

            ejecutarScraperPorModelo(puerto, modelo, onDone);
        }));
        ssh.start();
    }

    /**
     * Ejecuta el scraper según modelo. Llama a apagarYContinuar(puerto,onDone) cuando termina.
     */
    private void ejecutarScraperPorModelo(int puerto, String modelo, Runnable onDone) {
        prepararWebView();

        SubInterface si = portMap.getSubInterfaces()[puerto];
        String routerIp = si.getIp();

        // =================================================================
        // >>> BLOQUES DE MODELO - SIN LLAMAR A setValidationSets <<<
        // =================================================================
        if (modelo.contains("8145")) {
            appendSalida("⚙️ Detectado: Modelo 8145. Iniciando Scraping vía SSH anidado (Clase Hgu8145)...\n");

            Hgu8145 hgu = new Hgu8145();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);

            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.

            hgu.setHgu8145Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap8145Ssh(routerIp);
        } else if (modelo.contains("2541")) {
            Hgu2541 hgu = new Hgu2541();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu2541Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap2541(this.webView);

        } else if (modelo.contains("2741")) {
            Hgu2741 hgu = new Hgu2741();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu2741Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone)
            );
            hgu.scrap2741(this.webView);


        } else if (modelo.contains("2742")) {
            Hgu2742 hgu = new Hgu2742();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu2742Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap2742(this.webView);
        } else if (modelo.contains("3505")) {
            Hgu3505 hgu = new Hgu3505(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu3505Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap3505(this.webView);
        } else if (modelo.contains("8115")) {
            Hgu8115 hgu = new Hgu8115(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu8115Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap8115(this.webView);
        } else if (modelo.contains("8225")) {
            Hgu8225 hgu = new Hgu8225(context);
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);
            // ❌ Línea hgu.setValidationSets(...) ELIMINADA.
            hgu.setHgu8225Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));
            hgu.scrap8225(this.webView);
        } else {
            appendSalida("⚠️ Modelo no soportado: " + modelo + "\n");
            stopIndeterminateProgress(false); // Detiene con error
            onDone.run();
        }
    }

    private void procesarResultadoScrap(int puerto, String modelo, boolean success, Resultado resultado, int code, Runnable onDone) {
        if (resultado == null) resultado = new Resultado();

        // 1. Cargar metadatos
        resultado.setCatalogo(this.catalogo); // Usamos la variable de clase (catalogo)
        resultado.setMultiprobador(MultiLocal);

        // 2. Ejecutar validación, usando las variables de clase (serialesInvalidos, etc.)
        ValidadorResultado.ResultadoCompleto completo = ValidadorResultado.validar(
                resultado, // 1. El objeto Resultado extraído del HGU
                serialesInvalidos, // 2. Lista de seriales bloqueados (Accedidas directamente como variables de clase)
                firmwaresActuales, // 3. Firmware OK
                firmwaresCriticos, // 4. Firmware Crítico (Error)
                firmwaresObsoletos, // 5. Firmware Obsoleto (Warning)
                getRedesObservadas() // 6. Redes Observadas
        );
        // Obtener los datos validados/modificados y la validación para el log
        Resultado datosValidados = completo.getDatosModificados();
        ValidadorResultado.ResultadoValidacion val = completo.getValidacion();

        if (success) {
            appendSalida("✅ Scrap OK [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            appendSalida("Validación: " + val.getMensaje() + "\n");

            // Fijar el estado de la barra de progreso (Verde si no hay ERROR en la validación)
            stopIndeterminateProgress(val.getEstado() != ValidadorResultado.EstadoValidacion.ERROR);

            // Imprimir todos los valores de resultado (usando datosValidados)
            appendSalida("📊 Resultado completo:");
            appendSalida("Serial: " + datosValidados.getSerial());
            // ... (resto de logs de datos) ...

            // 📢 ¡ACCIÓN CLAVE 1: ENVIAR A LA PANTALLA!
            mainHandler.post(() -> {
                resultadoListener.onResultadoFinalizado(completo); // Llama a MainActivity
                this.probando=false;
            });

        } else {
            appendSalida("❌ Scrap falló [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            stopIndeterminateProgress(false); // Falla SSH/Scraping es siempre rojo

            // Crear un objeto completo de error
            ValidadorResultado.ResultadoValidacion errorVal = new ValidadorResultado.ResultadoValidacion(
                    ValidadorResultado.EstadoValidacion.ERROR,
                    "Error de conexión SSH/Scraping."
            );
            ValidadorResultado.ResultadoCompleto errorCompleto = new ValidadorResultado.ResultadoCompleto(errorVal, resultado);

            // 📢 ¡ACCIÓN CLAVE 2: ENVIAR FALLO!
            mainHandler.post(() -> {
                resultadoListener.onResultadoFinalizado(errorCompleto);
            });
        }

        resultados.add(datosValidados);
        apagarYContinuar(puerto, onDone);
    }

    /**
     * Apaga la última interfaz levantada y continúa con el siguiente puerto.
     */
    private void apagarYContinuar(int puerto, Runnable onDone) {
        // 📢 CORRECCIÓN CONFIRMADA: Apaga SOLAMENTE la última interfaz levantada.
        portMap.apagarUltimaInterfaz((success, salida) -> mainHandler.post(() -> {
            appendSalida("Interfaces limpiadas: " + (success ? "OK" : "FALLÓ") + "\n");
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



    private void enviar() {
        // Asumimos que 'context' es una Activity, sino se debe asegurar
        Activity activity = (Activity) context;

        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity);
        }

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {

                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                        appendSalida("✅ Actualización encontrada. Iniciando descarga en segundo plano.\n");

                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.FLEXIBLE,
                                    activity,
                                    MY_UPDATE_REQUEST_CODE);
                            enviarResultadosPorCorreo();

                        } catch (IntentSender.SendIntentException e) {
                            appendSalida("❌ Error iniciando flujo de actualización: " + e.getMessage() + "\n");
                            enviarResultadosPorCorreo();
                        }

                    } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        appendSalida("✅ Actualización descargada. Sugiriendo instalación...\n");
                        notifyUserAboutUpdate();
                        enviarResultadosPorCorreo();

                    } else {
                        appendSalida("⚠️ No hay actualizaciones disponibles. Continuando con el envío.\n");
                        enviarResultadosPorCorreo();
                    }
                });
    }

    private void notifyUserAboutUpdate() {
        mainHandler.post(() -> {
            appendSalida("👉 Vuelve a abrir la aplicación para completar la actualización.");
        });
    }

    public void enviarResultadosPorCorreo() {
        try {
            // 1️⃣ Generar CSV
            String csvFileName = "Resultados_" + System.currentTimeMillis() + ".csv";
            File csvFile = new File(context.getFilesDir(), csvFileName);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                String[] headers = {
                        "fecha","multiprobador","modelo","mac", "serial", "firmware","potencia_ref"
                        ,"potencia",
                        "ssid2", "estado2", "canal2", "rssi_ref_2","rssi2",
                        "ssid5", "estado5", "canal5","rssi_ref_5","rssi5",
                        "usuario", "voip","loteDescripcion",
                        "catalogo", "falla", "resultado"
                };
                writer.write(String.join(",", headers));
                writer.newLine();
/*
* fecha	multiprobador	modelo	mac	serial	firmware	potencia_ref	potencia	ssid2
estado2
	canal2	rssi_ref_2	rssi2	ssid5	estado5	canal5	rssi_ref_5	rssi5
	* usuario	voip	loteDescripcion	catalogo	falla	resultado
* */
                for (Resultado r : resultados) {
                    String[] values = {
                            r.getFecha(), r.getMultiprobador(), r.getModelo(),"mac", r.getSerial(), r.getFirmware(),"potencia_ref", r.getPotencia(),
                            r.getSsid2(), r.getEstado2(), r.getCanal2(),"rssi_ref_2", r.getRssi2(),
                            r.getSsid5(), r.getEstado5(), r.getCanal5(),"rssi_ref_5", r.getRssi5(),
                            r.getUsuario(), r.getVoip(),"loteDescripcion",
                            r.getCatalogo(), r.getFalla(), r.getResultado()
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