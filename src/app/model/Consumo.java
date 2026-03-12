package app.model;

import app.Registro;

import java.io.*;

/**
 * Consumo (tabela intermediária N:N):
 * - FK refeicaoId -> Refeicao.id
 * - FK alimentoId -> Alimento.id
 * - float: quantidade em gramas (real)
 */
public class Consumo implements Registro {
    private int id;
    private int refeicaoId;
    private int alimentoId;
    private float quantidadeGramas;

    public Consumo() {}

    public Consumo(int refeicaoId, int alimentoId, float quantidadeGramas) {
        this.refeicaoId = refeicaoId;
        this.alimentoId = alimentoId;
        this.quantidadeGramas = quantidadeGramas;
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public int getRefeicaoId() { return refeicaoId; }
    public void setRefeicaoId(int refeicaoId) { this.refeicaoId = refeicaoId; }

    public int getAlimentoId() { return alimentoId; }
    public void setAlimentoId(int alimentoId) { this.alimentoId = alimentoId; }

    public float getQuantidadeGramas() { return quantidadeGramas; }
    public void setQuantidadeGramas(float quantidadeGramas) { this.quantidadeGramas = quantidadeGramas; }

    @Override
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeInt(refeicaoId);
        dos.writeInt(alimentoId);
        dos.writeFloat(quantidadeGramas);

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        refeicaoId = dis.readInt();
        alimentoId = dis.readInt();
        quantidadeGramas = dis.readFloat();
    }

    @Override
    public String toString() {
        return "Consumo{id=" + id + ", refeicaoId=" + refeicaoId + ", alimentoId=" + alimentoId + ", g=" + quantidadeGramas + "}";
    }
}
