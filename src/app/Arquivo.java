package app;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;

/**
 * Arquivo<T> - Persistência em arquivo binário com:
 * - Cabeçalho: int (último ID) + long (ponteiro lista removidos)
 * - Registros: lápide (1 byte) + tamanho (short) + dados (byte[])
 *
 * Implementa CRUD e uma lista encadeada de espaços removidos (lápide) para reaproveitamento.
 */
public class Arquivo<T extends Registro> {

    // Cabeçalho: int (4) + long (8) = 12 bytes
    private static final int TAM_CABECALHO = 12;

    private final RandomAccessFile arquivo;
    private final String nomeArquivo;
    private final Constructor<T> construtor;

    public Arquivo(String nomeArquivo, Constructor<T> construtor) throws Exception {
        File diretorio = new File("./dados");
        if (!diretorio.exists()) diretorio.mkdir();

        diretorio = new File("./dados/" + nomeArquivo);
        if (!diretorio.exists()) diretorio.mkdir();

        this.nomeArquivo = "./dados/" + nomeArquivo + "/" + nomeArquivo + ".db";
        this.construtor = construtor;
        this.arquivo = new RandomAccessFile(this.nomeArquivo, "rw");

        if (arquivo.length() < TAM_CABECALHO) {
            arquivo.seek(0);
            arquivo.writeInt(0);    // último ID
            arquivo.writeLong(-1);  // cabeça da lista de removidos
        }
    }

    public int create(T obj) throws Exception {
        // incrementa ID no cabeçalho
        arquivo.seek(0);
        int novoID = arquivo.readInt() + 1;
        arquivo.seek(0);
        arquivo.writeInt(novoID);

        obj.setId(novoID);
        byte[] dados = obj.toByteArray();

        long endereco = getDeleted(dados.length);
        if (endereco == -1) {
            // grava no final
            arquivo.seek(arquivo.length());
            arquivo.writeByte(' ');     // lápide (ativo)
            arquivo.writeShort(dados.length);
            arquivo.write(dados);
        } else {
            // reaproveita espaço removido
            arquivo.seek(endereco);
            arquivo.writeByte(' ');
            arquivo.skipBytes(2);
            arquivo.write(dados);
        }
        return obj.getId();
    }

    public T read(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.read(dados);

            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) return obj;
            }
        }
        return null;
    }

    public boolean update(T novoObj) throws Exception {
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.read(dados);

            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == novoObj.getId()) {
                    byte[] novosDados = novoObj.toByteArray();
                    short novoTam = (short) novosDados.length;

                    if (novoTam <= tamanho) {
                        // atualiza no mesmo lugar
                        arquivo.seek(posicao + 3);
                        arquivo.write(novosDados);
                    } else {
                        // marca removido e regrava em outro espaço
                        arquivo.seek(posicao);
                        arquivo.writeByte('*');
                        addDeleted(tamanho, posicao);

                        long novoEndereco = getDeleted(novosDados.length);
                        if (novoEndereco == -1) {
                            arquivo.seek(arquivo.length());
                            arquivo.writeByte(' ');
                            arquivo.writeShort(novoTam);
                            arquivo.write(novosDados);
                        } else {
                            arquivo.seek(novoEndereco);
                            arquivo.writeByte(' ');
                            arquivo.skipBytes(2);
                            arquivo.write(novosDados);
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public boolean delete(int id) throws Exception {
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long posicao = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.read(dados);

            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                if (obj.getId() == id) {
                    arquivo.seek(posicao);
                    arquivo.writeByte('*');
                    addDeleted(tamanho, posicao);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Insere um espaço removido (posicao) na lista encadeada, ordenando por tamanho (best-fit simples).
     * No corpo do registro removido, armazenamos o "next" (long) em posicao+3.
     */
    private void addDeleted(int tamanhoEspaco, long enderecoEspaco) throws Exception {
        long posicao = 4; // posição do ponteiro da lista removidos no cabeçalho (após int)
        arquivo.seek(posicao);
        long endereco = arquivo.readLong();
        long proximo;

        if (endereco == -1) {
            // lista vazia
            arquivo.seek(4);
            arquivo.writeLong(enderecoEspaco);
            arquivo.seek(enderecoEspaco + 3);
            arquivo.writeLong(-1);
        } else {
            do {
                arquivo.seek(endereco + 1);
                int tam = arquivo.readShort();
                proximo = arquivo.readLong();

                if (tam > tamanhoEspaco) {
                    // insere antes
                    if (posicao == 4) arquivo.seek(posicao);
                    else arquivo.seek(posicao + 3);
                    arquivo.writeLong(enderecoEspaco);

                    arquivo.seek(enderecoEspaco + 3);
                    arquivo.writeLong(endereco);
                    break;
                }

                if (proximo == -1) {
                    // insere no fim
                    arquivo.seek(endereco + 3);
                    arquivo.writeLong(enderecoEspaco);

                    arquivo.seek(enderecoEspaco + 3);
                    arquivo.writeLong(-1);
                    break;
                }

                posicao = endereco;
                endereco = proximo;
            } while (endereco != -1);
        }
    }

    /**
     * Retorna um endereço de espaço removido que caiba (tam > necessario).
     * Remove esse nó da lista e devolve o endereço para reuso.
     */
    private long getDeleted(int tamanhoNecessario) throws Exception {
        long posicao = 4;
        arquivo.seek(posicao);
        long endereco = arquivo.readLong();
        long proximo;

        while (endereco != -1) {
            arquivo.seek(endereco + 1);
            int tamanho = arquivo.readShort();
            proximo = arquivo.readLong();

            if (tamanho > tamanhoNecessario) {
                if (posicao == 4) arquivo.seek(posicao);
                else arquivo.seek(posicao + 3);
                arquivo.writeLong(proximo);
                return endereco;
            }
            posicao = endereco;
            endereco = proximo;
        }
        return -1;
    }

    public void close() throws Exception {
        arquivo.close();
    }
}
