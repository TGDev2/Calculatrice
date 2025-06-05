import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JTextArea;

public class CalculatorActionListener implements ActionListener {
    private JTextField num1, num2;
    private JLabel result;
    private JTextArea history;
    private String operation;

    public CalculatorActionListener(JTextField num1, JTextField num2, JLabel result, JTextArea history, String operation) {
        this.num1 = num1;
        this.num2 = num2;
        this.result = result;
        this.history = history;
        this.operation = operation;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            double n1 = Double.parseDouble(num1.getText());
            double n2 = 0;
            if (!operation.equals("√")) {
                n2 = Double.parseDouble(num2.getText());
            }

            String textResult;
            if (operation.equals("√")) {
                if (n1 < 0) {
                    textResult = "Racine négative";
                } else {
                    double res = Math.sqrt(n1);
                    textResult = String.format("%.2f", res);
                    history.append(String.format("√(%.2f) = %s%n", n1, textResult));
                }
            } else if (operation.equals("/") && n2 == 0) {
                textResult = "Division par zéro";
            } else {
                double res = 0;
                switch (operation) {
                    case "+":
                        res = n1 + n2;
                        break;
                    case "-":
                        res = n1 - n2;
                        break;
                    case "*":
                        res = n1 * n2;
                        break;
                    case "/":
                        res = n1 / n2;
                        break;
                    case "^":
                        res = Math.pow(n1, n2);
                        break;
                }
                textResult = String.format("%.2f", res);
                history.append(String.format("%.2f %s %.2f = %s%n", n1, operation, n2, textResult));
            }

            result.setText(textResult);
        } catch (NumberFormatException ex) {
            result.setText("Entrée non valide");
        }
    }
}
