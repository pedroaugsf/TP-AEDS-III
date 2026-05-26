package app.dao;

/**
 * Contrato dos elementos armazenados na Hash Extensível.
 *
 * Cada elemento deve:
 *  - ter tamanho fixo em bytes (size()), pois os buckets do hash usam slots de tamanho fixo;
 *  - implementar hashCode() (a chave de busca é hashCode());
 *  - implementar equals() (usado em delete para identificar a entrada exata);
 *  - serializar/desserializar em byte[] de tamanho exatamente size().
 */
public interface RegistroHashExtensivel<T> {

    /** Tamanho fixo (em bytes) da entrada serializada. */
    short size();

    /** Hash da chave do registro (usado pela Hash Extensível para indexar). */
    int hashCode();

    /** Igualdade lógica entre dois registros (chave + valor). */
    boolean equals(Object o);

    byte[] toByteArray() throws java.io.IOException;
    void fromByteArray(byte[] ba) throws java.io.IOException;
}
