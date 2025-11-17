package com.ajmarcos.multiprobador; // Usa tu paquete real

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class SettingsDialogFragment extends DialogFragment {

    // En SettingsDialogFragment.java

    @Override
    public void onStart() {
        super.onStart();

        if (getDialog() != null) {
            // Obtener la ventana del diálogo
            Window window = getDialog().getWindow();

            if (window != null) {
                // Obtener los parámetros de layout actuales
                WindowManager.LayoutParams layoutParams = window.getAttributes();

                // Establecer el ancho como MATCH_PARENT (ancho completo de la pantalla)
                layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

                // Opcional: Establecer la altura como WRAP_CONTENT (se ajusta al ScrollView)
                layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

                // Aplicar los nuevos parámetros
                window.setAttributes(layoutParams);
            }
        }
    }

    public SettingsDialogFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla el layout del diálogo
        View view = inflater.inflate(R.layout.dialogo_settings, container, false);

        // --- 1. Enlazar componentes ---
        Button btnGuardar = view.findViewById(R.id.btnGuardar);
        Button btnCancelar = view.findViewById(R.id.btnCancelar);

        // ... (enlazar el resto de los EditText, CheckBox, Spinner) ...

        // --- 2. Lógica del botón GUARDAR ---
        btnGuardar.setOnClickListener(v -> {
            // Lógica para leer valores de los campos (e.g., etClaveWiFi.getText().toString())
            // Lógica para guardar en SharedPreferences o base de datos

            Toast.makeText(getContext(), "Configuración guardada.", Toast.LENGTH_SHORT).show();
            dismiss(); // Cierra el diálogo
        });

        // --- 3. Lógica del botón CANCELAR ---
        btnCancelar.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Configuración cancelada.", Toast.LENGTH_SHORT).show();
            dismiss(); // Cierra el diálogo
        });

        // ... (Implementar btnDefault, btnFirmware aquí) ...

        return view;
    }
}