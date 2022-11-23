package com.zsl2007;

import com.google.gson.GsonBuilder;
import com.zsl2007.model.ConvSignModel;
import com.zsl2007.model.SignModel;
import com.zsl2007.model.SignSaveModel;
import com.zsl2007.model.WavModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MP3PlayerControl {


    private static int ONE_FRAME = 1;

    public static String SUFFIX_MP3 = ".mp3";
    public static String SUFFIX_WAV = ".wav";

    public static String SUFFIX_WAV_MODEL = ".wavmodel";

    public static String SUFFIX_JSON = ".json";

    public static String TMP_FILENAME = "tmp";

    private String wavModelFileName;

    private String signModelFileName;

    public String getWavModelFileName() {
        return wavModelFileName;
    }

    public void setWavModelFileName(String wavModelFileName) {
        this.wavModelFileName = wavModelFileName;
    }

    public String getSignModelFileName() {
        return signModelFileName;
    }

    public void setSignModelFileName(String signModelFileName) {
        this.signModelFileName = signModelFileName;
    }

    //ArrayList<SignModel> signList = null;
    List<SignModel> synchronizedSignList = Collections.synchronizedList(new ArrayList<SignModel>());

    private String fileName;

    private String filePath;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public MP3Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(MP3Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public int getMaxFrames() {
        return maxFrames;
    }

    public void setMaxFrames(int maxFrames) {
        this.maxFrames = maxFrames;
    }

    public int getCurrentFrame() {
        return currentFrame;
    }

    public List<SignModel> getSynchronizedSignList() {
        return synchronizedSignList;
    }

    public void setSynchronizedSignList(List<SignModel> synchronizedSignList) {
        this.synchronizedSignList = synchronizedSignList;
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    private MP3Player currentPlayer;

    private int maxFrames;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private volatile int currentFrame;

    public int[] wavData = new int[0];


    public MP3PlayerControl(String fileName, String filePath) throws Exception{
        this.fileName = fileName;
        this.filePath = filePath;
        this.signModelFileName = this.fileName + SUFFIX_JSON;
        this.wavModelFileName = this.fileName + SUFFIX_WAV_MODEL;
        init();
    }

    public void computeMaxFrames() {
        try {
            newMP3Player();
//            while (true) {
//                float wav = currentPlayer.getWavAndSkipFrame();
//
//                if(Math.abs( wav + 2) < Math.exp(-5)){
//                    break;
//                }
//                wavList.add(wav);
//                this.maxFrames++;
//            }
            while (currentPlayer.skipFrame()) {
                this.maxFrames++;
            }
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init() throws Exception{

        computeMaxFrames();

        currentFrame = 0;

        if(new File(this.signModelFileName).exists()){
            loadSplitList(this.signModelFileName);
        }else{
            synchronizedSignList.add(new SignModel(0, true));
            synchronizedSignList.add(new SignModel(this.maxFrames, true));
            saveSignModel();
        }

        if(new File(this.wavModelFileName).exists()){
            loadWavModel(this.wavModelFileName);
        }


    }

    public void loadWavModel(String fileName) throws Exception{
        File file = new File(fileName);
        InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file));

        WavModel wavModel = new GsonBuilder().create().fromJson(streamReader, WavModel.class);
        this.wavData = wavModel.getWavData();

    }

    public void saveWavModel(String fileName) throws Exception{
        File file = new File(fileName);
        if (!file.exists()) file.createNewFile();
        FileWriter fileWriter = new FileWriter(file);
        WavModel wavModel = new WavModel();
        wavModel.setWavData(this.wavData);
        fileWriter.write(new GsonBuilder().create().toJson(wavModel));
        fileWriter.flush();
        fileWriter.close();

    }

    public synchronized void pause(int currentFrame) {
        try {
            this.currentFrame = currentFrame;
            close();
            newMP3Player();
            int count = 0;
            while (count < this.currentFrame) {
                count++;
                currentPlayer.skipFrame();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void pause() {
        pause(this.currentFrame);
    }

    public synchronized void close() {
        try {
            currentPlayer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void newMP3Player() {
        try {
            this.currentPlayer = new MP3Player(new FileInputStream(new File(fileName)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void play() {

        pause();

        new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        boolean isFinished = currentPlayer.play(ONE_FRAME);
                        if (!isFinished) {
                            break;
                        }
                        currentFrame++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public synchronized void play(final int start, final int end) {

        pause(start);

        new Thread(new Runnable() {
            public void run() {
                try {
                    int count = end - start;
                    while (count > 0) {
                        boolean isFinished = currentPlayer.play(ONE_FRAME);
                        count--;
                        if (!isFinished) {
                            break;
                        }
                        currentFrame++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void addSignList(int sign, boolean used) {
            SignModel signModel = new SignModel(sign, used);
            this.synchronizedSignList.add(signModel);
            sortSignList();
    }

    private void sortSignList() {
        synchronized (this.synchronizedSignList) {
            Collections.sort(this.synchronizedSignList, new Comparator<SignModel>() {
                public int compare(SignModel o1, SignModel o2) {
                    return o1.getSign() - o2.getSign();
                }
            });
        }
    }

    public void saveSignModel() {

            try {
                File file = new File(this.signModelFileName);
                if (!file.exists()) file.createNewFile();
                FileWriter fileWriter = new FileWriter(file);
                SignSaveModel saveModel = new SignSaveModel();
                saveModel.setFilename(this.fileName);
                int size = this.synchronizedSignList.size();
                saveModel.setSiglist(this.synchronizedSignList.subList(0,size));

                fileWriter.write(new GsonBuilder().create().toJson(saveModel));
                fileWriter.flush();
                fileWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }


    }


    public void loadSplitList(String fileName) throws Exception {

            File file = new File(fileName);

            InputStreamReader streamReader = new InputStreamReader(new FileInputStream(file));

            SignSaveModel saveModel = new GsonBuilder().create().fromJson(streamReader, SignSaveModel.class);
            if (saveModel.getSiglist().get(0).getSign() != 0) {
                saveModel.getSiglist().add(new SignModel(0, true));
                saveModel.getSiglist().add(new SignModel(maxFrames, true));
            }
            this.synchronizedSignList = Collections.synchronizedList(saveModel.getSiglist());
            sortSignList();
    }

    public int[] getWavData() {
        try {
            if(this.wavData.length == 0){
                new Mp3Spliter().convToWav(this.fileName, this.filePath, TMP_FILENAME, new ConvSignModel(0, this.getMaxFrames()));
                String wavName = this.filePath + TMP_FILENAME + SUFFIX_WAV;
                WaveFileReader wav = new WaveFileReader(wavName);
                this.wavData = wav.readSampleData(this.getMaxFrames());
                File file = new File(wavName);
                if (file.exists()) {
                    file.delete();
                }
                this.saveWavModel(this.wavModelFileName);
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return this.wavData;
    }
}
