package jpms.server;

import jpms.server.core.config.Config;

record AppConfig(int port) {

    private static final String PORT = "jpms.server.app.port";

    static AppConfig from(Config config) {
        return new AppConfig(config.getInt(PORT, 8080));
    }
}
