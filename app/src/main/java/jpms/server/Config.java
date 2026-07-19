package jpms.server;

import java.util.Map;

public record Config(int port) {

    public static Config fromEnv(Map<String, String> env) {
        return new Config(
                Integer.parseInt(
                        env.getOrDefault(
                                "JPMS_SERVER_APP_PORT", env.getOrDefault("PORT", "8080"))));
    }
}
