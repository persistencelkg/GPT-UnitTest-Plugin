package org.lkg.util;

import java.io.*;
import java.util.Objects;

public class FileUtil {


    public static String readFile(String filename) throws IOException {
        InputStreamReader inputStreamReader = new InputStreamReader(Objects.requireNonNull(FileUtil.class.getResourceAsStream(filename)));
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            sb.append(line).append(System.lineSeparator());
        }
        bufferedReader.close();
        inputStreamReader.close();
        return sb.toString();
//        return sb.delete(sb.lastIndexOf(System.lineSeparator()), sb.length()).toString();
    }

    public static void main(String[] args) {
        try {
            System.out.println(readFile("/system-sample.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
