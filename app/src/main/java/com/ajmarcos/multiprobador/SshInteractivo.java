package com.ajmarcos.multiprobador;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class SshInteractivo extends Thread {

    private final String TAG = "Deploy";
    private final String CLASS = getClass().getSimpleName();

    private final String host;
    private final String user;
    private final String pass;

    private volatile boolean detener = false;
    private SshListener listener;
    private String[] comandos = new String[]{"show device_model"};

    public interface SshListener {
        void onSshResult(boolean success, String[] message, int code);
    }

    public SshInteractivo(String host, String user, String pass) {
        this.host = host;
        this.user = user;
        this.pass = pass;
    }

    public void setSshListener(SshListener listener) {
        this.listener = listener;
    }

    public void setCommands(String[] comandos) {
        this.comandos = comandos;
    }

    public void detener() {
        this.detener = true;
        Log.d(TAG, CLASS + " → Señal de detención recibida");
    }

    @Override
    public void run() {
        Session session = null;
        ChannelShell channel = null;
        InputStream in = null;
        OutputStream out = null;
        ByteArrayOutputStream acc = new ByteArrayOutputStream();

        Log.d(TAG, CLASS + " → Iniciando conexión SSH interactiva con " + host);

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, 22);
            session.setPassword(pass);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(3000);

            Log.d(TAG, CLASS + " → Sesión SSH conectada con " + host);

            channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);

            in = channel.getInputStream();
            out = channel.getOutputStream();

            channel.connect(3000);
            Log.d(TAG, CLASS + " → Canal shell abierto");

            // ⚡ OPTIMIZACIÓN: Reducción de espera inicial (300ms + 600ms -> 100ms + 400ms)
            sleepMillis(100);
            drainInputToAccumulator(in, acc, 400);

            // ---- Entra a sh ----
            writeCmd(out, "sh");
            waitForPrompt(acc, in, new String[]{"sh>", "#", ">"}, 3000);

            // ---- Limpia known_hosts dentro del router (necesario para el ssh anidado) ----
            // ⚡ OPTIMIZACIÓN: Reducción de espera (800ms -> 500ms, 400ms -> 100ms)
            writeCmd(out, "rm -f /root/.ssh/known_hosts");
            drainInputToAccumulator(in, acc, 500);
            sleepMillis(100);

            // ---- Espera el prompt del router después de la limpieza ----
            waitForPrompt(acc, in, new String[]{"sh>", "#", ">"}, 2000);
            acc.reset(); // Limpiamos para la salida de comandos

            // ---- Ejecutar comandos ----
            for (String cmd : comandos) {
                writeCmd(out, cmd);

                // Si el comando es un 'ssh ...' anidado, manejamos el login:
                if (cmd.trim().toLowerCase().startsWith("ssh")) {
                    Log.d(TAG, CLASS + " → Detectado SSH anidado, iniciando secuencia de login robusta...");
                    if (!handleLoginSequence(acc, in, out)) {
                        Log.e(TAG, CLASS + " → Error de login interno. Abortando comando.");
                        // Si falla, no podemos continuar.
                        if (listener != null)
                            listener.onSshResult(false, new String[]{"Error de login SSH anidado"}, -5);
                        return;
                    }
                    Log.d(TAG, CLASS + " → Login interno exitoso. Esperando prompt del 1.1...");
                    // Esperamos el prompt del 1.1 para continuar con el siguiente comando (show device_model)
                    // ⚡ OPTIMIZACIÓN: Reducir espera del prompt de la ONT, ya que se asume éxito en handleLoginSequence
                    waitForPrompt(acc, in, new String[]{">", "#", "$", "WAP>"}, 1500);
                }

                // Drenamos el resto del output del comando (para comandos que no son SSH anidado)
                drainInputToAccumulator(in, acc, 2000);
            }

            drainInputToAccumulator(in, acc, 800);

            String all = acc.toString(StandardCharsets.UTF_8.name());
            Log.d(TAG, CLASS + " → Salida completa acumulada:\n" + all);

            String[] lines = all.split("\\r?\\n");

            if (listener != null) listener.onSshResult(true, lines, 0);

        } catch (Exception e) {
            Log.e(TAG, CLASS + " → Error SSH: " + e.getMessage());
            if (listener != null) listener.onSshResult(false, new String[]{e.getMessage()}, -1);
        } finally {
            closeQuietly(out);
            closeQuietly(in);
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
            Log.d(TAG, CLASS + " → SSH interactivo finalizado para " + host);
        }
    }

    // ---------------------------------------------------------
    //          MÉTODO: LOGIN ROBUSTO MULTI-INTENTO (CORREGIDO)
    // ---------------------------------------------------------
    private boolean handleLoginSequence(ByteArrayOutputStream acc, InputStream in, OutputStream out) throws Exception {

        final String ONT_PASS = "Te2010An_2014Ma"; // Contraseña de la ONT
        final String PASSWORD_PROMPT = "password:";
        final String LOGIN_PROMPT = "login:";

        // Intentamos hasta 2 veces (por seguridad)
        for (int i = 0; i < 2; i++) {

            // 1. Drenar y verificar si hay prompt de login
            // ⚡ OPTIMIZACIÓN: Reducción de espera.
            drainInputToAccumulator(in, acc, 800);
            String s = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();

            boolean sawLogin = s.contains(LOGIN_PROMPT);
            boolean sawPass = s.contains(PASSWORD_PROMPT);

            if (sawLogin || sawPass) {
                // Si vemos login (Caso 8145), enviamos usuario y esperamos password
                if (sawLogin) {
                    writeCmd(out, user);
                    waitForText(acc, in, PASSWORD_PROMPT, 2000);
                    s = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();
                }

                // Si vemos password (Caso 8225 o después de enviar usuario), enviamos password
                writeCmd(out, ONT_PASS);

                // ⚡ OPTIMIZACIÓN: Espera mínima de procesamiento + chequeo de éxito.
                // Usamos un tiempo fijo seguro (2500ms) para que la ONT procese la contraseña.
                sleepMillis(2500);

                // 4. Chequear fallos si no hay error explícito
                drainInputToAccumulator(in, acc, 500);
                String result = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();

                if (result.contains("wrong") || result.contains("failed") || result.contains("permission denied") || result.contains("disconnected")) {
                    Log.w(TAG, CLASS + " → Login incorrecto detectado en intento " + (i + 1));
                    acc.reset();
                    continue;
                }

                // Asume éxito si no hay mensaje de error y el shell está abierto
                return true;

            } else if (i == 0) {
                // Si no vemos ni 'login' ni 'password', chequeamos si ya estamos logueados
                drainInputToAccumulator(in, acc, 500);
                if (acc.toString(StandardCharsets.UTF_8.name()).toLowerCase().contains(">") || acc.toString(StandardCharsets.UTF_8.name()).toLowerCase().contains("#")) {
                    return true; // Ya logueado
                }
            }
        }

        // Si fallan los dos intentos
        return false;
    }

    // ---------------------------------------------------------
    //                              UTILIDADES
    // ---------------------------------------------------------

    private void writeCmd(OutputStream out, String cmd) throws Exception {
        out.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        Log.d(TAG, CLASS + " → Enviando: " + cmd);
        // ⚡ OPTIMIZACIÓN: Reducción del sleep después de escribir el comando
        sleepMillis(50);
    }

    private void drainInputToAccumulator(InputStream in, ByteArrayOutputStream acc, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        byte[] buf = new byte[4096];
        while (System.currentTimeMillis() - start < timeout && !detener) {
            int available = in.available();
            if (available > 0) {
                int read = in.read(buf, 0, Math.min(available, buf.length));
                if (read > 0) acc.write(buf, 0, read);
                start = System.currentTimeMillis();
            } else {
                // ⚡ OPTIMIZACIÓN: Reducción del sleep dentro del loop de drenado
                sleepMillis(50);
            }
        }
    }

    private boolean waitForPrompt(ByteArrayOutputStream acc, InputStream in, String[] prompts, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeout && !detener) {
            drainInputToAccumulator(in, acc, 200);
            String s = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();
            for (String p : prompts) {
                if (s.endsWith(p.toLowerCase()) || s.contains("\n" + p.toLowerCase())) {
                    return true;
                }
            }
            // ⚡ OPTIMIZACIÓN: Reducción del sleep
            sleepMillis(50);
        }
        return false;
    }

    private boolean waitForText(ByteArrayOutputStream acc, InputStream in, String texto, long timeout) throws Exception {
        long start = System.currentTimeMillis();
        String lower = texto.toLowerCase();
        while (System.currentTimeMillis() - start < timeout && !detener) {
            drainInputToAccumulator(in, acc, 200);
            if (acc.toString(StandardCharsets.UTF_8.name()).toLowerCase().contains(lower))
                return true;
            // ⚡ OPTIMIZACIÓN: Reducción del sleep
            sleepMillis(50);
        }
        return false;
    }

    private void sleepMillis(long ms) {
        try { TimeUnit.MILLISECONDS.sleep(ms); } catch (Exception ignored) {}
    }

    private void closeQuietly(AutoCloseable c) {
        try { if (c != null) c.close(); } catch (Exception ignored) {}
    }
}