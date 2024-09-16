package io.github.yajuhua.invidious.dlj.command;

import io.github.yajuhua.invidious.dlj.command.annotations.CmdOption;
import io.github.yajuhua.invidious.dlj.utils.FileUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * 命令行解析
 */
public class CommandLineParser {

    public static CommandInfo parse(String[] args) throws Exception {
        CommandInfo commandInfo = new CommandInfo();
        List<String> argList = toList(args);
        Options options = new Options();
        commandInfo.setOptions(options);
        commandInfo.setFilter(argList);

        Field[] fields = Options.class.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            //获取有@CmdOption注解的成员变量
            if (field.isAnnotationPresent(CmdOption.class)){
                CmdOption cmdOption = field.getAnnotation(CmdOption.class);
                String shortForm = cmdOption.shortForm();
                String fieldName = field.getName();
                String cmdOptionValue = cmdOption.value();
                String optionName = getOptionName(argList,fieldName,shortForm,cmdOptionValue);
                String value = null;

                //检查是否参数中是否包含该选项
                if (field.getType().isAssignableFrom(boolean.class) || field.getType().isAssignableFrom(Boolean.class)) {
                    if (!(optionName != null && argList.contains(optionName))){
                        continue;
                    }
                }else {
                    if (argList.contains(optionName)){
                        int optionIndex = argList.indexOf(optionName);
                        value = argList.get(optionIndex + 1);
                    }else {
                        continue;
                    }
                }

                //赋值
                Field field1 = options.getClass().getField(fieldName);
                if (List.class.isAssignableFrom(field1.getType())){
                    //List集合
                    handleListField(field1,options,value);
                } else if (field1.getType().isAssignableFrom(boolean.class)
                        || field1.getType().isAssignableFrom(Boolean.class)) {
                    //布尔类型
                    field1.set(options,true);
                } else if (field1.getType().isAssignableFrom(Integer.class)
                        || field1.getType().isAssignableFrom(int.class)) {
                    field1.set(options,Integer.parseInt(value));
                } else {
                    //其他类型
                    field1.set(options,value);
                }

                //过滤
                removeOptionFromArgs(argList,optionName,field);
            }
        }

       //去掉命令尾部的URL链接，如果有的话
        try {
            new URL(argList.get(argList.size() - 1));
            argList.remove(argList.size() - 1);//去掉尾部链接
        } catch (MalformedURLException ignored) {

        }
        return commandInfo;
    }

    /**
     * 根据类型名称创建List泛型集合
     * @param typeName
     * @param value
     * @return
     */
    private static List<?> createList(String typeName, String value) {
        return parseList(value,typeName);
    }

    /**
     * 解析集合
     * 支持 1-2 1,2,3
     * @param value
     * @return
     */
    public static List<?> parseList(String value, String typeName) {
        List<Object> list = new ArrayList<>();

        // 根据分隔符拆分
        String[] split = null;
        if (value.contains(",")) {
            split = value.split(",");
        } else if (value.contains("-")) {
            split = value.split("-");
        } else {
            // 如果没有分隔符，直接解析为单个值
            list.add(parseValue(value, typeName));
            return list;
        }

        // 如果是范围形式（如 "1-10"）
        if (split.length == 2 && value.contains("-")) {
            int start = Integer.parseInt(split[0]);
            int end = Integer.parseInt(split[1]);
            // 使用 IntStream 生成范围
            return IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
        }

        // 如果是逗号分隔的多个值
        for (String s : split) {
            list.add(parseValue(s, typeName));
        }

        return list;
    }

    /**
     * 根据类型名称解析值
     * @param value
     * @param typeName
     * @return
     */
    private static Object parseValue(String value, String typeName) {
        if (Integer.class.getName().equals(typeName)) {
            return Integer.parseInt(value);
        } else {
            return value;
        }
    }

    /**
     * 处理成员变量为List的集合
     * @param field
     * @param options
     * @param value
     * @throws IllegalAccessException
     */
    private static void handleListField(Field field, Options options, String value) throws IllegalAccessException {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
            if (actualTypeArguments.length > 0) {
                String typeName = actualTypeArguments[0].getTypeName();
                List<?> list = createList(typeName, value);
                field.set(options, list);
            }
        }
    }

    /**
     * 根据选项移除来自argList中
     * @param argList
     * @param optionName 选项名称
     * @param field 成员变量
     */
    private static void removeOptionFromArgs(List<String> argList, String optionName,Field field) {
        boolean filter = field.getAnnotation(CmdOption.class).filter();
        if (!filter){
            int index = argList.indexOf(optionName);
            if (field.getType().isAssignableFrom(Boolean.class) || field.getType().isAssignableFrom(boolean.class)) {
                argList.remove(index);
            } else {
                argList.remove(index); // Remove option
                if (index < argList.size()) {
                    argList.remove(index); // Remove value
                }
            }
        }
    }

    /**
     * 将数组转换成List集合
     * @param args
     * @return
     */
    public static List<String> toList(String[] args){
        return new ArrayList<>(Arrays.asList(args));
    }

    /**
     * 返回选项名称 如--version
     * @param argList 用户传入的参数
     * @param fieldName Options.class 中的成员变量
     * @param shortForm 选项的缩写 如 --batch-file -> -a
     * @param cmdOptionValue 自定义的选项名称
     * @return
     */
    private static String getOptionName(List<String> argList, String fieldName, String shortForm, String cmdOptionValue) {
        if (argList.contains("--" + fieldName)) {
            return "--" + fieldName;
        } else if (argList.contains("-" + shortForm)) {
            return "-" + shortForm;
        } else if (argList.contains("--" + cmdOptionValue)) {
            return "--" + cmdOptionValue;
        }
        return null;
    }

    /**
     * 获取命令行中断代理信息
     * @param args
     * @return
     */
    public static Proxy getProxy(String[] args) throws Exception {
        String proxyStr = parse(args).getOptions().getProxy();
        Proxy proxy = null;
        URL url = new URL(proxyStr);
        int port = url.getPort();
        String host = url.getHost();
        InetSocketAddress address = new InetSocketAddress(host, port);
        if (proxyStr.startsWith("http")){
            proxy = new Proxy(Proxy.Type.HTTP,address);
        } else if (proxyStr.startsWith("socks5") || proxyStr.startsWith("socks")) {
            proxy = new Proxy(Proxy.Type.HTTP,address);
        }
        return proxy;
    }

    /**
     * 获取所有连接
     * @param args
     * @return
     */
    public static List<String> getAllUrl(String[] args) throws Exception {
        List<String> urlList = new ArrayList<>();
        CommandInfo parse = parse(args);
        try {
            if (parse.getOptions().batchFile != null){
                File batchFile = new File(parse.getOptions().getBatchFile());
                if (batchFile.exists()){
                    urlList = FileUtils.readLines(batchFile,"UTF-8").stream().filter(new Predicate<String>() {
                        @Override
                        public boolean test(String s) {
                            //排除不合法的链接
                            try {
                                new URL(s);
                                return true;
                            } catch (Exception e) {
                                return false;
                            }
                        }
                    }).collect(Collectors.toList());
                    return urlList;
                }else {
                    throw new Exception("找不到文件: " + batchFile);
                }
            }else {
                URL url = new URL(args[args.length - 1]);
                urlList.add(url.toString());
                return urlList;
            }
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取选择节目序号
     * @param options
     * @return
     */
    public static List<Integer> getSelectItemsNumbers(Options options){
        List<Integer> nums = new ArrayList<>();
        nums.add(1);//默认第一集
        if (options.playlistItems != null){
            nums = options.playlistItems;
        }else if (options.playlistStart != null && options.playlistEnd != null){
            return IntStream
                    .rangeClosed(options.playlistStart, options.playlistEnd)
                    .boxed()
                    .collect(Collectors.toList());
        } else if (options.playlistStart != null) {
            return IntStream
                    .rangeClosed(options.playlistStart,200)
                    .boxed()
                    .collect(Collectors.toList());
        } else if (options.playlistEnd != null) {
            return IntStream
                    .rangeClosed(1, options.playlistEnd)
                    .boxed()
                    .collect(Collectors.toList());
        }
        return nums;
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
}
