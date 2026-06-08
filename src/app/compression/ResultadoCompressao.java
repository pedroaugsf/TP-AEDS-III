package app.compression;

/**
 * Resultado de uma operação de compressão: guarda os tamanhos envolvidos, a
 * taxa de compressão calculada e o status da verificação de integridade.
 */
public final class ResultadoCompressao {

    public final String algoritmo;       // "Huffman" ou "LZW"
    public final String arquivoGerado;   // caminho do arquivo único gerado
    public final int quantidadeArquivos; // arquivos incluídos no backup
    public final long tamanhoOriginal;   // bytes do pacote (soma dos dados)
    public final long tamanhoComprimido; // bytes do arquivo final gerado
    public final boolean integridadeOk;  // descompressão reproduz o original?
    public final long milissegundos;     // tempo gasto

    public ResultadoCompressao(String algoritmo, String arquivoGerado, int quantidadeArquivos,
                               long tamanhoOriginal, long tamanhoComprimido,
                               boolean integridadeOk, long milissegundos) {
        this.algoritmo = algoritmo;
        this.arquivoGerado = arquivoGerado;
        this.quantidadeArquivos = quantidadeArquivos;
        this.tamanhoOriginal = tamanhoOriginal;
        this.tamanhoComprimido = tamanhoComprimido;
        this.integridadeOk = integridadeOk;
        this.milissegundos = milissegundos;
    }

    /** Taxa de compressão = (1 - comprimido/original) * 100, em %. */
    public double taxaCompressao() {
        if (tamanhoOriginal == 0) return 0.0;
        return (1.0 - ((double) tamanhoComprimido / (double) tamanhoOriginal)) * 100.0;
    }

    /** Razão original:comprimido (ex.: 2.35 => o original é 2,35x maior). */
    public double razao() {
        if (tamanhoComprimido == 0) return 0.0;
        return (double) tamanhoOriginal / (double) tamanhoComprimido;
    }
}
