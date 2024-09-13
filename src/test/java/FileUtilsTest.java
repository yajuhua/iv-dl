import io.github.yajuhua.invidious.dlj.utils.FileUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtilsTest {

    /**
     * 将字符串写入文件
     */
    @Test
    public void write() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tempDir,"write.txt");
        System.out.println(file);
        String str = "测试 write\nhello";
        FileUtils.write(file,str,"UTF-8");
    }

    /**
     * 将字符串写入文件
     */
    @Test
    public void readLines() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tempDir,"write.txt");
        System.out.println(file);
        String str = "测试 write\nhello";
        FileUtils.write(file,str,"UTF-8");
        List<String> lines = FileUtils.readLines(file, "UTF-8");
        for (String line : lines) {
            System.out.println(line);
        }
    }

    @Test
    public void writeLines() throws IOException {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File file = new File(tempDir,"writeLines.txt");
        System.out.println(file);
        List<String> lines = new ArrayList<>();
        lines.add("1");
        lines.add("2");
        lines.add("3");
        lines.add("4");
        FileUtils.writeLines(file,lines);
    }
}
