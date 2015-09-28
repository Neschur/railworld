package net.kolls.railworld.opening;

import net.kolls.railworld.Images;
import net.kolls.railworld.play.script.ScriptManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.nio.file.Path;
import java.nio.file.Paths;

public class NewGame extends JDialog {
    public NewGame() {
        super();
        setTitle("New Game");
        setIconImage(Images.frameIcon);

        addWidgets();
        pack();
        setSize(640, 480);
    }

    private void addWidgets() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JComponent panel1 = makeFreeplayPanel();
        tabbedPane.addTab("Freeplay", null, panel1);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

        JComponent panel2 = makeMissionsPanel();
        tabbedPane.addTab("Missions", null, panel2);
        tabbedPane.setMnemonicAt(0, KeyEvent.VK_2);

        JComponent panel3 = makeTextPanel("Panel #3");
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
        JButton ok, cancel;
        ok = new JButton("Start");
        final JDialog misd = this;
        ok.addActionListener(e -> {
            if (mp.getSelectedMission() == null) {
                JOptionPane.showMessageDialog(misd, "You must select a mission to begin");

            } else {
                setVisible(false);
            }

        });

        okcan.add(ok);
        okcan.add(Box.createHorizontalGlue());
        cancel = new JButton("Cancel");
        cancel.addActionListener(e -> setVisible(false));
        okcan.add(cancel);
        okcan.add(Box.createHorizontalGlue());

        panel.add(okcan);
        panel.add(new JLabel(" ")); // too lazy to do proper margin right now

        setModalityType(JDialog.DEFAULT_MODALITY_TYPE);
        setPreferredSize(new Dimension(500, 300));
        setIconImage(Images.frameIcon);
        setTitle("Select Mission");

        pack();

        return panel;
    }

    protected JComponent makeFreeplayPanel() {
        JPanel panel = new JPanel();

        setLayout(new BorderLayout());


        JList missions = new JList(makeMapList());
        panel.add(new JScrollPane(missions), BorderLayout.WEST);
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.revalidate();

        return panel;
    }

    protected JComponent makeTextPanel(String text) {
        JPanel panel = new JPanel(false);
        JLabel filler = new JLabel(text);
        filler.setHorizontalAlignment(JLabel.CENTER);
        panel.setLayout(new GridLayout(1, 1));
        panel.add(filler);
        return panel;
    }

    private ListModel makeMapList() {
        DefaultListModel<String> listModel = new DefaultListModel();
        String configPath = System.getProperty("user.home") + File.separator + ".railworld";

        File dir = new File(configPath + "/maps/");

        File [] files = dir.listFiles((dir1, name) -> {
            return name.endsWith(".rwm");
        });

        for (File file: files) {
            listModel.addElement(file.toPath().getFileName().toString());
        }

        return listModel;
    }
}
