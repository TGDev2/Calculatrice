package Calcul;

import java.awt.FlowLayout;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JLabel;

public class Calculator extends JFrame {
    public JTextField num1, num2;
    public JButton addition, soustraction, multiplication, division, racine, puissance, effacer;
    public JLabel result;

    public Calculator() {
        setLayout(new FlowLayout());
        num1 = new JTextField(10);
        num2 = new JTextField(10);
        addition = new JButton("+");
        soustraction = new JButton("-");
        multiplication = new JButton("*");
        division = new JButton("/");
        racine = new JButton("√");
        puissance = new JButton("^");
        result = new JLabel();
        effacer = new JButton("Effacer");
        add(num1);
        add(num2);
        add(addition);
        add(soustraction);
        add(multiplication);
        add(division);
        add(racine);
        add(puissance);
        add(result);
        add(effacer);
    }
}
