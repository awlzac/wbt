package wbtempest;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineEvent.Type;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * A singleton class that manages sound effects.
 * @author ugliest
 *
 */
public class SoundManager {

	private static SoundManager sm = null;
	private Map<Sound, GameSoundData> soundMap = null;
	
	public static SoundManager get() {
		if (sm == null)
			sm = new SoundManager();
		return sm;
	}
	
	private SoundManager() {
		soundMap = new HashMap<Sound, GameSoundData>();

        try {
        	soundMap.put(Sound.FIRE, loadSound("fire.aiff"));
        	soundMap.put(Sound.LEVELCLEAR, loadSound("levchg1.aiff"));
        	soundMap.put(Sound.ENEMYFIRE, loadSound("enemyfire.aiff"));
        	soundMap.put(Sound.CRAWLERDEATH, loadSound("crawldeath.aiff"));
        	soundMap.put(Sound.ENEMYDEATH, loadSound("enemydeath.aiff"));
        	soundMap.put(Sound.CRAWLERMOVE, loadSound("crawlding.aiff"));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
	}

	private static int BUFLEN = 1024;

	/**
	 * load the desired sound data into memory.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 * @throws UnsupportedAudioFileException
	 */
	private GameSoundData loadSound(String filename) throws IOException, UnsupportedAudioFileException {
		// should find sounds placed in root of jar file, which build file should do; 
		// in an ide, setting should be tweaked to see audio dir as on path. 
		// (e.g., in eclipse, run -> debug configs -> classpath -> add folder, 
		//  or place audio files in root of runtime dir, likely "bin")
    	InputStream is = this.getClass().getClassLoader().getResourceAsStream(filename);
    	is = new BufferedInputStream(is);

    	AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(is);
		AudioInputStream ais = AudioSystem.getAudioInputStream(is);

		AudioFormat audioFormat = fileFormat.getFormat();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int bufSize = BUFLEN * audioFormat.getFrameSize();
		byte[]	byteBuf = new byte[bufSize];
		while (true) {
			int	nRead = ais.read(byteBuf);
			if (nRead == -1)
				break;
			baos.write(byteBuf, 0, nRead);
		};
		ais.close();
		is.close();
		GameSoundData gsd = new GameSoundData();
		gsd.format = audioFormat;
		gsd.rawData = baos.toByteArray();
		baos.close();
		return gsd;
	}

	/**
	 * play the desired sound
	 * 
	 * @param s
	 */
	public void play(Sound s) {
        try {
        	// construct a new audioinputstream from our cached sound data, 
        	// and play it in a new thread
        	
        	GameSoundData gsd = soundMap.get(s);
    		ByteArrayInputStream bais = new ByteArrayInputStream(gsd.rawData);
    		AudioInputStream audio = new AudioInputStream(bais, gsd.format, gsd.rawData.length);
            (new Thread(new SoundPlayer(s, audio))).start();
        }
        catch(LineUnavailableException e) {
            // sometimes we run out of lines.  shrug.
        }
        catch(Exception e) {
            e.printStackTrace();
        }
	}
	
	private class GameSoundData {
		public AudioFormat format;
		public byte[] rawData;
	}

	/**
	 * the way sound is triggered in java, from what i can tell, is a fucking disaster.
	 * 
	 * so basically, we create a thread, which then plays the sound (in yet 
	 * another secondary thread), and then waits around until the second thread notifies
	 * it that it's done and mindlessly idling, so that we 
	 * can tell the second thread to close down.
	 * 
	 * @author ugliest
	 *
	 */
	private class SoundPlayer implements Runnable, LineListener{
		private Clip c;
		private Sound s;
		private boolean active = true;

		public SoundPlayer(Sound s, AudioInputStream ais)
			throws LineUnavailableException, IOException
		{
			this.s = s;
            AudioFormat format = ais.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            c = (Clip)AudioSystem.getLine(info);
            c.addLineListener(this);
            c.open(ais);
        	c.start();
		}
		
		@Override
		public void run (){
			// the sole purpose of this thread is to wait, to close the sound clip
			// after the sound plays and knows it's done, but isn't allowed to
			// close itself.
			while (active) {
				try {
					Thread.sleep(20);
				}
				catch (InterruptedException ie) {
					// yawn
				}
			}
			c.close();
		}
		
		@Override
		public void update (LineEvent e){
			Type t = e.getType();
			
			if (t == Type.STOP && active && e.getLine() == c) {
				active = false;
			}
		}
	}
}
