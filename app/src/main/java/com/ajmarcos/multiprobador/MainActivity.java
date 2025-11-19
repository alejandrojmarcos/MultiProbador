package com.ajmarcos.multiprobador;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.firebase.messaging.FirebaseMessaging;
import com.ajmarcos.multiprobador.ValidadorResultado.ResultadoCompleto;
import com.ajmarcos.multiprobador.ValidadorResultado.EstadoValidacion;

// Implementación de la interfaz de comunicación local (Callback)
public class MainActivity extends AppCompatActivity implements PruebaResultadoListener {

    // --- Variables de Actualización In-App ---
    private AppUpdateManager appUpdateManager;
    private static final int MY_REQUEST_CODE = 101;

    private ProgressBar progressBar;

    // --- Variables de Interfaz y Lógica ---
    private Button btnComenzar;
    private Button btnEnviar;
    private Button btnCancelar;
    private boolean[] puertosSeleccionados;
    private CheckBox[] arrayCheckBoxSeleccionPuerto;
    private Button [] arrayBotonesPuerto;
    private TextView tvSalida;

    // Declaración de todos los TextViews de respuesta
    private TextView tvActualResp, tvPingResp, tvPuertoResp, tvModeloResp, tvSerieResp, tvFirmwareResp,
            tvMacResp, tvPotenciaResp, tvSSID2Resp, tvRssi2Resp, tvEstado2Resp, tvSSID5Resp, tvRssi5Resp,
            tvEstado5Resp, tvVoipResp, tvCanal2Resp, tvCanal5Resp;

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_8);

        progressBar = findViewById(R.id.progressBar);

        //firebase

        // En onCreate:
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();

                    // Log and toast
                    Log.d(TAG, "FCM Token: " + token);
                    // Toast.makeText(MainActivity.this, "Token: " + token, Toast.LENGTH_SHORT).show();
                });



        // 1. INICIALIZACIÓN DE IN-APP UPDATE MANAGER
        appUpdateManager = AppUpdateManagerFactory.create(this);
        checkForAppUpdates();

        // 2. INICIALIZACIÓN DE VISTAS Y ARRAYS
        initializeTextViews();

        tvSalida = findViewById(R.id.tvSalida);

        // Inicialización de arrays (CheckBoxes y Botones)
        arrayCheckBoxSeleccionPuerto = new CheckBox[]{
                findViewById(R.id.cbPuerto01), findViewById(R.id.cbPuerto02), findViewById(R.id.cbPuerto03),
                findViewById(R.id.cbPuerto04), findViewById(R.id.cbPuerto05), findViewById(R.id.cbPuerto06),
                findViewById(R.id.cbPuerto07), findViewById(R.id.cbPuerto08)
        };
        arrayBotonesPuerto = new Button[]{
                findViewById(R.id.button1), findViewById(R.id.button2), findViewById(R.id.button3),
                findViewById(R.id.button4), findViewById(R.id.button5), findViewById(R.id.button6),
                findViewById(R.id.button7), findViewById(R.id.button8)
        };

        // 3. CONFIGURACIÓN DE LISTENERS
        if (arrayBotonesPuerto.length == arrayCheckBoxSeleccionPuerto.length) {
            setupPortButtonListeners();
        } else {
            Log.e("MainActivity", "Error: Los arrays de botones y checkboxes no coinciden en tamaño.");
        }

        btnComenzar = findViewById(R.id.buttonComenzar);
        btnEnviar = findViewById(R.id.buttonEnviar);
        btnCancelar = findViewById(R.id.buttonCancelar);

        // Marcar todos los checkboxes por defecto
        for (CheckBox cb : arrayCheckBoxSeleccionPuerto) cb.setChecked(true);
        puertosSeleccionados = new boolean[8];

        btnComenzar.setOnClickListener(v -> mostrarDialogoLoteCatalogo());
    }

    // --- MÉTODOS AUXILIARES Y LÓGICA DE ACTUALIZACIÓN ---

    private void initializeTextViews() {
        tvActualResp = findViewById(R.id.tvActualResp);
        tvPingResp = findViewById(R.id.tvPingResp);
        tvPuertoResp = findViewById(R.id.tvPuertoResp);
        tvModeloResp = findViewById(R.id.tvModeloResp);
        tvSerieResp = findViewById(R.id.tvSerieResp);
        tvFirmwareResp = findViewById(R.id.tvFirmwareResp);
        tvMacResp = findViewById(R.id.tvMacResp);
        tvPotenciaResp = findViewById(R.id.tvPotenciaResp);
        tvSSID2Resp = findViewById(R.id.tvSSID2Resp);
        tvRssi2Resp = findViewById(R.id.tvRssi2Resp);
        tvEstado2Resp = findViewById(R.id.tvEstado2Resp);
        tvSSID5Resp = findViewById(R.id.tvSSID5Resp);
        tvRssi5Resp = findViewById(R.id.tvRssi5Resp);
        tvEstado5Resp = findViewById(R.id.tvEstado5Resp);
        tvVoipResp = findViewById(R.id.tvVoipResp);
        tvCanal2Resp = findViewById(R.id.tvCanal2Resp);
        tvCanal5Resp = findViewById(R.id.tvCanal5Resp);
    }

    private void setupPortButtonListeners() {
        for (int i = 0; i < arrayBotonesPuerto.length; i++) {
            final int index = i;

            arrayBotonesPuerto[i].setOnClickListener(v -> {
                boolean isChecked = arrayCheckBoxSeleccionPuerto[index].isChecked();
                arrayCheckBoxSeleccionPuerto[index].setChecked(!isChecked);
                Toast.makeText(this, "Puerto " + (index + 1) + ": Checkbox " + (arrayCheckBoxSeleccionPuerto[index].isChecked() ? "SELECCIONADO" : "DESELECCIONADO"), Toast.LENGTH_SHORT).show();
            });

            arrayBotonesPuerto[i].setOnLongClickListener(v -> {
                Toast.makeText(this, "LONG CLICK en Puerto " + (index + 1) + ". Aquí iría la prueba individual.", Toast.LENGTH_LONG).show();
                return true;
            });
        }
    }


    private void mostrarDialogoLoteCatalogo() {
        AlertDialog.Builder builderTipo = new AlertDialog.Builder(this);
        builderTipo.setTitle("Seleccionar tipo de lote");

        String[] tipos = {"Logística", "Revisador", "Garantía"};
        builderTipo.setItems(tipos, (dialog, whichTipo) -> {
            String tipoSeleccionado = tipos[whichTipo];

            String[] lotes;
            switch (tipoSeleccionado) {
                case "Logística":
                    lotes = new String[]{
                            "10206110004 HGU Averia", "10206110006 HGU Provision",
                            "10206110010 WIFI6 Provision", "10206110029 WIFI6 Averia"
                    };
                    break;
                case "Revisador":
                case "Garantía":
                    lotes = new String[]{"10206110004 HGU", "10206110029 WIFI6"};
                    break;
                default:
                    lotes = new String[]{};
                    break;
            }

            AlertDialog.Builder builderLote = new AlertDialog.Builder(this);
            builderLote.setTitle("Seleccionar lote de " + tipoSeleccionado);
            builderLote.setItems(lotes, (dialogLote, whichLote) -> {
                String catalogoSeleccionado = lotes[whichLote];

                // 1. Recopila la selección final de puertos
                PortMap portMap = new PortMap();
                puertosSeleccionados = new boolean[arrayCheckBoxSeleccionPuerto.length];
                for (int i = 0; i < arrayCheckBoxSeleccionPuerto.length; i++) {
                    puertosSeleccionados[i] = arrayCheckBoxSeleccionPuerto[i].isChecked();
                }

                WebView webView = findViewById(R.id.webViewScrap);

                // 2. CONSTRUCTOR DE PRUEBA COMPLETO (Pasa 'this' como Listener)
                Prueba prueba = new Prueba(
                        webView,
                        puertosSeleccionados,
                        this,
                        tvSalida,
                        btnComenzar,
                        btnEnviar,
                        portMap,
                        this,
                        progressBar// 👈 PASA 'this' como el PruebaResultadoListener
                );
                prueba.setCatalogo(tipoSeleccionado + "-" + catalogoSeleccionado);

                Log.d(TAG, CLASS + ": Selección => " + prueba.getCatalogo());

                portMap.apagarTodasLasIPs((success, salida) -> runOnUiThread(() -> {
                    if (success) {
                        tvSalida.append("Todas las interfaces apagadas.\n");
                        prueba.iniciar();
                    } else {
                        tvSalida.append("Error apagando interfaces.\n");
                        btnComenzar.setEnabled(true);
                    }
                }));
            });

            builderLote.setNegativeButton("Cancelar", (dialogLote, whichLote) -> {
                dialogLote.dismiss();
                Log.d(TAG, CLASS + ": Cancelado lote de " + tipoSeleccionado);
                tvSalida.append("Selección cancelada. No se inició ninguna secuencia.\n");
                btnComenzar.setEnabled(true);
            });

            builderLote.show();
        });

        builderTipo.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
            Log.d(TAG, CLASS + ": Selección de tipo cancelada");
            tvSalida.append("Selección cancelada. No se inició ninguna secuencia.\n");
            btnComenzar.setEnabled(true);
        });

        builderTipo.show();
    }

    // --- LÓGICA DE ACTUALIZACIÓN IN-APP (PLAY CORE) ---

// MainActivity.java

// MainActivity.java

    @Override
    protected void onResume() {
        super.onResume();

        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {

                    // Verifica si la actualización inmediata (IMMEDIATE) se interrumpió y necesita reanudarse.
                    // 📢 CORRECCIÓN: Usamos el valor entero 3 (que reemplaza a DEVELOPER_TRIGGERED_UPDATE_NEEDED)
                    if (appUpdateInfo.updateAvailability() == 3) {
                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.IMMEDIATE,
                                    this,
                                    MY_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error reanudando flujo inmediato", e);
                        }
                    }
                });
        // ... (El resto del código de onResume para el Flujo Flexible) ...
    }

    // MainActivity.java

    private void checkForAppUpdates() {
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {

                    // 📢 CAMBIO CLAVE: Usamos AppUpdateType.IMMEDIATE
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                        try {
                            appUpdateManager.startUpdateFlowForResult(
                                    appUpdateInfo,
                                    AppUpdateType.IMMEDIATE, // 👈 FUERZA LA INSTALACIÓN
                                    this,
                                    MY_REQUEST_CODE);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e(TAG, "Error iniciando flujo inmediato", e);
                        }
                    }
                });
    }

    private void notifyUserAboutUpdate() {
        new AlertDialog.Builder(this)
                .setTitle("Actualización lista")
                .setMessage("Se ha descargado una nueva versión. ¿Desea instalarla ahora?")
                .setPositiveButton("Instalar y Reiniciar", (dialog, which) -> {
                    appUpdateManager.completeUpdate();
                })
                .setNegativeButton("Más tarde", (dialog, which) -> {
                })
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Log.w(TAG, "Actualización cancelada o fallida. Código: " + resultCode);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // --- IMPLEMENTACIÓN DE PRUEBARESULTADOLISTENER (CALLBACK) ---
    @Override
    public void onResultadoFinalizado(ResultadoCompleto completo) {

        ValidadorResultado.ResultadoValidacion validacion = completo.getValidacion();
        Resultado datos = completo.getDatosModificados();

        // 1. Actualiza el mensaje principal y el color
        tvActualResp.setText(validacion.getMensaje());

        switch (validacion.getEstado()) {
            case OK: tvActualResp.setTextColor(Color.parseColor("#4CAF50")); break;
            case WARNING: tvActualResp.setTextColor(Color.parseColor("#FF9800")); break;
            case ERROR: tvActualResp.setTextColor(Color.parseColor("#F44336")); break;
        }

        // 2. Actualiza los campos de datos individuales
        if (datos != null) {
            tvModeloResp.setText(datos.getModelo());
            tvSerieResp.setText(datos.getSerial());
            tvFirmwareResp.setText(datos.getFirmware());
            tvPotenciaResp.setText(datos.getPotencia());
            tvVoipResp.setText(datos.getVoip());
            tvMacResp.setText(datos.getUsuario());
            tvSSID2Resp.setText(datos.getSsid2());
            tvSSID5Resp.setText(datos.getSsid5());
            tvEstado2Resp.setText(datos.getEstado2());
            tvEstado5Resp.setText(datos.getEstado5());
            tvCanal2Resp.setText(datos.getCanal2());
            tvCanal5Resp.setText(datos.getCanal5());
            tvRssi2Resp.setText(datos.getRssi2());
            tvRssi5Resp.setText(datos.getRssi5());
            // tvPingResp y tvPuertoResp se dejan para otra lógica si no se actualizan aquí
        }
    }

    // --- MÉTODOS DE MENÚ ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(this, "Abriendo Configuración...", Toast.LENGTH_SHORT).show();
            SettingsDialogFragment dialog = new SettingsDialogFragment();
            dialog.show(getSupportFragmentManager(), "SettingsDialogTag");
            return true;
        }

        if (id == R.id.action_acerca_de) {
            Toast.makeText(this, "Información de la aplicación Multiprobador.", Toast.LENGTH_LONG).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}