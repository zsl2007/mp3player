/*
 * 11/19/04		1.0 moved to LGPL.
 * 29/01/00		Initial version. mdm@techie.com
 *-----------------------------------------------------------------------
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as published
 *   by the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Library General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the Free Software
 *   Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *----------------------------------------------------------------------
 */

package com.zsl2007;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.*;

/**
 * The <code>Player</code> class implements a simple player for playback
 * of an MPEG audio stream.
 *
 * @author	Mat McGowan
 * @since	0.0.8
 */

// REVIEW: the audio device should not be opened until the
// first MPEG audio frame has been decoded.
public class MP3Player
{
    /**
     * The current frame number.
     */
    private int frame = 0;

    /**
     * The MPEG audio bitstream.
     */
    // javac blank final bug.
    /*final*/ private Bitstream		bitstream;

    /**
     * The MPEG audio decoder.
     */
    /*final*/ private Decoder		decoder;

    public int getFrame() {
        return frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public Bitstream getBitstream() {
        return bitstream;
    }

    public void setBitstream(Bitstream bitstream) {
        this.bitstream = bitstream;
    }

    public Decoder getDecoder() {
        return decoder;
    }

    public void setDecoder(Decoder decoder) {
        this.decoder = decoder;
    }

    public AudioDevice getAudio() {
        return audio;
    }

    public void setAudio(AudioDevice audio) {
        this.audio = audio;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public int getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(int lastPosition) {
        this.lastPosition = lastPosition;
    }

    /**
     * The AudioDevice the audio samples are written to.
     */
    private AudioDevice	audio;

    /**
     * Has the player been closed?
     */
    private boolean		closed = false;

    /**
     * Has the player played back all frames from the stream?
     */
    private boolean		complete = false;

    private int			lastPosition = 0;

    /**
     * Creates a new <code>Player</code> instance.
     */
    public MP3Player(InputStream stream) throws JavaLayerException
    {
        this(stream, null);
    }

    public MP3Player(InputStream stream, AudioDevice device) throws JavaLayerException
    {
        bitstream = new Bitstream(stream);
        decoder = new Decoder();

        if (device!=null)
        {
            audio = device;
        }
        else
        {
            FactoryRegistry r = FactoryRegistry.systemRegistry();
            audio = r.createAudioDevice();
        }
        audio.open(decoder);
    }

    public void play() throws JavaLayerException
    {
        play(Integer.MAX_VALUE);
    }

    /**
     * Plays a number of MPEG audio frames.
     *
     * @param frames	The number of frames to play.
     * @return	true if the last frame was played, or false if there are
     *			more frames.
     */
    public boolean play(int frames) throws JavaLayerException
    {
        boolean ret = true;

        while (frames-- > 0 && ret)
        {
            ret = decodeFrame();
        }

        if (!ret)
        {
            // last frame, ensure all data flushed to the audio device.
            AudioDevice out = audio;
            if (out!=null)
            {
                out.flush();
                synchronized (this)
                {
                    complete = (!closed);
                    close();
                }
            }
        }
        return ret;
    }

    /**
     * Cloases this player. Any audio currently playing is stopped
     * immediately.
     */
    public synchronized void close()
    {
        AudioDevice out = audio;
        if (out!=null)
        {
            closed = true;
            audio = null;
            // this may fail, so ensure object state is set up before
            // calling this method.
            out.close();
            lastPosition = out.getPosition();
            try
            {
                bitstream.close();
            }
            catch (BitstreamException ex)
            {
            }
        }
    }

    /**
     * Returns the completed status of this player.
     *
     * @return	true if all available MPEG audio frames have been
     *			decoded, or false otherwise.
     */
    public synchronized boolean isComplete()
    {
        return complete;
    }

    /**
     * Retrieves the position in milliseconds of the current audio
     * sample being played. This method delegates to the <code>
     * AudioDevice</code> that is used by this player to sound
     * the decoded audio samples.
     */
    public int getPosition()
    {
        int position = lastPosition;

        AudioDevice out = audio;
        if (out!=null)
        {
            position = out.getPosition();
        }
        return position;
    }

    /**
     * Decodes a single frame.
     *
     * @return true if there are no more frames to decode, false otherwise.
     */
    protected boolean decodeFrame() throws JavaLayerException
    {
        try
        {
            AudioDevice out = audio;
            if (out==null)
                return false;

            Header h = bitstream.readFrame();

            if (h==null)
                return false;

            // sample buffer set when decoder constructed
            SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);


            synchronized (this)
            {
                out = audio;
                if (out!=null)
                {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }

            bitstream.closeFrame();
        }
        catch (RuntimeException ex)
        {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
/*
		catch (IOException ex)
		{
			System.out.println("exception decoding audio frame: "+ex);
			return false;
		}
		catch (BitstreamException bitex)
		{
			System.out.println("exception decoding audio frame: "+bitex);
			return false;
		}
		catch (DecoderException decex)
		{
			System.out.println("exception decoding audio frame: "+decex);
			return false;
		}
*/
        return true;
    }

    public boolean skipFrame() throws JavaLayerException {
        Header h = this.bitstream.readFrame();

        if (h == null) {
            return false;
        } else {
            this.bitstream.closeFrame();
            return true;
        }
    }

    public float getWavAndSkipFrame() throws JavaLayerException{

        Header h = this.bitstream.readFrame();

        if (h == null) {
            return -2;
        } else {
            try{
                float num = 0;
                SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
                for(int i = 0; i < output.getBufferLength(); ++i){
                    num += output.getBuffer()[i];
                }
                this.bitstream.closeFrame();
                return num/output.getBufferLength();
            }catch (Exception e){
                e.printStackTrace();
                throw new JavaLayerException("Exception decoding audio frame", e);
            }

        }
    }

}
