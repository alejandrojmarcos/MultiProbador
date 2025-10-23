package com.ajmarcos.multiprobador;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btn;
    boolean[] puertosSeleccionados;
    CheckBox[] arrayCheckBoxSeleccionPuerto;
    TextView tvSalida;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_8);

        tvSalida = findViewById(R.id.tvSalida);

        puertosSeleccionados = new boolean[8];

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


        for (CheckBox cb : arrayCheckBoxSeleccionPuerto) {
            cb.setChecked(true);
        }

        btn = findViewById(R.id.buttonComenzar);
        btn.setOnClickListener(v -> {
            PortMap portMap = new PortMap();


            portMap.apagarTodasLasIPs((success, salida) -> {
                if (success) {
                    Log.d("PORTMAP", "Todas las interfaces apagadas correctamente");

                    for (int i = 0; i < arrayCheckBoxSeleccionPuerto.length; i++) {
                        puertosSeleccionados[i] = arrayCheckBoxSeleccionPuerto[i].isChecked();
                    }


                    Prueba prueba = new Prueba(puertosSeleccionados, this, tvSalida, arrayBotonesPuerto, portMap);
                    prueba.probar();

                } else {
                    Log.e("PORTMAP", "Error al apagar interfaces");
                    runOnUiThread(() -> tvSalida.append("❌ Error apagando interfaces.\n"));
                }
            });
        });
    }
}
