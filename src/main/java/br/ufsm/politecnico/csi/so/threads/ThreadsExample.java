package br.ufsm.politecnico.csi.so.threads;

public class ThreadsExample {

    private static class Exemplo implements Runnable {

        @Override
        public void run() {
            System.out.println("[" + Thread.currentThread() + "] Iniciando...");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("[" + Thread.currentThread() + "] Terminando...");
        }
    }

    public static void main(String[] args) {
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        for (int i = 0; i < 10; i++) {
            new Thread(new Exemplo()).start();
        }
    }

}
