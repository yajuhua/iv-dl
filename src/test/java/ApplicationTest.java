import io.github.yajuhua.invidious.dlj.Application;
import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.util.List;

public class ApplicationTest {

//    @Test
    public void test() throws Exception {
        Application.main(new String[]{"--test"});
    }

    @Test
    public void proxy(){

        String httpProxy = System.getenv("HTTP_PROXY");
        String httpsProxy = System.getenv("HTTPS_PROXY");

        System.out.println("HTTP Proxy: " + httpProxy);
        System.out.println("HTTPS Proxy: " + httpsProxy);

        ProxySelector proxySelector = ProxySelector.getDefault();
        URI uri = URI.create("https://www.youtube.com");
        List<Proxy> proxies = proxySelector.select(uri);

        for (Proxy proxy : proxies) {
            if (proxy.type() == Proxy.Type.HTTP) {
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                if (address != null) {
                    System.out.println("Proxy Host: " + address.getHostName());
                    System.out.println("Proxy Port: " + address.getPort());
                } else {
                    System.out.println("No proxy is set");
                }
            } else {
                System.out.println("No HTTP proxy set");
            }
        }
    }
}
