package com.zrmiller.gui;

import com.zrmiller.zupdate.IUpdateProgressListener;

import javax.swing.*;
import java.awt.*;

public class ProgressFrame extends JDialog implements IUpdateProgressListener {

    private final JProgressBar progressBar = new JProgressBar();

    public ProgressFrame() {
        setTitle("ZUpdater");
        Container panel = getContentPane();
        panel.setLayout(new FlowLayout());

        // Progress bar
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        panel.add(progressBar);

        pack();
    }

    @Override
    public void onDownloadProgress(int progressPercent) {
        assert (SwingUtilities.isEventDispatchThread());
        progressBar.setValue(progressPercent);
    }

}
