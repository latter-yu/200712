import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServerV1 {
    // HTTP 底层要基于 TCP 来实现.需要按照 TCP 的基本格式来进行开发
    private ServerSocket serverSocket = null;

    public HttpServerV1(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void start() throws IOException {
        System.out.println("服务器启动");
        ExecutorService executorService = Executors.newCachedThreadPool();
        // 标准库自带的 线程池
        // 工厂模式
        // Executors.newCachedThreadPool(); 创建一个数目无上限, 但是线程不会轻易销毁的线程池
        // Executors.newFixedThreadPool(); 创建一固定大小的线程池(常用)
        // ...
        while (true) {
            // 1. 获取连接
            Socket clientSocket = serverSocket.accept();
            // 2. 处理连接(使用短连接的方式实现)
            // 短连接: 一次交互, 一次响应
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    process(clientSocket);
                }
            });
        }
    }

    private void process(Socket clientSocket) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {
            // 下面的操作都要严格按照 HTTP 协议来进行操作.
            // 1. 读取请求并解析
            //    a. 解析首行, 三个部分使用空格切分
            String firstLine = bufferedReader.readLine();
            String[] firstLineTokens = firstLine.split(" ");
            String method = firstLineTokens[0];
            String url = firstLineTokens[1];
            String version = firstLineTokens[2];
            //    b. 解析 header, 按行读取, 按照 冒号及空格 来分割键值对
            Map<String, String> headers = new HashMap<>();
            String line = "";
            // readLine 读取的一行内容, 是会自动去掉换行符的
            // 对于空行来说, 去掉了换行符, 就变成空字符串
            while ((line = bufferedReader.readLine()) != null && line.length() != 0) {
                // 不能使用 : 来切分. 像 referer 字段, 里面的内容是可能包含 : .
                String[] headerTokens = line.split(": ");
                headers.put(headerTokens[0], headerTokens[1]);
            }
            //    c. 解析 body(暂时先不考虑)
            //    请求解析完毕, 加上一个日志, 观察请求的内容是否正确.
            System.out.printf("%s %s %s\n", method, url, version); // 打印首行
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                // 打印 协议头(header): 键值对
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
            System.out.println(); // 输出空行
            // 2. 根据请求计算响应
            String resp = "";
            if (url.equals("/ok")) {
                bufferedWriter.write(version + " 200 OK\n");
                resp = "<h1>hello</h1>";
            } else if (url.equals("/notfound")) {
                bufferedWriter.write(version + " 404 Not Found\n");
                resp = "<h1>not found</h1>";
            } else if (url.equals("/seeother")) {
                // 重定向情况: 将页面转向 重定向地址.
                bufferedWriter.write(version + " 303 See Other\n");
                bufferedWriter.write("Location: http://www.sogou.com\n"); // 重定向地址
                resp = "";
            } else {
                bufferedWriter.write(version + " 200 OK\n");
                resp = "<h1>default</h1>";
            }
            // 3. 把响应写会到客户端
            bufferedWriter.write(version + " 200 OK\n");
            bufferedWriter.write("Content-Type: text/html\n");
            bufferedWriter.write("Content-Length: " + resp.getBytes().length);
            // 此处的长度为字符长度, 不是字节长度
            // resp.length() 是字节的长度
            bufferedWriter.write("\n");
            bufferedWriter.write(resp);
            bufferedWriter.flush(); // 保证数据被传入 body

        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close(); // 服务器端主动断开连接
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        HttpServerV1 server = new HttpServerV1(9090);
        server.start();
        // GET /abc HTTP/1.1: 表示默认图标资源文件(小地球图标)
    }
}


