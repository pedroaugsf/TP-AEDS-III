package app.dao;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Hash Extensível genérica baseada em dois arquivos:
 *
 *   {nome}.d.db  -> diretório
 *      [int profundidadeGlobal][long endereco_0][long endereco_1] ... [long endereco_{2^p - 1}]
 *
 *   {nome}.c.db  -> cestos (buckets)
 *      Cada bucket tem tamanho fixo:
 *        [byte profundidadeLocal][short quantidade][slots[QUANTIDADE_MAX] de tamanho size()]
 *
 * Operações suportadas:
 *  - create(T)  : insere (permite chaves duplicadas; útil para 1:N)
 *  - read(int chave) : retorna TODOS os elementos cuja hashCode() == chave
 *  - readUm(int chave) : retorna o primeiro (útil para índices únicos como o primário)
 *  - update(T) : substitui no lugar (mesma chave, considerando a entrada como "mesma" via equals da chave)
 *  - delete(T) : remove a entrada que satisfaz equals()
 *  - deletePorChave(int chave) : remove todos cuja hashCode() == chave (índice único)
 *
 * Quando um bucket transborda, ele é dividido (split) e, se necessário, o diretório
 * é dobrado.
 */
public class HashExtensivel<T extends RegistroHashExtensivel<T>> {

    private final int QUANTIDADE_MAX_BUCKET; // slots por bucket
    private final Constructor<T> construtor;
    private final RandomAccessFile arqDiretorio;
    private final RandomAccessFile arqCestos;
    private final short tamElemento;
    private final int tamBucket;

    public HashExtensivel(String nomeArquivo, int slotsPorBucket, Constructor<T> construtor) throws Exception {
        this.QUANTIDADE_MAX_BUCKET = slotsPorBucket;
        this.construtor = construtor;

        // tamanho de um elemento é dado por uma instância recém-criada
        T tmp = construtor.newInstance();
        this.tamElemento = tmp.size();
        // bucket: 1 (prof local) + 2 (qtd) + N * tamElemento
        this.tamBucket = 3 + QUANTIDADE_MAX_BUCKET * tamElemento;

        File dir = new File("./dados/" + nomeArquivo);
        if (!dir.exists()) dir.mkdirs();

        String pathDir = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".idx.d.db";
        String pathCes = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".idx.c.db";

        this.arqDiretorio = new RandomAccessFile(pathDir, "rw");
        this.arqCestos = new RandomAccessFile(pathCes, "rw");

        if (arqDiretorio.length() == 0) {
            // diretório inicial com profundidade global = 0 -> 1 ponteiro
            arqDiretorio.seek(0);
            arqDiretorio.writeInt(0);            // profundidade global
            arqDiretorio.writeLong(0L);          // único endereço aponta para bucket 0
            // cria bucket 0 vazio
            escreverBucketVazio(0L, (byte) 0);
        }
    }

    private void escreverBucketVazio(long endereco, byte profundidadeLocal) throws IOException {
        arqCestos.seek(endereco);
        arqCestos.writeByte(profundidadeLocal);
        arqCestos.writeShort(0);
        // preenche com zeros
        byte[] zeros = new byte[QUANTIDADE_MAX_BUCKET * tamElemento];
        arqCestos.write(zeros);
    }

    private int profundidadeGlobal() throws IOException {
        arqDiretorio.seek(0);
        return arqDiretorio.readInt();
    }

    private long enderecoBucket(int indiceDir) throws IOException {
        arqDiretorio.seek(4L + (long) indiceDir * 8L);
        return arqDiretorio.readLong();
    }

    private void escreverEnderecoBucket(int indiceDir, long endereco) throws IOException {
        arqDiretorio.seek(4L + (long) indiceDir * 8L);
        arqDiretorio.writeLong(endereco);
    }

    /** Bits menos significativos da chave para indexar o diretório. */
    private int hashIndice(int chave, int profGlobal) {
        if (profGlobal == 0) return 0;
        int mask = (1 << profGlobal) - 1;
        return (chave & 0x7FFFFFFF) & mask;
    }

    // ===== leitura =====

    public List<T> read(int chave) throws Exception {
        List<T> result = new ArrayList<>();
        int p = profundidadeGlobal();
        int idx = hashIndice(chave, p);
        long endBucket = enderecoBucket(idx);
        Bucket b = lerBucket(endBucket);
        for (T e : b.elementos) {
            if (e.hashCode() == chave) result.add(e);
        }
        return result;
    }

    public T readUm(int chave) throws Exception {
        List<T> r = read(chave);
        return r.isEmpty() ? null : r.get(0);
    }

    // ===== escrita =====

    public boolean create(T elem) throws Exception {
        int p = profundidadeGlobal();
        int idx = hashIndice(elem.hashCode(), p);
        long endBucket = enderecoBucket(idx);
        Bucket b = lerBucket(endBucket);

        if (b.elementos.size() < QUANTIDADE_MAX_BUCKET) {
            b.elementos.add(elem);
            escreverBucket(endBucket, b);
            return true;
        }

        // overflow: split + retry
        split(idx);
        return create(elem);
    }

    /** Atualiza UMA entrada cuja chave (hashCode) bate; troca por elementoNovo. */
    public boolean update(T elementoNovo) throws Exception {
        int chave = elementoNovo.hashCode();
        int p = profundidadeGlobal();
        int idx = hashIndice(chave, p);
        long endBucket = enderecoBucket(idx);
        Bucket b = lerBucket(endBucket);
        for (int i = 0; i < b.elementos.size(); i++) {
            if (b.elementos.get(i).hashCode() == chave) {
                b.elementos.set(i, elementoNovo);
                escreverBucket(endBucket, b);
                return true;
            }
        }
        return false;
    }

    /** Remove a entrada cujo equals(elem) é true (considera chave+valor). */
    public boolean delete(T elem) throws Exception {
        int p = profundidadeGlobal();
        int idx = hashIndice(elem.hashCode(), p);
        long endBucket = enderecoBucket(idx);
        Bucket b = lerBucket(endBucket);
        for (int i = 0; i < b.elementos.size(); i++) {
            if (b.elementos.get(i).equals(elem)) {
                b.elementos.remove(i);
                escreverBucket(endBucket, b);
                return true;
            }
        }
        return false;
    }

    /** Remove a primeira entrada cuja hashCode() == chave (para índices de chave única). */
    public boolean deletePorChave(int chave) throws Exception {
        int p = profundidadeGlobal();
        int idx = hashIndice(chave, p);
        long endBucket = enderecoBucket(idx);
        Bucket b = lerBucket(endBucket);
        for (int i = 0; i < b.elementos.size(); i++) {
            if (b.elementos.get(i).hashCode() == chave) {
                b.elementos.remove(i);
                escreverBucket(endBucket, b);
                return true;
            }
        }
        return false;
    }

    // ===== split =====

    private void split(int idxOriginal) throws Exception {
        int profGlobal = profundidadeGlobal();
        long endBucket = enderecoBucket(idxOriginal);
        Bucket b = lerBucket(endBucket);

        if (b.profundidadeLocal == profGlobal) {
            // dobra o diretório
            int tamantes = 1 << profGlobal;
            int tamnovo = tamantes * 2;

            // copia ponteiros (cada ponteiro original aponta para 2 entradas no novo)
            long[] ponteiros = new long[tamantes];
            for (int i = 0; i < tamantes; i++) ponteiros[i] = enderecoBucket(i);

            arqDiretorio.setLength(4L + (long) tamnovo * 8L);
            arqDiretorio.seek(0);
            arqDiretorio.writeInt(profGlobal + 1);
            for (int i = 0; i < tamantes; i++) {
                escreverEnderecoBucket(i, ponteiros[i]);
                escreverEnderecoBucket(i + tamantes, ponteiros[i]);
            }
            profGlobal++;
        }

        // agora profGlobal > b.profundidadeLocal
        byte novaProfLocal = (byte) (b.profundidadeLocal + 1);
        long endBucket2 = arqCestos.length();
        escreverBucketVazio(endBucket2, novaProfLocal);

        // atualiza profundidade local do bucket original (ainda guarda o conteúdo, será reescrito abaixo)
        b.profundidadeLocal = novaProfLocal;

        // Atualiza ponteiros do diretório:
        // todos os índices cujos b.profundidadeLocal-1 bits inferiores batem com (idxOriginal mod 2^(profLocal-1))
        // dependerão do bit (profLocal-1) para decidir se vão para o original ou para o novo
        int dirSize = 1 << profGlobal;
        int padraoBaixo = idxOriginal & ((1 << (novaProfLocal - 1)) - 1);
        int bitNovo = 1 << (novaProfLocal - 1);
        for (int i = 0; i < dirSize; i++) {
            if ((i & ((1 << (novaProfLocal - 1)) - 1)) == padraoBaixo) {
                if ((i & bitNovo) != 0) escreverEnderecoBucket(i, endBucket2);
                else escreverEnderecoBucket(i, endBucket);
            }
        }

        // Redistribui os elementos
        List<T> antigos = new ArrayList<>(b.elementos);
        b.elementos.clear();
        Bucket b2 = new Bucket(novaProfLocal, new ArrayList<>());
        for (T e : antigos) {
            int idxNovo = hashIndice(e.hashCode(), profGlobal);
            if ((idxNovo & bitNovo) != 0) b2.elementos.add(e);
            else b.elementos.add(e);
        }
        escreverBucket(endBucket, b);
        escreverBucket(endBucket2, b2);
    }

    // ===== bucket I/O =====

    private class Bucket {
        byte profundidadeLocal;
        List<T> elementos;

        Bucket(byte p, List<T> es) { this.profundidadeLocal = p; this.elementos = es; }
    }

    private Bucket lerBucket(long endereco) throws Exception {
        arqCestos.seek(endereco);
        byte profLocal = arqCestos.readByte();
        short qtd = arqCestos.readShort();
        List<T> elems = new ArrayList<>(qtd);
        for (int i = 0; i < qtd; i++) {
            byte[] dados = new byte[tamElemento];
            arqCestos.read(dados);
            T e = construtor.newInstance();
            e.fromByteArray(dados);
            elems.add(e);
        }
        // pula o resto (slots vazios)
        long restante = (long) (QUANTIDADE_MAX_BUCKET - qtd) * tamElemento;
        if (restante > 0) arqCestos.seek(arqCestos.getFilePointer() + restante);
        return new Bucket(profLocal, elems);
    }

    private void escreverBucket(long endereco, Bucket b) throws Exception {
        arqCestos.seek(endereco);
        arqCestos.writeByte(b.profundidadeLocal);
        arqCestos.writeShort((short) b.elementos.size());
        for (T e : b.elementos) {
            byte[] dados = e.toByteArray();
            // garante tamanho fixo
            if (dados.length != tamElemento) {
                byte[] fixo = new byte[tamElemento];
                System.arraycopy(dados, 0, fixo, 0, Math.min(dados.length, tamElemento));
                dados = fixo;
            }
            arqCestos.write(dados);
        }
        // preenche slots vazios com zeros
        int restantes = QUANTIDADE_MAX_BUCKET - b.elementos.size();
        if (restantes > 0) arqCestos.write(new byte[restantes * tamElemento]);
    }

    public void close() throws IOException {
        arqDiretorio.close();
        arqCestos.close();
    }

    /** Apenas para depuração / documentação técnica. */
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder();
            int p = profundidadeGlobal();
            sb.append("HashExtensivel{profGlobal=").append(p)
              .append(", buckets=").append(arqCestos.length() / tamBucket)
              .append(", slotsPorBucket=").append(QUANTIDADE_MAX_BUCKET)
              .append("}");
            return sb.toString();
        } catch (Exception e) {
            return "HashExtensivel{?}";
        }
    }
}
