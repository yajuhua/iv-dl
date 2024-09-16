package io.github.yajuhua.invidious.dlj.compiler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import io.github.yajuhua.invidious.dlj.Application;
import io.github.yajuhua.invidious.dlj.CustomDownloader;
import io.github.yajuhua.invidious.dlj.Info;
import io.github.yajuhua.invidious.dlj.command.CommandInfo;
import io.github.yajuhua.invidious.dlj.command.CommandLineParser;
import io.github.yajuhua.invidious.dlj.command.Options;
import io.github.yajuhua.invidious.dlj.pojo.Video;
import io.github.yajuhua.invidious.dlj.utils.FileUtils;
import io.github.yajuhua.invidious.wrapper.Invidious;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 运行所有功能以生成 native image 配置文件
 */
@Slf4j
public class RunAllFeatForConfig {

    public static boolean ignoreWarnLog;//忽略warn日志

    public static void base(String[] args) throws Exception {
        //解析命令
        CommandInfo parse = CommandLineParser.parse(args);

        //打印帮助
        Options options = parse.getOptions();
        if (options.ivDlHelp){
            System.out.println("--iv-dl-help            帮助");
            System.out.println("--iv-dl-version         版本信息");
            System.out.println("--yt-dlp-path           yt-dlp可执行文件路径");
            return;
        }

        //yt-dlp路径
        System.setProperty("yt-dlp",options.ytDlpPath);

        //打印版本信息
        if (options.ivDlVersion){
            printVersion();
            return;
        }

        //打印各种调试信息
        if (options.verbose){
            printVerbose();
        }

        //设置为error级别日志
        if (options.noWarnings){
            ignoreWarnLog = true;
            setLogLevel(Level.ERROR);
        }

        log.debug(Arrays.toString(args));

        //设置代理
        if (options.proxy != null){
            Invidious.proxy = CommandLineParser.getProxy(args);
        }

        //获取所有链接
        List<String> allUrl = CommandLineParser.getAllUrl(args);

        //没有下载时，可能是输出yt-dlp版本信息之类的
        if (allUrl.isEmpty()){
            List<String> filter = parse.getFilter();
            filter.add(0, parse.getOptions().ytDlpPath);
            // 创建
            Process process = Runtime.getRuntime().exec(CommandLineParser.toArray(filter));

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
                if (waitFor != 0) {
                    bre = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                    while ((line = bre.readLine()) != null){
                        System.out.println(line);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }

        //获取自定义节目序号
        List<Integer> itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());

        //开始下载
        for (String url : allUrl) {
            download(parse.getFilter(),url,itemsNumbers);
        }
    }

    /**
     * 下载单个视频
     */
    public static void video() throws Exception {
        String url = "https://www.youtube.com/watch?v=_r6CgaFNAGg";
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
     * yt-dlp版本输出
     */
    public static void ytDlpVersion() throws Exception {
        String[] args = new String[]{"--version"};
        base(args);
    }

    /**
     * iv-dl版本输出
     */
    public static void ivDlVersion() throws Exception {
        String[] args = new String[]{"--iv-dl-version"};
        base(args);
    }


    /**
     * iv-dl帮助输出
     */
    public static void ivDlHelp() throws Exception {
        String[] args = new String[]{"--iv-dl-help"};
        base(args);
    }

    /**
     * 忽略warn日志
     */
    public static void ignoreWarnLog() throws Exception {
        String url = "https://www.youtube.com/watch?v=_r6CgaFNAGg";
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
        CommandInfo parse = CommandLineParser.parse(args);
        List<String> allUrl = CommandLineParser.getAllUrl(args);
        List<Integer> itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());
        for (String url : allUrl) {
            download(parse.getFilter(),url,itemsNumbers);
        }
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
        ytDlpVersion();
        ivDlVersion();
        ivDlHelp();
        ignoreWarnLog();
        batchFile();
        log.info("end");
        System.exit(0);
    }

    /**
     * 下载
     * @param
     * @throws Exception
     */
    public static void download(List<String> ytDlpOption,String url,List<Integer> selectItemsNumbers) throws Exception{

        //设置downloader
        Invidious.init(new CustomDownloader());

        //获取链接实例,如果链接是从实例复制出来的
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
        } else if (url.startsWith("https://www.youtube.com/playlist?list=")) {
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

        //移除下载链接

        //执行cmd命令
        String execFile = System.getProperty("yt-dlp");
        ytDlpOption.add(0,execFile);
        ytDlpOption.add("--load-info-json");
        ytDlpOption.add(tempJsonFile.getAbsolutePath());

        log.debug("cmd: {}",ytDlpOption.toString());

        // 创建
        Process process = Runtime.getRuntime().exec(CommandLineParser.toArray(ytDlpOption));

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
            if (waitFor != 0) {
                bre = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));
                while ((line = bre.readLine()) != null){
                    System.out.println(line);
                }
            }
            System.out.println();
        } catch (Exception e) {
            log.error(e.getMessage());
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
     * 打印应用版本信息
     */
    public static void printVersion(){
        Properties properties = getProperties();
        String version = properties.getProperty("version");
        System.out.println(version);
    }

    /**
     * 设置日志级别
     * @param level
     */
    public static void setLogLevel(Level level){
        // 获取 LoggerContext
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 获取根 Logger
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // 设置日志级别为 DEBUG
        rootLogger.setLevel(level);
    }

    /**
     * 打印调试信息
     */
    public static void printVerbose(){
        setLogLevel(Level.DEBUG);
    }

    /**
     * 获取应用属性
     * @return
     */
    public static Properties getProperties(){
        Properties properties = new Properties();
        try( InputStream inputStream = Application.class.getClassLoader()
                .getResourceAsStream("application.properties");) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
