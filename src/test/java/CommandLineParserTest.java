import io.github.yajuhua.invidious.dlj.command.CommandInfo;
import io.github.yajuhua.invidious.dlj.command.CommandLineParser;
import org.testng.annotations.Test;

import java.net.Proxy;
import java.util.List;

import static org.testng.AssertJUnit.*;

public class CommandLineParserTest {

    @Test
    public void parse() throws Exception {
        String[] args = {"-h","-a","a.txt","--iv-dl-version"};
        CommandInfo parse = CommandLineParser.parse(args);
        System.out.println(parse);
    }

    /**
     * 获取代理
     * @throws Exception
     */
    @Test
    public void proxy() throws Exception {
        String[] args = {"-h","-a","a.txt","--iv-dl-version","--playlist-items","1,2,3","--proxy","http://127.0.0.1:10809"};
        Proxy proxy = CommandLineParser.getProxy(args);
        assertNotNull("无法获取代理",proxy);
    }

    /**
     * 解析字符串集合
     */
    @Test
    public void parseList() throws Exception {
        String[] args = {"-h","-a","a.txt","--iv-dl-version","--playlist-items","1-3"};
        CommandInfo parse = CommandLineParser.parse(args);
        List<Integer> itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());
        assertTrue("解析集合失败",itemsNumbers.size() == 3);

        args = new String[]{"-h", "-a", "a.txt", "--iv-dl-version", "--playlist-start", "1"};
        parse = CommandLineParser.parse(args);
        itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());
        assertTrue("解析集合失败",itemsNumbers.size() == 200);

        args = new String[]{"-h", "-a", "a.txt", "--iv-dl-version", "--playlist-start", "2","--playlist-end","5"};
        parse = CommandLineParser.parse(args);
        itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());
        assertTrue("解析集合失败",itemsNumbers.size() == 4);
    }
}
