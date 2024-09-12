package io.github.yajuhua.invidious.dlj;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.google.gson.Gson;
import io.github.yajuhua.invidious.dlj.command.Command;
import io.github.yajuhua.invidious.dlj.compiler.RunAllFeatForConfig;
import io.github.yajuhua.invidious.dlj.pojo.Video;
import io.github.yajuhua.invidious.wrapper.Invidious;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Proxy;
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

        //执行测试代码
        if (Command.toList(args).contains("--test")){
            RunAllFeatForConfig.run();
        }

        //打印版本信息
        if (Command.hasVersion(args)){
            printVersion();
            return;
        }

        if (Command.hasVerbose(args)){
            //打印各种调试信息
            printVerbose();
        }

        if (Command.isIgnoreWarnLog(args)){
            //设置为error级别日志
            ignoreWarnLog = true;
            setLogLevel(Level.ERROR);
        }

        log.debug(Arrays.toString(args));

        //获取批量下载文件中的链接，如果有的话
        List<String> urlList = new ArrayList<>();
        if (Command.hasBatchFile(args)){
            File batchFile = Command.getBatchFile(args);
            if (batchFile.exists()){
                urlList = FileUtils.readLines(batchFile,"UTF-8");
            }else {
                throw new Exception("找不到文件: " + batchFile);
            }
        }

        //校验和过滤参数
        List<String> argList = Command.filterYtDlpArguments(args);
        for (String url : urlList) {
            argList.add(url);
            download(Command.toArray(argList));
        }
    }

    /**
     * 下载
     * @param args
     * @throws Exception
     */
    public static void download(String[] args) throws Exception{
        List<String> argList = Command.toList(args);
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
            System.out.println();
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
