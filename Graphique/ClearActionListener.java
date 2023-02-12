import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JTextField;
import javax.swing.JLabel;

public class ClearActionListener implements ActionListener {
    private JTextField num1, num2;
    private JLabel result;

    public ClearActionListener(JTextField num1, JTextField num2, JLabel result) {
        this.num1 = num1;
        this.num2 = num2;
        this.result = result;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        num1.setText("");
        num2.setText("");
        result.setText("");
    }
}
