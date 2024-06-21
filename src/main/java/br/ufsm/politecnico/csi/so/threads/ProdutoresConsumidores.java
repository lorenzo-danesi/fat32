package br.ufsm.politecnico.csi.so.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProdutoresConsumidores {

    private final int MAX_BUFFER = 30;
    private final List<Integer> buffer = new ArrayList<>(MAX_BUFFER);

    public ProdutoresConsumidores() {
        new Thread(new Consumidor()).start();
        new Thread(new Produtor()).start();
    }

    public class Produtor implements Runnable {

        private Random rnd = new Random();
        @Override
        public void run() {
            while (true) {
                synchronized (buffer) {
                    if (buffer.size() < MAX_BUFFER) {
                        int i = rnd.nextInt();
                        System.out.println("[PRODUTOR] Produziu " + i + ".");
                        buffer.add(i);
                        buffer.notify();
                    } else {
                        System.out.println("[PRODUTOR] Vai dormir...");
                        try { buffer.wait(); } catch (InterruptedException e) { }
                        System.out.println("[PRODUTOR] Acordou...");
                    }
                }
            }
        }
    }

    public class Consumidor implements Runnable {

        @Override
        public void run() {
            while (true) {
                synchronized (buffer) {
                    if (!buffer.isEmpty()) {
                        int i = buffer.remove(0);
                        System.out.println("[CONSUMIDOR] Consumiu " + i + ".");
                        buffer.notify();
                    } else {
                        System.out.println("[CONSUMIDOR] Vai dormir...");
                        try { buffer.wait(); } catch (InterruptedException e) { }
                        System.out.println("[CONSUMIDOR] Acordou...");
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        new ProdutoresConsumidores();
    }

}
