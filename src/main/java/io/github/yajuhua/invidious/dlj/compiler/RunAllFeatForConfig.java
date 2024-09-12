package io.github.yajuhua.invidious.dlj.compiler;

import ch.qos.logback.classic.Level;
import com.google.gson.Gson;
import io.github.yajuhua.invidious.dlj.Application;
import io.github.yajuhua.invidious.dlj.CustomDownloader;
import io.github.yajuhua.invidious.dlj.Info;
import io.github.yajuhua.invidious.dlj.command.Command;
import io.github.yajuhua.invidious.dlj.pojo.Video;
import io.github.yajuhua.invidious.wrapper.Invidious;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 运行所有功能以生成 native image 配置文件
 */
@Slf4j
public class RunAllFeatForConfig {

    public static void base(String[] args) throws Exception {
        //打印版本信息
        if (Command.hasVersion(args)){
            Application.printVersion();
            return;
        }

        if (Command.hasVerbose(args)){
            //打印各种调试信息
            Application.printVerbose();
        }

        if (Command.isIgnoreWarnLog(args)){
            //设置为error级别日志
            Application.ignoreWarnLog = true;
            Application.setLogLevel(Level.ERROR);
        }

        log.debug(Arrays.toString(args));

        //校验和过滤参数
        List<String> argList = Command.filterYtDlpArguments(args);

        //获取并设置代理
        Proxy proxy = Command.getProxy(args);
        if (proxy != null){
            Invidious.proxy = proxy;
        }

        //设置downloader
        Invidious.init(new CustomDownloader());

        //获取自定义项目数,如果有的话
        List<Integer> selectItemsNumbers = Command.getSelectItemsNumbers(args);

        //获取链接实例,如果链接是从实例复制出来的
        String url = Command.getUrl(args);
        String instance = Info.getInvidiousInstanceFromUrl(url);
        Invidious.setApi(instance);

        //分类,video/videos/streams/playlist
        log.info("downloading invidious api json");
        List<Video> videoList = new ArrayList<>();
        if (url.contains("/videos")){
            //视频列表
            videoList = Info.getVideos(url,selectItemsNumbers);
        } else if (url.contains("/streams")) {
            //直播列表
            videoList = Info.getStreams(url,selectItemsNumbers);
        } else if (url.contains("/playlist?list=")) {
            //playlist列表
            videoList = Info.getPlaylist(url,selectItemsNumbers);
        }else {
            //默认是单个视频
            videoList.add(Info.getVideo(url));
        }

        log.debug("videoList: {}",videoList.toString());

        //获取临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"));

        //将json数据写入临时目录
        Gson gson = new Gson();
        File tempJsonFile = new File(tempDir,System.currentTimeMillis() + ".json");
        System.out.println("[info] generate json file: " + tempJsonFile.getAbsolutePath());
        FileUtils.write(tempJsonFile,gson.toJson(videoList),"UTF-8");

        //执行cmd命令
        argList.add(0,"yt-dlp");
        argList.add("--load-info-json");
        argList.add(tempJsonFile.getAbsolutePath());

        log.debug("cmd: {}",argList.toString());

        // 创建
        Process process = Runtime.getRuntime().exec(Command.toArray(argList));

        // 启动进程
        BufferedReader bri = null;//info
        BufferedReader bre = null;//error
        try {
            String line;
            bri = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            while ((line = bri.readLine()) != null) {
                if (line.startsWith("[download]")) {
                    // 使用回车符回到行首，覆盖上一行
                    System.out.print("\r" + line);
                } else {
                    // 打印其他信息
                    System.out.println(line);
                }
            }
            int waitFor = process.waitFor();
            if (waitFor != 0){
                bre = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                while ((line = bre.readLine()) != null){
                    System.out.println(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //清理临时文件
            if (tempJsonFile.exists()){
                if (!tempJsonFile.delete()){
                    log.warn("无法删除临时文件: {}",tempJsonFile.getAbsolutePath());
                }
            }
            if (bri != null){
                bri.close();
            }
            if (bre != null){
                bre.close();
            }
        }
    }

    /**
     * 下载单个视频
     */
    public static void video() throws Exception {
        String url = "https://www.youtube.com/watch?v=fS2q-hZ-pyY";
        log.info("video: {}",url);
        String[] args = new String[]{"-f","m4a","-v",url};
        base(args);
    }

    /**
     * videos视频列表
     */
    public static void videos() throws Exception {
        String url = "https://www.youtube.com/@RADWIMPS_official/videos";
        log.info("videos: {}",url);
        String[] args = new String[]{"-f","m4a","-v","--playlist-items","1",url};
        base(args);
    }

    /**
     * streams直播列表
     */
    public static void streams() throws Exception {
        String url = "https://www.youtube.com/@TianLiangTimes/streams";
        log.info("streams: {}",url);
        String[] args = new String[]{"-f","m4a","-v","--playlist-items","2",url};
        base(args);
    }

    /**
     * playlist
     */
    public static void playlist() throws Exception {
        String url = "https://www.youtube.com/playlist?list=PLIKD2pVXF4VHEG_DSBTW_dZB2q6XH4VDb";
        log.info("playlist: {}",url);
        String[] args = new String[]{"-f","m4a","-v","--playlist-items","1",url};
        base(args);
    }

    /**
     * 版本输出
     */
    public static void version() throws Exception {
        String[] args = new String[]{"--version"};
        base(args);
    }

    /**
     * 忽略warn日志
     */
    public static void ignoreWarnLog() throws Exception {
        String url = "https://www.youtube.com/watch?v=fS2q-hZ-pyY";
        log.info("ignoreWarnLog: {}",url);
        String[] args = new String[]{"-f","m4a","--no-warnings",url};
        base(args);
    }

    /**
     * 批量下载
     */
    public static void batchFile() throws Exception {
        List<String> urlList = new ArrayList<>();
        urlList.add("https://www.youtube.com/watch?v=eDqfg_LexCQ");
        urlList.add("https://www.youtube.com/watch?v=fE6XAeZfAsk");
        FileUtils.writeLines(new File("a.txt"),urlList);
        String[] args = new String[]{"--batch-file","a.txt"};
        base(args);
    }

    /**
     * 使用所有功能，用于生成编译用的配置文件
     * @throws Exception
     */
    public static void run()throws Exception{
        log.info("start");
        video();
        videos();
        streams();
        playlist();
        version();
        ignoreWarnLog();
        log.info("end");
        System.exit(0);
    }


}
