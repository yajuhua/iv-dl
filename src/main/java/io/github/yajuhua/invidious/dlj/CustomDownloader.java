package io.github.yajuhua.invidious.dlj;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.github.yajuhua.invidious.wrapper.Invidious;
import io.github.yajuhua.invidious.wrapper.downloader.Downloader;
import io.github.yajuhua.invidious.wrapper.downloader.Request;
import io.github.yajuhua.invidious.wrapper.downloader.Response;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自定义invidious-wrapper下载
 */
@Slf4j
public class CustomDownloader extends Downloader {


    @Override
    public Response execute(Request request) throws Exception {
        String url = request.getUrl();
        Gson gson = new Gson();
        if (url.contains("/api/v1/")){
            //说明是API请求
            List<String> instanceUriList = Invidious.getInstanceUriList();
            if (Invidious.getApi() != null){
                instanceUriList.add(0,Invidious.getApi());
            }
            if (instanceUriList == null && instanceUriList.isEmpty()){
                throw new RuntimeException("未获取到InvidousAPI");
            }
            String latestUrl;
            for (String instance : instanceUriList) {
                latestUrl = instance + url;
                Request get = Request.builder()
                        .httpMethod("GET")
                        .url(latestUrl)
                        .build();
                Response response = null;
                try {
                    response = execHttp(get);
                    if (response.getResponseCode() == 200){
                        return response;
                    } else if (response.getResponseCode() == 404) {
                        if (!Application.ignoreWarnLog){
                            String rs = null;
                            if (response.getResponseBody() != null && !response.getResponseBody().isEmpty()){
                                JsonObject object = gson.fromJson(response.getResponseBody(), JsonObject.class);
                                if (object.has("error")){
                                    rs = object.get("error").getAsString();
                                }
                            }
                            log.error(rs);
                        }
                    } else {
                        if (!Application.ignoreWarnLog){
                            log.warn("请求失败: {} - {} - 正在尝试其他",response.getResponseCode(),response.getLatestUrl());
                        }
                    }
                }catch (IOException e){
                    if (!Application.ignoreWarnLog){
                        if (e.toString().contains("java.io.FileNotFoundException")){
                            log.error("找不到该视频: {}",request.getUrl());
                            System.exit(1);
                        }
                        log.warn("请求失败: {},正在尝试其他",e.getMessage());
                    }
                }
            }
            throw new RuntimeException("未找到可用InvidiousAPI");
        }else {
            //默认请求
            return execHttp(request);
        }
    }

    /**
     * 执行http请求
     * @param request
     * @return
     * @throws IOException
     */
    public Response execHttp(Request request) throws Exception {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            // 创建 URL 对象
            URL url = new URL(request.getUrl());

            //设置代理
            if (Invidious.proxy != null){
                connection = (HttpURLConnection) url.openConnection(Invidious.proxy);
            }else {
                connection = (HttpURLConnection) url.openConnection();
            }

            // 打开连接
            connection.setRequestMethod(request.getHttpMethod());
            connection.setDoOutput(request.getHttpMethod().equalsIgnoreCase("POST"));

            // 设置请求头
            if (request.getHeader() != null){
                for (Map.Entry<String, List<String>> entry : request.getHeader().entrySet()) {
                    connection.setRequestProperty(entry.getKey(), String.join(", ", entry.getValue()));
                }
            }

            // 写入请求体（仅在 POST 请求时）
            if (request.getHttpMethod().equalsIgnoreCase("POST") && request.getBody() != null) {
                connection.setRequestProperty("Content-Length", request.getBody().length() + "");
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(request.getBody().getBytes());
                }
            }

            // 获取响应码和响应消息
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            // 读取响应内容
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream(),"UTF-8"));
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }

            // 获取响应头
            Map<String, List<String>> responseHeaders = new HashMap<>(connection.getHeaderFields());

            // 返回 Response 对象
            return new Response(
                    responseCode,
                    responseMessage,
                    responseHeaders,
                    responseBody.toString(),
                    request.getUrl()
            );

        } catch (IOException e) {
            // 处理 IO 异常
           throw new IOException(e);
        }finally {
            // 关闭连接和读取器
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
