package app.dao;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Entrada usada pela Árvore B+ para indexar uma tabela por <b>nome</b>:
 * (nome:String fixo) -&gt; (id:int).
 *
 * Tamanho fixo: <code>TAM_NOME (60) + 4 = 64 bytes</code> — o nome é truncado/
 * preenchido com zeros para caber. O <code>compareTo</code> ordena por
 * <em>nome</em> e, em caso de empate, pelo <em>id</em>, garantindo unicidade
 * total da chave dentro da árvore.
 */
public class ParNomeID implements RegistroArvoreBMais<ParNomeID> {

    public static final int TAM_NOME = 60;

    private String nome;
    private int id;

    public ParNomeID() { this.nome = ""; this.id = 0; }
    public ParNomeID(String nome, int id) {
        this.nome = nome == null ? "" : nome;
        this.id = id;
    }

    public String getNome() { return nome; }
    public int getId() { return id; }

    @Override public short size() { return (short) (TAM_NOME + 4); }

    @Override
    public int compareTo(ParNomeID o) {
        int c = chaveOrdenacao(this.nome).compareTo(chaveOrdenacao(o.nome));
        if (c != 0) return c;
        return Integer.compare(this.id, o.id);
    }

    /** Normaliza para comparação case/locale-insensitive estável. */
    private static String chaveOrdenacao(String s) {
        return (s == null ? "" : s).trim().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public byte[] toByteArray() throws IOException {
        byte[] out = new byte[TAM_NOME + 4];
        byte[] nb = (nome == null ? "" : nome).getBytes(StandardCharsets.UTF_8);
        int n = Math.min(nb.length, TAM_NOME);
        System.arraycopy(nb, 0, out, 0, n);
        // 4 bytes finais = id (big-endian)
        out[TAM_NOME]     = (byte) ((id >>> 24) & 0xFF);
        out[TAM_NOME + 1] = (byte) ((id >>> 16) & 0xFF);
        out[TAM_NOME + 2] = (byte) ((id >>> 8)  & 0xFF);
        out[TAM_NOME + 3] = (byte) (id & 0xFF);
        return out;
    }

    @Override
    public void fromByteArray(byte[] ba) throws IOException {
        int nLen = 0;
        while (nLen < TAM_NOME && ba[nLen] != 0) nLen++;
        this.nome = new String(ba, 0, nLen, StandardCharsets.UTF_8);
        this.id = ((ba[TAM_NOME] & 0xFF) << 24)
                | ((ba[TAM_NOME + 1] & 0xFF) << 16)
                | ((ba[TAM_NOME + 2] & 0xFF) << 8)
                | (ba[TAM_NOME + 3] & 0xFF);
    }

    @Override public String toString() { return "(\"" + nome + "\" -> " + id + ")"; }
}
