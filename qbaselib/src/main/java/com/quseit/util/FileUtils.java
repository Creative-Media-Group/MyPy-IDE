/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.quseit.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.system.ErrnoException;
import android.system.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility functions for handling files.
 *
 * @author Damon Kohler (damonkohler@gmail.com)
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    private FileUtils() {}

    public static String getScriptsRootPath(Context context){
        return getQyPath() + "/sl4a/scripts/";
    }

    public static String getCloudMapCachePath(Context context){
        return getAbsolutePath(context) + "/lib/.cloud_cache";
    }

    public static String getPyCachePath(Context context){
        return getAbsolutePath(context) + ".qpyc";
    }

    public static String getAbsoluteLogPath(Context context){
        return getAbsolutePath(context) + "/log/last.log";
    }

    public static String getLibDownloadTempPath(Context context){
        return getAbsolutePath(context) + "/cache";
    }

    public static String getAbsolutePath(Context context){
//        return context.getExternalFilesDir(null).getPath() + "/qpython";
        //return Environment.getExternalStorageDirectory().getPath() + "/qpython";
        return context.getExternalFilesDir("").getPath();
    }

    public static File getPath(Context context){
        return context.getExternalFilesDir(null);
    }

    public static String getQyPath(){
//        return context.getExternalFilesDir(null).getAbsolutePath();
        return Environment.getExternalStorageDirectory().getPath();
    }

    static public boolean externalStorageMounted() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public static int chmod(File path, int mode) throws Exception {
        Class<?> fileUtils = Class.forName("android.os.FileUtils");
        Method setPermissions = fileUtils.getMethod("setPermissions", String.class, int.class, int.class, int.class);
        Object invokePer = setPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
        if (invokePer instanceof Integer){
            return (Integer) invokePer;
        }else {
            return 0;
        }
//        return (Integer) setPermissions.invoke(null, path.getAbsolutePath(), mode, -1, -1);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void setPermission(File file) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);

        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);

        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);

        Files.setPosixFilePermissions(file.toPath(), perms);
    }

    public static boolean recursiveChmod(File root, int mode) throws Exception {
        boolean success = chmod(root, mode) == 0;
        File[] files = root.listFiles();
        if (files != null){
            for (File path : files) {
                if (path.isDirectory()) {
                    success = recursiveChmod(path, mode);
                }
                success &= (chmod(path, mode) == 0);
            }
        }
        return success;
    }

    public static boolean delete(File path) {
        boolean result = true;
        if (path.exists()) {
            if (path.isDirectory()) {
                File[] files = path.listFiles();
                if (files != null){
                    for (File child : files) {
                        result &= delete(child);
                    }
                }
                // Delete empty directory.
                result &= path.delete();
            }
            if (path.isFile()) {
                result &= path.delete();
            }
            if (!result) {
                Log.e(TAG, "Delete failed;");
            }
            return result;
        } else {
            Log.e(TAG, "File does not exist.");
            return false;
        }
    }

    public static File copyFromStream(String name, InputStream input) {
        if (name == null || name.length() == 0) {
            Log.e(TAG, "No script name specified.");
            return null;
        }
        File file = new File(name);
        if (!makeDirectories(file.getParentFile(), 0755)) {
            return null;
        }
        try {
            OutputStream output = new FileOutputStream(file);
            IoUtils.copy(input, output);
        } catch (Exception e) {
            Log.e(TAG, e);
            return null;
        }
        return file;
    }

    public static boolean makeDirectories(File directory, int mode) {
        File parent = directory;
        while (parent.getParentFile() != null && !parent.exists()) {
            parent = parent.getParentFile();
        }
        if (!directory.exists()) {
            Log.d(TAG, "Creating directory: " + directory.getName());
            if (!directory.mkdirs()) {
                Log.e(TAG, "Failed to create directory.");
                return false;
            }
        }
        try {
            recursiveChmod(parent, mode);
        } catch (Exception e) {
            Log.e(TAG, e);
            return false;
        }
        return true;
    }

//    public static File getExternalDownload() {
//        try {
//            Class<?> c = Class.forName("android.os.Environment");
//            Method m = c.getDeclaredMethod("getExternalStoragePublicDirectory", String.class);
//            String download = c.getDeclaredField("DIRECTORY_DOWNLOADS").get(null).toString();
//            return (File) m.invoke(null, download);
//        } catch (Exception e) {
////            return new File(Environment.getExternalStorageDirectory(), "Download");
//            return new File(Environment.getExternalStorageDirectory(), "Download");
//        }
//    }

    public static boolean rename(File file, String name) {
        return file.renameTo(new File(file.getParent(), name));
    }

    public static String readToString(File file) throws IOException {
        if (file == null || !file.exists()) {
            return null;
        }
        FileReader reader = new FileReader(file);
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[1024 * 4];
        int numRead = 0;
        while ((numRead = reader.read(buffer)) > -1) {
            out.append(String.valueOf(buffer, 0, numRead));
        }
        reader.close();
        return out.toString();
    }

    public static String readFromAssetsFile(Context context, String name) throws IOException {
        AssetManager am = context.getAssets();
        BufferedReader reader = new BufferedReader(new InputStreamReader(am.open(name)));
        String line;
        StringBuilder builder = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public static void lnOrcopy(File src, File dst, int sdk) throws IOException, ErrnoException {

        if (sdk>=21) {
            Os.symlink(src.getAbsolutePath(), dst.getAbsolutePath());
        } else {
            FileInputStream inStream = new FileInputStream(src);
            FileOutputStream outStream = new FileOutputStream(dst);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
        }
    }

}
