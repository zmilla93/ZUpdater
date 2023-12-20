package com.zrmiller.gui;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    public MainFrame(String[] args, String version) {
        setSize(400, 400);
        JPanel argsPanel = new JPanel(new FlowLayout());
        argsPanel.add(new JLabel(version));
        for (int i = 0; i < args.length; i++) {
            argsPanel.add(new JLabel(args[i]));
        }
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(argsPanel, BorderLayout.CENTER);
        argsPanel.setBackground(Color.ORANGE);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

}
