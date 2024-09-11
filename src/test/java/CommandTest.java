import io.github.yajuhua.invidious.dlj.command.Command;
import org.testng.annotations.Test;

import java.net.Proxy;
import java.util.List;

import static org.testng.AssertJUnit.*;

public class CommandTest {

    @Test
    public void getProxyHas() throws Exception {
        Proxy proxy = Command.getProxy(new String[]{"yt-dlp", "-J", "--proxy", "http://127.0.0.1:10809", "-f"
                , "bestvideo+bestaudio"});
        assertNotNull("无法获取代理",proxy);
    }

    @Test
    public void getProxyNo() throws Exception {
        Proxy proxy = Command.getProxy(new String[]{"yt-dlp", "-J","-f", "bestvideo+bestaudio"});
        assertTrue("不应该有代理的",proxy == null);
    }

    @Test
    public void hasSelectItems(){
        boolean hasSelectItems = Command.hasSelectItems(new String[]{"yt-dlp","--playlist-items","1,3"});
        assertTrue("无法判断是否有选择节目",hasSelectItems);
    }

    @Test
    public void getSelectItemsNumbers(){
        String[] args = {"yt-dlp","--proxy","http://127.0.0.1:10809","--playlist-items","1,3"};
        List<Integer> selectItemsNumbers = Command.getSelectItemsNumbers(args);
        assertTrue("--playlist-items解析失败",selectItemsNumbers.size() == 2);

        args = new String[]{"yt-dlp", "--proxy", "http://127.0.0.1:10809", "--playlist-start", "1","--playlist-end","3"};
        selectItemsNumbers = Command.getSelectItemsNumbers(args);
        assertTrue("--playlist-start和--playlist-end解析失败",selectItemsNumbers.size() == 3);

        args = new String[]{"yt-dlp", "--proxy", "http://127.0.0.1:10809", "--playlist-start", "3"};
        selectItemsNumbers = Command.getSelectItemsNumbers(args);
        assertTrue("--playlist-start解析失败",selectItemsNumbers.size() == 198);

        args = new String[]{"yt-dlp", "--proxy", "http://127.0.0.1:10809", "--playlist-end", "3"};
        selectItemsNumbers = Command.getSelectItemsNumbers(args);
        assertTrue("--playlist-end解析失败",selectItemsNumbers.size() == 3);
    }

    @Test
    public void hasVersion(){
        String[] args = {"yt-dlp","--proxy","http://127.0.0.1:10809","--version"};
        boolean hasVersion = Command.hasVersion(args);
        assertTrue("无法解析版本命令",hasVersion);
    }

    @Test
    public void getUrl(){
        String[] args = {"yt-dlp","--proxy","http://127.0.0.1:10809","https://www.youtube.com/watch?v=YLcz5hjqVLQ"};
        String url = Command.getUrl(args);
        assertNotNull("无法链接链接",url);
    }

    /**
     * 有些参数不能传给yt-dlp,需要过滤
     * <p>
     *     1.下载链接
     *     2.--playlist-items
     *     3.--version
     * <p/>
     */
    @Test
    public void filterYtDlpArguments(){
        String[] args = {"yt-dlp","--proxy","http://127.0.0.1:10809","--playlist-items","1,3","https://www.youtube.com/watch?v=YLcz5hjqVLQ"};
        List<String> argsList = Command.filterYtDlpArguments(args);
        assertFalse("无法过滤--playlist-items选项",argsList.contains("--playlist-items"));
    }

}
