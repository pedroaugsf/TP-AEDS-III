package app.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Serviço de alto nível para gerar e restaurar backups compactados.
 *
 * <p>Fluxo de geração:</p>
 * <ol>
 *   <li>{@link Backup#empacotar()} reúne todos os arquivos de {@code ./dados}
 *       em um único pacote de bytes;</li>
 *   <li>o pacote é comprimido com {@link Huffman} ou {@link LZW};</li>
 *   <li>o resultado é gravado como um <b>único arquivo</b> em {@code ./backups},
 *       precedido de um pequeno cabeçalho que identifica o algoritmo.</li>
 * </ol>
 *
 * <p>Cabeçalho do arquivo único gerado:</p>
 * <pre>
 *   4 bytes "NTC1"              (assinatura)
 *   1 byte  algoritmo           (1 = Huffman, 2 = LZW)
 *   8 bytes tamanhoOriginal     (bytes do pacote antes de comprimir)
 *   4 bytes quantidadeArquivos
 *   byte[]  payload             (fluxo Huffman/LZW)
 * </pre>
 */
public final class CompressaoService {

    public static final String DIR_BACKUP = "./backups";
    public static final String ARQ_HUFFMAN = DIR_BACKUP + "/nutritrack_huffman.huff";
    public static final String ARQ_LZW = DIR_BACKUP + "/nutritrack_lzw.lzw";

    private static final byte[] ASSINATURA = "NTC1".getBytes(StandardCharsets.US_ASCII);
    public static final byte ALGO_HUFFMAN = 1;
    public static final byte ALGO_LZW = 2;

    private CompressaoService() {
    }

    // ====================================================================
    //  GERAÇÃO
    // ====================================================================
    public static ResultadoCompressao gerarBackupHuffman() throws IOException {
        return gerar("Huffman", ALGO_HUFFMAN, ARQ_HUFFMAN);
    }

    public static ResultadoCompressao gerarBackupLZW() throws IOException {
        return gerar("LZW", ALGO_LZW, ARQ_LZW);
    }

    private static ResultadoCompressao gerar(String nomeAlgo, byte algo, String caminhoSaida) throws IOException {
        long inicio = System.currentTimeMillis();

        byte[] pacote = Backup.empacotar();
        int quantidadeArquivos = Backup.listarArquivosDados().size();

        byte[] payload = (algo == ALGO_HUFFMAN)
                ? Huffman.comprimir(pacote)
                : LZW.comprimir(pacote);

        // verificação de integridade (round-trip) antes de gravar
        byte[] reconstruido = (algo == ALGO_HUFFMAN)
                ? Huffman.descomprimir(payload)
                : LZW.descomprimir(payload);
        boolean integridadeOk = Arrays.equals(pacote, reconstruido);

        Files.createDirectories(Paths.get(DIR_BACKUP));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.write(ASSINATURA);
        dos.writeByte(algo);
        dos.writeLong(pacote.length);
        dos.writeInt(quantidadeArquivos);
        dos.write(payload);
        dos.flush();

        byte[] arquivoFinal = out.toByteArray();
        Path destino = Paths.get(caminhoSaida);
        Files.write(destino, arquivoFinal);

        long fim = System.currentTimeMillis();
        return new ResultadoCompressao(
                nomeAlgo,
                destino.toAbsolutePath().normalize().toString(),
                quantidadeArquivos,
                pacote.length,
                arquivoFinal.length,
                integridadeOk,
                fim - inicio);
    }

    // ====================================================================
    //  RESTAURAÇÃO
    // ====================================================================
    /**
     * Restaura, em {@code destino}, o conteúdo de um arquivo gerado por este
     * serviço (detectando automaticamente Huffman ou LZW pelo cabeçalho).
     *
     * @return quantidade de arquivos restaurados.
     */
    public static int restaurar(String caminhoArquivo, String destino) throws IOException {
        byte[] arquivo = Files.readAllBytes(Paths.get(caminhoArquivo));
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(arquivo));

        byte[] assinatura = new byte[ASSINATURA.length];
        dis.readFully(assinatura);
        if (!Arrays.equals(assinatura, ASSINATURA)) {
            throw new IOException("Arquivo de backup inválido (assinatura incorreta).");
        }

        byte algo = dis.readByte();
        dis.readLong();  // tamanho original (informativo)
        dis.readInt();   // quantidade de arquivos (informativo)

        byte[] payload = new byte[dis.available()];
        dis.readFully(payload);

        byte[] pacote = (algo == ALGO_HUFFMAN)
                ? Huffman.descomprimir(payload)
                : LZW.descomprimir(payload);

        return Backup.desempacotar(pacote, destino);
    }

    /** Indica se um arquivo de backup já existe e devolve seu tamanho (ou -1). */
    public static long tamanhoArquivo(String caminho) {
        Path p = Paths.get(caminho);
        try {
            return Files.exists(p) ? Files.size(p) : -1L;
        } catch (IOException e) {
            return -1L;
        }
    }
}
