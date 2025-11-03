package com.ajmarcos.multiprobador;

import android.os.Bundle;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_8);

        tvSalida = findViewById(R.id.tvSalida);

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
        Prueba prueba = new Prueba(puertosSeleccionados, this, tvSalida, btnComenzar, btnEnviar, portMap);

        // Listener del botón Comenzar
        btnComenzar.setOnClickListener(v -> {
            // Abrir diálogo de selección de lote y catálogo
            mostrarDialogoLoteCatalogo(prueba, portMap);
        });
    }

        private void mostrarDialogoLoteCatalogo(Prueba prueba, PortMap portMap) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Seleccionar lote de equipos");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        android.widget.Spinner spinnerCatalogo = new android.widget.Spinner(this);
        String[] lotes = {"10206110004", "10206110006", "10206110010", "10206110029"};
        android.widget.ArrayAdapter<String> adapterCatalogo = new android.widget.ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, lotes);
        adapterCatalogo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCatalogo.setAdapter(adapterCatalogo);

        layout.addView(spinnerCatalogo);
        builder.setView(layout);

        builder.setPositiveButton("Aceptar", (dialog, which) -> {
            String catalogoSeleccionado = spinnerCatalogo.getSelectedItem().toString();

            // Actualizamos los puertos seleccionados
            for (int i = 0; i < arrayCheckBoxSeleccionPuerto.length; i++) {
                puertosSeleccionados[i] = arrayCheckBoxSeleccionPuerto[i].isChecked();
            }
            // Guardamos lote y catálogo en la instancia de prueba
            prueba.setCatalogo(catalogoSeleccionado);
            Log.d("PRUEBA", "✅ Lote seleccionado: " + catalogoSeleccionado);



            // Apagamos interfaces y arrancamos la prueba
            portMap.apagarTodasLasIPs((success, salida) -> {
                runOnUiThread(() -> {
                    if (success) {
                        tvSalida.append("✅ Todas las interfaces apagadas.\n");
                        prueba.iniciar(); // empieza la secuencia
                    } else {
                        tvSalida.append("❌ Error apagando interfaces.\n");
                    }
                });
            });
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

}
