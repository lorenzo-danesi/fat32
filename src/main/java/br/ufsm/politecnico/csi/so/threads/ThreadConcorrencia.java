package br.ufsm.politecnico.csi.so.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class ThreadConcorrencia {

    private long iDeTodos = 0;
    private class SomadorThread implements Runnable {

        private final Object monitor = new Object();
        private long i = 0;
        @Override
        public void run() {
            while (true) {
                synchronized (monitor) {
                    i++;
                    ThreadConcorrencia.this.iDeTodos++;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new ThreadConcorrencia();
    }

    public ThreadConcorrencia() throws InterruptedException {
        init();
    }

    private void init() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<SomadorThread> somadores = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            SomadorThread somador = new SomadorThread();
            Thread t = new Thread(somador);
            somadores.add(somador);
            threads.add(t);
            t.start();
        }
        monitoraSoma(somadores);
    }

    private void monitoraSoma(List<SomadorThread> somadores) throws InterruptedException {
        new Thread(() -> {
            Object monitor = new Object();
            while (true) {
                synchronized (monitor) {
                    long total = 0;
                    for (SomadorThread somador : somadores) {
                        total += somador.i;
                    }
                    System.out.println("Diferença: " + (total - iDeTodos));
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}
