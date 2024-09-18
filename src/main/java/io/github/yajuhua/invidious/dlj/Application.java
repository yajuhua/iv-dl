package io.github.yajuhua.invidious.dlj;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import io.github.yajuhua.invidious.dlj.command.CommandInfo;
import io.github.yajuhua.invidious.dlj.command.CommandLineParser;
import io.github.yajuhua.invidious.dlj.command.Options;
import io.github.yajuhua.invidious.dlj.compiler.RunAllFeatForConfig;
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

@Slf4j
public class Application {

    public static boolean ignoreWarnLog;//忽略warn日志

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

    /**
     * 程序入口
     * @param args
     */
    public static void main(String[] args) throws Exception {

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

        //执行测试代码
        if (options.test){
            RunAllFeatForConfig.run();
        }

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

        //获取自定义节目序号
        List<Integer> itemsNumbers = CommandLineParser.getSelectItemsNumbers(parse.getOptions());


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

        //开始下载
        for (String url : allUrl) {
            download(parse.getFilter(),url,itemsNumbers);
        }
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

    //TODO 自定义yt-dlp可执行文件路径 --yt-dlp-path
    //TODO 若未安装yt-dlp将提示
}
