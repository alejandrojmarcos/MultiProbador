package com.ajmarcos.multiprobador;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button btnComenzar;
    private Button btnEnviar;
    private Button btnCancelar;
    private boolean[] puertosSeleccionados;
    private CheckBox[] arrayCheckBoxSeleccionPuerto;
    private Button [] arrayBotonesPuerto;
    private TextView tvSalida;
    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_8);

        tvSalida = findViewById(R.id.tvSalida);
        WebView webView = findViewById(R.id.webViewScrap);

        arrayCheckBoxSeleccionPuerto = new CheckBox[]{
                findViewById(R.id.cbPuerto01),
                findViewById(R.id.cbPuerto02),
                findViewById(R.id.cbPuerto03),
                findViewById(R.id.cbPuerto04),
                findViewById(R.id.cbPuerto05),
                findViewById(R.id.cbPuerto06),
                findViewById(R.id.cbPuerto07),
                findViewById(R.id.cbPuerto08)
        };

        arrayBotonesPuerto = new Button[]{
                findViewById(R.id.button1),
                findViewById(R.id.button2),
                findViewById(R.id.button3),
                findViewById(R.id.button4),
                findViewById(R.id.button5),
                findViewById(R.id.button6),
                findViewById(R.id.button7),
                findViewById(R.id.button8)
        };

        // Asegúrate de que estos arrays estén declarados como variables de clase o pasados al método
// CheckBox[] arrayCheckBoxSeleccionPuerto;
// Button[] arrayBotonesPuerto;

// Asumiendo que ambos arrays tienen el mismo tamaño (8 elementos)
        if (arrayBotonesPuerto.length == arrayCheckBoxSeleccionPuerto.length) {

            for (int i = 0; i < arrayBotonesPuerto.length; i++) {
                final int index = i; // Necesario para usar 'i' dentro de los listeners

                // 1. Manejo del Click Corto (Alternar CheckBox)
                arrayBotonesPuerto[i].setOnClickListener(v -> {
                    // Alterna el estado del CheckBox correspondiente
                    boolean isChecked = arrayCheckBoxSeleccionPuerto[index].isChecked();
                    arrayCheckBoxSeleccionPuerto[index].setChecked(!isChecked);

                    // Opcional: Mostrar un mensaje
                    Toast.makeText(this, "Puerto " + (index + 1) + ": Checkbox " + (arrayCheckBoxSeleccionPuerto[index].isChecked() ? "SELECCIONADO" : "DESELECCIONADO"), Toast.LENGTH_SHORT).show();
                });

                // 2. Manejo del Long Click (Mostrar Toast Inicial)
                arrayBotonesPuerto[i].setOnLongClickListener(v -> {
                    // Muestra un Toast indicando el puerto
                    Toast.makeText(this, "LONG CLICK en Puerto " + (index + 1) + ". Aquí iría la prueba individual.", Toast.LENGTH_LONG).show();

                    // Devuelve true para indicar que consumimos el evento (para que no se dispare también el click corto)
                    return true;
                });
            }

        } else {
            // Manejo de error si los arrays no coinciden en tamaño
            Log.e("MainActivity", "Error: Los arrays de botones y checkboxes no coinciden en tamaño.");
        }


        btnComenzar = findViewById(R.id.buttonComenzar);
        btnEnviar = findViewById(R.id.buttonEnviar);
        btnCancelar = findViewById(R.id.buttonCancelar);

        // marcar todos los checkboxes por defecto
        for (CheckBox cb : arrayCheckBoxSeleccionPuerto) cb.setChecked(true);

        puertosSeleccionados = new boolean[8];

        PortMap portMap = new PortMap();
        Prueba prueba = new Prueba(webView,puertosSeleccionados, this, tvSalida, btnComenzar, btnEnviar, portMap);

        btnComenzar.setOnClickListener(v -> mostrarDialogoLoteCatalogo());

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
                            "10206110004 HGU Averia",
                            "10206110006 HGU Provision",
                            "10206110010 WIFI6 Provision",
                            "10206110029 WIFI6 Averia"
                    };
                    break;
                case "Revisador":
                case "Garantía":
                    lotes = new String[]{
                            "10206110004 HGU",
                            "10206110029 WIFI6"
                    };
                    break;
                default:
                    lotes = new String[]{};
                    break;
            }

            AlertDialog.Builder builderLote = new AlertDialog.Builder(this);
            builderLote.setTitle("Seleccionar lote de " + tipoSeleccionado);
            builderLote.setItems(lotes, (dialogLote, whichLote) -> {
                String catalogoSeleccionado = lotes[whichLote];

                // ✅ SOLO AQUÍ se crea la prueba
                PortMap portMap = new PortMap();
                puertosSeleccionados = new boolean[arrayCheckBoxSeleccionPuerto.length];
                for (int i = 0; i < arrayCheckBoxSeleccionPuerto.length; i++) {
                    puertosSeleccionados[i] = arrayCheckBoxSeleccionPuerto[i].isChecked();
                }

                WebView webView = findViewById(R.id.webViewScrap);
                Prueba prueba = new Prueba(webView, puertosSeleccionados, this, tvSalida, btnComenzar, btnEnviar, portMap);
                prueba.setCatalogo(tipoSeleccionado + "-" + catalogoSeleccionado);

                Log.d(TAG, CLASS + ": Selección => " + prueba.getCatalogo());

                portMap.apagarTodasLasIPs((success, salida) -> runOnUiThread(() -> {
                    if (success) {
                        tvSalida.append("Todas las interfaces apagadas.\n");
                        prueba.iniciar(); // ✅ Solo acá se ejecuta la secuencia
                    } else {
                        tvSalida.append("Error apagando interfaces.\n");
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 1. Infla (carga) el archivo XML del menú en la barra de tareas.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 2. Maneja el clic en los ítems del menú.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(this, "Abriendo Configuración...", Toast.LENGTH_SHORT).show();

            SettingsDialogFragment dialog = new SettingsDialogFragment();

            // Usa getSupportFragmentManager() para mostrar el DialogFragment
            dialog.show(getSupportFragmentManager(), "SettingsDialogTag");
            // Aquí iría la lógica para iniciar la Activity de Settings
            return true;
        }

        if (id == R.id.action_acerca_de) {
            Toast.makeText(this, "Información de la aplicación Multiprobador.", Toast.LENGTH_LONG).show();
            // Aquí iría la lógica para mostrar un Dialog o abrir la Activity "Acerca de"
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
