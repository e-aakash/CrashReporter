/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.crashreporter.pages;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.Callable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.terasology.crashreporter.I18N;
import org.terasology.crashreporter.Resources;
import org.terasology.crashreporter.Supplier;

/**
 * The panel where the log file content is uploaded to some web storage
 * @author Martin Steiger
 */
public class UploadPanel extends JPanel {

    private static final long serialVersionUID = -8247883237201535146L;

    private JButton uploadPasteBinButton;
    private JButton uploadHostedButton;
    private boolean isComplete;
    private URL uploadURL;

    private JLabel statusLabel;

    private Supplier<String> textSupplier;

    private JButton uploadSkipButton;

    public UploadPanel(Supplier<String> supplier) {

        this.textSupplier = supplier;
        setLayout(new BorderLayout(50, 20));
        statusLabel = new JLabel(I18N.getMessage("noUpload"), SwingConstants.RIGHT);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        statusLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
        String title = "<html><h3>" + I18N.getMessage("uploadLog2") + "</h></html>";
        JLabel titleLabel = new JLabel(title, Resources.loadIcon("icons/Arrow-up-icon.png"), SwingConstants.CENTER);
        titleLabel.setBorder(new EmptyBorder(10, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        Font buttonFont = getFont().deriveFont(Font.BOLD).deriveFont(14f);

        JPanel hosterPanel = new JPanel(new GridLayout(0, 1, 0, 20));
        hosterPanel.setBorder(new EmptyBorder(0, 50, 0, 50));
        uploadPasteBinButton = new JButton("PasteBin", Resources.loadIcon("icons/pastebin.png"));
        uploadPasteBinButton.setFont(buttonFont);
        uploadPasteBinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText(I18N.getMessage("waitForUpload"));
                uploadPasteBinButton.setEnabled(false);
                uploadHostedButton.setEnabled(false);

                String text = textSupplier.get();
                upload(new PastebinUploadRunnable(text));
            }
        });
        hosterPanel.add(uploadPasteBinButton);

        uploadHostedButton = new JButton("Terasology Servers", Resources.loadIcon("icons/starry-gooey.png"));

        // ------- ENABLE when Server is ready -----
        uploadHostedButton.setVisible(false);
        // ------- ENABLE when Server is ready -----

        uploadHostedButton.setFont(buttonFont);
        uploadHostedButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                statusLabel.setText(I18N.getMessage("waitForUpload"));
                uploadHostedButton.setEnabled(false);
                uploadPasteBinButton.setEnabled(false);

                String text = textSupplier.get();
//                URI host = URI.create("http://terasology.org/uploadLog");
                URI host = URI.create("http://localhost:8080/uploadFile");
                upload(new HostedUploadRunnable(host, text));
            }
        });
        hosterPanel.add(uploadHostedButton);

        uploadSkipButton = new JButton(I18N.getMessage("skipUpload"), Resources.loadIcon("icons/Actions-edit-delete-icon.png"));
        uploadSkipButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                uploadSkipButton.setEnabled(false);
                UploadPanel.this.firePropertyChange("pageComplete", Boolean.FALSE, Boolean.TRUE);
                isComplete = true;
            }
        });
        uploadSkipButton.setFont(buttonFont);
        hosterPanel.add(uploadSkipButton);

        add(hosterPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);

        if (!aFlag) {
            return;
        }

        firePropertyChange("pageComplete", !isComplete, isComplete);

//        uploadPasteBinButton.setEnabled(canUpload);
    }

    /**
     * @return the URL of the log file that was uploaded or <code>null</code>
     */
    public URL getUploadedFileURL() {
        return uploadURL;
    }

    private void upload(Callable<URL> callable) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try {
                    URL link = callable.call();
                    uploadSuccess(link);
                } catch (Exception e) {
                    uploadFailed(e);
                }
            }
        };

        Thread thread = new Thread(runnable, "Upload");
        thread.start();
    }

    private void updateStatus() {
        if (uploadURL != null) {
            String uploadText = I18N.getMessage("uploadComplete");
            statusLabel.setText(String.format("<html>%s <a href=\"%s\">%s</a></html>", uploadText, uploadURL, uploadURL));
            statusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            statusLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    openInBrowser(uploadURL.toString());
                }
            });
        } else {
            statusLabel.setText(I18N.getMessage("noUpload"));
        }
    }

    private void uploadSuccess(URL link) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                uploadURL = link;
                updateStatus();
                uploadSkipButton.setEnabled(false);
                uploadHostedButton.setEnabled(true);
                uploadPasteBinButton.setEnabled(true);
                firePropertyChange("pageComplete", Boolean.FALSE, Boolean.TRUE);
                isComplete = true;
            }
        });
    }

    private void uploadFailed(Exception e) {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                String uploadFailed = I18N.getMessage("uploadFailed");
                JOptionPane.showMessageDialog(null, e.getLocalizedMessage(), uploadFailed, JOptionPane.ERROR_MESSAGE);
                uploadHostedButton.setEnabled(true);
                uploadPasteBinButton.setEnabled(true);
                updateStatus();
            }
        });
    }

    private static void openInBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();

            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(new URI(url));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace(System.err);
                }
            }
        }
    }

}
