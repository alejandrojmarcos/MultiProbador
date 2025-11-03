package com.ajmarcos.multiprobador;

import android.util.Log;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Ssh8225 extends Thread {
    String host, username, password,command;
    private SshListener listener;
    private volatile boolean detener; // Bandera para detener el hilo de manera controlada
    private String[] wifiData; // Almacenará los resultados

    String ssid2, canal2, estado2, ssid5, canal5, estado5,potencia;



    public interface SshListener {
        void onSsh8225Result(boolean success, String [] message, int code);
    }
    public void setSshListener(SshListener listener) {
        this.listener = listener;
    }
    public Ssh8225() {
        ssid2="";
        ssid5="";
        canal2="";
        canal5="";
        estado2="";
        estado5="";
        this.host = "192.168.1.2";
        this.username = "Support";
        this.password = "Te2010An_2014Ma";
        this.detener = false; // Inicializar la bandera
        this.wifiData = new String[6]; // Para guardar los valores estado2, ssid2, canal2, estado5, ssid5, canal5


    }

    @Override
    public void run() {
        JSch jsch = new JSch();
        Session session = null;

        try {
            // Crear sesión SSH
            session = jsch.getSession(username, host, 22);
            session.setPassword(password);

            // Configurar para no verificar claves del host
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            if (session.isConnected()) {
                //System.out.println("Conectado al servidor SSH.");
            }

            // Crear canal de tipo "shell"
            ChannelShell channelShell = (ChannelShell) session.openChannel("shell");

            // Capturar la salida estándar
            InputStream inputStream = channelShell.getInputStream();

            // Crear un flujo de escritura para enviar comandos
            DataOutputStream outputStream = new DataOutputStream(channelShell.getOutputStream());

            // Conectar el canal
            channelShell.connect();

            // Variable para monitorear el estado del hilo de lectura
            final boolean[] isReading = {true};



            // Hilo para leer la salida de la sesión de shell
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null && !detener) { // Revisar la bandera
                        output.append(line).append("\n");
                        // System.out.println(line); // Imprime la salida en tiempo real


                        if (line.startsWith("wifi0_status")) {
                            estado2 = line.split(" ")[1]; // Ejemplo: "on"
                        }
                        if (line.startsWith("wifi ssid")) {
                            ssid2 = line.split(" ")[2]; // Ejemplo: "Puerto06"
                        }

                        if (line.startsWith("wifi channel")) {
                            canal2 = line.split(" ")[2]; // Ejemplo: "6"
                        }

                        if (line.startsWith("wifi_plus0_status")) {
                            estado5 = line.split(" ")[1]; // Ejemplo: "on"
                        }
                        if (line.startsWith("wifi_plus ssid")) {
                            ssid5 = line.split(" ")[2]; // Ejemplo: "Puerto06"
                        }

                        if (line.startsWith("wifi_plus channel")) {
                            canal5 = line.split(" ")[2]; // Ejemplo: "6"
                        }
                        if (line.startsWith("received_optical power")) {
                            potencia = line.split(" ")[2]; // Ejemplo: "6"

                        }
                    }

                    if (listener != null) {
                        //String modelo = validaModelo(output.toString());
                        String [] resultados = new String[]{ssid2,canal2,estado2,ssid5,canal5,estado5,potencia};
                        //
                       listener.onSsh8225Result(true, resultados, 3);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) {
                        //listener.onSshResult(false, "Error al leer salida: " + e.getMessage(), 2);
                    }
                } finally {
                    isReading[0] = false; // Marcar que el hilo ha terminado
                }
            });
            readerThread.start();

            // Enviar comandos al shell
            String[] commands = {
                    "show wifi",
                    "show wifi_plus",
                    "show primary_diagnosis"
            };

            // Enviar cada comando de forma secuencial
            for (String command : commands) {
                outputStream.writeBytes(command + "\n");
                outputStream.flush();
                Thread.sleep(800);  // Espera entre comandos para permitir que el segundo se ejecute
            }

            // Esperar a que el lector termine o un tiempo máximo
            int maxWait = 1000; // Tiempo máximo en milisegundos
            int interval = 100; // Intervalo para verificar el estado
            int waited = 0;

            while (isReading[0] && waited < maxWait && !detener) {
                Thread.sleep(interval);
                waited += interval;
            }

            // Cerrar recursos y desconectar
            outputStream.close();
            channelShell.disconnect();
            session.disconnect();
            Log.d("Deploy","SSH cerrado");
            if (isReading[0]) {
                //System.out.println("El hilo lector no terminó dentro del tiempo esperado.");
            } else {
                Log.d("Deploy","SSH finalizado");
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                //listener.onSshResult(false, "Error al ejecutar la sesión de shell SSH: " + e.getMessage(), 3);
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }



}

