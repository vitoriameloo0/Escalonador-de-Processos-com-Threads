import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.LinkedList;
import java.util.Queue;

class Escalonador {	
	private int quantum;
	private final Object sincronizar = new Object();
	private DefaultTableModel tabela;
	private Queue<Processo> filaProcessos;
	private Queue<Processo> filaProcessosSuspensos;
	private volatile boolean pausado = false;
	
	// Construtor da Escalonador 
	public Escalonador(int quantum, DefaultTableModel tabela) {
		this.filaProcessos = new LinkedList<>();
		this.filaProcessosSuspensos = new LinkedList<>();
		this.quantum = quantum;
		this.tabela = tabela;		
	}

	//Retorna a fila de processos que estão prontos
	public Queue<Processo> getFilaProcessos() { 
		return filaProcessos;
	}

	//Retorna a fila de processos que estão suspensos
	public Queue<Processo> getFilaProcessosSuspensos() { 
		return filaProcessosSuspensos;
	}

	// Adiciona um processo a fila de processos
	public void adicionarProcesso(Processo processo) { 
		synchronized (sincronizar) {
			// Se não estiver suspenso, o estado vai para pronto e entra na fila de processos para ser executado
			if (!processo.getEstado().equals("Suspenso")) {
				SwingUtilities.invokeLater(() -> {
					tabela.addRow(new Object[] { processo.getId(), processo.getPrioridade(),
							processo.getTempoExecucao(), "Pronto" });
				});
				filaProcessos.add(processo);
				sincronizar.notify();
			}
		}
	}

	// Adiciona um processo a fila de processos suspensos
	public void adicionarProcessoSuspenso(Processo processo) { 
		synchronized (sincronizar) {
			// Atualiza o estado como suspenso e depois adiciona na fila de suspenso
			SwingUtilities.invokeLater(() -> {
				tabela.addRow(new Object[] { processo.getId(), processo.getPrioridade(), processo.getTempoExecucao(),
						"Suspenso" });
			});
			filaProcessosSuspensos.add(processo);
			sincronizar.notify();
		}
	}
	
	// Verifica se uma fila esta vazia
	public boolean filaProcessosVazia() { 
		synchronized (sincronizar) {
			return filaProcessos.isEmpty(); // retorna um boolean que indica se a variavel foi inicializada
		}
	}

	// Retorna o valor do quantum
	public int getQuantum() { 
		return quantum;
	}

	// Verifica se o escalonador esta pausado
	public boolean isPausado() { 
		return pausado;
	}

	// Bloqueia um processo com base no id da linha da tabela.
	public void bloquearProcesso(int rowIndex) { 
		synchronized (sincronizar) {
			Processo processo = filaProcessos.stream().filter(p -> p.getId() == (int) tabela.getValueAt(rowIndex, 0))
					.findFirst().orElse(null);
			// Verifica se o processo é diferente e esta no estado de "em execução" ou "pronto" para assim poder bloquear o processo
			if (processo != null && (processo.getEstado().equals("Em Execução") || processo.getEstado().equals("Pronto"))) {
				int tempoRestante = Math.min(quantum, processo.getTempoExecucao());
				processo.setTempoRestanteBloqueado(tempoRestante);
				processo.setSuspender(true);
				processo.setEstado("Bloqueado");
				
				filaProcessos.remove(processo); // Remover o processo da fila de execução se estiver bloqueado				
				filaProcessosSuspensos.add(processo); // Adiciona a fila de processos bloqueados
				
				// Atualiza tabela como Suspenso
				SwingUtilities.invokeLater(() -> {
					int rowIndexUpdated = processo.getId() - 1;
					tabela.setValueAt("Suspenso", rowIndexUpdated, 3);
				});
			}
			sincronizar.notify();
		}
	}

	// Retoma um processo suspenso com base no id da linha da tabela.
	public void retomarProcesso(int rowIndex) { 
		synchronized (sincronizar) {
			Processo processo = filaProcessosSuspensos.stream()
					.filter(p -> p.getId() == (int) tabela.getValueAt(rowIndex, 0)).findFirst().orElse(null);
			// Se o processo for diferente de null, ele so retoma o programa com o estado de "pronto"
			if (processo != null) {
				processo.retomar();
				processo.setEstado("Pronto");
				processo.setTempoExecucao(processo.getTempoRestanteBloqueado());
				filaProcessos.add(processo);
				filaProcessosSuspensos.remove(processo);

			}
			sincronizar.notify();
		}
	}
	
	// Função auxiliar para esperar a retomada do processo.
	private void esperarRetomada(Processo processo) { 
		synchronized (processo.getSicronizar()) {
			try {
				while (processo.getSuspenso()) {
					processo.getSicronizar().wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// Executa o escalonador
	public void executar() { 
		while (!filaProcessos.isEmpty()) { // Caso a fila de processos nao esteja vazia
			Processo processoAtual;
			synchronized (sincronizar) {
				processoAtual = filaProcessos.poll();
			}

			if (!processoAtual.getSuspenso()) { // Se o processo atual não estiver com o estado suspenso 
				int tempoRestante = Math.min(quantum, processoAtual.getTempoExecucao());

				processoAtual.setEstado("Em Execução");
				
				// Atualiza a tabela
				SwingUtilities.invokeLater(() -> {
					int rowIndex = processoAtual.getId() - 1;
					tabela.setValueAt("Em Execução", rowIndex, 3);
				});

				try { // Executa pelo tempo e depois pausa
					long startTime = System.currentTimeMillis();
					while ((System.currentTimeMillis() - startTime) < tempoRestante * 1000) {
						if (pausado) {
							synchronized (sincronizar) {
								sincronizar.wait();
							}
						}
					}
				} catch (InterruptedException ex) {
					System.out.println("Thread interrompida durante a espera.");
					Thread.currentThread().interrupt(); // Restabelecer a flag de interrupção
				}

				processoAtual.setTempoExecucao(processoAtual.getTempoExecucao() - tempoRestante);
				// Se o processo não estiver concluido, ou seja, ainda tiver tempo pra executar
				if (processoAtual.getTempoExecucao() > 0) {
					processoAtual.setEstado("Pronto");
					// Atualiza a tabela com "Pronto"
					SwingUtilities.invokeLater(() -> {
						int rowIndex = processoAtual.getId() - 1;
						tabela.setValueAt("Pronto", rowIndex, 3);
						
						synchronized (sincronizar) {
							// Se o processo não for suspenso, adiciona na fila de processos
							if (!processoAtual.getSuspenso())
								filaProcessos.add(processoAtual);
						}
					});
				} else { // Caso contrario esta concluido
					processoAtual.setEstado("Concluído");
					// Atualiza a tabela
					SwingUtilities.invokeLater(() -> {
						int rowIndex = processoAtual.getId() - 1;
						tabela.setValueAt("Concluído", rowIndex, 3);
					});
				}
			} else {// Se o processo estiver suspenso, aguarde até que o usuário o retome							
				esperarRetomada(processoAtual);
			}
		}
	}
}
