package app.model;

import app.Registro;

import java.io.*;
import java.time.LocalDate;

/**
 * Favorito (tabela intermediária do relacionamento N:N entre Usuário e Alimento):
 *
 *  - Chave primária composta: (usuarioId, alimentoId) — único por par.
 *  - Cada favorito também recebe um <b>id sequencial interno</b> (PK do arquivo)
 *    para se beneficiar do mesmo padrão de cabeçalho/lápide/lista de removidos
 *    e do índice primário (Hash Extensível id -&gt; endereço) já existentes.
 *
 *  Atributos adicionais:
 *   - dataInclusao (LocalDate)
 *   - nota (byte 1..5)
 *   - observacao (String)
 */
public class Favorito implements Registro {

    private int id;
    private int usuarioId;
    private int alimentoId;
    private LocalDate dataInclusao;
    private byte nota;
    private String observacao;

    public Favorito() {}

    public Favorito(int usuarioId, int alimentoId, LocalDate dataInclusao, byte nota, String observacao) {
        this.usuarioId = usuarioId;
        this.alimentoId = alimentoId;
        this.dataInclusao = dataInclusao;
        this.nota = nota;
        this.observacao = observacao;
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public int getAlimentoId() { return alimentoId; }
    public void setAlimentoId(int alimentoId) { this.alimentoId = alimentoId; }

    public LocalDate getDataInclusao() { return dataInclusao; }
    public void setDataInclusao(LocalDate dataInclusao) { this.dataInclusao = dataInclusao; }

    public byte getNota() { return nota; }
    public void setNota(byte nota) { this.nota = nota; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    @Override
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(id);
        dos.writeInt(usuarioId);
        dos.writeInt(alimentoId);
        dos.writeLong(dataInclusao != null ? dataInclusao.toEpochDay() : 0L);
        dos.writeByte(nota);
        dos.writeUTF(observacao != null ? observacao : "");
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);
        id = dis.readInt();
        usuarioId = dis.readInt();
        alimentoId = dis.readInt();
        long ep = dis.readLong();
        dataInclusao = LocalDate.ofEpochDay(ep);
        nota = dis.readByte();
        observacao = dis.readUTF();
    }

    @Override
    public String toString() {
        return "Favorito{id=" + id + ", usuarioId=" + usuarioId + ", alimentoId=" + alimentoId
                + ", data=" + dataInclusao + ", nota=" + nota + "}";
    }
}
