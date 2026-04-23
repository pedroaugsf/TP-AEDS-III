package app.dao;

import java.io.*;

/**
 * Entrada para o índice 1:N: chave = chave1 (FK, ex.: refeicaoId) -> valor = chave2 (PK do dependente).
 * Tamanho fixo: 4 + 4 = 8 bytes.
 *
 * O hashCode() retorna chave1 para indexar pela FK; o equals() considera os dois campos
 * para permitir delete preciso de uma associação específica.
 */
public class ParIDID implements RegistroHashExtensivel<ParIDID> {

    private int chave1; // FK (ex.: refeicaoId)
    private int chave2; // PK do dependente (ex.: consumoId)

    public ParIDID() {}
    public ParIDID(int chave1, int chave2) { this.chave1 = chave1; this.chave2 = chave2; }

    public int getChave1() { return chave1; }
    public int getChave2() { return chave2; }

    @Override public short size() { return 8; }

    @Override public int hashCode() { return chave1; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ParIDID)) return false;
        ParIDID p = (ParIDID) o;
        return p.chave1 == this.chave1 && p.chave2 == this.chave2;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(chave1);
        dos.writeInt(chave2);
        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);
        this.chave1 = dis.readInt();
        this.chave2 = dis.readInt();
    }

    @Override
    public String toString() { return "(" + chave1 + " -> " + chave2 + ")"; }
}
