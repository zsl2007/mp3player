package com.zsl2007;

import com.zsl2007.model.ConvSignModel;
import com.zsl2007.model.SignModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MP3PlayerWindow extends JFrame {


    //窗口组件
    // 播放文件顯示標籤
    JLabel playFilenameShowLabel;
    JLabel totalFrameShowLabel;
    JLabel currentFrameShowLabel;
    JLabel hintsLabel;
    JLabel autoSaveLabel;

    //
    JProgressBar progressBar = null;
    JSlider slider;
    //JWavPanel wavPanel;

    //剪切列表模型
    SignTableModel signTable;
    MP3WavWindow wavWindow;

    //逻辑部分
    volatile MP3PlayerControl mp3PlayerControl;

    static char SIGN = 's';
    static char RESUME = 'r';
    static char PAUSE = 'p';

    public static int MAX_INDEX = 9999;

    public static long AUTOSAVE_INTERVAL_MS = 10 * 1000;

    public void initMainWindow() {
        setTitle("MP3播放器 ");
        setLayout(new BorderLayout(10, 10));
        setSize(400, 600);
        initMenu();
        add(initNorthPanel(), "North");
        add(initSouthPanel(), "South");
        add(initCenterPanel(), "Center");
        showWindow();
    }

    public void initMenu() {

        JMenuBar menubar = new JMenuBar();
        JMenu menufile = new JMenu(" 文件 ");
        JMenuItem menuopen = new JMenuItem(" 打开 ");
        menufile.add(menuopen);
        menubar.add(menufile);
        this.setJMenuBar(menubar);

        menuopen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                FileDialog dialog = new FileDialog(MP3PlayerWindow.this, "Open", FileDialog.LOAD);

                dialog.setFilenameFilter(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        if (name.endsWith(MP3PlayerControl.SUFFIX_MP3)) {
                            return true;
                        }
                        return false;
                    }
                });

                dialog.setVisible(true);
                String filepath = dialog.getDirectory();
                String filename = dialog.getFile();


                if (filename == null) return;

                //初始化mp3PlayerControl


                MP3PlayerControl newMP3PlayerControl;
                try {
                    newMP3PlayerControl = new MP3PlayerControl(filepath + filename, filepath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    MP3PlayerWindow.this.setHintsLabel(filename + " 加载失败，" + ex.getMessage(), 1);
                    return;
                }

                if (mp3PlayerControl != null)
                    mp3PlayerControl.close();

                mp3PlayerControl = newMP3PlayerControl;


                playFilenameShowLabel.setText(" 播放文件： " + filepath + filename);

                signTable.setMp3PlayerControl(mp3PlayerControl);
                signTable.fireTableDataChanged();

                int frames = mp3PlayerControl.getMaxFrames();
                totalFrameShowLabel.setText("总frame：" + frames);
                slider.setMaximum(frames);
                progressBar.setMaximum(frames);

                if (wavWindow != null) {
                    wavWindow.setVisible(false);
                    wavWindow = null;
                }
            }
        });
    }

    public JPanel initNorthPanel() {
        JPanel northPanel = new JPanel(new GridLayout(2, 1));
        JPanel panel = new JPanel(new GridLayout(5, 1));
        playFilenameShowLabel = new JLabel(" 播放文件： ");
        panel.add(playFilenameShowLabel);
        totalFrameShowLabel = new JLabel("总frame：");
        panel.add(totalFrameShowLabel);

        currentFrameShowLabel = new JLabel("当前frame：");
        panel.add(currentFrameShowLabel);

        hintsLabel = new JLabel("tips：");
        panel.add(hintsLabel);

        autoSaveLabel = new JLabel("");
        autoSaveLabel.setForeground(Color.blue);
        panel.add(autoSaveLabel);

        northPanel.add(panel);

        //播放控制
        JPanel contralPanel = new JPanel(new GridLayout(1, 4));


        JButton playButton = new JButton("播放(r)");
        contralPanel.add(playButton);

        JButton pauseButton = new JButton("暂停(p)");
        contralPanel.add(pauseButton);

        JButton signButton = new JButton("标记暂停(s)");
        contralPanel.add(signButton);

        JButton wavButton = new JButton("生成波形图");
        contralPanel.add(wavButton);

        northPanel.add(contralPanel);

        playButton.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // 播放選中的文件
                    play();
                }
            }
        });

        signButton.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // 播放選中的文件
                    if (mp3PlayerControl != null) {
                        mp3PlayerControl.pause();
                        mp3PlayerControl.addSignList(mp3PlayerControl.getCurrentFrame(), true);
                    }
                    signTable.fireTableDataChanged();
                }

            }
        });

        pauseButton.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    // 播放選中的文件
                    pause();

                }
            }
        });

        wavButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    if (mp3PlayerControl != null) {
                        if (wavWindow == null) {
                            mp3PlayerControl.getWavData();
                            wavWindow = new MP3WavWindow(MP3PlayerWindow.this, mp3PlayerControl);
                            wavWindow.repaintWavLine();
                        } else {
                            wavWindow.setVisible(true);
                        }


                    }
                }
            }
        });
        return northPanel;
    }

    public void sign() {
        if (mp3PlayerControl != null) {
            mp3PlayerControl.pause();
            mp3PlayerControl.addSignList(mp3PlayerControl.getCurrentFrame(), true);
        }
        signTable.fireTableDataChanged();
    }

    public void play() {
        if (mp3PlayerControl != null) {
            mp3PlayerControl.play();
        }
    }

    public void pause() {
        if (mp3PlayerControl != null) {
            mp3PlayerControl.pause();
        }
    }

    public JPanel initSouthPanel() {
        JPanel southPanel = new JPanel(new GridLayout(2, 1));

        //播放进度条
        JPanel barPanel = new JPanel(new GridLayout(2, 1));

        slider = new JSlider(0, 0, 0);
        barPanel.add(slider);
        progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(0);
        progressBar.setBorderPainted(false);// 不显示下边框 // 		前景色
        progressBar.setForeground(new Color(4, 253, 19)); //背景底色
        progressBar.setBackground(new Color(0x01012F));
        progressBar.setValue(0);
        barPanel.add(progressBar);


        southPanel.add(barPanel);

        JPanel signControlPanel = new JPanel(new GridLayout(1, 1));

//        JPanel signControlPanel1 = new JPanel(new GridLayout(1, 2));
//        JButton saveButton = new JButton("保存列表(自动)");
//        signControlPanel1.add(saveButton);
//
//        JButton loadButton = new JButton("加载列表");
//        signControlPanel1.add(loadButton);


//        signControlPanel.add(signControlPanel1);


        JPanel signControlPanel2 = new JPanel(new GridLayout(1, 3));

        final JButton indexButton = new JButton("起始序号");
        signControlPanel2.add(indexButton);

        final JTextField jTextField = new JTextField("1");
        signControlPanel2.add(jTextField);

        JButton convButton = new JButton("切分");
        signControlPanel2.add(convButton);

        signControlPanel.add(signControlPanel2);

        southPanel.add(signControlPanel);

        slider.addMouseListener(new MouseAdapter() {
            @Override
            public synchronized void mouseReleased(MouseEvent e) {

                slider.setValueIsAdjusting(false);
                int value = slider.getValue();
                mp3PlayerControl.pause(value);
            }

            public synchronized void mouseDragged(MouseEvent e) {
                slider.setValueIsAdjusting(true);
                mp3PlayerControl.pause();
            }

        });

        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (slider.getValueIsAdjusting() == true) {

                    mp3PlayerControl.pause(slider.getValue());
                }
            }
        });

        convButton.addMouseListener(new MouseAdapter() {

            String lastFilePath = null;
            @Override
            public synchronized void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    try {
                        if (mp3PlayerControl == null) return;

                        int startIndex = Integer.valueOf(jTextField.getText());

//                        FileDialog dialog = new FileDialog(MP3PlayerWindow.this, "输出目录", FileDialog.LOAD);
//                        dialog.setVisible(true);

                        JFileChooser dirChooser;
                        if(lastFilePath == null){
                            dirChooser = new JFileChooser(mp3PlayerControl.getFilePath());
                        }else{
                            dirChooser=new JFileChooser(lastFilePath);
                        }

                        dirChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                        int ret = dirChooser.showOpenDialog(MP3PlayerWindow.this);

                        if(ret != JFileChooser.APPROVE_OPTION){
                            return;
                        }

                        boolean succeed = true;


                        final String filePath;

                        String tmpFilePath = "";

                        File selectedFile = dirChooser.getSelectedFile();

                        if(selectedFile != null){
                            tmpFilePath = selectedFile.getAbsolutePath();
                        }

                        if(selectedFile.isFile()){
                            tmpFilePath = dirChooser.getCurrentDirectory().getAbsolutePath();
                        }

                        filePath = tmpFilePath;

                        lastFilePath = filePath;

//                        if(lastFilePath != null){
//                            System.out.println(lastFilePath);
//                            return;
//                        }

                        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
                        ArrayList<Future<Integer>> taskList = new ArrayList<Future<Integer>>();
                        int count = 0;
                        String error = "";
                        synchronized (mp3PlayerControl.getSynchronizedSignList()) {
                            MP3PlayerWindow.this.setHintsLabel("切分进行中...", 0);
                            MP3PlayerWindow.this.hintsLabel.repaint();
                            for (int i = 0; i < mp3PlayerControl.getSynchronizedSignList().size() - 1; ++i) {
                                final int start = mp3PlayerControl.getSynchronizedSignList().get(i).getSign();
                                final int end = mp3PlayerControl.getSynchronizedSignList().get(i + 1).getSign();
                                if (mp3PlayerControl.getSynchronizedSignList().get(i).isUsed()) {
                                    int index = startIndex + count;
                                    if (index > MAX_INDEX || index <= 0) {
                                        error = "标签序号错误：" + index;
                                        succeed = false;
                                        break;
                                    }
                                    String sindex = String.format("%04d", index);
                                    final String filename = "REC" + sindex;
                                    Future<Integer> future = executorService.submit(new Callable<Integer>() {
                                        public Integer call() throws Exception {
                                            new Mp3Spliter().convert(mp3PlayerControl.getFileName(), filePath, filename, new ConvSignModel(start, end));
                                            return 0;
                                        }
                                    });
                                    taskList.add(future);
                                    count++;
                                }
                            }
                            for (int i = 0; i < taskList.size(); ++i) {
                                try {
                                    taskList.get(i).get();
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                    succeed = false;
                                    error += " " + e2.getMessage();
                                }
                            }
                        }

                        if (succeed) {
                            MP3PlayerWindow.this.setHintsLabel("转换成功: " + filePath, 0);
                        } else {
                            MP3PlayerWindow.this.setHintsLabel("转换失败: " + error, 1);
                        }


                    } catch (Exception e1) {
                        e1.printStackTrace();
                        MP3PlayerWindow.this.setHintsLabel("转换失败: " + e1.getMessage(), 1);
                    }

                }
            }
        });


//        saveButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public synchronized void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 1) {
//
//                    if (mp3PlayerControl != null && mp3PlayerControl.getSignList().size() > 0) {
//                        if(mp3PlayerControl.getSavePath() == null){
//                            FileDialog dialog = new FileDialog(MP3PlayerWindow.this, "save", FileDialog.SAVE);
//                            dialog.setVisible(true);
//                            String saveFilepath = dialog.getDirectory();
//                            String saveFileName = dialog.getFile();
//                            String finalName = saveFilepath + saveFileName;
//
//                            if (saveFilepath == null || saveFileName == null) {
//                                return;
//                            }
//                            if (!finalName.endsWith(MP3PlayerControl.SUFFIX_JSON)) {
//                                finalName = finalName + MP3PlayerControl.SUFFIX_JSON;
//                            }
//                            mp3PlayerControl.setSavePath(finalName);
//                        }
//
//                        mp3PlayerControl.save();
//                        MP3PlayerWindow.this.setHintsLabel("保存成功，" + mp3PlayerControl.getSavePath(), 0);
//                    }
//
//
//                }
//            }
//        });

//        loadButton.addMouseListener(new MouseAdapter() {
//            @Override
//            public synchronized void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 1) {
//
//                    if (mp3PlayerControl != null) {
//                        FileDialog dialog = new FileDialog(MP3PlayerWindow.this, "load", FileDialog.LOAD);
//                        dialog.setFilenameFilter(new FilenameFilter() {
//                            public boolean accept(File dir, String name) {
//                                if(name.endsWith(MP3PlayerControl.SUFFIX_JSON)){
//                                    return true;
//                                }
//                                return false;
//                            }
//                        });
//                        dialog.setVisible(true);
//                        String loadFilepath = dialog.getDirectory();
//                        String loadFileName = dialog.getFile();
//                        if (loadFilepath == null || loadFileName == null) {
//                            return;
//                        }
//                        try{
//                            mp3PlayerControl.loadSplitList(loadFilepath + loadFileName);
//                            MP3PlayerWindow.this.setHintsLabel("load成功，" + loadFilepath + loadFileName, 0);
//                            signTable.fireTableDataChanged();
//                        }catch (Exception e1){
//                            e1.printStackTrace();
//                            MP3PlayerWindow.this.setHintsLabel("load失败，" + e1.getMessage(), 1);
//                        }
//
//                    }
//
//
//                }
//            }
//        });

        return southPanel;
    }

    public void setHintsLabel(String text, int type) {
        if (type == 0) {
            this.hintsLabel.setForeground(Color.blue);
            this.hintsLabel.setText(text);
        } else {
            this.hintsLabel.setForeground(Color.RED);
            this.hintsLabel.setText(text);
        }
    }

    public JPanel initCenterPanel() {
        //标记列表
        JPanel signPanel = new JPanel(new GridLayout(1, 2));

        signTable = new SignTableModel();
        final JTable table = new JTable(signTable);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane scrollPane = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        signPanel.add(scrollPane);

        TableColumnModel tcm = table.getColumnModel();

        tcm.getColumn(2).setCellRenderer(new JButtonRender("试听"));
        tcm.getColumn(3).setCellRenderer(new JCheckBoxRender());
        tcm.getColumn(4).setCellRenderer(new JButtonRender("删除"));

        //为scrollPane指定显示对象为table
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {

            long lastActionTime = System.currentTimeMillis();

            public synchronized void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && System.currentTimeMillis() - lastActionTime > 1000) {
                    int columnIndex = table.columnAtPoint(e.getPoint()); //获取点击的列
                    int rowIndex = table.rowAtPoint(e.getPoint()); //获取点击的行
                    //试听
                    synchronized (mp3PlayerControl.getSynchronizedSignList()) {
                        List<SignModel> modelList = mp3PlayerControl.getSynchronizedSignList();
                        if (columnIndex == 2) {
                            mp3PlayerControl.play(modelList.get(rowIndex).getSign(), modelList.get(rowIndex + 1).getSign());
                        }
                        //checkbox
                        if (columnIndex == 3) {
                            modelList.get(rowIndex).setUsed(!modelList.get(rowIndex).isUsed());
                            signTable.fireTableDataChanged();
                        }
                        //remove
                        if (columnIndex == 4) {
                            if (rowIndex != 0) {
                                modelList.remove(modelList.get(rowIndex));
                                signTable.fireTableDataChanged();

                            }
                        }
                    }


                }
            }
        });

//        wavPanel = new JWavPanel(mp3PlayerControl,new GridLayout(1,1));
//        wavPanel.setBackground(Color.WHITE);

        signPanel.add(scrollPane);

        //signPanel.add(wavPanel);

        return signPanel;
    }

    public void showWindow() {
        // 註冊窗體關閉事件
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        Thread refreshThread = new Thread(new RefreshWindowThread(this));
        refreshThread.start();

        this.setFocusable(true);

        setVisible(true);

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == SIGN) {
                    sign();
                } else if (e.getKeyChar() == RESUME) {
                    play();
                } else if (e.getKeyChar() == PAUSE) {
                    pause();
                }
            }
        });
    }


    public MP3PlayerWindow() {
        // 設置窗體屬性
        initMainWindow();
    }


    public static void main(String args[]) {
        new MP3PlayerWindow();
    }
}

class RefreshWindowThread extends Thread {

    MP3PlayerWindow mp3PlayerWindow;

    static int FRESH_INTERVAL_MS = 30;

    public RefreshWindowThread(MP3PlayerWindow mp3PlayerWindow) {
        this.mp3PlayerWindow = mp3PlayerWindow;
    }

    public void run() {
        try {
            int lastProgress = 0;
            long lastAutoSaveListTime = System.currentTimeMillis();
            int autoSaveCount = 0;
            while (true) {
                if (mp3PlayerWindow.mp3PlayerControl != null) {

                    int currentProgress = mp3PlayerWindow.mp3PlayerControl.getCurrentFrame();
                    if (lastProgress != currentProgress) {
                        mp3PlayerWindow.progressBar.setValue(currentProgress);
                        mp3PlayerWindow.slider.setValue(currentProgress);
                        mp3PlayerWindow.currentFrameShowLabel.setText("当前Frame：" + currentProgress);
                        if (mp3PlayerWindow.wavWindow != null && mp3PlayerWindow.wavWindow.isVisible()) {
                            mp3PlayerWindow.wavWindow.repaintWavLine();
                        }

                    }
//                    if(mp3PlayerWindow.wavWindow != null && mp3PlayerWindow.isVisible()){
//                        mp3PlayerWindow.wavWindow.setLocation( mp3PlayerWindow.getWidth()+ mp3PlayerWindow.getX(), mp3PlayerWindow.getY());
//
//                    }
                    if (System.currentTimeMillis() - lastAutoSaveListTime > mp3PlayerWindow.AUTOSAVE_INTERVAL_MS) {
                        mp3PlayerWindow.mp3PlayerControl.saveSignModel();
                        mp3PlayerWindow.autoSaveLabel.setText("自动保存成功 " + autoSaveCount % 10);
                        lastAutoSaveListTime = System.currentTimeMillis();
                        autoSaveCount++;
                        if (autoSaveCount >= 10) autoSaveCount = 0;
                    }
                }
                sleep(FRESH_INTERVAL_MS);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


class SignTableModel extends AbstractTableModel {
    //表格中第一行所要显示的内容存放在字符串数组columnNames中


    MP3PlayerControl mp3PlayerControl;

    public void setMp3PlayerControl(MP3PlayerControl mp3PlayerControl) {
        this.mp3PlayerControl = mp3PlayerControl;
    }

    final static String[] COLUMN_NAMES = {"Frame", "Comment", "试听", "是否使用", "删除"};//表格中各行的内容保存在二维数组data中

    public void setData(Object[][] data) {
        this.data = data;
    }

    Object[][] data = new Object[0][COLUMN_NAMES.length];


    public int getRowCount() {

        if (mp3PlayerControl != null) {
            return mp3PlayerControl.getSynchronizedSignList().size() - 1;
        }

        return 0;
    }

    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    public Object getValueAt(int row, int col) {
        if (mp3PlayerControl != null) {
            if (col == 0) {
                return mp3PlayerControl.getSynchronizedSignList().get(row).getSign();
            }
            if (col == 1) {
                return mp3PlayerControl.getSynchronizedSignList().get(row).getSign() + "--" +
                        mp3PlayerControl.getSynchronizedSignList().get(row + 1).getSign();
            }
            if (col == 3) {
                return mp3PlayerControl.getSynchronizedSignList().get(row).isUsed();
            }
        }

        return "";
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

}

class JCheckBoxRender extends JCheckBox implements TableCellRenderer {

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        Boolean b = (Boolean) value;
        setSelected(b.booleanValue());
        return this;
    }
}


class JButtonRender extends JButton implements TableCellRenderer {


    public JButtonRender(String text) {
        super(text);
    }

    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        return this;
    }

}

//class JWavPanel extends JPanel{
//
//    MP3PlayerControl mp3PlayerControl;
//
//    public void setMp3PlayerControl(MP3PlayerControl mp3PlayerControl) {
//        this.mp3PlayerControl = mp3PlayerControl;
//    }
//
//    public JWavPanel(MP3PlayerControl mp3PlayerControl, LayoutManager layout){
//        super(layout);
//        this.mp3PlayerControl = mp3PlayerControl;
//    }
//
//    public void paint(Graphics g){
//
//        Graphics2D g2d = (Graphics2D) g;
//
//        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.setColor(Color.blue);
//
//        int ww = getWidth();
//        int hh = getHeight();
//
//        if(mp3PlayerControl != null){
//
//            int len = mp3PlayerControl.wavData.length;
//            if(len == 0) return;
//            int step = len/ww;
//            if(step==0)  step = 1;
//
//            int prex = 0, prey = 0; //上一个坐标
//            int x = 0, y = 0;
//
//            double k = hh/2.0/32768.0;
//
//            for(int i=0; i<ww && i < len; ++i){
//                x = i;
//                // 下面是个三点取出并绘制
//                // 实际中应该按照采样率来设置间隔
//                y = hh-(int)(mp3PlayerControl.wavData[i * step ]*k+hh/2);
//                if(i!=0){
//                    g.drawLine(x, y, prex, prey);
//                }
//                prex = x;
//                prey = y;
//            }
//            g2d.setColor(Color.red);
//            x = (int)((double)ww/mp3PlayerControl.getMaxFrames() * mp3PlayerControl.getCurrentFrame());
//
//            g2d.drawLine(0,hh/2, x,hh/2);
//        }
//
//    }
//}







