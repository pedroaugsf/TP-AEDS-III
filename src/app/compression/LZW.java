package app.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Compressão de dados pelo algoritmo <b>LZW</b> (Lempel–Ziv–Welch), baseado em
 * dicionário adaptativo.
 *
 * <p>O dicionário começa com as 256 entradas de byte único (códigos 0..255) e
 * cresce dinamicamente à medida que novas sequências são encontradas. Os
 * códigos são gravados com largura fixa de <b>16 bits</b>; quando o dicionário
 * atinge 65.536 entradas, ele para de crescer (mas continua codificando com as
 * sequências já aprendidas). Como compressor e descompressor param de crescer
 * exatamente no mesmo ponto, a reconstrução é sempre íntegra.</p>
 *
 * <pre>
 *   int    tamanhoOriginal      (nº de bytes da entrada)
 *   short* codigos              (sequência de códigos de 16 bits)
 * </pre>
 */
public final class LZW {

    private LZW() {
    }

    private static final int LARGURA_BITS = 16;
    private static final int DICIONARIO_MAX = 1 << LARGURA_BITS; // 65.536

    // ====================================================================
    //  COMPRESSÃO
    // ====================================================================
    public static byte[] comprimir(byte[] entrada) throws IOException {
        if (entrada == null) entrada = new byte[0];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(out);
        dos.writeInt(entrada.length);

        if (entrada.length == 0) {
            dos.flush();
            return out.toByteArray();
        }

        // Dicionário de compressão: (códigoPrefixo << 8 | próximoByte) -> código
        Map<Long, Integer> dicionario = new HashMap<>();
        int proximoCodigo = 256;

        int prefixo = entrada[0] & 0xFF; // sequência corrente (W)
        for (int i = 1; i < entrada.length; i++) {
            int simbolo = entrada[i] & 0xFF;
            long chave = ((long) prefixo << 8) | simbolo;
            Integer codigo = dicionario.get(chave);
            if (codigo != null) {
                prefixo = codigo; // W = W + c
            } else {
                dos.writeShort(prefixo); // emite código de W
                if (proximoCodigo < DICIONARIO_MAX) {
                    dicionario.put(chave, proximoCodigo++);
                }
                prefixo = simbolo; // W = c
            }
        }
        dos.writeShort(prefixo); // emite a última sequência

        dos.flush();
        return out.toByteArray();
    }

    // ====================================================================
    //  DESCOMPRESSÃO
    // ====================================================================
    public static byte[] descomprimir(byte[] entrada) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(entrada));
        int tamanhoOriginal = dis.readInt();

        ByteArrayOutputStream saida = new ByteArrayOutputStream(Math.max(16, tamanhoOriginal));
        if (tamanhoOriginal == 0) {
            return saida.toByteArray();
        }

        // Dicionário de descompressão: código -> sequência de bytes
        List<byte[]> dicionario = new ArrayList<>(DICIONARIO_MAX);
        for (int i = 0; i < 256; i++) {
            dicionario.add(new byte[] { (byte) i });
        }

        int codigoAnterior = dis.readUnsignedShort();
        byte[] anterior = dicionario.get(codigoAnterior);
        saida.write(anterior, 0, anterior.length);

        int totalBytes = entrada.length;
        int lidos = 4 + 2; // int do cabeçalho + primeiro short já lido
        while (lidos + 2 <= totalBytes) {
            int codigo = dis.readUnsignedShort();
            lidos += 2;

            byte[] atual;
            if (codigo < dicionario.size()) {
                atual = dicionario.get(codigo);
            } else if (codigo == dicionario.size()) {
                // caso especial KwKwK: W + primeiro byte de W
                atual = new byte[anterior.length + 1];
                System.arraycopy(anterior, 0, atual, 0, anterior.length);
                atual[anterior.length] = anterior[0];
            } else {
                throw new IOException("Fluxo LZW corrompido: código inválido " + codigo);
            }

            saida.write(atual, 0, atual.length);

            // adiciona ao dicionário: anterior + primeiro byte do atual
            if (dicionario.size() < DICIONARIO_MAX) {
                byte[] nova = new byte[anterior.length + 1];
                System.arraycopy(anterior, 0, nova, 0, anterior.length);
                nova[anterior.length] = atual[0];
                dicionario.add(nova);
            }

            anterior = atual;
        }

        return saida.toByteArray();
    }
}
