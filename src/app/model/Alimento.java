package app.model;

import app.Registro;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Alimento:
 * - String: nome, marca
 * - float: macros por 100g
 * - String multivalorada: tags
 */
public class Alimento implements Registro {
    private int id;
    private String nome;
    private String marca;
    private float kcalPor100g;
    private float proteinaPor100g;
    private float carboPor100g;
    private float gorduraPor100g;
    private List<String> tags;

    public Alimento() {
        this.tags = new ArrayList<>();
    }

    public Alimento(String nome, String marca, float kcal, float p, float c, float g, List<String> tags) {
        this.nome = nome;
        this.marca = marca;
        this.kcalPor100g = kcal;
        this.proteinaPor100g = p;
        this.carboPor100g = c;
        this.gorduraPor100g = g;
        this.tags = (tags == null) ? new ArrayList<>() : new ArrayList<>(tags);
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public float getKcalPor100g() { return kcalPor100g; }
    public void setKcalPor100g(float kcalPor100g) { this.kcalPor100g = kcalPor100g; }

    public float getProteinaPor100g() { return proteinaPor100g; }
    public void setProteinaPor100g(float proteinaPor100g) { this.proteinaPor100g = proteinaPor100g; }

    public float getCarboPor100g() { return carboPor100g; }
    public void setCarboPor100g(float carboPor100g) { this.carboPor100g = carboPor100g; }

    public float getGorduraPor100g() { return gorduraPor100g; }
    public void setGorduraPor100g(float gorduraPor100g) { this.gorduraPor100g = gorduraPor100g; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @Override
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeUTF(nome != null ? nome : "");
        dos.writeUTF(marca != null ? marca : "");
        dos.writeFloat(kcalPor100g);
        dos.writeFloat(proteinaPor100g);
        dos.writeFloat(carboPor100g);
        dos.writeFloat(gorduraPor100g);

        // multivalorado: escreve quantidade + cada string
        dos.writeShort(tags != null ? tags.size() : 0);
        if (tags != null) {
            for (String t : tags) dos.writeUTF(t != null ? t : "");
        }

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        nome = dis.readUTF();
        marca = dis.readUTF();
        kcalPor100g = dis.readFloat();
        proteinaPor100g = dis.readFloat();
        carboPor100g = dis.readFloat();
        gorduraPor100g = dis.readFloat();

        short n = dis.readShort();
        tags = new ArrayList<>(n);
        for (int i = 0; i < n; i++) tags.add(dis.readUTF());
    }

    @Override
    public String toString() {
        return "Alimento{id=" + id + ", nome='" + nome + "', marca='" + marca + "', kcal/100g=" + kcalPor100g + ", tags=" + tags + "}";
    }
}
