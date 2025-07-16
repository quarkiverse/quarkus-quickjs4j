package io.quarkiverse.quickjs4j.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ScriptInterfaceUtils {

    private static final Logger LOGGER = Logger.getLogger(ScriptInterfaceUtils.class.getName());

    public static String loadScriptLibrary(String scriptLocation) {
        LOGGER.fine("Loading script library from: " + scriptLocation);

        Path path = Paths.get(scriptLocation);
        if (!path.isAbsolute()) {
            String workingDir = System.getProperty("user.dir");
            path = Paths.get(workingDir).resolve(path).normalize();
        }
        if (Files.exists(path)) {
            LOGGER.fine("Script library file found at: " + path.toAbsolutePath());
            return loadScriptLibrary(path);
        }

        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(scriptLocation);
        if (resourceUrl != null) {
            LOGGER.fine("Script library resource [1] found at: " + resourceUrl.toExternalForm());
            return loadScriptLibrary(resourceUrl);
        }

        resourceUrl = ScriptInterfaceUtils.class.getClassLoader().getResource(scriptLocation);
        if (resourceUrl != null) {
            LOGGER.fine("Script library resource [2] found at: " + resourceUrl.toExternalForm());
            return loadScriptLibrary(resourceUrl);
        }

        LOGGER.warning("Script library [" + scriptLocation + "] not found!!");
        return null;
    }

    public static String loadScriptLibrary(URL scriptLocation) {
        try {
            URLConnection connection = scriptLocation.openConnection();

            // Read from the input stream
            try (InputStream inputStream = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadScriptLibrary(Path scriptLocation) {
        try {
            return Files.readString(scriptLocation);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
