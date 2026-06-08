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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Empacotador (estilo TAR) que reúne <b>todos</b> os arquivos de dados do
 * sistema (a árvore inteira sob {@code ./dados}) em um único vetor de bytes e,
 * de volta, restaura essa árvore a partir do vetor.
 *
 * <p>Esse empacotamento é o passo anterior à compressão: primeiro juntamos
 * tudo em um só fluxo, depois aplicamos Huffman ou LZW sobre ele, gerando um
 * <b>único arquivo compactado</b> que funciona como backup completo.</p>
 *
 * <p>Layout do pacote:</p>
 * <pre>
 *   6 bytes  "NTBKP1"            (assinatura)
 *   int      quantidadeArquivos
 *   para cada arquivo:
 *     short  tamanhoNome
 *     byte[] nomeRelativo        (UTF-8, separador '/')
 *     long   tamanhoConteudo
 *     byte[] conteudo
 * </pre>
 */
public final class Backup {

    public static final String DIR_DADOS = "./dados";
    private static final String ASSINATURA = "NTBKP1";

    private Backup() {
    }

    /** Lista (ordenada) todos os arquivos regulares sob {@code ./dados}. */
    public static List<Path> listarArquivosDados() throws IOException {
        Path raiz = Paths.get(DIR_DADOS).toAbsolutePath().normalize();
        List<Path> arquivos = new ArrayList<>();
        if (Files.exists(raiz)) {
            try (Stream<Path> stream = Files.walk(raiz)) {
                stream.filter(Files::isRegularFile).sorted().forEach(arquivos::add);
            }
        }
        return arquivos;
    }

    /** Reúne todos os arquivos de {@code ./dados} em um único vetor de bytes. */
    public static byte[] empacotar() throws IOException {
        Path raiz = Paths.get(DIR_DADOS).toAbsolutePath().normalize();
        List<Path> arquivos = listarArquivosDados();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);

        dos.writeBytes(ASSINATURA);
        dos.writeInt(arquivos.size());
        for (Path p : arquivos) {
            String relativo = raiz.relativize(p).toString().replace('\\', '/');
            byte[] nome = relativo.getBytes(StandardCharsets.UTF_8);
            byte[] conteudo = Files.readAllBytes(p);

            dos.writeShort(nome.length);
            dos.write(nome);
            dos.writeLong(conteudo.length);
            dos.write(conteudo);
        }

        dos.flush();
        return out.toByteArray();
    }

    /**
     * Restaura, na pasta {@code destino}, todos os arquivos contidos no pacote.
     * Diretórios são criados conforme necessário.
     */
    public static int desempacotar(byte[] pacote, String destino) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pacote));

        byte[] assinatura = new byte[ASSINATURA.length()];
        dis.readFully(assinatura);
        if (!new String(assinatura, StandardCharsets.US_ASCII).equals(ASSINATURA)) {
            throw new IOException("Pacote de backup inválido (assinatura incorreta).");
        }

        Path raiz = Paths.get(destino).toAbsolutePath().normalize();
        Files.createDirectories(raiz);

        int quantidade = dis.readInt();
        for (int i = 0; i < quantidade; i++) {
            int tamNome = dis.readUnsignedShort();
            byte[] nome = new byte[tamNome];
            dis.readFully(nome);
            String relativo = new String(nome, StandardCharsets.UTF_8);

            long tamConteudo = dis.readLong();
            byte[] conteudo = new byte[(int) tamConteudo];
            dis.readFully(conteudo);

            Path alvo = raiz.resolve(relativo).normalize();
            if (!alvo.startsWith(raiz)) {
                throw new IOException("Tentativa de path traversal detectada: " + relativo);
            }
            Path pai = alvo.getParent();
            if (pai != null) {
                Files.createDirectories(pai);
            }
            Files.write(alvo, conteudo);
        }
        return quantidade;
    }
}
