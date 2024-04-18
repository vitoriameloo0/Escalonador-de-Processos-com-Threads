import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class Estado extends DefaultTableCellRenderer {
	// Metodo para personalizar as cores da tabela
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		String estado = (String) table.getModel().getValueAt(row, 3);

		switch (estado) {
		case "Pronto":
			cell.setBackground(Color.CYAN);
			break;
		case "Em Execução":
			cell.setBackground(Color.ORANGE);
			break;
		case "Concluído":
			cell.setBackground(Color.GREEN);
			break;
		case "Suspenso":
			cell.setBackground(Color.RED);
			break;
		default:
			cell.setBackground(table.getBackground());
			break;
		}

		return cell;
	}
}
