package com.ajmarcos.multiprobador;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
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


}
