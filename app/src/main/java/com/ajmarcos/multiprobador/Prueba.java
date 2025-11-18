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
// Prueba.java (Añadir estas importaciones y variables)
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.install.model.InstallStatus;
import android.content.IntentSender;
import android.app.Activity; // Necesario para iniciar el flujo

public class Prueba {

    private AppUpdateManager appUpdateManager;
    private static final int MY_UPDATE_REQUEST_CODE = 101;

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
    private boolean probando;

    private final PruebaResultadoListener resultadoListener; // 👈 📢 NUEVA DECLARACIÓN


    public void setCatalogo(String catalogo) {
        this.catalogo = catalogo;
    }
    public String getCatalogo() { return this.catalogo; }

    public Prueba(WebView webView, boolean[] puertosSeleccionados, Context context, TextView tvSalida,
                  Button btnComenzar, Button btnEnviar, PortMap portMap, PruebaResultadoListener resultadoListener) { // 👈 CAMBIO AQUÍ
        this.puertosSeleccionados = puertosSeleccionados;
        this.context = context;
        this.tvSalida = tvSalida;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.portMap = portMap;
        this.btnComenzar = btnComenzar;
        this.btnEnviar = btnEnviar;
        this.webView = webView;
        this.resultadoListener = resultadoListener; // 👈 GUARDA LA REFERENCIA
        this.probando=false;

        btnComenzar.setOnClickListener(v -> {

            this.probando=true;
            btnComenzar.setEnabled(false);
            btnEnviar.setEnabled(false);
            resultados.clear();
            currentIndex = 0;
            // arranca la secuencia
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
//    private void ejecutarSiguientePuerto() {
//        // buscar siguiente puerto seleccionado
//        while (currentIndex < puertosSeleccionados.length && !puertosSeleccionados[currentIndex]) {
//            currentIndex++;
//        }
//
//        if (currentIndex >= puertosSeleccionados.length) {
//            appendSalida("\n✅ Secuencia finalizada.\n");
//            mainHandler.post(() -> btnEnviar.setEnabled(true));
//            btnComenzar.setEnabled(true);
//            return;
//        }
//
//        final int puerto = currentIndex;
//        appendSalida("\n=== Configurando Puerto " + (puerto + 1) + " ===\n");
//
//        // levantar subinterfaz correspondiente
//        SubInterface si = portMap.getSubInterfaces()[puerto];
//        // NOTA: portMap.levantarSubinterfaz ahora SOLO levanta la interfaz.
//        portMap.levantarSubinterfaz(si.getIp(), si.getNombre(), (success, salida) -> {
//            if (!success) {
//                appendSalida("❌ Error levantando interfaz " + si.getNombre() + " en " + si.getIp() + "\n");
//                // avanzar al siguiente puerto
//                currentIndex++;
//                ejecutarSiguientePuerto();
//                return;
//            }
//
//            // esperar un poco y llamar a device_model (todo en secuencia)
//            mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto, () -> {
//                // llamado cuando termina todo para este puerto (incluye apagar interfaces)
//                currentIndex++;
//                ejecutarSiguientePuerto();
//            }), 1500);
//        });
//    }


    // En Prueba.java

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

        // 📢 AÑADIR RETRASO AQUÍ (Ej. 3 segundos)
        // Esto permite que las conexiones SSH previas se cierren completamente en el router.
        appendSalida("⏳ Esperando estabilidad del sistema (3s)...");

        mainHandler.postDelayed(() -> {
            appendSalida("\n=== Configurando Puerto " + (puerto + 1) + " ===\n");

            // levantar subinterfaz correspondiente
            SubInterface si = portMap.getSubInterfaces()[puerto];

            portMap.levantarSubinterfaz(si.getIp(), si.getNombre(), (success, salida) -> {
                if (!success) {
                    appendSalida("❌ Error levantando interfaz " + si.getNombre() + " en " + si.getIp() + "\n");
                    currentIndex++;
                    ejecutarSiguientePuerto();
                    return;
                }

                // El retraso posterior a levantar la interfaz también es importante
                mainHandler.postDelayed(() -> ejecutarDeviceModel(puerto, () -> {
                    currentIndex++;
                    ejecutarSiguientePuerto();
                }), 2000); // Aumentado a 2000ms (2s)
            });

        }, 3000); // Retraso inicial de 3000ms
    }

    /**
     * Ejecuta "show device_model" vía SSH anidado al equipo de gestión (192.168.1.1).
     * Se conecta al router 24X y desde allí al 1.1.
     */
    /**
     * Ejecuta "show device_model" vía SSH anidado al equipo de gestión (192.168.1.1).
     * Se conecta al router 24X y desde allí al 1.1.
     */
    private void ejecutarDeviceModel(int puerto, Runnable onDone) {
        SubInterface si = portMap.getSubInterfaces()[puerto];
        String routerIp = si.getIp(); // IP del router de control (240, 241, 242)

        // Se conecta al router y envía los comandos anidados
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

            // --- Lógica de Extracción de Modelo MÁS FLEXIBLE (CORREGIDA) ---
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

                    // Aseguramos que no sea un prompt (> o #) o un mensaje de cierre de conexión.
                    if (!posibleModelo.isEmpty() &&
                            !posibleModelo.startsWith(">") &&
                            !posibleModelo.startsWith("#") &&
                            !posibleModelo.toLowerCase().contains("connection to")) {

                        modelo = posibleModelo;
                    }
                }
            }

            // Si el modelo es "Modelo desconocido" después del intento, registramos el fallo.
            if (modelo.equals("Modelo desconocido")) {
                appendSalida("⚠️ No se pudo obtener el modelo. Mensaje completo de salida SSH:\n" + (message != null ? String.join("\n", message) : "Nulo") + "\n");
            }

            // -----------------------------------------------------------------

            appendSalida("📡 " + modelo + "\n");

            // Ejecutar scraper asociado; cuando el scraper termine, se debe llamar a apagarYContinuar(puerto,onDone)
            ejecutarScraperPorModelo(puerto, modelo, onDone);
        }));
        ssh.start();
    }

    /**
     * Ejecuta el scraper según modelo. Llama a apagarYContinuar(puerto,onDone) cuando termina.
     */
// En Prueba.java

    /**
     * Ejecuta el scraper según modelo. Llama a apagarYContinuar(puerto,onDone) cuando termina.
     */
    private void ejecutarScraperPorModelo(int puerto, String modelo, Runnable onDone) {
        prepararWebView();

        // Necesitamos la IP del router actual para el SSH anidado
        SubInterface si = portMap.getSubInterfaces()[puerto];
        String routerIp = si.getIp();

        // cada HguXXXX debe llamar a su listener que finalmente invoque procesarResultadoScrap(..., onDone)

        // =================================================================
        // >>> NUEVO BLOQUE: Manejo del modelo 8145 vía SSH <<<
        // =================================================================
        if (modelo.contains("8145")) {
            appendSalida("⚙️ Detectado: Modelo 8145. Iniciando Scraping vía SSH anidado (Clase Hgu8145)...\n");

            Hgu8145 hgu = new Hgu8145();
            hgu.setModel(modelo);
            hgu.setMulti(MultiLocal);
            hgu.setCatal(catalogo);

            // El listener llama al procesador final de la clase Prueba
            hgu.setHgu8145Listener((success, res, code) -> procesarResultadoScrap(puerto, modelo, success, res, code, onDone));

            // Inicia el proceso de scraping SSH, pasando la IP del router de control
            hgu.scrap8145Ssh(routerIp);
        } else if (modelo.contains("2541")) {
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
            hgu.scrap2741(this.webView);


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

        // 1. Cargar metadatos
        resultado.setCatalogo(this.getCatalogo());
        resultado.setMultiprobador(MultiLocal);

        // 2. Ejecutar validación y obtener el contenedor completo
        ValidadorResultado.ResultadoCompleto completo = ValidadorResultado.validar(resultado);

        // Obtener los datos validados/modificados y la validación para el log
        Resultado datosValidados = completo.getDatosModificados();
        ValidadorResultado.ResultadoValidacion val = completo.getValidacion();

        if (success) {
            appendSalida("✅ Scrap OK [" + modelo + "] Puerto " + (puerto + 1) + "\n");
            appendSalida("Validación: " + val.getMensaje() + "\n");

            // Imprimir todos los valores de resultado (usando datosValidados)
            appendSalida("📊 Resultado completo:");
            appendSalida("Serial: " + datosValidados.getSerial());
            // ... (resto de impresiones usando datosValidados) ...

            // 📢 ¡ACCIÓN CLAVE 1: ENVIAR A LA PANTALLA!
            mainHandler.post(() -> {
                resultadoListener.onResultadoFinalizado(completo); // Llama a MainActivity
                this.probando=false;
            });

        } else {
            appendSalida("❌ Scrap falló [" + modelo + "] Puerto " + (puerto + 1) + "\n");

            // Crear un objeto completo de error
            ValidadorResultado.ResultadoValidacion errorVal = new ValidadorResultado.ResultadoValidacion(
                    ValidadorResultado.EstadoValidacion.ERROR,
                    "Scrap Fallido en " + modelo
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

    // 🔥 EL MÉTODO FALTANTE 🔥
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

    // Prueba.java

    public void prepararInternetYEnviar() throws InterruptedException {
        String ipInternet = "192.168.1.230";
        String subInterfaz = "eth3.0";

        appendSalida("🌐 Levantando interfaz " + ipInternet + " / " + subInterfaz + "...\n");
        // Usamos un nuevo hilo para la lógica que puede ser bloqueante y la conexión
        new Thread(() -> {
            try {
                portMap.levantarSubinterfaz(ipInternet, subInterfaz, (success, salida) -> mainHandler.post(() -> {
                    if (!success) {
                        appendSalida("❌ No se pudo levantar la interfaz " + subInterfaz + " en " + ipInternet + "\n");
                        return;
                    }

                    appendSalida("✅ Interfaz levantada. Verificando conexión a Internet...\n");

                    // Mueve la verificación de internet a un hilo secundario si aún no lo está
                    new Thread(() -> {
                        if (tieneInternet()) {
                            mainHandler.post(() -> {
                                appendSalida("✅ Conexión a Internet OK.\n");

                                // 📢 PASO CLAVE: Verificar actualización AHORA
                                checkForAppUpdatesAndContinue();

                            });
                        } else {
                            mainHandler.post(() -> appendSalida("❌ No hay conexión a Internet. No se puede enviar.\n"));
                        }
                    }).start();
                }));
            } catch (Exception e) {
                mainHandler.post(() -> appendSalida("Error en el flujo de conexión: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * 📢 NUEVO MÉTODO: Verifica y descarga la actualización si está disponible.
     */
    private void checkForAppUpdatesAndContinue() {

        // Si el contexto pasado al constructor no es una Activity, esto fallará.
        // Asumiremos que el 'context' es la MainActivity (Activity).
        Activity activity = (Activity) context;

        if (appUpdateManager == null) {
            appUpdateManager = AppUpdateManagerFactory.create(activity);
        }

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {

                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) { // Flujo Flexible

                        appendSalida("✅ Actualización encontrada. Iniciando descarga en segundo plano.\n");

                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.FLEXIBLE,
                                    activity, // Necesita la Activity
                                    MY_UPDATE_REQUEST_CODE);

                            // Continúa con el envío de correo mientras se descarga
                            enviarResultadosPorCorreo();

                        } catch (IntentSender.SendIntentException e) {
                            appendSalida("❌ Error iniciando flujo de actualización: " + e.getMessage() + "\n");
                            enviarResultadosPorCorreo(); // Si falla la actualización, envía igual.
                        }

                    } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        // Si ya estaba descargada de una ejecución anterior, notifica para instalar.
                        appendSalida("✅ Actualización descargada. Sugiriendo instalación...\n");
                        notifyUserAboutUpdate();
                        enviarResultadosPorCorreo(); // Envía igual.

                    } else {
                        appendSalida("⚠️ No hay actualizaciones disponibles. Continuando con el envío.\n");
                        enviarResultadosPorCorreo();
                    }
                });
    }

    /**
     * 📢 NUEVO MÉTODO: Notifica a MainActivity para que muestre el diálogo de instalación.
     */
    private void notifyUserAboutUpdate() {
        // Usamos el listener para comunicar a MainActivity (si implementaste un método para esto)
        // o mostramos un Toast/log simple para que el usuario sepa que debe reiniciar.

        // Lo más seguro es que MainActivity maneje esto en su onResume,
        // pero si quieres un diálogo inmediato:

        mainHandler.post(() -> {
            // Esto requiere que MainActivity tenga una función pública para mostrar el diálogo.
            // Por simplicidad, solo notificaremos vía log/salida
            appendSalida("👉 Vuelve a abrir la aplicación para completar la actualización.");
        });
    }

    private boolean tieneInternet() {
        try {
            // Envía 10 paquetes ICMP a 8.8.8.8 para verificar internet real
            Process p = Runtime.getRuntime().exec("ping -c 10 8.8.8.8");
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
