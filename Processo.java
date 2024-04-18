import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

class Processo implements Runnable {
	private int id; // Id do processo
	private int prioridade; // Prioridade do processo
	private int tempoExecucao; // Tempo de execução do processo
	private String estado; // Estado em que se encontra o processo
	private boolean suspenso = false; // Variavel para estado suspenso
	private int tempoRestanteBloqueado = 0; // Variavel para o tempo restante para desbloquear
	private final Object sincronizar = new Object();// Variavel para sincronizar o acesso dos processos nos metodos
	private DefaultTableModel tabela; // Tabela para fazer a atualização grafica
	private Escalonador escalonador; // Classe escalonador que vai fazer a parte de escalonamento

	// Construtor da classe Processo
	public Processo(int id, DefaultTableModel tabela, Escalonador escalonador) {
		this.id = id;
		this.prioridade = (int) (Math.random() * 5) + 1; // vai gerar valores ate 5 para a prioridade
		this.tempoExecucao = (int) (Math.random() * 7) + 1; // vai gerar valores ate 5 para o tempo de execucao
		this.estado = "Pronto";
		this.tabela = tabela;
		this.escalonador = escalonador;
	}

	// Retorna o id do processo
	public int getId() { 
		atualizaTabela();
		return id;
	}

	// Retorna a prioridade do processo
	public int getPrioridade() { 
		return prioridade;
	}
	
	// Retorna o tempo de execução do processo
	public int getTempoExecucao() { 
		return tempoExecucao;
	}
	
	// Atualiza o tempo de execuçao do processo
	public void setTempoExecucao(int tempoExecucao) { 
		synchronized (sincronizar) {
			this.tempoExecucao = tempoExecucao;
		}
		atualizaTabela();
	}

	// Retorna o estado atual do processo
	public String getEstado() { 
		synchronized (sincronizar) {
			return estado;
		}
	}

	// Atualiza o estado do processo e notifica a GUI.
	public void setEstado(String estado) { 
		synchronized (sincronizar) {
			this.estado = estado;
		}
		atualizaTabela();
	}
	
	// Retorna true se o processo estiver suspenso, caso contrário, retorna false.
	public boolean getSuspenso() { 
		return suspenso;
	}

	// Atualiza o estado de suspensão do processo.
	public void setSuspender(boolean suspenso) { 
		synchronized (sincronizar) {
			this.suspenso = suspenso;
		}
		atualizaTabela();
	}

	// Metodo para colocar o processo em estado de suspensão
	public void suspender() { 
		synchronized (sincronizar) {
			suspenso = true;
			tempoRestanteBloqueado = getTempoExecucao();
		}
	}
	
	// Retorna o tempo que ainda falta para ele executar
	public int getTempoRestanteBloqueado() {
		return tempoRestanteBloqueado;
	}
	
	// Atualiza o tempo que ainda falta para ele executar
	public void setTempoRestanteBloqueado(int tempoRestanteBloqueado) { 
		synchronized (sincronizar) {
			this.tempoRestanteBloqueado = tempoRestanteBloqueado;
		}
	}

	// Retorna o status da sicronização do processo
	public Object getSicronizar() { 
		return sincronizar;
	}

	// Encontra o indice do processo na tabela
	private int encontraIndiceNaTabela() { 
		for (int i = 0; i < tabela.getRowCount(); i++) {
			if ((int) tabela.getValueAt(i, 0) == id) {
				return i;
			}
		}
		return -1;
	}

	// Metodo para retomar a execução do processo
	public boolean retomar() { 
		synchronized (sincronizar) {
			suspenso = false;
			setEstado("Pronto");
			sincronizar.notify();
		}

		atualizaTabela();
		return true;
	}

	// Função auxiliar esperar a retomada do processo
	private void esperarRetomada() { 
		synchronized (sincronizar) {
			try {
				while (suspenso) {
					sincronizar.wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	// Função auxiliar para atualizar a tabela
	private void atualizaTabela() { 
		SwingUtilities.invokeLater(() -> {
			int rowIndex = encontraIndiceNaTabela();
			if (rowIndex != -1) {
				tabela.fireTableRowsUpdated(rowIndex, rowIndex);
			}
		});
	}

	// Metodo para verificar se o processo esta bloqueado inicialmente
	public boolean isBloqueadoInicialmente() { 
		return suspenso && tempoRestanteBloqueado == tempoExecucao;
	}
	
	@Override
	// Metodo principal da execução, que vai atualizando os tempo de execução e quanto falta para terminar	
	public void run() { 
		esperarRetomada();
		int tempoTotalExecucao = tempoExecucao;
		int tempoExecutado = 0;

		while (tempoExecutado < tempoTotalExecucao) {
			int tempoRestante;

			if (suspenso) {
				tempoRestante = getTempoRestanteBloqueado();
			} else {
				tempoRestante = Math.min(escalonador.getQuantum(), tempoTotalExecucao - tempoExecutado);
			}

			try {
				Thread.sleep(tempoRestante * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			tempoExecutado += tempoRestante;

			synchronized (sincronizar) {
				// Atualiza o estado do processo dependendo o tempo de execução que ainda falta para executar
				setTempoExecucao(tempoTotalExecucao - tempoExecutado);
				if (getTempoExecucao() > 0 && !suspenso) {
					setEstado("Pronto");
				} else if (suspenso) {
					esperarRetomada();
				} else {
					setEstado("Concluído");
				}
			}

		}
	}
}