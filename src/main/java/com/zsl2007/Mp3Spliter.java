package com.zsl2007;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import com.zsl2007.model.ConvSignModel;
import com.zsl2007.model.SignModel;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.Obuffer;
import javazoom.jl.converter.*;
import java.util.*;
import ws.schild.jave.*;

/**
 * The <code>Converter</code> class implements the conversion of
 * an MPEG audio file to a .WAV file. To convert an MPEG audio stream,
 * just create an instance of this class and call the convert()
 * method, passing in the names of the input and output files. You can
 * pass in optional <code>ProgressListener</code> and
 * <code>Decoder.Params</code> objects also to customize the conversion.
 *
 * @author	MDM		12/12/99
 * @since	0.0.7
 */
public class Mp3Spliter
{
    /**
     * Creates a new converter instance.
     */
    public Mp3Spliter()
    {
    }

    public void convert(String sourceFile, String filePath, String fileName, ConvSignModel model) throws Exception {

        try{
            MP3Player mp3Player = new MP3Player(new FileInputStream(new File(sourceFile)));

            int count = 0;
            while(count < model.getStart()){
                count ++;
                mp3Player.skipFrame();
            }

            Bitstream stream = mp3Player.getBitstream();
            Header header = stream.readFrame();
            Obuffer output = null;
            Decoder decoder = new Decoder(null);
            String wavFileName = fileName + ".wav";
            String mp3FileName = fileName + ".mp3";

            int channels = (header.mode()==Header.SINGLE_CHANNEL) ? 1 : 2;
            int freq = header.frequency();
            output = new WaveFileObuffer(channels, freq, filePath+File.separator+ wavFileName);
            decoder.setOutputBuffer(output);

            count = model.getEnd() - model.getStart();
            while(count > 0){
                count --;
                Obuffer decoderOutput = decoder.decodeFrame(header, stream);
                // REVIEW: the way the output buffer is set
                // on the decoder is a bit dodgy. Even though
                // this exception should never happen, we test to be sure.
                if (decoderOutput!=output)
                    throw new InternalError("Output buffers are different.");
                stream.closeFrame();
                header = stream.readFrame();
            }
            output.close();
            wavToMp3(filePath + File.separator+ wavFileName, filePath + File.separator + mp3FileName);

            File file = new File(filePath+File.separator+ wavFileName);
            if(file.exists()){
                file.delete();
            }

            mp3Player.close();
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    public void convToWav(String sourceFile, String filePath, String fileName, ConvSignModel model) throws Exception {

        try{
            MP3Player mp3Player = new MP3Player(new FileInputStream(new File(sourceFile)));

            int count = 0;
            while(count < model.getStart()){
                count ++;
                mp3Player.skipFrame();
            }

            Bitstream stream = mp3Player.getBitstream();
            Header header = stream.readFrame();
            Obuffer output = null;
            Decoder decoder = new Decoder(null);
            String wavFileName = fileName + ".wav";

            int channels = (header.mode()==Header.SINGLE_CHANNEL) ? 1 : 2;
            int freq = header.frequency();
            output = new WaveFileObuffer(channels, freq, filePath+File.separator+ wavFileName);
            decoder.setOutputBuffer(output);

            count = model.getEnd() - model.getStart();
            while(count > 0){
                count --;
                Obuffer decoderOutput = decoder.decodeFrame(header, stream);
                if (decoderOutput!=output)
                    throw new InternalError("Output buffers are different.");
                stream.closeFrame();
                header = stream.readFrame();
            }

            output.close();
            mp3Player.close();
        }catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }


    public void wavToMp3(String source, String target) throws Exception{
        File sourceFile = new File(source);
        File targetFile = new File(target);
        AudioAttributes audio = new AudioAttributes();
        audio.setCodec("libmp3lame");
        audio.setBitRate(new Integer(128000));
        audio.setChannels(new Integer(2));
        audio.setSamplingRate(new Integer(44100));
        EncodingAttributes attrs = new EncodingAttributes();
        attrs.setFormat("mp3");
        attrs.setAudioAttributes(audio);
        Encoder encoder = new Encoder();
        encoder.encode(new MultimediaObject(sourceFile), targetFile, attrs);

    }

    protected int countFrames(InputStream in)
    {
        return -1;
    }


    protected InputStream openInput(String fileName)
            throws IOException
    {
        // ensure name is abstract path name
        File file = new File(fileName);
        InputStream fileIn = new FileInputStream(file);
        BufferedInputStream bufIn = new BufferedInputStream(fileIn);

        return bufIn;
    }


    /**
     * This interface is used by the Converter to provide
     * notification of tasks being carried out by the converter,
     * and to provide new information as it becomes available.
     */

    static public interface ProgressListener
    {
        public static final int	UPDATE_FRAME_COUNT = 1;

        /**
         * Conversion is complete. Param1 contains the time
         * to convert in milliseconds. Param2 contains the number
         * of MPEG audio frames converted.
         */
        public static final int UPDATE_CONVERT_COMPLETE = 2;


        /**
         * Notifies the listener that new information is available.
         *
         * @param updateID	Code indicating the information that has been
         *					updated.
         *
         * @param param1	Parameter whose value depends upon the update code.
         * @param param2	Parameter whose value depends upon the update code.
         *
         * The <code>updateID</code> parameter can take these values:
         *
         * UPDATE_FRAME_COUNT: param1 is the frame count, or -1 if not known.
         * UPDATE_CONVERT_COMPLETE: param1 is the conversion time, param2
         *		is the number of frames converted.
         */
        public void converterUpdate(int updateID, int param1, int param2);

        /**
         * If the converter wishes to make a first pass over the
         * audio frames, this is called as each frame is parsed.
         */
        public void parsedFrame(int frameNo, Header header);

        /**
         * This method is called after each frame has been read,
         * but before it has been decoded.
         *
         * @param frameNo	The 0-based sequence number of the frame.
         * @param header	The Header rerpesenting the frame just read.
         */
        public void readFrame(int frameNo, Header header);

        /**
         * This method is called after a frame has been decoded.
         *
         * @param frameNo	The 0-based sequence number of the frame.
         * @param header	The Header rerpesenting the frame just read.
         * @param o			The Obuffer the deocded data was written to.
         */
        public void decodedFrame(int frameNo, Header header, Obuffer o);

        /**
         * Called when an exception is thrown during while converting
         * a frame.
         *
         * @param	t	The <code>Throwable</code> instance that
         *				was thrown.
         *
         * @return <code>true</code> to continue processing, or false
         *			to abort conversion.
         *
         * If this method returns <code>false</code>, the exception
         * is propagated to the caller of the convert() method. If
         * <code>true</code> is returned, the exception is silently
         * ignored and the converter moves onto the next frame.
         */
        public boolean converterException(Throwable t);

    }


    /**
     * Implementation of <code>ProgressListener</code> that writes
     * notification text to a <code>PrintWriter</code>.
     */
    // REVIEW: i18n of text and order required.
    static public class PrintWriterProgressListener implements ProgressListener
    {
        static public final int	NO_DETAIL = 0;

        /**
         * Level of detail typically expected of expert
         * users.
         */
        static public final int EXPERT_DETAIL = 1;

        /**
         * Verbose detail.
         */
        static public final int VERBOSE_DETAIL = 2;

        /**
         * Debug detail. All frame read notifications are shown.
         */
        static public final int DEBUG_DETAIL = 7;

        static public final int MAX_DETAIL = 10;

        private PrintWriter pw;

        private int detailLevel;

        static public PrintWriterProgressListener newStdOut(int detail)
        {
            return new PrintWriterProgressListener(
                    new PrintWriter(System.out, true), detail);
        }

        public PrintWriterProgressListener(PrintWriter writer, int detailLevel)
        {
            this.pw = writer;
            this.detailLevel = detailLevel;
        }


        public boolean isDetail(int detail)
        {
            return (this.detailLevel >= detail);
        }

        public void converterUpdate(int updateID, int param1, int param2)
        {
            if (isDetail(VERBOSE_DETAIL))
            {
                switch (updateID)
                {
                    case UPDATE_CONVERT_COMPLETE:
                        // catch divide by zero errors.
                        if (param2==0)
                            param2 = 1;

                        pw.println();
                        pw.println("Converted "+param2+" frames in "+param1+" ms ("+
                                (param1/param2)+" ms per frame.)");
                }
            }
        }

        public void parsedFrame(int frameNo, Header header)
        {
            if ((frameNo==0) && isDetail(VERBOSE_DETAIL))
            {
                String headerString = header.toString();
                pw.println("File is a "+headerString);
            }
            else if (isDetail(MAX_DETAIL))
            {
                String headerString = header.toString();
                pw.println("Prased frame "+frameNo+": "+headerString);
            }
        }

        public void readFrame(int frameNo, Header header)
        {
            if ((frameNo==0) && isDetail(VERBOSE_DETAIL))
            {
                String headerString = header.toString();
                pw.println("File is a "+headerString);
            }
            else if (isDetail(MAX_DETAIL))
            {
                String headerString = header.toString();
                pw.println("Read frame "+frameNo+": "+headerString);
            }
        }

        public void decodedFrame(int frameNo, Header header, Obuffer o)
        {
            if (isDetail(MAX_DETAIL))
            {
                String headerString = header.toString();
                pw.println("Decoded frame "+frameNo+": "+headerString);
                pw.println("Output: "+o);
            }
            else if (isDetail(VERBOSE_DETAIL))
            {
                if (frameNo==0)
                {
                    pw.print("Converting.");
                    pw.flush();
                }

                if ((frameNo % 10)==0)
                {
                    pw.print('.');
                    pw.flush();
                }
            }
        }

        public boolean converterException(Throwable t)
        {
            if (this.detailLevel>NO_DETAIL)
            {
                t.printStackTrace(pw);
                pw.flush();
            }
            return false;
        }

    }


}