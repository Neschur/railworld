package net.kolls.railworld.opening;

import net.kolls.railworld.Distance;
import net.kolls.railworld.Images;
import net.kolls.railworld.RailCanvas;
import net.kolls.railworld.RailFrame;
import net.kolls.railworld.play.PlayFrame;
import net.kolls.railworld.play.script.ScriptManager;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;

public class NewGame extends JDialog {
    private JFrame mainWindow;
    private MapListModel<MapLoader> freePlayMaps = null;
    private String mapsPath = null;
    private String tmpPath = null;

    private void run(final RailFrame frame) {

        // if we just fire it up in this code, it will be running in the event
        // loop thread, and block all events!
        // so we must spawn it into a different thread
        Thread t  = new Thread(() -> {
            frame.setVisible(true);

            // loop will run until the window is closed
            try {
                frame.startLoop();
            } catch (Throwable ex) {
                JOptionPane.showMessageDialog(mainWindow,
                        "An error occurred while running, and the game has been stopped. " +
                                "Reason: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }

            frame.dispose();
            mainWindow.setVisible(true);
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
        JButton ok = new JButton("Start");
        ok.addActionListener(e -> {
                    try {
                        startGame(MapLoader.loadFromFile(new File(getMapsPath(), mp.getSelectedMission().rwmFilename())),
                                mp.getSelectedMission().createScriptManager());
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(this, "Please, download map " +
                                mp.getSelectedMission().rwmFilename(), "Error1", JOptionPane.INFORMATION_MESSAGE);
                    } catch (SAXException e1) {
                        e1.printStackTrace();
                    }
                }
        );

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

        ScriptPanel sp = new ScriptPanel();
        panel.add(sp, BorderLayout.EAST);

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
        panelButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(panelButtons, BorderLayout.SOUTH);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnPlay = new JButton("Play");
        btnPlay.addActionListener(e -> {
                    ScriptManager scripts = new ScriptManager();
                    if (scripts != null) {
                        scripts.clear();
                        for (int i = 0; i < sp.getScripts().length; i++) {
                            scripts.add(sp.getScripts()[i]);
                        }
                    }
                    startGame(freePlayMaps.getMapAt(list.getSelectedIndex()), scripts);
                }
        );
        panelButtons.add(btnPlay);

        panelButtons.add(Box.createHorizontalGlue());

        return panel;
    }

    protected JComponent makeDownloadPanel() {
        JPanel panel = new JPanel(false);
        panel.setLayout(new BorderLayout());
        panel.setBorder(new EmptyBorder(5, 5, 5, 5));

        MapListModel<String> listModel = new MapListModel<>();
        JList list = new JList(listModel);
        panel.add(new JScrollPane(list), BorderLayout.WEST);

        JPanel panelButtons = new JPanel();
        panelButtons.setLayout(new BoxLayout(panelButtons, BoxLayout.X_AXIS));
        panelButtons.setBorder(new EmptyBorder(5, 5, 5, 5));
        panel.add(panelButtons, BorderLayout.SOUTH);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnCheck = new JButton("Check");
        btnCheck.addActionListener(e -> {
            checkDownload(listModel);

        });
        panelButtons.add(btnCheck);

        panelButtons.add(Box.createHorizontalGlue());

        JButton btnDownload = new JButton("Download Selected");
        btnDownload.addActionListener(e -> {
            downloadSelected(list);
        });
        panelButtons.add(btnDownload);

        panelButtons.add(Box.createHorizontalGlue());

        return panel;
    }

    private ListModel makeMapList() {
        if(freePlayMaps == null)
            freePlayMaps = new MapListModel();
        else
            freePlayMaps.clear();

        File dir = new File(getMapsPath());

        File [] files = dir.listFiles((dir1, name) -> {
            return name.endsWith(".rwm");
        });

        for (File file: files) {
            try {
                MapLoader map = MapLoader.loadFromFile(file);
                freePlayMaps.addElement(map.getMetaData().title, map);
            } catch (IOException |SAXException e) {
                e.printStackTrace();
            }

        }

        return freePlayMaps;
    }

    private String getConfigPath() {
        return System.getProperty("user.home") + File.separator + ".railworld";
    }

    private String getMapsPath() {
        if(mapsPath != null) {
            return mapsPath;
        }
        mapsPath = getConfigPath() + File.separator + "maps" + File.separator;
        if(Files.notExists(Paths.get(mapsPath))) {
            (new File(mapsPath.toString())).mkdirs();
        }
        return mapsPath;
    }

    private String getTmpPath() {
        if(tmpPath != null) {
            return tmpPath;
        }
        tmpPath = getConfigPath() + File.separator + "tmp" + File.separator;
        if(Files.notExists(Paths.get(tmpPath))) {
            (new File(tmpPath)).mkdirs();
        }
        return tmpPath;
    }

    protected void startGame(MapLoader mi, ScriptManager scripts) {
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

        if (frame != null)
            run(frame);
        else
            setVisible(true);

        mainWindow.setVisible(false);
    }

    private void checkDownload(MapListModel listModel) {
        listModel.clear();

        try {
            URL url = new URL("http://railworld.siarhei.by/maps.json");

            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = in.readLine();
            JSONObject json = new JSONObject(line);

            JSONArray maps = json.getJSONArray("maps");
            for(int i = 0; i < maps.length(); i++) {
                JSONObject map = maps.getJSONObject(i);

                File infoFile = new File(getMapsPath() + map.getString("mapFileName"));
                if(!infoFile.exists()) {
                    listModel.addElement(map.getString("name"), map.getString("zipFileName"));
                }
            }

            in.close();

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadSelected(JList list) {
        int[] indices = list.getSelectedIndices();
        MapListModel<String> model = (MapListModel<String>)list.getModel();
        for(int index: indices) {
            String map = model.getMapAt(index);
            try {
                URI uri = new URI("http",  "railworld.siarhei.by", "/maps/" + map, null);
                ReadableByteChannel rbc = Channels.newChannel(uri.toURL().openStream());
                String zipPath = getTmpPath() + map;
                FileOutputStream fos = new FileOutputStream(zipPath);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                ZipFile zipFile = new ZipFile(zipPath);
                zipFile.extractAll(getMapsPath());
            } catch (URISyntaxException | IOException | ZipException e) {
                JOptionPane.showMessageDialog(this, "An error occurred on download" +
                        "Reason: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        checkDownload((MapListModel)list.getModel());
        makeMapList();
    }
}
