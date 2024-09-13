import com.google.gson.Gson;
import io.github.yajuhua.invidious.dlj.Info;
import io.github.yajuhua.invidious.dlj.pojo.Video;
import io.github.yajuhua.invidious.dlj.utils.FileUtils;
import io.github.yajuhua.invidious.wrapper.Invidious;
import org.testng.annotations.Test;

import java.io.File;
import java.net.*;
import java.util.Arrays;
import java.util.List;

import static org.testng.AssertJUnit.assertNotNull;

public class InfoTest {

//    @BeforeTest
    public void setProxy(){
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Win")){
            Invidious.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(10809));
        }
    }

    /**
     * 使用官方链接
     * @throws Exception
     */
    @Test
    public void getVideoFromOffice() throws Exception {
        Video video = Info.getVideo("https://www.youtube.com/watch?v=YLcz5hjqVLQ");
        assertNotNull("无法获取Video信息",video);
    }

    /**
     * 使用invidious 实例
     * @throws Exception
     */
    @Test
    public void getVideoFromInvidious() throws Exception {
        Video video = Info.getVideo("https://invidious.jing.rocks/watch?v=YLcz5hjqVLQ");
        assertNotNull("无法获取Video信息",video);
    }

    /**
     * 第一个视频
     * @throws Exception
     */
    @Test
    public void getVideosSingle() throws Exception {
        String url = "https://www.youtube.com/@%E5%A4%A7%E8%80%B3%E6%9C%B5TV/videos";
        List<Video> videos = Info.getVideos(url);
        assertNotNull("无法获取Videos信息",videos);
    }

    /**
     * 第一个视频
     * @throws Exception
     */
    @Test
    public void getVideosMore() throws Exception {
        String url = "https://www.youtube.com/@RADWIMPS_official/videos";
        List<Video> videos = Info.getVideos(url, Arrays.asList(1,3));
        assertNotNull("无法获取Videos信息",videos);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void getStreamsMore() throws Exception {
        String url = "https://www.youtube.com/@TianliangZhang/streams";
        List<Video> streams = Info.getStreams(url, Arrays.asList(1,3));
/*        Gson gson  = new Gson();
        File infoJson = new File("D:\\Tmp\\test-load-info-json\\streamsMore.json");
        FileUtils.write(infoJson,gson.toJson(streams),"UTF-8");*/
        assertNotNull("无法获取Streams信息",streams);
    }

    /**
     *
     * @throws Exception
     */
    @Test
    public void getPlaylist() throws Exception {
        String url = "https://www.youtube.com/playlist?list=PLMigSoFRkQUl-vLVtYIWNYst0YibGl1VX";
        List<Video> playlist = Info.getPlaylist(url, Arrays.asList(1,3));
        Gson gson  = new Gson();
        File infoJson = new File("D:\\Tmp\\test-load-info-json\\playlist.json");
        FileUtils.write(infoJson,gson.toJson(playlist),"UTF-8");
        assertNotNull("无法获取Playlist信息",playlist);
    }

}
