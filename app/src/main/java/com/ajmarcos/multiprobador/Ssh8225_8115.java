package com.ajmarcos.multiprobador;

import android.util.Log;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Ssh8225_8115 extends Thread {
    String host, username, password;
    private SshListener listener;
    private volatile boolean detener; // Bandera para detener el hilo de manera controlada

    String ssid2, canal2, estado2, ssid5, canal5, estado5, potencia;

    public interface SshListener {
        void onSsh8225_8115Result(boolean success, String[] message, int code);
    }

    public void setSshListener(SshListener listener) {
        this.listener = listener;
    }

    public Ssh8225_8115() {
        ssid2 = "";
        ssid5 = "";
        canal2 = "";
        canal5 = "";
        estado2 = "";
        estado5 = "";
        potencia = "";
        this.host = "192.168.1.1";
        this.username = "Support";
        this.password = "Te2010An_2014Ma";
        this.detener = false;
    }

    @Override
    public void run() {
        JSch jsch = new JSch();
        Session session = null;

        try {
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
            InputStream inputStream = channelShell.getInputStream();
            DataOutputStream outputStream = new DataOutputStream(channelShell.getOutputStream());
            channelShell.connect();

            final boolean[] isReading = {true};

            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null && !detener) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        Log.d("SSH_LINE", line); // 👈 LOG COMPLETO de cada línea

                        if (line.startsWith("wifi0_status")) {
                            estado2 = line.split(" ")[1];
                            Log.d("SSH_PARSE", "estado2=" + estado2);
                        }
                        if (line.startsWith("wifi ssid")) {
                            ssid2 = line.split(" ")[2];
                            Log.d("SSH_PARSE", "ssid2=" + ssid2);
                        }
                        if (line.startsWith("wifi channel")) {
                            canal2 = line.split(" ")[2];
                            Log.d("SSH_PARSE", "canal2=" + canal2);
                        }

                        if (line.startsWith("wifi_plus0_status")) {
                            estado5 = line.split(" ")[1];
                            Log.d("SSH_PARSE", "estado5=" + estado5);
                        }
                        if (line.startsWith("wifi_plus ssid")) {
                            ssid5 = line.split(" ")[2];
                            Log.d("SSH_PARSE", "ssid5=" + ssid5);
                        }
                        if (line.startsWith("wifi_plus channel")) {
                            canal5 = line.split(" ")[2];
                            Log.d("SSH_PARSE", "canal5=" + canal5);
                        }

                        if (line.startsWith("received_optical power")) {
                            String[] parts = line.split(" ");
                            if (parts.length > 2) {
                                potencia = parts[2];
                                Log.d("SSH_PARSE", "potencia=" + potencia);
                            }
                        }
                    }

                    if (listener != null) {
                        String[] resultados = new String[]{
                                ssid2, canal2, estado2,
                                ssid5, canal5, estado5,
                                potencia
                        };
                        Log.d("SSH_RESULT", "Final SSH -> " +
                                "ssid2=" + ssid2 + ", canal2=" + canal2 + ", estado2=" + estado2 +
                                ", ssid5=" + ssid5 + ", canal5=" + canal5 + ", estado5=" + estado5 +
                                ", potencia=" + potencia);
                        listener.onSsh8225_8115Result(true, resultados, 3);
                    }

                } catch (Exception e) {
                    Log.e("SSH_ERROR", "Error leyendo SSH: " + e.getMessage(), e);
                } finally {
                    isReading[0] = false;
                }
            });
            readerThread.start();

            String[] commands = {"show wifi", "show wifi_plus", "show primary_diagnosis"};
            for (String cmd : commands) {
                outputStream.writeBytes(cmd + "\n");
                outputStream.flush();
                Thread.sleep(500);
            }

            Thread.sleep(1000);
            outputStream.close();
            channelShell.disconnect();
            session.disconnect();
            Log.d("SSH_STATUS", "Sesión SSH finalizada correctamente.");

        } catch (Exception e) {
            Log.e("SSH_FATAL", "Error SSH general: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
