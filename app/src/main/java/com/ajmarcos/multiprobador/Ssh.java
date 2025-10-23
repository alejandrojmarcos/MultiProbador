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

            // Hilo para leer salida
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    StringBuilder output = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null && !detener) {
                        output.append(line).append("\n");
                        //System.out.println(line);
                    }

                    if (listener != null) {
                        String[] salida = output.toString().split("\n");
                        listener.onSshResult(true, salida, 0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (listener != null) {
                        listener.onSshResult(false, new String[]{e.getMessage()}, 1);
                    }
                } finally {
                    isReading[0] = false;
                }
            });
            readerThread.start();

            // Enviar comandos
            // Enviar comandos
            if (commands != null) {
                for (String command : commands) {
                    outputStream.writeBytes(command + "\n");
                    outputStream.flush();

                    // Si el comando es "ip link set ... up", esperamos un poco
                    if (command.startsWith("ip link set") && command.endsWith("up")) {
                        Log.d("SSH", "Subinterfaz levantada: " + command + " - esperando 2 segundos...");
                        Thread.sleep(2000); // ajustable
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
            Log.d("Deploy","SSH cerrado");

        } catch (Exception e) {
            e.printStackTrace();
            if (listener != null) {
                listener.onSshResult(false, new String[]{e.getMessage()}, 2);
            }
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
    public void detener() {
        this.detener = true;
    }

}
