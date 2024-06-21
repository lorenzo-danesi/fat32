package br.ufsm.politecnico.csi.so;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws IOException {
        System.out.println("***********************");
        System.out.println("Iniciando FILESYSTEM...");
        System.out.println("***********************");
        Fat32FS fat32FS = new Fat32FS();
        //byte[] data = disco.leBloco(0);
        //System.out.println(new String(data, StandardCharsets.UTF_8));
        byte[] arqBytes = Files.readAllBytes(Paths.get("arq.txt"));
        fat32FS.create("arq.txt", arqBytes);
        // CREATE (criação e exibição do arquivo: OK)
        System.out.println(new String(arqBytes, StandardCharsets.UTF_8));
        System.out.println("-------------------------------------------------");

        // APPEND (adiciona dados no final do arquivo)
        byte[] adicionaDados = " PS: Este é um conteúdo adicional.".getBytes(StandardCharsets.UTF_8);
        fat32FS.append("arq.txt", adicionaDados);

        // READ (lê e exibe o arquivo atulizado agora)
        byte[] conteudoFile = fat32FS.read("arq.txt", 0, -1);
        System.out.println("Conteúdo atualizado do arquivo:");
        System.out.println(new String(conteudoFile, StandardCharsets.UTF_8));

        System.out.println("-------------------------------------");
        // REMOVE (exclusão do arquivo)
        fat32FS.remove("arq.txt");

        // tenta ler o arquivo "arq.txt" de novo (tem que lançar FileNotFoundException)
        try {
            byte[] arquivoRemovido = fat32FS.read("arq.txt", 0, -1);
            System.out.println("Conteúdo do arquivo 'arq.txt' após remoção: " + new String(arquivoRemovido, StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            System.out.println("O arquivo 'arq.txt' não foi encontrado após remoção.");
        }
    }
}