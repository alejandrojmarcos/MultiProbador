package com.ajmarcos.multiprobador;

import android.util.Log;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Ssh extends Thread {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private String host, username, password;
    private String[] commands;
    private boolean detener = false;
    private SshListener listener;

    public interface SshListener {
        void onSshResult(boolean success, String[] message, int code);
    }

    public void setSshListener(SshListener listener) {
        this.listener = listener;
    }

    public void setCommands(String[] commands) {
        this.commands = commands;
    }

    public Ssh(String ip, String user, String password) {
        this.host = ip;
        this.username = user;
        this.password = password;
    }

    @Override
    public void run() {
        Log.d(TAG, CLASS + " → Iniciando conexión SSH con " + host);
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            Log.d(TAG, CLASS + " → Sesión SSH conectada con " + host);

            ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
            InputStream inputStream = channelShell.getInputStream();
            DataOutputStream outputStream = new DataOutputStream(channelShell.getOutputStream());
            channelShell.connect();
            Log.d(TAG, CLASS + " → Canal shell abierto en " + host);

            final boolean[] isReading = {true};

            // Hilo para leer salida
            Thread readerThread = new Thread(() -> {
                Log.d(TAG, CLASS + " → Iniciando lectura de salida SSH...");
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null && !detener) {
                        Log.d(TAG, CLASS + " → SSH[" + host + "]: " + line);
                        output.append(line).append("\n");
                    }

                    if (listener != null) {
                        String[] salida = output.toString().split("\n");
                        listener.onSshResult(true, salida, 0);
                        Log.d(TAG, CLASS + " → Lectura finalizada correctamente para " + host);
                    }
                } catch (Exception e) {
                    Log.e(TAG, CLASS + " → Error leyendo salida SSH: " + e.getMessage());
                    if (listener != null) {
                        listener.onSshResult(false, new String[]{e.getMessage()}, 1);
                    }
                } finally {
                    isReading[0] = false;
                }
            });
            readerThread.start();

            // Enviar comandos
            if (commands != null) {
                for (String command : commands) {
                    Log.d(TAG, CLASS + " → Enviando comando: " + command);
                    outputStream.writeBytes(command + "\n");
                    outputStream.flush();

                    if (command.startsWith("ip link set") && command.endsWith("up")) {
                        Log.d(TAG, CLASS + " → Subinterfaz levantada (" + command + "), esperando 1s...");
                        Thread.sleep(1000);
                    } else {
                        Thread.sleep(400);
                    }
                }
            }

            // Esperar a que el lector termine
            int maxWait = 5000; // 5 segundos
            int interval = 100;
            int waited = 0;
            while (isReading[0] && waited < maxWait && !detener) {
                Thread.sleep(interval);
                waited += interval;
            }

            outputStream.close();
            channelShell.disconnect();
            session.disconnect();
            Log.d(TAG, CLASS + " → SSH cerrado correctamente para " + host);

        } catch (Exception e) {
            Log.e(TAG, CLASS + " → Error general SSH (" + host + "): " + e.getMessage());
            if (listener != null) {
                listener.onSshResult(false, new String[]{e.getMessage()}, 2);
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
                Log.d(TAG, CLASS + " → Sesión SSH desconectada en finally");
            }
        }
    }

    public void detener() {
        this.detener = true;
        Log.d(TAG, CLASS + " → Señal de detención recibida para SSH en " + host);
    }
}
