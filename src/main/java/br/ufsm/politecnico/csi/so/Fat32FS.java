package br.ufsm.politecnico.csi.so;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Fat32FS implements FileSystem {

    private Disco disco;

    private int[] fat = new int[16*1024];

    public Fat32FS() throws IOException {
        this.disco = new Disco();
        if (this.disco.init()) {
            leFat();
            leDiretorio();
        } else {
            criaFat();
            escreveFat();
        }

    }

    private void criaFat() {
        for (int i = 2; i < fat.length; i++) {
            fat[i] = -1;
        }
    }

    private void escreveFat() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(64*1024);
        for (int f : fat) {
            bb.putInt(f);
        }
        byte[] blocoFat = bb.array();
        disco.escreveBloco(BLOCO_FAT, blocoFat);
    }

    private static final int BLOCO_FAT = 1;
    private void leFat() throws IOException {
        byte[] blocoFat = disco.leBloco(BLOCO_FAT);
        ByteBuffer bb = ByteBuffer.wrap(blocoFat);
        for (int i = 0; i < 16*1024; i++) {
            fat[i] = bb.getInt();
        }
    }


    @Override
    public void create(String fileName, byte[] data) throws IOException {

        // Ver se tem espaço suficiente.
        if (data.length > freeSpace()) {
            throw new IOException("Não há espaço suficiente no disco para criar o arquivo.");
        }

        // Ver se o arquivo já não existe.
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            if (entrada.nomeArquivo.trim().equals(fileName)) {
                throw new IOException("O arquivo já existe.");
            }
        }

        try {
            // encontrar um bloco livre para arquivo
            int blocoLivre = encontraBlocoLivre();
            // caso todos os blocos estejam ocupados
            if (blocoLivre == -1) {
                System.out.println("Não há blocos livres disponíveis para criar o arquivo.");
                return;
            }

            // atualiza o bloco encontrado como ocupado na FAT
            fat[blocoLivre] = -2; // marcador (bloco ocupado)

            // cria a entrada do arquivo no diretório e depois adiciona
            EntradaArquivoDiretorio entrada = new EntradaArquivoDiretorio(fileName, "dat", data.length, blocoLivre);
            diretorioRaiz.add(entrada);

            // reescreve a entrada do diretório (atualizado) no disco
            escreveDiretorio();
            // grava os dados do arquivo no bloco do disco que estamos alocando o arq.txt
            disco.escreveBloco(blocoLivre, data);
            System.out.println("O arquivo foi criado com sucesso: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao criar o arquivo.");
        }
    }

    private int encontraBlocoLivre() {
        for (int i = 2; i < fat.length; i++) {
            if (fat[i] == -1) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void append(String fileName, byte[] data) {
        try {
            // verificando se o arquivo existe no diretório
            EntradaArquivoDiretorio entrada = null;
            for (EntradaArquivoDiretorio e : diretorioRaiz) {
                if (e.nomeArquivo.trim().equals(fileName)) {
                    entrada = e;
                    break;
                }
            }
            //caso não encontre nehuma entrada correspondente com o arquivo ...
            if (entrada == null) {
                throw new FileNotFoundException("Arquivo não encontrado: " + fileName);
            }

            int tamanhoAtual = entrada.tamanho;
            int blocoInicial = entrada.blocoInicial;

            // determinar o bloco onde o último byte do arquivo está atualmente
            int ultimoBloco = blocoInicial + (tamanhoAtual / Disco.TAM_BLOCO);

            // calcular onde começar a escrever os novos dados
            int posicaoInicial = tamanhoAtual % Disco.TAM_BLOCO;

            // verificar se tem espaço no bloco atual para adicionar mais dados
            if (posicaoInicial + data.length > Disco.TAM_BLOCO) {
                throw new IOException("Não há espaço suficiente no bloco para adicionar os novos dados.");
            }

            // lê dados atuais no bloco (para onde os novos serão adicionados)
            byte[] blocoAtual = disco.leBloco(ultimoBloco);
            // copia os novos dados para o bloco atual
            System.arraycopy(data, 0, blocoAtual, posicaoInicial, data.length);

            // escreve o bloco atualizado de volta no disco
            disco.escreveBloco(ultimoBloco, blocoAtual);
            // atualiza tamanho do arquivo
            entrada.tamanho += data.length;
            escreveDiretorio();
            System.out.println("Dados adicionados com sucesso ao arquivo: " + fileName);

        } catch (IOException e) {
            System.out.println("Erro ao adicionar dados ao arquivo: " + e.getMessage());
        }
    }


    @Override
    public byte[] read(String fileName, int offset, int limit) throws IOException {
        // verificando se o arquivo existe no diretório
        EntradaArquivoDiretorio entrada = null;
        for (EntradaArquivoDiretorio e : diretorioRaiz) {
            if (e.nomeArquivo.trim().equals(fileName)) {
                entrada = e;
                break;
            }
        }
        //caso não encontre nehuma entrada correspondente com o arquivo ...
        if (entrada == null) {
            throw new FileNotFoundException("Arquivo não encontrado: " + fileName);
        }

        // calcula o número total de blocos (ocupados em disco) a serem lidos
        int numBlocos = (int) Math.ceil((double) entrada.tamanho / Disco.TAM_BLOCO);

        // verificando se o offset está de acordo
        if (offset < 0 || offset >= entrada.tamanho) {
            throw new IllegalArgumentException("Offset inválido para leitura.");
        }

        // determinar o limite da leitura
        int limiteFinal;
        if (limit == -1 || offset + limit > entrada.tamanho) {
            limiteFinal = entrada.tamanho - offset; // lê até o final do arquivo
        } else {
            limiteFinal = limit; // só lê até o limite
        }

        // armazenar os dados lidos
        byte[] dadosLidos = new byte[limiteFinal];

        int posicaoAtual = offset;
        int bytesLidos = 0;

        while (bytesLidos < limiteFinal) {
            int blocoAtual = entrada.blocoInicial + (posicaoAtual / Disco.TAM_BLOCO);
            int posicaoNoBloco = posicaoAtual % Disco.TAM_BLOCO;

            // calcular quantidade de bytes a ler no bloco
            int bytesRestantes = limiteFinal - bytesLidos;
            int bytesNesteBloco = Math.min(bytesRestantes, Disco.TAM_BLOCO - posicaoNoBloco);

            // lê o bloco atual do disco
            byte[] blocoDados = disco.leBloco(blocoAtual);

            // copia os dados do bloco para o array de dados lidos
            System.arraycopy(blocoDados, posicaoNoBloco, dadosLidos, bytesLidos, bytesNesteBloco);

            // atualiza a posição atual e bytes lidos
            posicaoAtual += bytesNesteBloco;
            bytesLidos += bytesNesteBloco;
        }

        return dadosLidos;
    }


    @Override
    public void remove(String fileName) {
        // verificando se o arquivo existe no diretório
        EntradaArquivoDiretorio entradaRemover = null;
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            if (entrada.nomeArquivo.trim().equals(fileName)) {
                entradaRemover = entrada;
                break;
            }
        }
        // caso não encontre nehuma entrada correspondente com o arquivo ...
        if (entradaRemover == null) {
            System.out.println("Arquivo não encontrado: " + fileName);
            return;
        }

        // marcar os blocos na FAT como livres (-1)
        int blocoAtual = entradaRemover.blocoInicial;
        while (blocoAtual != -1) {
            int proximoBloco = fat[blocoAtual];
            fat[blocoAtual] = -1;
            blocoAtual = proximoBloco;
            if (blocoAtual >= fat.length || blocoAtual < 0) {
                break; // verifica se blocoAtual é válido para evitar exceção
            }
        }

        // remove entrada do diretório raiz
        diretorioRaiz.remove(entradaRemover);

        try {
            // atualiza o diretório no disco
            escreveDiretorio();
            System.out.println("Arquivo removido com sucesso: " + fileName);
        } catch (IOException e) {
            System.out.println("Erro ao remover arquivo: " + e.getMessage());
        }
    }



    /**
     * Método para calcular a quantidade de
     * espaço livre no disco
     * varrendo e verrificando os blocos livres na FAT.
     * @return
     */
    @Override
    public int freeSpace() {
        // contador para blocos livres na FAT
        int blocoLivre = 0;

        // varre toda FAT a partir do bloco 2
        for (int i = 2; i < fat.length; i++) {
            // verifica os blocos livres (-1)
            if (fat[i] == -1) {
                blocoLivre++;
            }
        }
        // sabemos o total de espaço livre em disco
        return blocoLivre * Disco.TAM_BLOCO;
    }

    private static class EntradaArquivoDiretorio {
        private String nomeArquivo;
        private String extensao;
        private int tamanho;
        private int blocoInicial;

        public EntradaArquivoDiretorio(String nomeArquivo,
                                       String extensao,
                                       int tamanho,
                                       int blocoInicial) {
            this.nomeArquivo = nomeArquivo;
            if (this.nomeArquivo.length() > 8) {
                this.nomeArquivo = nomeArquivo.substring(0, 8);
            } else if (this.nomeArquivo.length() < 8) {
                do {
                    this.nomeArquivo += " ";
                } while (this.nomeArquivo.length() < 8);
            }
            this.extensao = extensao;
            if (this.extensao.length() > 3) {
                this.extensao = extensao.substring(0, 3);
            } else if (this.extensao.length() < 3) {
                do {
                    this.extensao += " ";
                } while (this.extensao.length() < 3);
            }
            this.tamanho = tamanho;
            this.blocoInicial = blocoInicial;
            if (blocoInicial < 2 || blocoInicial >= Disco.NUM_BLOCOS) {
                throw new IllegalArgumentException("numero de bloco invalido");
            }
        }

        public byte[] toByteArray(ByteBuffer bb) {
            bb.put(nomeArquivo.getBytes(StandardCharsets.ISO_8859_1));
            bb.put(extensao.getBytes(StandardCharsets.ISO_8859_1));
            bb.putInt(tamanho);
            bb.putInt(blocoInicial);
            return bb.array();
        }

        private static int intFromBytes(byte[] data, int index) {
            ByteBuffer bb = ByteBuffer.wrap(data);
            return bb.getInt(index);
        }

        public static EntradaArquivoDiretorio fromBytes(byte[] bytes) {
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);
            System.out.println(nome);
            System.out.println(extensao);
            System.out.println(tamanho);
            System.out.println(blocoInicial);
            return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
        }

        public static EntradaArquivoDiretorio fromStream(InputStream inputStream) throws IOException {
            byte[] bytes = new byte[19];
            inputStream.read(bytes);
            String nome = new String(bytes,
                    0, 8, StandardCharsets.ISO_8859_1);
            String extensao = new String(bytes,
                    8, 3, StandardCharsets.ISO_8859_1);
            int tamanho = intFromBytes(bytes, 11);
            int blocoInicial = intFromBytes(bytes, 15);
            System.out.println(nome);
            System.out.println(extensao);
            System.out.println(tamanho);
            System.out.println(blocoInicial);
            return new EntradaArquivoDiretorio(nome, extensao, tamanho, blocoInicial);
        }

    }

    private static final int BLOCO_DIRETORIO = 0;
    private List<EntradaArquivoDiretorio> diretorioRaiz = new ArrayList<>();

    private void leDiretorio() throws IOException {
        byte[] dirBytes = disco.leBloco(BLOCO_DIRETORIO);
        ByteArrayInputStream bin = new ByteArrayInputStream(dirBytes);
        EntradaArquivoDiretorio entrada = null;
        do {
            entrada = EntradaArquivoDiretorio.fromStream(bin);
            if (entrada.tamanho > 0) {
                diretorioRaiz.add(entrada);
            }
        } while(entrada.tamanho > 0);

    }

    private void escreveDiretorio() throws IOException {
        ByteBuffer bb = ByteBuffer.allocate(Disco.TAM_BLOCO);
        for (EntradaArquivoDiretorio entrada : diretorioRaiz) {
            entrada.toByteArray(bb);
        }
        disco.escreveBloco(BLOCO_DIRETORIO, bb.array());
    }

}
