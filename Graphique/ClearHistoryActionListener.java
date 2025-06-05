import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextArea;

public class ClearHistoryActionListener implements ActionListener {
    private JTextArea history;

    public ClearHistoryActionListener(JTextArea history) {
        this.history = history;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        history.setText("");
    }
}
