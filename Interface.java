import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Interface {
	private static JFrame frame;
	private static DefaultTableModel tabela;
	private static int quantidadeTotal;
	private static int quantidadeSuspensos;
	private static Escalonador escalonador;
	
	// Metodo para iniciar o programa
	public static void main(String[] args) { 
		SwingUtilities.invokeLater(() -> criarInterfaceInicial());
	}

	// Interface inicial(primeira tela), para inserir os dados
	private static void criarInterfaceInicial() { 
		frame = new JFrame("Escalonamento de Processos");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 150);
		frame.setLayout(new GridLayout(3, 2, 5, 5));

		JTextField quantidadeField = new JTextField();
		JTextField quantidadeSuspensosField = new JTextField();
		JButton inserirButton = new JButton("Iniciar Simulação");
		inserirButton.addActionListener(e -> iniciarSimulacao(quantidadeField, quantidadeSuspensosField));

		frame.add(new JLabel("Quantidade de Processos:"));
		frame.add(quantidadeField);
		frame.add(new JLabel("Quantidade de Processos Suspensos:"));
		frame.add(quantidadeSuspensosField);
		frame.add(new JLabel());
		frame.add(inserirButton);

		frame.setVisible(true);
	}

	// Inicia a Simulação a partir dos dados de entrada
	private static void iniciarSimulacao(JTextField quantidadeField, JTextField quantidadeSuspensosField) {
		try {
			quantidadeTotal = Integer.parseInt(quantidadeField.getText());
			quantidadeSuspensos = Integer.parseInt(quantidadeSuspensosField.getText());

			JFrame secundariaFrame = new JFrame("Simulação de Escalonamento");
			secundariaFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			secundariaFrame.setSize(600, 300);
			secundariaFrame.setLayout(new BorderLayout());

			Object[] colunas = { "ID", "Prioridade", "Tempo de Execução", "Estado" };
			tabela = new DefaultTableModel(colunas, 0);

			JTable table = new JTable(tabela);
			table.setDefaultRenderer(Object.class, new Estado());
			JScrollPane scrollPane = new JScrollPane(table);

			JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));

			JButton bloquearButton = new JButton("Suspender Processo");
			bloquearButton.addActionListener(e -> {
				int selectedRow = table.getSelectedRow();
				if (selectedRow != -1) {
					bloquearProcesso(selectedRow);
				} else {
					JOptionPane.showMessageDialog(secundariaFrame, "Selecione um processo para bloquear.", "Aviso",
							JOptionPane.WARNING_MESSAGE);
				}
			});

			JButton retomarButton = new JButton("Retomar Processo");
			retomarButton.addActionListener(e -> {
				int selectedRow = table.getSelectedRow();
				if (selectedRow != -1) {
					retomarProcesso(selectedRow);
				} else {
					JOptionPane.showMessageDialog(secundariaFrame, "Selecione um processo para retomar.", "Aviso",
							JOptionPane.WARNING_MESSAGE);
				}
			});

			buttonPanel.add(bloquearButton);
			buttonPanel.add(retomarButton);

			secundariaFrame.add(scrollPane, BorderLayout.CENTER);
			secundariaFrame.add(buttonPanel, BorderLayout.SOUTH);

			secundariaFrame.setVisible(true);

			escalonador = new Escalonador(2, tabela);
			adicionarProcessosTabela(escalonador);

			// Usando SwingWorker para executar o escalonador em uma thread separada
			SwingWorker<Void, Void> worker = new SwingWorker<>() {
				@Override
				protected Void doInBackground() {
					escalonador.executar();
					return null;
				}
			};

			// Atualização periódica da GUI
			Timer timer = new Timer(1000, e -> {
				// Verifica se a fila de processos está vazia
				if (escalonador.filaProcessosVazia()) {
					worker.cancel(true); // Cancela a execução do SwingWorker se a fila de processos estiver vazia
				}

				// Atualiza a interface gráfica aqui usando SwingUtilities.invokeLater()
				SwingUtilities.invokeLater(() -> {
					for (Processo processo : escalonador.getFilaProcessos()) {
						int rowIndex = processo.getId() - 1;
						tabela.setValueAt(processo.getEstado(), rowIndex, 3);
					}
				});
			});
			timer.start();

			// Executa o SwingWorker em uma thread separada
			worker.execute();

			frame.dispose();
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Digite um número válido para a quantidade de processos.", "Erro",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	// Bloqueia um processo com base no índice da linha da tabela.
	private static void bloquearProcesso(int rowIndex) {
		if (rowIndex != -1) {
			escalonador.bloquearProcesso(rowIndex);
		}
	}
	
	// Retoma um processo suspenso com base no índice da linha da tabela.
	private static void retomarProcesso(int rowIndex) {
		if (rowIndex != -1) {
			escalonador.retomarProcesso(rowIndex);
		}
	}
	
	// Adiciona processo a tabela
	private static void adicionarProcessosTabela(Escalonador escalonador) {
		List<Processo> processosSuspensos = new ArrayList<>();
		int quantidadeExecutavel = quantidadeTotal - quantidadeSuspensos;

		for (int i = 1; i <= quantidadeTotal; i++) {
			Processo processo = new Processo(i, tabela, escalonador);

			if (i <= quantidadeExecutavel) {
				processo.setEstado("Pronto");
				escalonador.adicionarProcesso(processo);
			} else {
				processo.setEstado("Suspenso");
				processosSuspensos.add(processo);
			}

		}
		for (Processo processoSuspenso : processosSuspensos) {
			// if (!escalonador.getFilaProcessos().contains(processoSuspenso))
			escalonador.adicionarProcessoSuspenso(processoSuspenso);
		}
	}
}