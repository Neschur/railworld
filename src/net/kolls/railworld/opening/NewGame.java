package net.kolls.railworld.opening;

import net.kolls.railworld.Distance;
import net.kolls.railworld.Images;
import net.kolls.railworld.RailCanvas;
import net.kolls.railworld.RailFrame;
import net.kolls.railworld.play.PlayFrame;
import net.kolls.railworld.play.script.ScriptManager;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

public class NewGame extends JDialog {
//    private String selectedMap;

    private JFrame mainWindow;

    private void run(final RailFrame frame) {

        // if we just fire it up in this code, it will be running in the event
        // loop thread, and block all events!
        // so we must spawn it into a different thread
        Thread t  = new Thread(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);

                // loop will run until the window is closed
                try {
                    frame.startLoop();
                } catch (Throwable ex) {
                    JOptionPane.showMessageDialog(mainWindow, "An error occured while running, and the game has been stopped.  Reason: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }

                frame.dispose();
                mainWindow.setVisible(true);
            }

        });
        t.start();
    }

    public NewGame(JFrame mainWindow) {
        super();
        this.mainWindow = mainWindow;
        setTitle("New Game");
        setIconImage(Images.frameIcon);

        addWidgets();
        setSize(440, 380);
        setLocationRelativeTo(null);
    }

    private void addWidgets() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JComponent panel1 = makeFreeplayPanel();
        tabbedPane.addTab("Freeplay", null, panel1);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        JComponent panel2 = makeMissionsPanel();
        tabbedPane.addTab("Missions", null, panel2);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_2);

        JComponent panel3 = makeDownloadPanel();
        tabbedPane.addTab("Download", null, panel3);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_3);

        this.add(tabbedPane);

    }

    protected JComponent makeMissionsPanel() {
        JPanel panel = new JPanel();

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        MissionPanel mp = new MissionPanel();
        panel.add(mp);

        JPanel okcan = new JPanel();
        okcan.setLayout(new BoxLayout(okcan, BoxLayout.X_AXIS));
        okcan.add(Box.createHorizontalGlue());
        JButton ok;
        ok = new JButton("Start");
        final JDialog misd = this;
        ok.addActionListener(e -> {
            if (mp.getSelectedMission() == null) {
                JOptionPane.showMessageDialog(misd, "You must select a mission to begin");
            } else {
                MapLoader mi = null;
                try {
                    mi = MapLoader.loadFromFile(new File(getMapsPath(), mp.getSelectedMission().rwmFilename()));
                } catch (IOException e1) {
                    e1.printStackTrace();
                } catch (SAXException e1) {
                    e1.printStackTrace();
                }

                ScriptManager scripts = new ScriptManager();
                scripts = mp.getSelectedMission().createScriptManager();

                setVisible(false);

                RailCanvas.zoom = mi.getMetaData().zoom;
                Distance.feetPerPixels = mi.getMetaData().feetPerPixel;

                RailFrame frame = null;

                try {
                    frame = new PlayFrame(mi.getSegments(), mi.getImage(), mi.getMetaData(), scripts);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                scripts.init( (PlayFrame)frame);

                // 8. run the frame
                if (frame != null)
                    run(frame);
                else
                    setVisible(true);

                mi = null;

                mainWindow.setVisible(false);
            }

        });

        okcan.add(ok);
        okcan.add(Box.createHorizontalGlue());

        panel.add(okcan);
        panel.add(new JLabel(" "));

        setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
        setPreferredSize(new Dimension(500, 300));
        setIconImage(Images.frameIcon);
        setTitle("Select Mission");

        pack();

        return panel;
    }

    protected JComponent makeFreeplayPanel() {
        JPanel panel = new JPanel(false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JList list = new JList(makeMapList());
        panel.add(new JScrollPane(list), BorderLayout.WEST);

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
        panelButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(panelButtons, BorderLayout.SOUTH);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnCheck = new JButton("Play");
        panelButtons.add(btnCheck);

        panelButtons.add(Box.createHorizontalGlue());

        return panel;
    }

    protected JComponent makeDownloadPanel() {
        JPanel panel = new JPanel(false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JList list = new JList(makeMapList());
        panel.add(new JScrollPane(list), BorderLayout.WEST);

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
        panelButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(panelButtons, BorderLayout.SOUTH);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnCheck = new JButton("Check");
        panelButtons.add(btnCheck);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnDownload = new JButton("Download Selected");
        panelButtons.add(btnDownload);

        panelButtons.add(Box.createHorizontalGlue());

        return panel;
    }

    private ListModel makeMapList() {
        DefaultListModel<String> listModel = new DefaultListModel();

        File dir = new File(getMapsPath());

        File [] files = dir.listFiles((dir1, name) -> {
            return name.endsWith(".rwm");
        });

        for (File file: files) {
            listModel.addElement(file.toPath().getFileName().toString());
        }

        return listModel;
    }

    private String getConfigPath() {
        return System.getProperty("user.home") + File.separator + ".railworld";
    }

    private String getMapsPath() {
        return getConfigPath() + File.separator + "maps";
    }
}
