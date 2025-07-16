package io.quarkiverse.quickjs4j;

public interface ScriptInterfaceFactory<T, C> {

    T create(String scriptLibrary, C context);

}
