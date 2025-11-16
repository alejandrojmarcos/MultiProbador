package com.ajmarcos.multiprobador;

import android.util.Log;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

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

            sleepMillis(300);
            drainInputToAccumulator(in, acc, 600);

            // ---- Entra a sh ----
            writeCmd(out, "sh");
            waitForPrompt(acc, in, new String[]{"sh>", "#", ">"}, 3000);

            // ---- Limpia known_hosts dentro del router (necesario para el ssh anidado) ----
            writeCmd(out, "rm -f /root/.ssh/known_hosts");
            drainInputToAccumulator(in, acc, 500);
            sleepMillis(300);

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
                    waitForPrompt(acc, in, new String[]{">", "#", "$"}, 5000);
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
    //          MÉTODO: LOGIN ROBUSTO MULTI-INTENTO (REUTILIZADO)
    // ---------------------------------------------------------
    // Este método es crucial para manejar el login de la sesión SSH anidada.
    private boolean handleLoginSequence(ByteArrayOutputStream acc, InputStream in, OutputStream out) throws Exception {

        // El bucle de login original que busca 'login:' o 'password:' y responde.
        for (int i = 0; i < 2; i++) {

            // 1. Drenar y verificar si hay prompt de login
            drainInputToAccumulator(in, acc, 1000);
            String s = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();

            boolean sawLogin = s.contains("login:");
            boolean sawPass = s.contains("password:");

            if (sawLogin || sawPass) {
                // Si vemos login, enviamos usuario y esperamos password
                if (sawLogin) {
                    Log.d(TAG, CLASS + " → Login Prompt detectado. Enviando usuario...");
                    writeCmd(out, user);
                    waitForText(acc, in, "password", 5000);
                }

                // Si vemos password (o después de enviar el usuario), enviamos password
                Log.d(TAG, CLASS + " → Password Prompt detectado. Enviando contraseña...");
                writeCmd(out, pass);

                // Drenar y revisar si devolvió error (e.g., 'wrong password')
                drainInputToAccumulator(in, acc, 1500);
                String result = acc.toString(StandardCharsets.UTF_8.name()).toLowerCase();

                if (result.contains("wrong") || result.contains("failed")) {
                    Log.w(TAG, CLASS + " → Login incorrecto detectado en intento " + (i + 1));
                    acc.reset(); // Limpiamos para el siguiente intento o fallo final.
                    continue; // Intentamos de nuevo si hay un segundo prompt, si no, caemos en el false.
                }

                // Si no hay error, asume éxito (debería ver el prompt > o #)
                return true;

            } else if (i == 0) {
                // Si no vemos ni 'login' ni 'password' en el primer chequeo,
                // puede ser que ya se haya logueado por caché o el prompt está listo.
                // Drenamos un poco más y continuamos el bucle principal.
                drainInputToAccumulator(in, acc, 500);
                if (acc.toString(StandardCharsets.UTF_8.name()).toLowerCase().contains(">") || acc.toString(StandardCharsets.UTF_8.name()).toLowerCase().contains("#")) {
                    return true;
                }
            }
        }

        // Si fallan los dos intentos
        return false;
    }

    // ---------------------------------------------------------
    // utilidades (sin cambios, excepto que se elimina el método handleLoginSequence original,
    // que se reemplaza por la nueva lógica de login dentro de run())
    // ---------------------------------------------------------

    private void writeCmd(OutputStream out, String cmd) throws Exception {
        out.write((cmd + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
        Log.d(TAG, CLASS + " → Enviando: " + cmd);
        sleepMillis(150);
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
                sleepMillis(120);
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
            sleepMillis(120);
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
            sleepMillis(120);
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