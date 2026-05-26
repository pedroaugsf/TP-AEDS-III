package app.dao;

/**
 * Contrato para entradas armazenadas em uma {@link ArvoreBMais}.
 *
 * Cada entrada deve:
 *  - ter tamanho fixo em bytes (size()), pois as páginas da B+ usam slots fixos;
 *  - implementar {@link #compareTo(Object)} definindo a ordem total (chave + desempate);
 *  - serializar/desserializar em byte[] de tamanho exatamente size().
 *
 * Observação: a B+ usa <em>somente</em> compareTo para roteamento; duas entradas com
 * compareTo == 0 são consideradas a mesma chave. Inclua um campo de desempate
 * (por ex.: id) no compareTo se a chave principal puder repetir.
 */
public interface RegistroArvoreBMais<T> extends Comparable<T> {

    /** Tamanho fixo (em bytes) da entrada serializada. */
    short size();

    byte[] toByteArray() throws java.io.IOException;
    void fromByteArray(byte[] ba) throws java.io.IOException;
}
