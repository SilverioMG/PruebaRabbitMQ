package net.atopecode.pruebarabbitmq.manager.connection;

public class ConnectionValues {
    private final static int defaultPort = 5672;

    private String host;
    private int port;
    private String userName;
    private String password;
    private String virtualHost;

    public ConnectionValues(String host, String userName, String password, String virtualHost) {
        this.host = host;
        this.port = defaultPort;
        this.userName = userName;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    public ConnectionValues(String host, int port, String userName, String password, String virtualHost) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.virtualHost = virtualHost;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }
}