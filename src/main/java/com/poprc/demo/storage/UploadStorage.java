package com.poprc.demo.storage;

import java.nio.file.Path;

public final class UploadStorage {

    private static final String SYSTEM_PROPERTY = "app.upload.dir";
    private static final String ENVIRONMENT_VARIABLE = "APP_UPLOAD_DIR";

    private UploadStorage() {
    }

    public static Path root() {
        String configured = System.getProperty(SYSTEM_PROPERTY);
        if (configured == null || configured.isBlank()) {
            configured = System.getenv(ENVIRONMENT_VARIABLE);
        }
        Path root = configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "rc_uploads")
                : Path.of(configured);
        return root.toAbsolutePath().normalize();
    }

    public static Path directory(String relativeDirectory) {
        Path relative = Path.of(relativeDirectory).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new IllegalArgumentException("Diretório de upload inválido.");
        }
        Path directory = root().resolve(relative).normalize();
        if (!directory.startsWith(root())) {
            throw new IllegalArgumentException("Diretório de upload fora da raiz permitida.");
        }
        return directory;
    }
}
