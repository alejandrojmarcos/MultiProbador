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
    private boolean[] puertosSeleccionados;
    private CheckBox[] arrayCheckBoxSeleccionPuerto;
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

        Button[] arrayBotonesPuerto = new Button[]{
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
        btnEnviar   = findViewById(R.id.btnEnviar);

        // marcar todos los checkboxes por defecto
        for (CheckBox cb : arrayCheckBoxSeleccionPuerto) cb.setChecked(true);

        puertosSeleccionados = new boolean[8];

        PortMap portMap = new PortMap();
        Prueba prueba = new Prueba(puertosSeleccionados, this, tvSalida, btnComenzar, btnEnviar, portMap);

        // Listener del botón Comenzar
        btnComenzar.setOnClickListener(v -> {
            for (int i = 0; i < arrayCheckBoxSeleccionPuerto.length; i++) {
                puertosSeleccionados[i] = arrayCheckBoxSeleccionPuerto[i].isChecked();
            }
            portMap.apagarTodasLasIPs((success, salida) -> {
                if (success) {
                    runOnUiThread(() -> tvSalida.append("✅ Todas las interfaces apagadas.\n"));
                    prueba.iniciar(); // método que empieza la prueba
                } else {
                    runOnUiThread(() -> tvSalida.append("❌ Error apagando interfaces.\n"));
                }
            });
        });
    }
}
