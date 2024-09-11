package io.github.yajuhua.invidious.dlj.command;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 命令处理
 */
public class Command {
    /**
     * 获取命令中的代理如果有的话
     * @param args
     * @return没有就返回null
     */
    public static Proxy getProxy(String[] args) throws Exception {
        //TODO 获取系统代理
        Proxy proxy = null;
       if (hasProxy(args)){
           List<String> argList = toList(args);
           String proxyStr = argList.get(argList.indexOf("--proxy") + 1);
           URL url = new URL(proxyStr);
           int port = url.getPort();
           String host = url.getHost();
           InetSocketAddress address = new InetSocketAddress(host, port);
           if (proxyStr.startsWith("http")){
               proxy = new Proxy(Proxy.Type.HTTP,address);
           } else if (proxyStr.startsWith("socks5") || proxyStr.startsWith("socks")) {
               proxy = new Proxy(Proxy.Type.HTTP,address);
           }
       }
        return proxy;
    }

    /**
     * 判断是否有代理
     * @param args
     * @return
     */
    public static boolean hasProxy(String[] args){
        List<String> argList = toList(args);
        return argList.contains("--proxy");
    }

    /**
     * 将数组转换成List集合
     * @param args
     * @return
     */
    public static List<String> toList(String[] args){
        List<String> argList = new ArrayList<String>();
        if (args == null || args.length == 0){
            throw new RuntimeException("数组转换成List集合失败");
        }
        for (String arg : args) {
            argList.add(arg);
        }
        return argList;
    }

    /**
     * 判断是否有选择节目
     * @param args
     * @return
     */
    public static boolean hasSelectItems(String[] args){
        List<String> list = Arrays.asList("--playlist-items", "--playlist-start", "--playlist-end");
        List<String> argList = toList(args);
        //是否包含指定的任意一个元素
        return list.stream().anyMatch(argList::contains);
    }

    /**
     * 获取选择节目的序号
     * @param args
     * @return
     */
    public static List<Integer> getSelectItemsNumbers(String[] args){
        List<String> argList = toList(args);
        List<Integer> selectItemsNumbers = new ArrayList<>();

        if (argList.contains("--playlist-items")){
            String s = argList.get(argList.indexOf("--playlist-items") + 1);
            selectItemsNumbers = Arrays.asList(s.split("\\,")).stream()
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

        } else if (argList.contains("--playlist-start") && argList.contains("--playlist-end")) {
            //获取开始和结束索引
            Integer startIndex = Integer.parseInt(argList.get(argList.indexOf("--playlist-start") + 1));
            Integer endIndex = Integer.parseInt(argList.get(argList.indexOf("--playlist-end") + 1));
            //生成区间数字集合
            selectItemsNumbers = IntStream.range(startIndex,endIndex + 1)
                    .boxed()
                    .collect(Collectors.toList());

        } else if (argList.contains("--playlist-start")) {
            //最多也就200个视频
            int startIndex = Integer.parseInt(argList.get(argList.indexOf("--playlist-start") + 1));
            //生成区间数字集合
            selectItemsNumbers = IntStream.range(startIndex,201)
                    .boxed()
                    .collect(Collectors.toList());

        } else if (argList.contains("--playlist-end")) {
            int endIndex = Integer.parseInt(argList.get(argList.indexOf("--playlist-end") + 1));
            //生成区间数字集合
            selectItemsNumbers = IntStream.range(1,endIndex + 1)
                    .boxed()
                    .collect(Collectors.toList());
        }else {
            //默认选择第一个
            selectItemsNumbers.add(1);
        }
        return selectItemsNumbers;
    }

    /**
     * 判断命令中是否包含--version
     * @param args
     * @return
     */
    public static boolean hasVersion(String[] args){
        return toList(args).contains("--version");
    }

    /**
     * 获取下载链接
     * @param args
     * @return
     */
    public static String getUrl(String[] args){
        List<String> argList = toList(args);
        if (argList.isEmpty()){
            throw new RuntimeException("请输入URL");
        }
        return argList.get(argList.size() - 1);
    }

    /**
     * 有些参数不能传给yt-dlp,需要过滤
     * <p>
     *     1.下载链接
     *     2.--playlist-items
     *     3.--version
     * <p/>
     * @param args 用户输入的
     * @return 返回给yt-dlp可用的参数
     */
    public static List<String> filterYtDlpArguments(String[] args){
        //校验参数
        validArgs(args);
        List<String> argList = toList(args);
        List<String> filteredArgs = new ArrayList<>();
        //删除--playlist-items选项
        if (hasSelectItems(args)){
            for (int i = 0; i < argList.size(); i++) {
                String arg = argList.get(i);
                // 过滤掉 --playlist-items 选项
                if (arg.equals("--playlist-items") || arg.equals("--playlist-start") || arg.equals("--playlist-end")) {
                    // 如果有 --playlist-items、--playlist-start、--playlist-end，跳过其后面的值
                    i++; // 跳过其后的参数值
                } else {
                    filteredArgs.add(arg);
                }
            }
        }else {
            filteredArgs = argList;
        }
        filteredArgs.remove("--version");//删除版本信息选项
        filteredArgs.remove(filteredArgs.size() - 1);//删除下载链接

        return filteredArgs;
    }

    /**
     * 校验参数格式是否合法
     * @param args
     * @return
     */
    public static void validArgs(String[] args){
        List<String> argList = toList(args);
        if (argList != null && !argList.isEmpty()){
            List<String> supportUrlKey = Arrays.asList("/videos", "/streams", "youtube.com", "playlist?list=","watch?v=");
            String url = getUrl(args);
            // 正则表达式
            String regex = "https?://(?:www\\.)?[a-zA-Z0-9-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(url);
            if (!matcher.find()){
                throw new RuntimeException("请输入合法链接: " + url);
            }
            if (!supportUrlKey.stream().anyMatch(url::contains)){
                throw new RuntimeException("不支持该链接: " + url);
            }
        }else {
            throw new RuntimeException("请输入合法参数");
        }
    }

    /**
     * 是否忽略warn日志信息
     * @return
     */
    public static boolean isIgnoreWarnLog(String[] args){
        List<String> argList = toList(args);
        return argList.contains("--no-warnings");
    }

    /**
     * List集合转换成数组
     * @param argList
     * @return
     */
    public static String[] toArray(List<String> argList){
        String[] arr = new String[argList.size()];
        for (int i = 0; i < argList.size(); i++) {
            arr[i] = argList.get(i);
        }
        return arr;
    }

    /**
     * 是否开启打印调试信息
     * @param args
     * @return
     */
    public static boolean hasVerbose(String[] args){
        return toList(args).contains("-v") || toList(args).contains("--verbose");
    }
}
