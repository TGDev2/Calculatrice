import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class CalculatorActionListener implements ActionListener {
    private JTextField num1, num2;
    private JLabel result;
    private String operation;

    public CalculatorActionListener(JTextField num1, JTextField num2, JLabel result, String operation) {
        this.num1 = num1;
        this.num2 = num2;
        this.result = result;
        this.operation = operation;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            int n1 = Integer.parseInt(num1.getText());
            int n2 = Integer.parseInt(num2.getText());
            if (operation.equals("√")) {
                result.setText(String.valueOf(Math.sqrt(n1)));
            } else if (n2 == 0 && operation.equals("/")) {
                result.setText("Division par zero");
            } else {
                switch (operation) {
                    case "+":
                        result.setText(String.valueOf(n1 + n2));
                        break;
                    case "-":
                        result.setText(String.valueOf(n1 - n2));
                        break;
                    case "*":
                        result.setText(String.valueOf(n1 * n2));
                        break;
                    case "/":
                        result.setText(String.valueOf(n1 / n2));
                        break;
                    case "^":
                        result.setText(String.valueOf(Math.pow(n1, n2)));
                        break;
                }
            }
        } catch (NumberFormatException ex) {
            result.setText("Entrée non valide");
        }
    }

}
