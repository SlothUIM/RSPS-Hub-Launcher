import java.util.Arrays;

public class main {
    public static void main(String[] args) {
        boolean apiMode = Arrays.asList(args).contains("--api-mode");

        if (apiMode) {
            // Find --port <n>, default 7890
            int port = 7890;
            String apiKey = "";
            for (int i = 0; i < args.length - 1; i++) {
                if ("--port".equals(args[i])) {
                    try { port = Integer.parseInt(args[i + 1]); } catch (NumberFormatException ignored) {}
                }
                if ("--api-key".equals(args[i])) {
                    apiKey = args[i + 1];
                }
            }
            LauncherEngine.loadSettings();
            LauncherEngine.loadSession();
            ApiServer.start(port, apiKey);
            // Keep the process alive
            try { Thread.currentThread().join(); } catch (InterruptedException ignored) {}
        } else {
            RSPSHub.main(args);
        }
    }
}