package io.quarkiverse.quickjs4j;

import java.io.IOException;
import java.nio.file.Path;

public interface ScriptInterfaceFactory<T, C> {

    T create(String scriptLibrary, C context);

}
