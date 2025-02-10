package main;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JViewport;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.imgscalr.Scalr;

/**
 *
 * @author Claudio Santos
 */
public class Gui extends javax.swing.JFrame {

    private final UndoRedo undoRedo = new UndoRedo();
    private MyImage myImage = new MyImage();

    public Gui() {
        initComponents();
        setSize(800, 500);
        setLocationRelativeTo(null);

        diaAbout.pack();
        diaSaveAs.pack();
        diaOpenBase64.pack();

        diaAbout.setSize(300, diaAbout.getHeight() + 40);
        diaSaveAs.setSize(660, diaSaveAs.getHeight() + 20);
        diaOpenBase64.setSize(800, 500);

        KeyStroke ctrlz = KeyStroke
                .getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK);

        KeyStroke ctrly = KeyStroke
                .getKeyStroke(KeyEvent.VK_Y, KeyEvent.CTRL_DOWN_MASK);

        KeyStroke ctrlc = KeyStroke
                .getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK);

        KeyStroke ctrlv = KeyStroke
                .getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK);

        KeyStroke shiftinsert = KeyStroke
                .getKeyStroke(KeyEvent.VK_INSERT, KeyEvent.SHIFT_DOWN_MASK);

        panImage.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ctrlz, "undo");

        panImage.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ctrly, "redo");

        panImage.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ctrlc, "copy");

        panImage.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(ctrlv, "paste");

        panImage.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(shiftinsert, "paste");

        panImage.getActionMap().put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        panImage.getActionMap().put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        panImage.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imageCopy();
            }
        });

        panImage.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                imagePaste();
            }
        });

        imageDrop();

        scrImage.getVerticalScrollBar().setUnitIncrement(16);

        imageDrag();

        jEditorPane1.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }
            }
        });
    }

    private void showPopup(String message) {
        SwingUtilities.invokeLater(() -> {
            showPopupAux(message, 5);
        });
    }

    private void showPopupAux(String message, int seconds) {
        JWindow popup = new JWindow(this);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        popup.getContentPane().add(label);
        popup.pack();
        popup.setLocationRelativeTo(this);

        Timer timer = new Timer(seconds * 1000, (ActionEvent e) -> {
            popup.dispose();
        });
        timer.setRepeats(false);
        timer.start();

        popup.setVisible(true);
    }

    private void setImage(MyImage image) {
        if (image == null) {
            return;
        }
        myImage = image;
        texCropW.setText(String.valueOf(image.getImage().getWidth()));
        texCropH.setText(String.valueOf(image.getImage().getHeight()));
        texResizeW.setText(String.valueOf(image.getImage().getWidth()));
        texResizeH.setText(String.valueOf(image.getImage().getHeight()));
        texImagePath.setText(image.getPath());
        texImageW.setText(String.valueOf(image.getImage().getWidth()));
        texImageH.setText(String.valueOf(image.getImage().getHeight()));
        labImage.setIcon(new ImageIcon(image.getImage()));
    }

    private void imageCopy() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(myImage.getImage(), "png", baos);
        } catch (IOException e) {
            System.out.println(e);
        }
        byte[] bs = baos.toByteArray();

        Transferable transferable = new Transferable() {
            @Override
            public DataFlavor[] getTransferDataFlavors() {
                return new DataFlavor[]{DataFlavor.imageFlavor};
            }

            @Override
            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return DataFlavor.imageFlavor.equals(flavor);
            }

            @Override
            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
                if (isDataFlavorSupported(flavor)) {
                    return ImageIO.read(new ByteArrayInputStream(bs));
                }
                return null;
            }
        };

        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(transferable, null);
        showPopup("Image copied.");

        try {
            baos.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private void imagePaste() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable transferable = clipboard.getContents(null);
        if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                setImage(new MyImage((BufferedImage) clipboard.getData(DataFlavor.imageFlavor), "", null));
                undoRedo.action(myImage);
            } catch (UnsupportedFlavorException | IOException e) {
                System.out.println(e);
            }
        } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                @SuppressWarnings("unchecked")
                List<File> files = (List<File>) clipboard.getData(DataFlavor.javaFileListFlavor);
                for (File f : files) {
                    setImage(new MyImage(ImageIO.read(f), f.getAbsolutePath(), f));
                    undoRedo.action(myImage);
                    break;
                }
            } catch (UnsupportedFlavorException | IOException e) {
                System.out.println(e);
                showPopup("Failed to paste image.");
            }
        }
    }

    private void imageDrop() {
        panImage.setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferHandler.TransferSupport support) {
                for (DataFlavor flavor : support.getDataFlavors()) {
                    if (flavor.isFlavorJavaFileListType()) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean importData(TransferHandler.TransferSupport support) {
                if (!this.canImport(support)) {
                    return false;
                }
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        setImage(new MyImage(ImageIO.read(f), f.getAbsolutePath(), f));
                        undoRedo.action(myImage);
                        break;
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    System.out.println(e);
                    showPopup("Failed to drop image.");
                    return false;
                }
                return true;
            }
        });
    }

    private void imageDrag() {
        Point last = new Point();
        labImage.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                labImage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                last.setLocation(e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                labImage.setCursor(null);
            }
        });
        labImage.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point epos = e.getPoint();
                JViewport view = scrImage.getViewport();
                Point vpos = view.getViewPosition();
                int maxvposx = labImage.getWidth() - view.getWidth();
                int maxvposy = labImage.getHeight() - view.getHeight();

                if (labImage.getWidth() > view.getWidth()) {
                    vpos.x -= epos.x - last.x;
                    if (vpos.x < 0) {
                        vpos.x = 0;
                        last.x = epos.x;
                    }
                    if (vpos.x > maxvposx) {
                        vpos.x = maxvposx;
                        last.x = epos.x;
                    }
                }

                if (labImage.getHeight() > view.getHeight()) {
                    vpos.y -= epos.y - last.y;
                    if (vpos.y < 0) {
                        vpos.y = 0;
                        last.y = epos.y;
                    }
                    if (vpos.y > maxvposy) {
                        vpos.y = maxvposy;
                        last.y = epos.y;
                    }
                }

                view.setViewPosition(vpos);
            }
        });
    }

    private BufferedImage getBufferedImageJpg() {
        BufferedImage bi = myImage.getImage();
        if (bi.getColorModel().hasAlpha()) {
            bi = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(myImage.getImage(), 0, 0, null);
            g2d.dispose();
        }
        return bi;
    }

    private void saveJpg(String filename, float quality) throws IOException {
        ImageWriter iw = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam iwp = iw.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.setCompressionQuality(quality);

        ImageOutputStream ios = ImageIO.createImageOutputStream(new File(filename));
        iw.setOutput(ios);
        iw.write(null, new IIOImage(getBufferedImageJpg(), null, null), iwp);

        ios.close();
        iw.dispose();
    }

    private void savePng(String filename) throws IOException {
        ImageIO.write(myImage.getImage(), "png", new File(filename));
    }

    private void undo() {
        MyImage bi = undoRedo.undo();
        if (bi != null) {
            setImage(bi);
        }
    }

    private void redo() {
        MyImage bi = undoRedo.redo();
        if (bi != null) {
            setImage(bi);
        }
    }

    private void save(String file, int quality) {
        try {
            if (new File(file).exists()) {
                //exists
            }
            if (file.endsWith("jpg")) {
                saveJpg(file, (float) quality / 100f);
            } else {
                savePng(file);
            }
            showPopup("File saved: " + file);
            if (myImage.isPathBlank()) {
                Desktop.getDesktop().open(new File(new File(file).getAbsoluteFile().getParent()));
            }
        } catch (IOException e) {
            System.out.println(e);
            showPopup("Failed to save file: " + file);
        }
    }

    private void actionOpen() {
        if (filOpen.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = filOpen.getSelectedFile();
            try {
                setImage(new MyImage(ImageIO.read(f), f.getAbsolutePath(), f));
                undoRedo.action(myImage);
            } catch (Exception e) {
                System.out.println(e);
                showPopup("Failed to open image.");
            }
        }
    }

    private void actionOpenBase64() {
        texOpenBase64String.setText("");
        diaOpenBase64.setLocationRelativeTo(this);
        diaOpenBase64.setVisible(true);
    }

    private void actionSaveNew() {
        String file = System.currentTimeMillis() + ".jpg";
        if (myImage.getImage() == null) {
            showPopup("Image is empty.");
            return;
        }
        if (!myImage.isPathBlank()) {
            file = new File(myImage.getPath()).getParent() + "\\" + file;
        }
        save(file, 95);
    }

    private void actionSaveAs() {
        if (myImage.getImage() == null) {
            showPopup("Image is empty.");
            return;
        }
        texSaveFilename.setText(myImage.getPath());
        diaSaveAs.setLocationRelativeTo(this);
        diaSaveAs.setVisible(true);
    }

    private void actionExit() {
        System.exit(0);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        groCrop = new javax.swing.ButtonGroup();
        groResize = new javax.swing.ButtonGroup();
        diaAbout = new javax.swing.JDialog(this, true);
        jPanel14 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jEditorPane1 = new javax.swing.JEditorPane();
        jPanel15 = new javax.swing.JPanel();
        butDiaAboutBack = new javax.swing.JButton();
        diaSaveAs = new javax.swing.JDialog(this, true);
        jPanel13 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        texSaveQuality = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        texSaveFilename = new javax.swing.JTextField();
        butSaveBrowse = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        butSaveSave = new javax.swing.JButton();
        butSaveCancel = new javax.swing.JButton();
        diaOpenBase64 = new javax.swing.JDialog(this, true);
        jPanel16 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        texOpenBase64String = new javax.swing.JTextArea();
        jPanel18 = new javax.swing.JPanel();
        butOpenBase64Open = new javax.swing.JButton();
        butOpenBase64Cancel = new javax.swing.JButton();
        filOpen = new javax.swing.JFileChooser();
        filSaveAs = new javax.swing.JFileChooser();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        butOpen = new javax.swing.JButton();
        butSaveNew = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        butSaveAs = new javax.swing.JButton();
        butUndo = new javax.swing.JButton();
        butRedo = new javax.swing.JButton();
        butAbout = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        pancrop = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        cheCropTL = new javax.swing.JCheckBox();
        cheCropT = new javax.swing.JCheckBox();
        cheCropTR = new javax.swing.JCheckBox();
        cheCropL = new javax.swing.JCheckBox();
        cheCropM = new javax.swing.JCheckBox();
        cheCropR = new javax.swing.JCheckBox();
        cheCropBL = new javax.swing.JCheckBox();
        cheCropB = new javax.swing.JCheckBox();
        cheCropBR = new javax.swing.JCheckBox();
        jPanel6 = new javax.swing.JPanel();
        texCropW = new javax.swing.JFormattedTextField();
        texCropH = new javax.swing.JFormattedTextField();
        jPanel7 = new javax.swing.JPanel();
        butCropApply = new javax.swing.JButton();
        panresize = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        radResizeBoth = new javax.swing.JRadioButton();
        radResizeWidth = new javax.swing.JRadioButton();
        radResizeHeight = new javax.swing.JRadioButton();
        radResizeExact = new javax.swing.JRadioButton();
        jPanel10 = new javax.swing.JPanel();
        texResizeW = new javax.swing.JFormattedTextField();
        texResizeH = new javax.swing.JFormattedTextField();
        jPanel8 = new javax.swing.JPanel();
        butResizeApply = new javax.swing.JButton();
        scrImage = new javax.swing.JScrollPane();
        panImage = new javax.swing.JPanel();
        labImage = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        texImageW = new javax.swing.JTextField();
        texImageH = new javax.swing.JTextField();
        texImagePath = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        menOpen = new javax.swing.JMenuItem();
        menOpenBase64 = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        menSaveNew = new javax.swing.JMenuItem();
        menSaveAs = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        menExit = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        menUndo = new javax.swing.JMenuItem();
        menRedo = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        menAbout = new javax.swing.JMenuItem();

        diaAbout.setTitle("About");

        jPanel14.setLayout(new java.awt.BorderLayout(0, 6));

        jScrollPane1.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jEditorPane1.setEditable(false);
        jEditorPane1.setContentType("text/html"); // NOI18N
        jEditorPane1.setText("<html><body>\n<h2>ImageTools - v1.1</h2>\n<p>A straightforward and user-friendly program for easy image manipulation. You can directly drop or paste an image to open it.</p>\n<p><a href=\"https://github.com/claudio-santos/ImageTools\">https://github.com/claudio-santos/ImageTools</a></p>\n<p>Third-Party:</p>\n<p><a href=\"https://github.com/rkalla/imgscalr\">imgscalr - Java Image-Scaling Library</a></p>\n<p><a href=\"https://haraldk.github.io/TwelveMonkeys\">TwelveMonkeys ImageIO</a></p>\n<p><a href=\"https://github.com/KDE/oxygen-icons\">Oxygen Icons</a></p>\n</body></html>");
        jScrollPane1.setViewportView(jEditorPane1);

        jPanel14.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        butDiaAboutBack.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butDiaAboutBack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/go-previous.png"))); // NOI18N
        butDiaAboutBack.setText("Back");
        butDiaAboutBack.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butDiaAboutBackActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(butDiaAboutBack)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(butDiaAboutBack)
                .addContainerGap())
        );

        jPanel14.add(jPanel15, java.awt.BorderLayout.PAGE_END);

        javax.swing.GroupLayout diaAboutLayout = new javax.swing.GroupLayout(diaAbout.getContentPane());
        diaAbout.getContentPane().setLayout(diaAboutLayout);
        diaAboutLayout.setHorizontalGroup(
            diaAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(diaAboutLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, 288, Short.MAX_VALUE)
                .addContainerGap())
        );
        diaAboutLayout.setVerticalGroup(
            diaAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, diaAboutLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        diaSaveAs.setTitle("Save As");

        jPanel13.setLayout(new java.awt.BorderLayout());

        texSaveQuality.setText("95");

        jLabel2.setText("File Name");

        jLabel3.setText("JPEG Quality");

        butSaveBrowse.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butSaveBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/edit-find.png"))); // NOI18N
        butSaveBrowse.setText("Browse");
        butSaveBrowse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveBrowseActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(texSaveFilename)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(butSaveBrowse))
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(texSaveQuality, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel11Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel3, texSaveQuality});

        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(texSaveQuality, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(texSaveFilename, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(butSaveBrowse))
                .addContainerGap())
        );

        jPanel13.add(jPanel11, java.awt.BorderLayout.CENTER);

        butSaveSave.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butSaveSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/dialog-ok-apply.png"))); // NOI18N
        butSaveSave.setText("Save");
        butSaveSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveSaveActionPerformed(evt);
            }
        });

        butSaveCancel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butSaveCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/edit-delete.png"))); // NOI18N
        butSaveCancel.setText("Cancel");
        butSaveCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(butSaveSave)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(butSaveCancel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel12Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butSaveCancel, butSaveSave});

        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(butSaveSave)
                    .addComponent(butSaveCancel))
                .addContainerGap())
        );

        jPanel13.add(jPanel12, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout diaSaveAsLayout = new javax.swing.GroupLayout(diaSaveAs.getContentPane());
        diaSaveAs.getContentPane().setLayout(diaSaveAsLayout);
        diaSaveAsLayout.setHorizontalGroup(
            diaSaveAsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(diaSaveAsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        diaSaveAsLayout.setVerticalGroup(
            diaSaveAsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, diaSaveAsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        diaOpenBase64.setTitle("Save As");

        jPanel16.setLayout(new java.awt.BorderLayout());

        jLabel4.setText("Base64 String");

        texOpenBase64String.setColumns(20);
        texOpenBase64String.setLineWrap(true);
        texOpenBase64String.setRows(5);
        jScrollPane2.setViewportView(texOpenBase64String);

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 776, Short.MAX_VALUE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel16.add(jPanel17, java.awt.BorderLayout.CENTER);

        butOpenBase64Open.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butOpenBase64Open.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/dialog-ok-apply.png"))); // NOI18N
        butOpenBase64Open.setText("Open");
        butOpenBase64Open.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butOpenBase64OpenActionPerformed(evt);
            }
        });

        butOpenBase64Cancel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butOpenBase64Cancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/edit-delete.png"))); // NOI18N
        butOpenBase64Cancel.setText("Cancel");
        butOpenBase64Cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butOpenBase64CancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(butOpenBase64Open)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(butOpenBase64Cancel)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(butOpenBase64Open)
                    .addComponent(butOpenBase64Cancel))
                .addContainerGap())
        );

        jPanel16.add(jPanel18, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout diaOpenBase64Layout = new javax.swing.GroupLayout(diaOpenBase64.getContentPane());
        diaOpenBase64.getContentPane().setLayout(diaOpenBase64Layout);
        diaOpenBase64Layout.setHorizontalGroup(
            diaOpenBase64Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(diaOpenBase64Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        diaOpenBase64Layout.setVerticalGroup(
            diaOpenBase64Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, diaOpenBase64Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel16, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        filOpen.setFileFilter(new FileNameExtensionFilter("Imagens", "bmp", "gif", "jpeg", "jpg", "png"));

        filSaveAs.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        filSaveAs.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setIconImage(Main.icon);

        jPanel1.setLayout(new java.awt.BorderLayout(0, 6));

        butOpen.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/document-open.png"))); // NOI18N
        butOpen.setText("Open");
        butOpen.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        butOpen.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        butOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butOpenActionPerformed(evt);
            }
        });

        butSaveNew.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butSaveNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/document-save.png"))); // NOI18N
        butSaveNew.setText("Save New");
        butSaveNew.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        butSaveNew.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        butSaveNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveNewActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 3, 14)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Image Tools");

        butSaveAs.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/document-save-as.png"))); // NOI18N
        butSaveAs.setText("Save As");
        butSaveAs.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        butSaveAs.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        butSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butSaveAsActionPerformed(evt);
            }
        });

        butUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/edit-undo.png"))); // NOI18N
        butUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butUndoActionPerformed(evt);
            }
        });

        butRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/edit-redo.png"))); // NOI18N
        butRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butRedoActionPerformed(evt);
            }
        });

        butAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/help-about.png"))); // NOI18N
        butAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butAboutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(butAbout)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(butUndo)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(butRedo))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(butOpen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(butSaveNew)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(butSaveAs)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {butOpen, butSaveAs, butSaveNew});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(butAbout)
                    .addComponent(butUndo)
                    .addComponent(butRedo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(butSaveAs)
                    .addComponent(butOpen)
                    .addComponent(butSaveNew))
                .addContainerGap())
        );

        jTabbedPane1.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N

        pancrop.setLayout(new javax.swing.BoxLayout(pancrop, javax.swing.BoxLayout.LINE_AXIS));

        groCrop.add(cheCropTL);

        groCrop.add(cheCropT);

        groCrop.add(cheCropTR);

        groCrop.add(cheCropL);

        groCrop.add(cheCropM);
        cheCropM.setSelected(true);

        groCrop.add(cheCropR);

        groCrop.add(cheCropBL);

        groCrop.add(cheCropB);

        groCrop.add(cheCropBR);

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(cheCropTL)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropT)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropTR))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(cheCropL)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropM)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropR))
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(cheCropBL)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropB)
                        .addGap(2, 2, 2)
                        .addComponent(cheCropBR)))
                .addGap(3, 3, 3))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cheCropTL)
                    .addComponent(cheCropT)
                    .addComponent(cheCropTR))
                .addGap(2, 2, 2)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cheCropL)
                    .addComponent(cheCropM)
                    .addComponent(cheCropR))
                .addGap(2, 2, 2)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cheCropBL)
                    .addComponent(cheCropB)
                    .addComponent(cheCropBR))
                .addContainerGap())
        );

        pancrop.add(jPanel5);

        texCropW.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        texCropW.setText("0");

        texCropH.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        texCropH.setText("0");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(texCropW, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(texCropH, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(texCropW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(texCropH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        pancrop.add(jPanel6);

        butCropApply.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butCropApply.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/dialog-ok-apply.png"))); // NOI18N
        butCropApply.setText("Apply");
        butCropApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butCropApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(butCropApply)
                .addGap(3, 3, 3))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(butCropApply)
                .addContainerGap())
        );

        pancrop.add(jPanel7);

        jTabbedPane1.addTab("Crop", pancrop);

        panresize.setLayout(new javax.swing.BoxLayout(panresize, javax.swing.BoxLayout.LINE_AXIS));

        groResize.add(radResizeBoth);
        radResizeBoth.setSelected(true);
        radResizeBoth.setText("Both");

        groResize.add(radResizeWidth);
        radResizeWidth.setText("Width");

        groResize.add(radResizeHeight);
        radResizeHeight.setText("Height");

        groResize.add(radResizeExact);
        radResizeExact.setText("Exact");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(radResizeBoth)
                        .addGap(2, 2, 2)
                        .addComponent(radResizeExact))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(radResizeWidth)
                        .addGap(2, 2, 2)
                        .addComponent(radResizeHeight)))
                .addGap(3, 3, 3))
        );

        jPanel4Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {radResizeBoth, radResizeExact, radResizeHeight, radResizeWidth});

        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radResizeBoth)
                    .addComponent(radResizeExact))
                .addGap(2, 2, 2)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(radResizeWidth)
                    .addComponent(radResizeHeight))
                .addContainerGap())
        );

        panresize.add(jPanel4);

        texResizeW.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        texResizeW.setText("0");

        texResizeH.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.NumberFormatter(new java.text.DecimalFormat("#0"))));
        texResizeH.setText("0");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(texResizeW, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(texResizeH, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(texResizeW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(texResizeH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        panresize.add(jPanel10);

        butResizeApply.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        butResizeApply.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x32/dialog-ok-apply.png"))); // NOI18N
        butResizeApply.setText("Apply");
        butResizeApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                butResizeApplyActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(butResizeApply)
                .addGap(3, 3, 3))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(butResizeApply)
                .addContainerGap())
        );

        panresize.add(jPanel8);

        jTabbedPane1.addTab("Resize", panresize);

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTabbedPane1)
                .addGap(0, 0, 0))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTabbedPane1)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, 0))
        );

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        panImage.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                panImageMouseClicked(evt);
            }
        });
        panImage.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING, 0, 0));

        labImage.setBackground(javax.swing.UIManager.getDefaults().getColor("Desktop.background"));
        labImage.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        labImage.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Component.focusedBorderColor")));
        labImage.setOpaque(true);
        panImage.add(labImage);

        scrImage.setViewportView(panImage);

        jPanel1.add(scrImage, java.awt.BorderLayout.CENTER);

        texImageW.setEditable(false);
        texImageW.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        texImageW.setText(" ");

        texImageH.setEditable(false);
        texImageH.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        texImageH.setText(" ");

        texImagePath.setEditable(false);

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(texImagePath)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(texImageW, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(texImageH, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(texImagePath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(texImageW, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(texImageH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jPanel1.add(jPanel9, java.awt.BorderLayout.SOUTH);

        jMenu1.setMnemonic('f');
        jMenu1.setText("File");

        menOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menOpen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/document-open.png"))); // NOI18N
        menOpen.setMnemonic('o');
        menOpen.setText("Open");
        menOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menOpenActionPerformed(evt);
            }
        });
        jMenu1.add(menOpen);

        menOpenBase64.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menOpenBase64.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/document-import.png"))); // NOI18N
        menOpenBase64.setMnemonic('p');
        menOpenBase64.setText("Open BASE64");
        menOpenBase64.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menOpenBase64ActionPerformed(evt);
            }
        });
        jMenu1.add(menOpenBase64);
        jMenu1.add(jSeparator1);

        menSaveNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menSaveNew.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/document-save.png"))); // NOI18N
        menSaveNew.setMnemonic('s');
        menSaveNew.setText("Save New");
        menSaveNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menSaveNewActionPerformed(evt);
            }
        });
        jMenu1.add(menSaveNew);

        menSaveAs.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menSaveAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/document-save-as.png"))); // NOI18N
        menSaveAs.setMnemonic('a');
        menSaveAs.setText("Save As");
        menSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menSaveAsActionPerformed(evt);
            }
        });
        jMenu1.add(menSaveAs);
        jMenu1.add(jSeparator2);

        menExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_X, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/application-exit.png"))); // NOI18N
        menExit.setMnemonic('e');
        menExit.setText("Exit");
        menExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menExitActionPerformed(evt);
            }
        });
        jMenu1.add(menExit);

        jMenuBar1.add(jMenu1);

        jMenu3.setMnemonic('e');
        jMenu3.setText("Edit");

        menUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menUndo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/edit-undo.png"))); // NOI18N
        menUndo.setMnemonic('u');
        menUndo.setText("Undo");
        menUndo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menUndoActionPerformed(evt);
            }
        });
        jMenu3.add(menUndo);

        menRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menRedo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/edit-redo.png"))); // NOI18N
        menRedo.setMnemonic('r');
        menRedo.setText("Redo");
        menRedo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menRedoActionPerformed(evt);
            }
        });
        jMenu3.add(menRedo);

        jMenuBar1.add(jMenu3);

        jMenu2.setMnemonic('h');
        jMenu2.setText("Help");

        menAbout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_H, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        menAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/x16/help-about.png"))); // NOI18N
        menAbout.setMnemonic('a');
        menAbout.setText("About ImageTools");
        menAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menAboutActionPerformed(evt);
            }
        });
        jMenu2.add(menAbout);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void panImageMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_panImageMouseClicked
        panImage.requestFocusInWindow();
    }//GEN-LAST:event_panImageMouseClicked

    private void butCropApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butCropApplyActionPerformed
        if (myImage.getImage() == null) {
            showPopup("Image is empty.");
            return;
        }

        int x = 0;
        int y = 0;
        int cw = Integer.parseInt(texCropW.getText().trim());
        int ch = Integer.parseInt(texCropH.getText().trim());
        int iw = myImage.getImage().getWidth();
        int ih = myImage.getImage().getHeight();

        int dw = cw - iw;
        int dh = ch - ih;
        int p = dw > dh ? dw : dh;

        if (p > 0) {
            setImage(new MyImage(Scalr.pad(
                    myImage.getImage(),
                    p,
                    Color.WHITE
            ), myImage));
        }

        iw = myImage.getImage().getWidth();
        ih = myImage.getImage().getHeight();

        if (cheCropT.isSelected() || cheCropM.isSelected() || cheCropB.isSelected()) {
            x = (iw - cw) / 2;
        } else if (cheCropTR.isSelected() || cheCropR.isSelected() || cheCropBR.isSelected()) {
            x = iw - cw;
        }

        if (cheCropL.isSelected() || cheCropM.isSelected() || cheCropR.isSelected()) {
            y = (ih - ch) / 2;
        } else if (cheCropBL.isSelected() || cheCropB.isSelected() || cheCropBR.isSelected()) {
            y = ih - ch;
        }

        setImage(new MyImage(Scalr.crop(
                myImage.getImage(),
                x,
                y,
                cw,
                ch
        ), myImage));

        undoRedo.action(myImage);
        showPopup("Image cropped.");
    }//GEN-LAST:event_butCropApplyActionPerformed

    private void butResizeApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butResizeApplyActionPerformed
        if (myImage.getImage() == null) {
            showPopup("Image is empty.");
            return;
        }

        Scalr.Mode mode = Scalr.Mode.BEST_FIT_BOTH;
        if (radResizeExact.isSelected()) {
            mode = Scalr.Mode.FIT_EXACT;
        } else if (radResizeWidth.isSelected()) {
            mode = Scalr.Mode.FIT_TO_WIDTH;
        } else if (radResizeHeight.isSelected()) {
            mode = Scalr.Mode.FIT_TO_HEIGHT;
        }

        setImage(new MyImage(Scalr.resize(
                myImage.getImage(),
                Scalr.Method.AUTOMATIC,
                mode,
                Integer.parseInt(texResizeW.getText().trim()),
                Integer.parseInt(texResizeH.getText().trim())
        ), myImage));

        undoRedo.action(myImage);
        showPopup("Image resized.");
    }//GEN-LAST:event_butResizeApplyActionPerformed

    private void butOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butOpenActionPerformed
        actionOpen();
    }//GEN-LAST:event_butOpenActionPerformed

    private void butSaveNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveNewActionPerformed
        actionSaveNew();
    }//GEN-LAST:event_butSaveNewActionPerformed

    private void butSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveAsActionPerformed
        actionSaveAs();
    }//GEN-LAST:event_butSaveAsActionPerformed

    private void butUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butUndoActionPerformed
        undo();
    }//GEN-LAST:event_butUndoActionPerformed

    private void butRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butRedoActionPerformed
        redo();
    }//GEN-LAST:event_butRedoActionPerformed

    private void butSaveCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveCancelActionPerformed
        diaSaveAs.setVisible(false);
    }//GEN-LAST:event_butSaveCancelActionPerformed

    private void butSaveSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveSaveActionPerformed
        save(texSaveFilename.getText().trim(), Integer.parseInt(texSaveQuality.getText().trim()));
    }//GEN-LAST:event_butSaveSaveActionPerformed

    private void butSaveBrowseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butSaveBrowseActionPerformed
        if (filSaveAs.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = filSaveAs.getSelectedFile();
            try {
                texSaveFilename.setText(f.getAbsolutePath() + "\\" + myImage.getFile().getName());
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }//GEN-LAST:event_butSaveBrowseActionPerformed

    private void butAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butAboutActionPerformed
        diaAbout.setLocationRelativeTo(this);
        diaAbout.setVisible(true);
    }//GEN-LAST:event_butAboutActionPerformed

    private void butDiaAboutBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butDiaAboutBackActionPerformed
        diaAbout.setVisible(false);
    }//GEN-LAST:event_butDiaAboutBackActionPerformed

    private void menAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menAboutActionPerformed
        diaAbout.setLocationRelativeTo(this);
        diaAbout.setVisible(true);
    }//GEN-LAST:event_menAboutActionPerformed

    private void menOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menOpenActionPerformed
        actionOpen();
    }//GEN-LAST:event_menOpenActionPerformed

    private void menOpenBase64ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menOpenBase64ActionPerformed
        actionOpenBase64();
    }//GEN-LAST:event_menOpenBase64ActionPerformed

    private void menSaveNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menSaveNewActionPerformed
        actionSaveNew();
    }//GEN-LAST:event_menSaveNewActionPerformed

    private void menSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menSaveAsActionPerformed
        actionSaveAs();
    }//GEN-LAST:event_menSaveAsActionPerformed

    private void menExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menExitActionPerformed
        actionExit();
    }//GEN-LAST:event_menExitActionPerformed

    private void butOpenBase64OpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butOpenBase64OpenActionPerformed
        try {
            String s = texOpenBase64String.getText();
            byte[] bs = Base64.getDecoder().decode(s);
            ByteArrayInputStream bis = new ByteArrayInputStream(bs);
            BufferedImage bi = ImageIO.read(bis);
            bis.close();
            setImage(new MyImage(bi, "", null));
            diaOpenBase64.setVisible(false);
        } catch (Exception e) {
            System.out.println(e);
            JOptionPane.showMessageDialog(diaOpenBase64, "Failed to convert the Base64 String", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_butOpenBase64OpenActionPerformed

    private void butOpenBase64CancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_butOpenBase64CancelActionPerformed
        diaOpenBase64.setVisible(false);
    }//GEN-LAST:event_butOpenBase64CancelActionPerformed

    private void menUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menUndoActionPerformed
        undo();
    }//GEN-LAST:event_menUndoActionPerformed

    private void menRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menRedoActionPerformed
        redo();
    }//GEN-LAST:event_menRedoActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton butAbout;
    private javax.swing.JButton butCropApply;
    private javax.swing.JButton butDiaAboutBack;
    private javax.swing.JButton butOpen;
    private javax.swing.JButton butOpenBase64Cancel;
    private javax.swing.JButton butOpenBase64Open;
    private javax.swing.JButton butRedo;
    private javax.swing.JButton butResizeApply;
    private javax.swing.JButton butSaveAs;
    private javax.swing.JButton butSaveBrowse;
    private javax.swing.JButton butSaveCancel;
    private javax.swing.JButton butSaveNew;
    private javax.swing.JButton butSaveSave;
    private javax.swing.JButton butUndo;
    private javax.swing.JCheckBox cheCropB;
    private javax.swing.JCheckBox cheCropBL;
    private javax.swing.JCheckBox cheCropBR;
    private javax.swing.JCheckBox cheCropL;
    private javax.swing.JCheckBox cheCropM;
    private javax.swing.JCheckBox cheCropR;
    private javax.swing.JCheckBox cheCropT;
    private javax.swing.JCheckBox cheCropTL;
    private javax.swing.JCheckBox cheCropTR;
    private javax.swing.JDialog diaAbout;
    private javax.swing.JDialog diaOpenBase64;
    private javax.swing.JDialog diaSaveAs;
    private javax.swing.JFileChooser filOpen;
    private javax.swing.JFileChooser filSaveAs;
    private javax.swing.ButtonGroup groCrop;
    private javax.swing.ButtonGroup groResize;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel labImage;
    private javax.swing.JMenuItem menAbout;
    private javax.swing.JMenuItem menExit;
    private javax.swing.JMenuItem menOpen;
    private javax.swing.JMenuItem menOpenBase64;
    private javax.swing.JMenuItem menRedo;
    private javax.swing.JMenuItem menSaveAs;
    private javax.swing.JMenuItem menSaveNew;
    private javax.swing.JMenuItem menUndo;
    private javax.swing.JPanel panImage;
    private javax.swing.JPanel pancrop;
    private javax.swing.JPanel panresize;
    private javax.swing.JRadioButton radResizeBoth;
    private javax.swing.JRadioButton radResizeExact;
    private javax.swing.JRadioButton radResizeHeight;
    private javax.swing.JRadioButton radResizeWidth;
    private javax.swing.JScrollPane scrImage;
    private javax.swing.JFormattedTextField texCropH;
    private javax.swing.JFormattedTextField texCropW;
    private javax.swing.JTextField texImageH;
    private javax.swing.JTextField texImagePath;
    private javax.swing.JTextField texImageW;
    private javax.swing.JTextArea texOpenBase64String;
    private javax.swing.JFormattedTextField texResizeH;
    private javax.swing.JFormattedTextField texResizeW;
    private javax.swing.JTextField texSaveFilename;
    private javax.swing.JTextField texSaveQuality;
    // End of variables declaration//GEN-END:variables
}
