package app.model;

import app.Registro;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Usuario:
 * - String: nome, email
 * - Data: dataNascimento (LocalDate armazenada como epochDay)
 * - String multivalorada: telefones
 */
public class Usuario implements Registro {
    private int id;
    private String nome;
    private String email;
    private LocalDate dataNascimento;
    private List<String> telefones;

    public Usuario() {
        this.telefones = new ArrayList<>();
    }

    public Usuario(String nome, String email, LocalDate dataNascimento, List<String> telefones) {
        this.nome = nome;
        this.email = email;
        this.dataNascimento = dataNascimento;
        this.telefones = (telefones == null) ? new ArrayList<>() : new ArrayList<>(telefones);
    }

    @Override public int getId() { return id; }
    @Override public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public List<String> getTelefones() { return telefones; }
    public void setTelefones(List<String> telefones) { this.telefones = telefones; }

    @Override
    public byte[] toByteArray() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeInt(id);
        dos.writeUTF(nome != null ? nome : "");
        dos.writeUTF(email != null ? email : "");

        long epochDay = (dataNascimento != null) ? dataNascimento.toEpochDay() : 0L;
        dos.writeLong(epochDay);

        dos.writeShort(telefones != null ? telefones.size() : 0);
        if (telefones != null) {
            for (String t : telefones) dos.writeUTF(t != null ? t : "");
        }

        return baos.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] ba) throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(ba);
        DataInputStream dis = new DataInputStream(bais);

        id = dis.readInt();
        nome = dis.readUTF();
        email = dis.readUTF();

        long epochDay = dis.readLong();
        dataNascimento = LocalDate.ofEpochDay(epochDay);

        short n = dis.readShort();
        telefones = new ArrayList<>(n);
        for (int i = 0; i < n; i++) telefones.add(dis.readUTF());
    }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", nome='" + nome + "', email='" + email + "', nasc=" + dataNascimento + ", telefones=" + telefones + "}";
    }
}
