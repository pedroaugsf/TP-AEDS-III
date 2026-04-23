package app.dao;

import java.io.*;

/**
 * Entrada do índice primário: chave = ID (int) -> valor = endereço no arquivo (long).
 * Tamanho fixo: 4 + 8 = 12 bytes.
 */
public class ParIDEndereco implements RegistroHashExtensivel<ParIDEndereco> {

    private int id;
    private long endereco;

    public ParIDEndereco() {}

    public ParIDEndereco(int id, long endereco) {
        this.id = id;
        this.endereco = endereco;
    }

    public int getId() { return id; }
    public long getEndereco() { return endereco; }

    @Override public short size() { return 12; }

    @Override public int hashCode() { return id; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParIDEndereco)) return false;
        ParIDEndereco p = (ParIDEndereco) o;
        return p.id == this.id; // chave única
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(id);
        dos.writeLong(endereco);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);
        this.id = dis.readInt();
        this.endereco = dis.readLong();
    }

    @Override
    public String toString() { return "(" + id + " -> " + endereco + ")"; }
}
