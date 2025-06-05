import Calcul.Calculator;
import javax.swing.JFrame;

public class Main {
        public static void main(String[] args) {
                Calculator calculator = new Calculator();
                calculator.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                calculator.setSize(700, 300);
                calculator.setVisible(true);

                calculator.addition.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "+"));
                calculator.soustraction.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "-"));
                calculator.multiplication.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "*"));
                calculator.division.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "/"));
                calculator.racine.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "√"));
                calculator.puissance.addActionListener(
                                new CalculatorActionListener(calculator.num1, calculator.num2, calculator.result, calculator.history, "^"));
                calculator.effacer.addActionListener(
                                new ClearActionListener(calculator.num1, calculator.num2, calculator.result));
                calculator.effacerHistorique.addActionListener(new ClearHistoryActionListener(calculator.history));
        }
}
