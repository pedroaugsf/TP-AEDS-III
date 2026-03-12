package app.model;

import app.Registro;

import java.io.*;
import java.time.LocalDate;

/**
 * Refeicao:
 * - data (LocalDate) armazenada como epochDay (long)
 * - string: tipo, observacao
 * - FK para usuario (opcional no seu projeto): usuarioId
 */
public class Refeicao implements Registro {
    private int id;
    private int usuarioId;
    private LocalDate data;
    private String tipo;
    private String observacao;

    public Refeicao() {}

    public Refeicao(int usuarioId, LocalDate data, String tipo, String observacao) {
        this.usuarioId = usuarioId;
        this.data = data;
        this.tipo = tipo;
        this.observacao = observacao;
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public int getUsuarioId() { return usuarioId; }
    public void setUsuarioId(int usuarioId) { this.usuarioId = usuarioId; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }

    @Override
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeInt(usuarioId);

        long epochDay = (data != null) ? data.toEpochDay() : 0L;
        dos.writeLong(epochDay);

        dos.writeUTF(tipo != null ? tipo : "");
        dos.writeUTF(observacao != null ? observacao : "");

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        usuarioId = dis.readInt();
        long epochDay = dis.readLong();
        data = LocalDate.ofEpochDay(epochDay);
        tipo = dis.readUTF();
        observacao = dis.readUTF();
    }

    @Override
    public String toString() {
        return "Refeicao{id=" + id + ", usuarioId=" + usuarioId + ", data=" + data + ", tipo='" + tipo + "'}";
    }
}
