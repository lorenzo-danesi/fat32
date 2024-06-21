package br.ufsm.politecnico.csi.so.threads;

import java.util.ArrayList;
import java.util.List;

public class CalculadorPi {

    private static class CalculadorPiParcial implements Runnable {

        private long inicio;
        private long fim;
        private double piParcial;

        public CalculadorPiParcial(long inicio, long fim) {
            this.inicio = inicio;
            this.fim = fim;
        }

        @Override
        public void run() {
            for (long i = inicio; i < fim; i++) {
                if (i % 2 == 0) { // i é par
                    piParcial += 1.0 / (2 * i + 1);
                } else { // i é ímpar
                    piParcial -= 1.0 / (2 * i + 1);
                }
            }
        }
    }

    private static final long NUM_TERMOS_PARTE_PI = 1000000000000L;

    public static void main(String[] args) throws InterruptedException {
        double pi = 0.0;
        int availableProcessors = 1;
        List<CalculadorPiParcial> calculadores = new ArrayList<>();
        for (int i = 0; i < availableProcessors; i++) {
            CalculadorPiParcial calculadorPi = new CalculadorPiParcial(i * NUM_TERMOS_PARTE_PI, (i+1) * NUM_TERMOS_PARTE_PI);
            calculadores.add(calculadorPi);
            new Thread(calculadorPi).start();
        }
        monitoraPi(calculadores);
        System.out.println("Pi: " + pi);
    }

    private static void monitoraPi(List<CalculadorPiParcial> calculadores) throws InterruptedException {
        long inicio = System.currentTimeMillis();
        while(true) {
            double pi = 0.0;
            for (CalculadorPiParcial calculadorPiParcial : calculadores) {
                pi += calculadorPiParcial.piParcial * 4;
            }
            System.out.println("Pi atual: " + pi +
                    ", precisão: " + (Math.PI - pi) +
                    ", tempo: " + (System.currentTimeMillis() - inicio) + "ms.");
            Thread.sleep(500);
        }
    }

}
