package com.zrmiller.zupdate;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame(String[] args) {
        setSize(400, 400);
        JPanel argsPanel = new JPanel(new FlowLayout());
        for (int i = 0; i < args.length; i++) {
            argsPanel.add(new JLabel(args[i]));
        }
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(argsPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

}
