package org.example.product;

import com.alibaba.cloud.nacos.discovery.NacosDiscoveryClient;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.DiscoveryClient;

@SpringBootTest
public class DiscoveryTest {
    @Autowired
    NacosServiceDiscovery nacosServiceDiscovery;


    @Autowired
    DiscoveryClient discoveryClient;
    @Test
    public void testDiscovery() {
        for (String service : discoveryClient.getServices()){
            System.out.println("service="+service);
            discoveryClient.getInstances(service).forEach(instance -> {
                System.out.println("instance="+instance.getUri());
                System.out.println("instance="+instance.getHost());
                System.out.println("instance="+instance.getPort());
            });
        }
    }
}
