/* This file is based on code from the Orbot project. */
/* Copyright (c) 2009, Nathan Freitas, Orbot / The Guardian Project - http://openideals.com/guardian */

package org.neg9.internetsetupwizard;

import android.util.Log;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class BinaryInstaller
{
	public final static String TAG = "BinaryInstaller";

	public final static String ST_APP_USERNAME = "org.neg9.internetsetupwizard";
	public final static String ST_BINARY_ASSET_KEY = "st-backend";

	public final static String ASSETS_BASE = "assets/";

	public final static String SHELL_CMD_CHMOD = "chmod";
	public final static String CHMOD_EXE_VALUE = "777";

    public final static int FILE_WRITE_BUFFER_SIZE = 2048;

	private String installPath = null;
	private String apkPath = null;

	public BinaryInstaller ()
	{
        this.installPath = "/data/data/" + ST_APP_USERNAME + "/";
        this.apkPath     = findAPK();
	}

	public void start (boolean force) throws Exception
    {
        boolean binaryExists = new File(installPath + ST_BINARY_ASSET_KEY).exists();
		Log.d(TAG, "binary exists=" + binaryExists);

		if ((!binaryExists) || force) {
			installFromZip();
            fixPermissions();
        }
	}

	private void installFromZip ()
	{
		try {
			ZipFile zip = new ZipFile(apkPath);

			ZipEntry zipen = zip.getEntry(ASSETS_BASE + ST_BINARY_ASSET_KEY);
			streamToFile(zip.getInputStream(zipen),installPath + ST_BINARY_ASSET_KEY);

			zip.close();

			Log.d(TAG,"SUCCESS: unzipped binaries from apk");
		} catch (IOException ioe) {
			Log.d(TAG,"FAIL: unable to unzip binaries from apk",ioe);
		}
	}

    private void fixPermissions () throws Exception
    {
        Log.d(TAG, "(re)Setting permission on binary");

        String binaryPath = installPath + ST_BINARY_ASSET_KEY;

        String[] cmd1 = {SHELL_CMD_CHMOD + ' ' + CHMOD_EXE_VALUE + ' ' + binaryPath};
		doShellCommand(cmd1, null, false, true);
    }

    private static void streamToFile(InputStream stm, String targetFilename)
    {
        FileOutputStream stmOut = null;

        byte[] buffer = new byte[FILE_WRITE_BUFFER_SIZE];

        int bytecount;

        File outFile = new File(targetFilename);

        try {
            outFile.createNewFile();
        	stmOut = new FileOutputStream(outFile);
        } catch (java.io.IOException e) {
        	Log.d(TAG,"Error opening output file " + targetFilename,e);
        	return;
        }

        try {
            while ((bytecount = stm.read(buffer)) > 0) {
                stmOut.write(buffer, 0, bytecount);
            }
            stmOut.close();
        } catch (java.io.IOException e) {
            Log.d(TAG,"Error writing output file '" + targetFilename + "': " + e.toString());
            return;
        }
    }

    private String findAPK ()
    {
        String apkBase = "/data/app/";

        String APK_EXT = ".apk";

        int MAX_TRIES = 10;

        String buildPath = apkBase + ST_APP_USERNAME + APK_EXT;
        Log.i(TAG, "Checking APK location: " + buildPath);

        File fileApk = new File(buildPath);

        if (fileApk.exists())
            return fileApk.getAbsolutePath();

        for (int i = 0; i < MAX_TRIES; i++)
        {
            buildPath = apkBase + ST_APP_USERNAME + '-' + i + APK_EXT;
            fileApk = new File(buildPath);

            Log.i(TAG, "Checking APK location: " + buildPath);

            if (fileApk.exists())
                return fileApk.getAbsolutePath();
        }

        String apkBaseExt = "/mnt/asec/" + ST_APP_USERNAME;
        String pkgFile = "/pkg.apk";

        buildPath = apkBaseExt + pkgFile;
        fileApk = new File(buildPath);

        Log.i(TAG, "Checking external storage APK location: " + buildPath);

        if (fileApk.exists())
            return fileApk.getAbsolutePath();

        for (int i = 0; i < MAX_TRIES; i++)
        {
            buildPath = apkBaseExt + '-' + i + pkgFile;
            fileApk = new File(buildPath);

            Log.i(TAG, "Checking external APK location: " + buildPath);

            if (fileApk.exists())
                return fileApk.getAbsolutePath();
        }


        apkBase = "/sd-ext/app/";

        APK_EXT = ".apk";

        MAX_TRIES = 10;

        buildPath = apkBase + ST_APP_USERNAME + APK_EXT;
        Log.i(TAG, "Checking Apps2SD APK location: " + buildPath);

        fileApk = new File(buildPath);

        if (fileApk.exists())
            return fileApk.getAbsolutePath();

        for (int i = 0; i < MAX_TRIES; i++)
        {
            buildPath = apkBase + ST_APP_USERNAME + '-' + i + APK_EXT;
            fileApk = new File(buildPath);

            Log.i(TAG, "Checking Apps2SD location: " + buildPath);

            if (fileApk.exists())
                return fileApk.getAbsolutePath();
        }

        return null;
    }
    
    private static int doShellCommand(String[] cmds, StringBuilder log, boolean runAsRoot, boolean waitFor) throws Exception
    {
        Process proc = null;
        int exitCode = -1;

        if (runAsRoot)
            proc = Runtime.getRuntime().exec("su");
        else
            proc = Runtime.getRuntime().exec("sh");


        OutputStreamWriter out = new OutputStreamWriter(proc.getOutputStream());

        for (int i = 0; i < cmds.length; i++)
        {
            out.write(cmds[i]);
            out.write("\n");
        }

        out.flush();
        out.write("exit\n");
        out.flush();

        if (waitFor)
        {
            final char buf[] = new char[10];

            // Consume the "stdout"
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            int read=0;
            while ((read=reader.read(buf)) != -1) {
                if (log != null) log.append(buf, 0, read);
            }

            // Consume the "stderr"
            reader = new InputStreamReader(proc.getErrorStream());
            read=0;
            while ((read=reader.read(buf)) != -1) {
                if (log != null) log.append(buf, 0, read);
            }

            exitCode = proc.waitFor();

            if (log != null) {
                log.append("process exit code: ");
                log.append(exitCode);
                log.append("\n");
            }
        }

        return exitCode;

    }
}
