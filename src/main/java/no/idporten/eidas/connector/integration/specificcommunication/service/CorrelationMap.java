//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package no.idporten.eidas.connector.integration.specificcommunication.service;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public interface CorrelationMap<T> {
    @Nullable
    T get(@Nonnull String var1);

    @Nullable
    T put(@Nonnull String var1, @Nonnull T var2);

    @Nullable
    T remove(@Nonnull String var1);
}
