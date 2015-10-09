package net.csdn.e28sean.ccpartner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import net.csdn.e28sean.ccpartner.R;

import xqwlight.Position;

import android.util.Log;

public class CcUtil {

	private final static String TAG = "ccpartner";
	
	public static int makeFileExecutable( File file ) {
		int ret = 0;
		String cmd = "/system/bin/chmod 744 " + file.getAbsolutePath();
		try {
		    Process process = Runtime.getRuntime().exec(cmd);
		    process.waitFor();
		    process.exitValue();
		    process.destroy();
		} catch ( Exception e )  {
			Log.e(TAG, e.toString());
			ret = -1;
		}
		return ret;
	}
	
	public static int writeToFile(InputStream in, File file) {
		int ret = 0;
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		byte[] buffer = new byte[4096];
		int length;
		try {
			bis = new BufferedInputStream( in );
			bos = new BufferedOutputStream( new FileOutputStream(file) );
			while( (length = bis.read(buffer)) > 0 ) {
				bos.write(buffer, 0, length);
			}
		} catch ( Exception e ) {
			Log.e(TAG, "Fail to write input stream to file: " + file.getAbsolutePath());
			ret = -1;
		} finally {
			if( bis != null ) {
				try {
					bis.close();
				} catch ( Exception e ) {
					
				}
			}
			
			if( bos != null ) {
				try {
					bos.close();
				} catch ( Exception e ) {
					
				}
			}
		}
		
		return ret;
	}
	
	public static String move2Coord(int mv) {
		int src = Position.SRC(mv);
		int dst = Position.DST(mv);
		char[] data = new char[4];
		data[0] = (char)((src % 16) + 94);
		data[1] = (char)( 60 - src / 16 );
		data[2] = (char)((dst % 16) + 94);
		data[3] = (char)( 60 - dst / 16 );
		String result = new String(data);
		return result;
	}
	
	public static int coord2Move(String coord) {
		int src = (60 - coord.charAt(1)) * 16 + coord.charAt(0) - 94;
		int dst = (60 - coord.charAt(3)) * 16 + coord.charAt(2) - 94;
		int result = Position.MOVE(src, dst); 
		return result;
	}
}
