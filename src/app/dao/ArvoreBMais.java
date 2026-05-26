package app.dao;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Árvore B+ persistente, genérica, baseada em páginas de tamanho fixo.
 *
 * Layout do arquivo {nome}.idx.bmais.db:
 *  - offset 0:   long  enderecoRaiz (-1 se árvore vazia)
 *  - offset 8+:  páginas de tamanho fixo {@code tamPagina}
 *
 * Layout de cada página:
 *  [byte tipo]            // 0 = interno, 1 = folha
 *  [short n]              // número de entradas atualmente armazenadas
 *  [long proximo]         // próxima folha (apenas folhas; -1 para internas/última folha)
 *  [entry × ordem]        // slots de entradas (sobra preenchido com zero)
 *  [long × (ordem + 1)]   // ponteiros para filhos (apenas internas; sobra com -1)
 *
 * Operações:
 *  - inserir(T)           : insere mantendo ordem; faz split com promoção para cima.
 *  - remover(T)           : remove entrada cuja chave (compareTo == 0) bata.
 *                           Não faz rebalanceamento (folhas podem ficar abaixo do mínimo).
 *  - buscar(T)            : retorna todas as entradas com compareTo == 0.
 *  - listarTodos()        : varre as folhas em ordem (sem ordenação em memória).
 *
 * <b>Justificativa de uso</b>: a B+ é ideal para consultas <em>ordenadas</em>
 * e por <em>faixa</em>, pois mantém as folhas encadeadas em ordem crescente —
 * recupera-se toda a sequência sem trazer os registros para memória e ordenar.
 */
public class ArvoreBMais<T extends RegistroArvoreBMais<T>> {

    private static final byte INTERNO = 0;
    private static final byte FOLHA   = 1;
    private static final long NULL    = -1L;

    private final int ordem;                // máximo de entradas por página
    private final Constructor<T> construtor;
    private final RandomAccessFile arq;
    private final short tamEntrada;
    private final int tamPagina;

    public ArvoreBMais(String nomeArquivo, int ordem, Constructor<T> construtor) throws Exception {
        if (ordem < 3) throw new IllegalArgumentException("ordem da B+ deve ser >= 3");
        this.ordem = ordem;
        this.construtor = construtor;

        T tmp = construtor.newInstance();
        this.tamEntrada = tmp.size();
        // header: tipo(1) + n(2) + proximo(8) = 11 bytes
        this.tamPagina = 11 + ordem * tamEntrada + (ordem + 1) * 8;

        File dir = new File("./dados/" + nomeArquivo);
        if (!dir.exists()) dir.mkdirs();
        String path = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".idx.bmais.db";
        this.arq = new RandomAccessFile(path, "rw");
        if (arq.length() == 0) {
            arq.seek(0);
            arq.writeLong(NULL);
        }
    }

    // ===================== API pública =====================

    /** Insere mantendo a ordem; permite chaves duplicadas (basta compareTo distinguir). */
    public void inserir(T elem) throws Exception {
        long raiz = lerRaiz();
        if (raiz == NULL) {
            Pagina folha = new Pagina();
            folha.tipo = FOLHA;
            folha.entradas.add(elem);
            folha.proximo = NULL;
            folha.endereco = novoEndereco();
            escreverPagina(folha);
            escreverRaiz(folha.endereco);
            return;
        }
        ResultadoInsercao r = inserirRec(raiz, elem);
        if (r.split) {
            @SuppressWarnings("unchecked")
            T promov = (T) r.chavePromovida;
            Pagina nova = new Pagina();
            nova.tipo = INTERNO;
            nova.filhos.add(raiz);
            nova.entradas.add(promov);
            nova.filhos.add(r.novaPagina);
            nova.endereco = novoEndereco();
            escreverPagina(nova);
            escreverRaiz(nova.endereco);
        }
    }

    /** Remove a primeira entrada com compareTo == 0. Sem rebalanceamento. */
    public boolean remover(T elem) throws Exception {
        long raiz = lerRaiz();
        if (raiz == NULL) return false;
        return removerRec(raiz, elem);
    }

    /** Retorna todas as entradas cujo compareTo == 0 com o argumento. */
    public List<T> buscar(T elem) throws Exception {
        List<T> res = new ArrayList<>();
        long raiz = lerRaiz();
        if (raiz == NULL) return res;
        Pagina p = lerPagina(raiz);
        while (p.tipo == INTERNO) {
            int idx = 0;
            while (idx < p.entradas.size() && p.entradas.get(idx).compareTo(elem) <= 0) idx++;
            p = lerPagina(p.filhos.get(idx));
        }
        out:
        while (true) {
            for (T e : p.entradas) {
                int cmp = e.compareTo(elem);
                if (cmp == 0) res.add(e);
                else if (cmp > 0) break out;
            }
            if (p.proximo == NULL) break;
            p = lerPagina(p.proximo);
        }
        return res;
    }

    /**
     * Varre as folhas em <b>ordem crescente</b> percorrendo a lista encadeada
     * de folhas — sem trazer registros para memória só para ordenar.
     */
    public List<T> listarTodos() throws Exception {
        List<T> tudo = new ArrayList<>();
        long raiz = lerRaiz();
        if (raiz == NULL) return tudo;
        Pagina p = lerPagina(raiz);
        while (p.tipo == INTERNO) p = lerPagina(p.filhos.get(0));
        while (true) {
            tudo.addAll(p.entradas);
            if (p.proximo == NULL) break;
            p = lerPagina(p.proximo);
        }
        return tudo;
    }

    public void close() throws IOException { arq.close(); }

    public String toString() {
        try {
            long n = (arq.length() - 8) / tamPagina;
            return "ArvoreBMais{ordem=" + ordem + ", paginas=" + n + ", tamPagina=" + tamPagina + "B}";
        } catch (Exception e) { return "ArvoreBMais{?}"; }
    }

    // ===================== Recursão =====================

    private static class ResultadoInsercao {
        boolean split;
        Object chavePromovida;
        long novaPagina = NULL;
        @SuppressWarnings("unchecked") <X> X chave() { return (X) chavePromovida; }
    }

    private ResultadoInsercao inserirRec(long endereco, T elem) throws Exception {
        Pagina p = lerPagina(endereco);
        ResultadoInsercao r = new ResultadoInsercao();

        if (p.tipo == FOLHA) {
            int idx = 0;
            while (idx < p.entradas.size() && p.entradas.get(idx).compareTo(elem) < 0) idx++;
            p.entradas.add(idx, elem);
            if (p.entradas.size() <= ordem) { escreverPagina(p); return r; }

            // split folha
            int mid = (p.entradas.size() + 1) / 2;
            Pagina dir = new Pagina();
            dir.tipo = FOLHA;
            dir.entradas = new ArrayList<>(p.entradas.subList(mid, p.entradas.size()));
            dir.proximo = p.proximo;
            dir.endereco = novoEndereco();
            p.entradas = new ArrayList<>(p.entradas.subList(0, mid));
            p.proximo = dir.endereco;
            escreverPagina(dir);
            escreverPagina(p);
            r.split = true;
            r.chavePromovida = dir.entradas.get(0);
            r.novaPagina = dir.endereco;
            return r;
        }

        // interno: escolhe filho
        int idx = 0;
        while (idx < p.entradas.size() && p.entradas.get(idx).compareTo(elem) <= 0) idx++;
        ResultadoInsercao filho = inserirRec(p.filhos.get(idx), elem);
        if (!filho.split) return r;

        @SuppressWarnings("unchecked")
        T promovida = (T) filho.chavePromovida;
        p.entradas.add(idx, promovida);
        p.filhos.add(idx + 1, filho.novaPagina);
        if (p.entradas.size() <= ordem) { escreverPagina(p); return r; }

        // split interno: meio sobe (não fica nos filhos)
        int mid = p.entradas.size() / 2;
        T sobe = p.entradas.get(mid);
        Pagina dir = new Pagina();
        dir.tipo = INTERNO;
        dir.entradas = new ArrayList<>(p.entradas.subList(mid + 1, p.entradas.size()));
        dir.filhos = new ArrayList<>(p.filhos.subList(mid + 1, p.filhos.size()));
        p.entradas = new ArrayList<>(p.entradas.subList(0, mid));
        p.filhos = new ArrayList<>(p.filhos.subList(0, mid + 1));
        dir.endereco = novoEndereco();
        escreverPagina(dir);
        escreverPagina(p);
        r.split = true;
        r.chavePromovida = sobe;
        r.novaPagina = dir.endereco;
        return r;
    }

    private boolean removerRec(long endereco, T elem) throws Exception {
        Pagina p = lerPagina(endereco);
        if (p.tipo == FOLHA) {
            for (int i = 0; i < p.entradas.size(); i++) {
                if (p.entradas.get(i).compareTo(elem) == 0) {
                    p.entradas.remove(i);
                    escreverPagina(p);
                    return true;
                }
            }
            return false;
        }
        int idx = 0;
        while (idx < p.entradas.size() && p.entradas.get(idx).compareTo(elem) <= 0) idx++;
        return removerRec(p.filhos.get(idx), elem);
    }

    // ===================== I/O de páginas =====================

    private class Pagina {
        byte tipo;
        List<T> entradas = new ArrayList<>();
        List<Long> filhos = new ArrayList<>();
        long proximo = NULL;
        long endereco;
    }

    private long lerRaiz() throws IOException { arq.seek(0); return arq.readLong(); }
    private void escreverRaiz(long e) throws IOException { arq.seek(0); arq.writeLong(e); }

    private long novoEndereco() throws IOException {
        long len = arq.length();
        if (len < 8) len = 8;
        return len;
    }

    private Pagina lerPagina(long endereco) throws Exception {
        Pagina p = new Pagina();
        p.endereco = endereco;
        arq.seek(endereco);
        p.tipo = arq.readByte();
        int n = arq.readShort();
        p.proximo = arq.readLong();
        for (int i = 0; i < n; i++) {
            byte[] data = new byte[tamEntrada];
            arq.readFully(data);
            T e = construtor.newInstance();
            e.fromByteArray(data);
            p.entradas.add(e);
        }
        // pula slots de entradas vazios
        long inicioFilhos = endereco + 11L + (long) ordem * tamEntrada;
        arq.seek(inicioFilhos);
        if (p.tipo == INTERNO) {
            for (int i = 0; i <= n; i++) p.filhos.add(arq.readLong());
        }
        return p;
    }

    private void escreverPagina(Pagina p) throws Exception {
        if (p.endereco < 8) throw new IllegalStateException("endereço inválido");
        if (arq.length() < p.endereco) arq.setLength(p.endereco);
        arq.seek(p.endereco);
        arq.writeByte(p.tipo);
        arq.writeShort((short) p.entradas.size());
        arq.writeLong(p.proximo);
        for (T e : p.entradas) {
            byte[] data = e.toByteArray();
            if (data.length != tamEntrada) {
                byte[] fix = new byte[tamEntrada];
                System.arraycopy(data, 0, fix, 0, Math.min(data.length, tamEntrada));
                data = fix;
            }
            arq.write(data);
        }
        int leftoverEntradas = (ordem - p.entradas.size()) * tamEntrada;
        if (leftoverEntradas > 0) arq.write(new byte[leftoverEntradas]);

        // ponteiros para filhos
        if (p.tipo == INTERNO) {
            for (Long c : p.filhos) arq.writeLong(c);
            int extra = (ordem + 1) - p.filhos.size();
            for (int i = 0; i < extra; i++) arq.writeLong(NULL);
        } else {
            // folhas não usam, preenchemos com -1 para manter padrão e fixar tamanho
            for (int i = 0; i < ordem + 1; i++) arq.writeLong(NULL);
        }
    }
}
