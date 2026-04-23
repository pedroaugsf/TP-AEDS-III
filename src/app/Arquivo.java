package app;

import app.dao.HashExtensivel;
import app.dao.ParIDEndereco;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Arquivo&lt;T&gt; — Persistência em arquivo binário com:
 *  - Cabeçalho: int (último ID) + long (ponteiro lista removidos) = 12 bytes.
 *  - Registros: lápide (1 byte) + tamanho (short) + dados (byte[]).
 *  - Lista encadeada de espaços removidos (best-fit) para reaproveitamento.
 *  - Índice primário em <b>Hash Extensível</b> (id -&gt; endereço no .db) para read/update/delete em O(1).
 *
 * Os índices ficam em <code>./dados/{nome}/{nome}.idx.d.db</code> e <code>...idx.c.db</code>.
 *
 * Se o índice primário estiver vazio mas o arquivo .db já tiver dados,
 * o índice é reconstruído automaticamente na inicialização.
 */
public class Arquivo<T extends Registro> {

    private static final int TAM_CABECALHO = 12;
    /** Slots por bucket do índice primário. */
    private static final int SLOTS_BUCKET_PRIMARIO = 8;

    private final RandomAccessFile arquivo;
    private final String nomeArquivo;
    private final Constructor<T> construtor;
    private final HashExtensivel<ParIDEndereco> indicePrimario;

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

        // Índice primário (id -> endereço)
        Constructor<ParIDEndereco> ctor = ParIDEndereco.class.getConstructor();
        this.indicePrimario = new HashExtensivel<>(nomeArquivo, SLOTS_BUCKET_PRIMARIO, ctor);

        // Reconstrução do índice caso o .db tenha registros mas o índice esteja vazio.
        reconstruirIndiceSeNecessario();
    }

    private void reconstruirIndiceSeNecessario() throws Exception {
        if (arquivo.length() <= TAM_CABECALHO) return;
        arquivo.seek(0);
        int ultimoId = arquivo.readInt();
        if (ultimoId == 0) return;
        if (indicePrimario.readUm(ultimoId) != null) return; // já indexado
        // varre o arquivo populando o índice
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            long pos = arquivo.getFilePointer();
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.read(dados);
            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                indicePrimario.create(new ParIDEndereco(obj.getId(), pos));
            }
        }
    }

    // ===== CRUD =====

    public int create(T obj) throws Exception {
        arquivo.seek(0);
        int novoID = arquivo.readInt() + 1;
        arquivo.seek(0);
        arquivo.writeInt(novoID);

        obj.setId(novoID);
        byte[] dados = obj.toByteArray();

        long endereco = getDeleted(dados.length);
        long posicaoFinal;
        if (endereco == -1) {
            posicaoFinal = arquivo.length();
            arquivo.seek(posicaoFinal);
            arquivo.writeByte(' ');
            arquivo.writeShort(dados.length);
            arquivo.write(dados);
        } else {
            posicaoFinal = endereco;
            arquivo.seek(endereco);
            arquivo.writeByte(' ');
            arquivo.skipBytes(2);
            arquivo.write(dados);
        }
        indicePrimario.create(new ParIDEndereco(novoID, posicaoFinal));
        return obj.getId();
    }

    public T read(int id) throws Exception {
        ParIDEndereco par = indicePrimario.readUm(id);
        if (par == null) return null;
        long pos = par.getEndereco();
        arquivo.seek(pos);
        byte lapide = arquivo.readByte();
        short tamanho = arquivo.readShort();
        byte[] dados = new byte[tamanho];
        arquivo.read(dados);
        if (lapide != ' ') return null;
        T obj = construtor.newInstance();
        obj.fromByteArray(dados);
        if (obj.getId() != id) return null;
        return obj;
    }

    public boolean exists(int id) throws Exception {
        return read(id) != null;
    }

    public boolean update(T novoObj) throws Exception {
        ParIDEndereco par = indicePrimario.readUm(novoObj.getId());
        if (par == null) return false;
        long posicao = par.getEndereco();

        arquivo.seek(posicao);
        byte lapide = arquivo.readByte();
        short tamanho = arquivo.readShort();
        if (lapide != ' ') return false;

        byte[] novosDados = novoObj.toByteArray();
        short novoTam = (short) novosDados.length;

        if (novoTam <= tamanho) {
            arquivo.seek(posicao + 3);
            arquivo.write(novosDados);
            return true;
        }

        // marca removido e regrava em outro espaço
        arquivo.seek(posicao);
        arquivo.writeByte('*');
        addDeleted(tamanho, posicao);

        long novoEndereco = getDeleted(novosDados.length);
        long posicaoFinal;
        if (novoEndereco == -1) {
            posicaoFinal = arquivo.length();
            arquivo.seek(posicaoFinal);
            arquivo.writeByte(' ');
            arquivo.writeShort(novoTam);
            arquivo.write(novosDados);
        } else {
            posicaoFinal = novoEndereco;
            arquivo.seek(novoEndereco);
            arquivo.writeByte(' ');
            arquivo.skipBytes(2);
            arquivo.write(novosDados);
        }
        indicePrimario.update(new ParIDEndereco(novoObj.getId(), posicaoFinal));
        return true;
    }

    public boolean delete(int id) throws Exception {
        ParIDEndereco par = indicePrimario.readUm(id);
        if (par == null) return false;
        long posicao = par.getEndereco();
        arquivo.seek(posicao);
        byte lapide = arquivo.readByte();
        short tamanho = arquivo.readShort();
        if (lapide != ' ') return false;
        arquivo.seek(posicao);
        arquivo.writeByte('*');
        addDeleted(tamanho, posicao);
        indicePrimario.deletePorChave(id);
        return true;
    }

    /** Varre todos os registros ativos (lápide = ' '). */
    public List<T> listar() throws Exception {
        List<T> result = new ArrayList<>();
        arquivo.seek(TAM_CABECALHO);
        while (arquivo.getFilePointer() < arquivo.length()) {
            byte lapide = arquivo.readByte();
            short tamanho = arquivo.readShort();
            byte[] dados = new byte[tamanho];
            arquivo.read(dados);
            if (lapide == ' ') {
                T obj = construtor.newInstance();
                obj.fromByteArray(dados);
                result.add(obj);
            }
        }
        return result;
    }

    // ===== lista de removidos (best-fit) =====

    private void addDeleted(int tamanhoEspaco, long enderecoEspaco) throws Exception {
        long posicao = 4;
        arquivo.seek(posicao);
        long endereco = arquivo.readLong();
        long proximo;

        if (endereco == -1) {
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
                    if (posicao == 4) arquivo.seek(posicao);
                    else arquivo.seek(posicao + 3);
                    arquivo.writeLong(enderecoEspaco);

                    arquivo.seek(enderecoEspaco + 3);
                    arquivo.writeLong(endereco);
                    break;
                }

                if (proximo == -1) {
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
        indicePrimario.close();
    }

    public String descricaoIndice() { return indicePrimario.toString(); }
}
