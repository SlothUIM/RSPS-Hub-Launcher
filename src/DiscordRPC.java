import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Discord Rich Presence via IPC named pipe.
 *
 * BEFORE THIS WORKS: replace CLIENT_ID with your real Discord Application ID.
 * Steps:
 *   1. Go to https://discord.com/developers/applications
 *   2. Create New Application → name it "RSPS Hub"
 *   3. Copy the Application ID and paste it below
 *   4. Under Rich Presence → Art Assets, upload a "logo" image
 */
public class DiscordRPC {

    // ← REPLACE THIS with your Discord Application ID
    public static final String CLIENT_ID = "REPLACE_WITH_YOUR_DISCORD_APP_ID";

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME     = 1;
    private static final int OP_CLOSE     = 2;

    private static volatile RandomAccessFile pipe = null;
    private static volatile boolean connected = false;

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    /** Connect to Discord IPC in a background thread (safe to call even if Discord isn't open). */
    public static void connectAsync() {
        new Thread(() -> {
            try { connect(); } catch (Exception e) {
                System.err.println("Discord RPC: " + e.getMessage());
            }
        }, "discord-rpc").start();
    }

    /** Push an activity to Discord. Call after connectAsync(). */
    public static synchronized void setActivity(String serverName, long startEpochSeconds) {
        if (!connected) return;
        try {
            int pid = (int) ProcessHandle.current().pid();
            String nonce = String.valueOf(System.currentTimeMillis());
            String safe  = serverName.replace("\\", "\\\\").replace("\"", "\\\"");
            String payload = String.format(
                "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":%d,\"activity\":{" +
                "\"details\":\"Playing %s\",\"state\":\"via RSPS Hub\"," +
                "\"timestamps\":{\"start\":%d}," +
                "\"assets\":{\"large_image\":\"logo\",\"large_text\":\"RSPS Hub\"}" +
                "}},\"nonce\":\"%s\"}",
                pid, safe, startEpochSeconds, nonce
            );
            send(OP_FRAME, payload);
            drain();
        } catch (Exception e) {
            System.err.println("Discord RPC setActivity: " + e.getMessage());
            connected = false;
        }
    }

    /** Clear the activity when the game closes. */
    public static synchronized void clearActivity() {
        if (!connected) return;
        try {
            int pid = (int) ProcessHandle.current().pid();
            String nonce = String.valueOf(System.currentTimeMillis());
            String payload = String.format(
                "{\"cmd\":\"SET_ACTIVITY\",\"args\":{\"pid\":%d,\"activity\":null},\"nonce\":\"%s\"}",
                pid, nonce
            );
            send(OP_FRAME, payload);
            drain();
        } catch (Exception e) {
            System.err.println("Discord RPC clearActivity: " + e.getMessage());
            connected = false;
        }
    }

    public static synchronized void disconnect() {
        if (!connected) return;
        try { send(OP_CLOSE, "{}"); } catch (Exception ignored) {}
        try { if (pipe != null) pipe.close(); } catch (Exception ignored) {}
        pipe = null;
        connected = false;
    }

    // ── INTERNAL ──────────────────────────────────────────────────────────────

    private static synchronized void connect() {
        if (connected) return;
        // Discord opens pipes discord-ipc-0 through discord-ipc-9
        for (int i = 0; i < 10; i++) {
            try {
                pipe = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                break;
            } catch (FileNotFoundException ignored) {}
        }
        if (pipe == null) { System.err.println("Discord RPC: Discord not running."); return; }

        try {
            String handshake = "{\"v\":1,\"client_id\":\"" + CLIENT_ID + "\"}";
            send(OP_HANDSHAKE, handshake);
            drain(); // read READY response
            connected = true;
            System.out.println("Discord RPC connected.");
        } catch (Exception e) {
            System.err.println("Discord RPC handshake failed: " + e.getMessage());
            try { pipe.close(); } catch (Exception ignored) {}
            pipe = null;
        }
    }

    private static void send(int opcode, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(opcode);
        buf.putInt(data.length);
        buf.put(data);
        pipe.write(buf.array());
    }

    /** Read and discard one frame from the pipe. */
    private static void drain() throws IOException {
        byte[] header = new byte[8];
        pipe.readFully(header);
        ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
        buf.getInt(); // opcode (unused)
        int length = buf.getInt();
        if (length > 0 && length <= 65536) {
            byte[] body = new byte[length];
            pipe.readFully(body);
        }
    }
}
