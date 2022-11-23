package com.zsl2007;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MP3WavWindow extends JFrame  {

    public MP3PlayerControl mp3PlayerControl;

    public JFrame parentWindow;

    public JWavPanel wavPanel;

    public MP3WavWindow(JFrame parentWindow, MP3PlayerControl mp3PlayerControl){
        this.parentWindow = parentWindow;
        this.mp3PlayerControl = mp3PlayerControl;
        initWindow();
    }

    public void initWindow(){
        setTitle("波形图");
        setLayout(new BorderLayout(10, 10));
        setSize(800, 600);

        wavPanel = new JWavPanel(mp3PlayerControl,new GridLayout(1,1));
        wavPanel.setBackground(Color.WHITE);
        add(wavPanel);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window
        int w = this.parentWindow.getWidth();
        int x = this.parentWindow.getX();
        int y = this.parentWindow.getY();

        this.setLocation(w + x, y);
        setVisible(true);
    }

    public void repaintWavLine(){
        this.wavPanel.repaint();
    }
}


class JWavPanel extends JPanel{

    MP3PlayerControl mp3PlayerControl;

    public void setMp3PlayerControl(MP3PlayerControl mp3PlayerControl) {
        this.mp3PlayerControl = mp3PlayerControl;
    }

    public JWavPanel(MP3PlayerControl mp3PlayerControl, LayoutManager layout){
        super(layout);
        this.mp3PlayerControl = mp3PlayerControl;
    }

    public void paint(Graphics g){

        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.blue);

        int ww = getWidth();
        int hh = getHeight();

        if(mp3PlayerControl != null){

            int len = mp3PlayerControl.wavData.length;
            if(len == 0) return;
            int step = len/ww;
            if(step==0)  step = 1;

            int prex = 0, prey = 0; //上一个坐标
            int x = 0, y = 0;

            double k = hh/2.0/32768.0;

            for(int i=0; i<ww && i < len; ++i){
                x = i;
                // 下面是个三点取出并绘制
                // 实际中应该按照采样率来设置间隔
                y = hh-(int)(mp3PlayerControl.wavData[i * step ]*k+hh/2);
                if(i!=0){
                    g.drawLine(x, y, prex, prey);
                }
                prex = x;
                prey = y;
            }
            g2d.setColor(Color.red);
            x = (int)((double)ww/mp3PlayerControl.getMaxFrames() * mp3PlayerControl.getCurrentFrame());

            g2d.drawLine(0,hh/2, x,hh/2);
        }

    }
}

