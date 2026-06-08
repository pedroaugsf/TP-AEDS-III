package app.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Compressão de dados pelo algoritmo de <b>Huffman</b> (codificação por
 * prefixos de comprimento variável).
 *
 * <p>O fluxo comprimido é autossuficiente: além dos bits codificados, ele
 * carrega a tabela de frequências utilizada para reconstruir exatamente a
 * mesma árvore na descompressão. Layout do fluxo:</p>
 *
 * <pre>
 *   int   tamanhoOriginal      (nº de bytes da entrada)
 *   int   simbolosDistintos    (quantidade de bytes diferentes)
 *   [ byte simbolo, long frequencia ] * simbolosDistintos
 *   byte[] dadosCodificados    (bits empacotados, alinhados a 8)
 * </pre>
 *
 * A implementação trata os casos de borda: entrada vazia e entrada composta
 * por um único símbolo distinto (cujo código passa a ser "0").
 */
public final class Huffman {

    private Huffman() {
    }

    /** Nó da árvore de Huffman. */
    private static final class No {
        final int simbolo;   // 0..255 para folha; -1 para nó interno
        final long freq;
        final No esquerda;
        final No direita;

        No(int simbolo, long freq) {
            this.simbolo = simbolo;
            this.freq = freq;
            this.esquerda = null;
            this.direita = null;
        }

        No(No esquerda, No direita) {
            this.simbolo = -1;
            this.freq = esquerda.freq + direita.freq;
            this.esquerda = esquerda;
            this.direita = direita;
        }

        boolean folha() {
            return esquerda == null && direita == null;
        }
    }

    // ====================================================================
    //  COMPRESSÃO
    // ====================================================================
    public static byte[] comprimir(byte[] entrada) throws IOException {
        if (entrada == null) entrada = new byte[0];

        long[] freq = new long[256];
        for (byte b : entrada) {
            freq[b & 0xFF]++;
        }

        int distintos = 0;
        for (long f : freq) {
            if (f > 0) distintos++;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeInt(entrada.length);
        dos.writeInt(distintos);
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                dos.writeByte(i);
                dos.writeLong(freq[i]);
            }
        }

        if (entrada.length == 0) {
            dos.flush();
            return out.toByteArray();
        }

        No raiz = construirArvore(freq);
        String[] codigos = new String[256];
        gerarCodigos(raiz, "", codigos);

        int buffer = 0;
        int nBits = 0;
        for (byte b : entrada) {
            String codigo = codigos[b & 0xFF];
            for (int k = 0; k < codigo.length(); k++) {
                buffer = (buffer << 1) | (codigo.charAt(k) == '1' ? 1 : 0);
                nBits++;
                if (nBits == 8) {
                    dos.writeByte(buffer);
                    buffer = 0;
                    nBits = 0;
                }
            }
        }
        if (nBits > 0) {
            buffer <<= (8 - nBits); // alinha o último byte à esquerda
            dos.writeByte(buffer);
        }

        dos.flush();
        return out.toByteArray();
    }

    // ====================================================================
    //  DESCOMPRESSÃO
    // ====================================================================
    public static byte[] descomprimir(byte[] entrada) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(entrada));

        int tamanhoOriginal = dis.readInt();
        int distintos = dis.readInt();

        long[] freq = new long[256];
        for (int i = 0; i < distintos; i++) {
            int simbolo = dis.readUnsignedByte();
            long f = dis.readLong();
            freq[simbolo] = f;
        }

        byte[] saida = new byte[tamanhoOriginal];
        if (tamanhoOriginal == 0) {
            return saida;
        }

        No raiz = construirArvore(freq);

        // Caso de borda: um único símbolo distinto -> a raiz é folha.
        if (raiz.folha()) {
            Arrays.fill(saida, (byte) raiz.simbolo);
            return saida;
        }

        int produzidos = 0;
        No atual = raiz;
        int b;
        while (produzidos < tamanhoOriginal && (b = dis.read()) != -1) {
            for (int bit = 7; bit >= 0 && produzidos < tamanhoOriginal; bit--) {
                int valor = (b >> bit) & 1;
                atual = (valor == 0) ? atual.esquerda : atual.direita;
                if (atual.folha()) {
                    saida[produzidos++] = (byte) atual.simbolo;
                    atual = raiz;
                }
            }
        }
        return saida;
    }

    // ====================================================================
    //  AUXILIARES
    // ====================================================================
    private static No construirArvore(long[] freq) {
        PriorityQueue<No> fila = new PriorityQueue<>((a, b) -> Long.compare(a.freq, b.freq));
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                fila.add(new No(i, freq[i]));
            }
        }
        if (fila.isEmpty()) {
            return null;
        }
        if (fila.size() == 1) {
            return fila.poll(); // único símbolo: árvore com apenas a folha
        }
        while (fila.size() > 1) {
            No a = fila.poll();
            No b = fila.poll();
            fila.add(new No(a, b));
        }
        return fila.poll();
    }

    private static void gerarCodigos(No no, String prefixo, String[] codigos) {
        if (no == null) {
            return;
        }
        if (no.folha()) {
            codigos[no.simbolo] = prefixo.isEmpty() ? "0" : prefixo;
            return;
        }
        gerarCodigos(no.esquerda, prefixo + "0", codigos);
        gerarCodigos(no.direita, prefixo + "1", codigos);
    }
}
