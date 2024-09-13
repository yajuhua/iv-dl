package io.github.yajuhua.invidious.dlj.utils;


import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 精简版文件处理工具类
 */
public class FileUtils {

    /**
     * 读取文件每行
     * @param file
     * @param charsetName
     * @return
     */
    public static List<String> readLines(File file,String charsetName){
        try(
            InputStreamReader is =
                    new InputStreamReader(
                            new FileInputStream(file),Charset.forName(charsetName))) {
            List<String> lines = new ArrayList<>();
            BufferedReader br = new BufferedReader(is);
            String line;
            while ((line = br.readLine()) != null){
                lines.add(line);
            }
            return lines;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将字符串写入文件
     * @param file
     * @param data
     * @param charsetName
     * @throws IOException
     */
    public static void write(File file, CharSequence data, String charsetName) throws IOException {
        try (OutputStreamWriter os = new FileWriter(file, Charset.forName(charsetName),false);
             BufferedWriter bufferedWriter = new BufferedWriter(os)){
            bufferedWriter.write(data.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 文件写入行
     * @param lines
     */
    public static void writeLines(File file, Collection<?> lines,String charsetName) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Object line : lines) {
            sb.append( line + "\n");
        }
        write(file,sb,charsetName);
    }


    /**
     * 文件写入行
     * @param lines
     */
    public static void writeLines(File file, Collection<?> lines) throws IOException {
        writeLines(file,lines,"UTF-8");
    }
}
