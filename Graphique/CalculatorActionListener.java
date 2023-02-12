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
            double n1 = Double.parseDouble(num1.getText());
            double n2 = 0;
            if (!operation.equals("√")) {
                n2 = Double.parseDouble(num2.getText());
            } else {
                result.setText(String.format("%.2f", Math.sqrt(n1)));
            }

            if (n2 == 0 && operation.equals("/")) {
                result.setText("Division par zéro");
            } else {
                switch (operation) {
                    case "+":
                        result.setText(String.format("%.2f", n1 + n2));
                        break;
                    case "-":
                        result.setText(String.format("%.2f", n1 - n2));
                        break;
                    case "*":
                        result.setText(String.format("%.2f", n1 * n2));
                        break;
                    case "/":
                        result.setText(String.format("%.2f", n1 / n2));
                        break;
                    case "^":
                        result.setText(String.format("%.2f", Math.pow(n1, n2)));
                        break;
                }
            }
        } catch (NumberFormatException ex) {
            result.setText("Entrée non valide");
        }
    }
}
