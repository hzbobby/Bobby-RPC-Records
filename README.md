参考

[https://javaguide.cn/distributed-system/rpc/rpc-intro.html](https://javaguide.cn/distributed-system/rpc/rpc-intro.html#rpc-%E7%9A%84%E5%8E%9F%E7%90%86%E6%98%AF%E4%BB%80%E4%B9%88)

[https://github.com/youngyangyang04/RPC-Java](https://github.com/youngyangyang04/RPC-Java/tree/f7767c39d2f8d4ad26a37ae2821d6cd043b7f24d)

[https://github.com/he2121/MyRPCFromZero?tab=readme-ov-file](https://github.com/he2121/MyRPCFromZero?tab=readme-ov-file)

# RPC 简介

参考：

[https://javaguide.cn/distributed-system/rpc/rpc-intro.html](https://javaguide.cn/distributed-system/rpc/rpc-intro.html#rpc-%E7%9A%84%E5%8E%9F%E7%90%86%E6%98%AF%E4%BB%80%E4%B9%88)

---

Remote Process Call 远程过程调用。在分布式服务下，不同服务部署在不同机器上，由于服务不在同一块内存上，服务A如何去调用服务B呢？

通过 网络 来调用，通过 HTTP 协议（例如，Feign系列, gRPC HTTP/2 ）或 TCP 协议（例如，Dubbo...， Dubbo3 开始使用 HTTP/2）。

> 为什么陆续开始使用 HTTP/2 协议，而不继续使用原生的 TCP 呢？



<font style="background-color:#FBDE28;">RPC 的本质是，</font>**<font style="background-color:#FBDE28;">使调用远程服务就像调用本地服务一样方便。</font>**

![](https://cdn.nlark.com/yuque/0/2025/jpeg/50582501/1743137593320-7ab1d08e-4062-461e-be27-e0c63aac1fab.jpeg?x-oss-process=image%2Fformat%2Cwebp%2Fresize%2Cw_499%2Climit_0%2Finterlace%2C1)

一个 RPC 可以由以下几部分组成

+ **客户端（服务消费端）**：调用远程方法的一端。
+ **客户端 Stub（桩）**：这其实就是一代理类。代理类主要做的事情很简单，就是把你调用方法、类、方法参数等信息封装好传递到服务端。
+ **网络传输**：把请求传输到服务端，接受服务端返回的响应。网络传输的实现方式有：最基本的 Socket 或者性能以及封装更加优秀的 Netty（推荐）。
+ **服务端 Stub（桩）**：这里相当于一个“服务引导者”（根据客户端的请求，找到对应的服务，并执行。将返回结果封装好再传输到网络上）。
+ **服务端（服务提供端）**：提供远程方法的一端。



本质上，RPC 就是，为了调用远程服务而做的一些措施，包括

+ 客户端调用哪些服务
+ 客户端怎么调用？通过请求
+ 一个调用请求需要哪些内容
+ 如何进行网络传输
+ 网络传输采用哪些序列化机制
+ tcp 粘包如何解决，自定义协议应该如何设计
+ 如何管理网络连接
+ 服务端提供哪些服务
+ 服务端解析请求后如何进行调用
+ 调用返回的响应应该是怎样的



OK，下面从最简单的 RPC 版本开始学起



---

# V1. 一个最简单的 RPC 结构

参考

[https://github.com/youngyangyang04/RPC-Java](https://github.com/youngyangyang04/RPC-Java/tree/f7767c39d2f8d4ad26a37ae2821d6cd043b7f24d)



在开始之前，我们先定义一个简单的服务

```java
@Builder
@Data
public class User  implements Serializable {
    private Long id;
    private String name;
    private Integer age;
}

public interface IUserService {

    public User getUser();
}

public class UserServiceImpl implements IUserService {
    @Override
    public User getUser() {
        return User.builder()
        .id(RandomUtil.randomLong())
        .name(RandomUtil.randomString(10))
        .age(RandomUtil.randomInt())
        .build();
    }
}
```



想想最开始是怎么进行网络编程，网络通信的 —— Socket 通信

没错，最简陋的版本就是利用 Socket 进行调用。



在服务端提供一个方法，并且开启监听，一旦监听到客户端连接，就返回服务的响应

在客户端向服务端连接，并且获取返回的响应信息。



```java
public class RpcServerTest {
    public static void main(String[] args) {
        UserServiceImpl userService = new UserServiceImpl();
        try {
            ServerSocket serverSocket = new ServerSocket(8899);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true) {
                Socket socket = serverSocket.accept();
                // 开启一个线程去处理
                new Thread(() -> {
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        // 读取客户端传过来的id
                        Long id = ois.readLong();
                        User user = userService.getUser(id);
                        // 写入User对象给客户端
                        oos.writeObject(user);
                        oos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("从IO中读取数据错误");
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }
}
```

```java
public class RpcClientTest {
    public static void main(String[] args) {
        try {
            // 建立Socket连接
            Socket socket = new Socket("127.0.0.1", 8899);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 传给服务器id
            objectOutputStream.writeLong(RandomUtil.randomLong());
            objectOutputStream.flush();
            // 服务器查询数据，返回对应的对象
            User user = (User) objectInputStream.readObject();
            System.out.println("服务端返回的User:" + user);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("客户端启动失败");
        }
    }
}
```



返回结果

```java
服务端返回的User:User(id=-6666395901832456143, name=UY505L3xUu, age=1948163949)
```



## 总结

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743592880407-c0bdc5e0-4a88-4637-a3de-5d6c1b61de36.png)



+ 客户端不能指定调用哪个服务，只知道连接上了，就获取一个 User 的响应。—— 封装请求和响应
+ 针对多种服务，能够统一进行封装请求 —— 代理



# V2. 封装请求和响应 & 动态代理统一构建请求

上述 RPCServer 和 RPCClient 的请求和响应都很“专用”，意味着对于每个服务都得写一个这样的server & client。

因此，将请求和响应抽象出来。

服务端要做的就是

+ 读取请求
+ 调用对应服务    （通过，反射机制，拿到对应方法）
+ 返回响应

客户端要做的就是

+ 构造请求
+ 接受响应



请求应该有哪些字段？

接口名，方法名，参数类型（类型擦除，用于恢复类型），参数

```java
@Data
@Builder
public class RPCRequest implements Serializable {
    // 服务类名，客户端只知道接口名，在服务端中用接口名指向实现类
    private String interfaceName;
    // 方法名
    private String methodName;
    // 参数列表
    private Object[] params;
    // 参数类型
    private Class<?>[] paramsTypes;
}
```

响应

```java
@Data
@Builder
public class RPCResponse implements Serializable {
    // 状态信息
    private int code;
    private String message;
    // 具体数据
    private Object data;

    public static RPCResponse success(Object data) {
        return RPCResponse.builder().code(200).data(data).build();
    }

    public static RPCResponse fail() {
        return RPCResponse.builder().code(500).message("服务器发生错误").build();
    }

    public static RPCResponse fail(String msg) {
        return RPCResponse.builder().code(500).message(msg).build();
    }
}
```



既然有了请求，那么服务端就可以通过反射获取接口名，方法名，然后根据参数调用该接口方法了。

服务端改造如下

```java

public class RpcServerTest {
    public static void main(String[] args) {
        UserServiceImpl userService = new UserServiceImpl();
        try {
            ServerSocket serverSocket = new ServerSocket(8899);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true) {
                Socket socket = serverSocket.accept();
                // 开启一个线程去处理
                new Thread(() -> {
                    try {
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        // 这里接受客户端传过来的 通用 请求
                        // 通过反射来解析
                        RPCRequest rpcRequest = (RPCRequest) ois.readObject();
                        Method method = userService.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamsTypes());
                        Object result = method.invoke(userService, rpcRequest.getParams());
                        // 将结果封装到 Response
                        RPCResponse response = RPCResponse.builder().data(result).code(200).message("OK").build();
                        oos.writeObject(response);
                        oos.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("从IO中读取数据错误");
                    } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }

}

```

客户端改造如下

```java
public class RpcClientTest {
    public static void main(String[] args) {
        try {
            // 建立Socket连接
            Socket socket = new Socket("127.0.0.1", 8899);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            // 客户端构造请求
            RPCRequest request = RPCRequest.builder()
                    .interfaceName("com.bobby.rpc.v2.sample.service.IUserService")
                    .methodName("getUser")
                    .paramsTypes(new Class[]{Long.class})
                    .params(new Object[]{RandomUtil.randomLong()})
                    .build();
            // 发送请求
            objectOutputStream.writeObject(request);
            RPCResponse response = (RPCResponse) objectInputStream.readObject();

            System.out.println("服务端返回的响应:" + response.toString());

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("客户端启动失败");
        }
    }
}

```

测试

```java
服务端返回的响应:RPCResponse(code=200, message=OK, data=User(id=-1330277087213570002, name=YAXCSp9juI, age=-1101263422))
```



这里服务端通过反射机制，能获取 UserService 里面各种方法的调用。

客户端虽然把请求和响应抽象出来了，但是 <font style="color:rgb(31, 35, 40);">host，port， 与调用的方法(只能调用 IUservice )都特定。客户端需要</font>通过指定接口，方法等参数，来调用服务端的方法。客户端针对不同方法，需要再次进行构建请求，较为繁琐。

<font style="color:rgb(31, 35, 40);">怎么改呢？我们的目的是希望客户端能够与 host, port 甚至 服务类的特定方法 抽离。</font>

<font style="color:rgb(31, 35, 40);">那就意味着，我们调用一个方法，有个东西能帮我们构建出请求，并且在</font>**<font style="color:rgb(31, 35, 40);">每一次调用时都能构建出对应请求</font>**<font style="color:rgb(31, 35, 40);">。例如，我们想要调用 </font>`<font style="color:rgb(31, 35, 40);">getUser</font>`<font style="color:rgb(31, 35, 40);">，那个东西就能帮助我们构建出对应的请求。</font>

<font style="color:rgb(31, 35, 40);">ok，那个东西就是 动态代理。JDK 动态代理，可以让代理类在调用每一个方法时，都执行 invoke 逻辑。（适合用来构建 request 请求）</font>

<font style="color:rgb(31, 35, 40);"></font>

```java
public class ClientProxy implements InvocationHandler {
    private String host;
    private int port;

    public ClientProxy(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 代理对象执行每个方法时，都将执行这里的逻辑
        // 我们的目的是，利用这个代理类帮助我构建请求。这样能够有效减少重复的代码
        RPCRequest request = RPCRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramsTypes(method.getParameterTypes())
                .params(args)
                .build();
        // 然后将这个请求发送到服务端，并获取响应
        RPCResponse response = SimpleRpcClient.sendRequest(host, port, request);
        return response.getData();
    }

    // 获取代理对象
    public <T> T createProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T) o;
    }
}
```

```java
public class SimpleRpcClient {
    public static RPCResponse sendRequest(String host, int port, RPCRequest request) {
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            // 发送请求
            objectOutputStream.writeObject(request);
            // 获取响应
            RPCResponse response = (RPCResponse) objectInputStream.readObject();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("SimpleRpcClient, sendRequest Exception: "+e.getMessage());
            return null;
        }
    }
}
```

```java
public class RpcClientTest {
    public static void main(String[] args) {
        // 使用代理类
        ClientProxy clientProxy = new ClientProxy("127.0.0.1", 8899);
        IUserService proxyService = clientProxy.createProxy(IUserService.class);

        User user = proxyService.getUser(RandomUtil.randomLong());
        System.out.println("接受的User: "+ user);

        // 调用其他方法
    }
}

```

测试

```java
接受的User: User(id=1972916228567809431, name=LK6yHvbA7B, age=-1938421705)
```

## 总结

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743596356695-1d4e7742-8640-434a-b1e6-1b2a77e441f9.png)



本小节中主要改造了：

1. 通用的 Requeset 和 Response
2. 利用代理类，通用地进行处理每个服务类方法请求的构建
3. socket 通信与构建请求分离，降低耦合度（用 SimpleRpcClient 进行通信）



存在问题：

+ 服务端只针对 UserService 接受请求，如果有别的服务呢？（多服务注册）
+ 服务端 BIO 性能低
+ 服务端耦合度高：监听、执行调用。。



# V3. 服务注册 & 服务松耦合

本节改造点

+ 服务提供者 —— 进行多服务注册，并由服务提供者选择服务进行提供
+ 服务端松耦合 —— 将服务的监听与处理分离



开始之前，我们先添加一些其他服务，如 BlogService 来模拟多服务

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Blog implements Serializable {
    private Integer id;
    private Integer useId;
    private String title;
}

public interface IBlogService {
    public Blog getBlogById(Integer id);
    public void addBlog(Blog blog);
}

public class BlogServiceImpl implements IBlogService {
    @Override
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("我的博客").useId(22).build();
        System.out.println("客户端查询了" + id + "博客");
        return blog;
    }

    @Override
    public void addBlog(Blog blog) {
        System.out.println("插入的 Blog 为："+ blog.toString());
    }
}
```

<font style="color:rgb(31, 35, 40);">ok，接下来先解决多服务问题</font>

<font style="color:rgb(31, 35, 40);">简单，加一个 Map 不就好了嘛，我们在 Server 处，添加一个 Map 或者抽象出一个 服务提供者。</font>

<font style="color:rgb(31, 35, 40);">服务提供者可以Map实现</font>

+ <font style="color:rgb(31, 35, 40);">存放服务接口名与服务端对应的实现类</font>
+ <font style="color:rgb(31, 35, 40);">服务启动时要暴露其相关的实现类</font>

```java
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个接口
     */
    private Map<String, Object> interfaceProvider;

    public ServiceProvider(){
        this.interfaceProvider = new HashMap<>();
    }

    public void provideServiceInterface(Object service){
        // 根据多态，这里 service 一般是一个具体实现类
        // 因此 serviceName 是 xxxServiceImpl
        // 我们需要获取其实现的接口，并进行接口与对应实现的注册
        String serviceName = service.getClass().getName();
        Class<?>[] interfaces = service.getClass().getInterfaces();

        for(Class clazz : interfaces){
            interfaceProvider.put(clazz.getName(),service);
        }

    }

    public Object getService(String interfaceName){
        return interfaceProvider.get(interfaceName);
    }
}
```



<font style="color:rgb(31, 35, 40);">ok，接下来解决耦合问题</font>

<font style="color:rgb(31, 35, 40);">在前面中，我们在服务端做的工作有：BIO监听、处理方式（接受请求、反射调用、返回结果）</font>

<font style="color:rgb(31, 35, 40);">现在，我们服务端不止解决一个服务的监听，我们想改造成一个更加通用的服务端。并且，后续改造中，我们可能也不想用 BIO 进行监听，可能也不想只用一个线程来进行反射调用（例如，利用线程池操作）等</font>

<font style="color:rgb(31, 35, 40);">所以，将上述功能抽象出来：</font>

+ 服务端启动/停止
+ 处理方式：简单处理、线程池处理... (这里利用服务端的具体实现来体现)

因此，先抽象出一个服务端接口，接口提供服务端启动和停止的方法



```java
public interface IRpcServer {
    void start(int port);
    void stop();
}
```

线程调用方法

```java
public class SimpleRPCServer implements IRpcServer {
    // 存着服务接口名-> service对象的map
    private ServiceProvider serviceProvider;

    public SimpleRPCServer(ServiceProvider serviceProvide) {
        this.serviceProvider = serviceProvide;
    }

    @Override
    public void start(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("服务端启动了");
            // BIO的方式监听Socket
            while (true) {
                Socket socket = serverSocket.accept();
                // 开启一个新线程去处理
                new Thread(new WorkThread(socket, serviceProvider)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("服务器启动失败");
        }
    }

    @Override
    public void stop() {

    }
}
```

线程池做法

```java
public class ThreadPoolRPCServer implements IRpcServer {
    private final ThreadPoolExecutor threadPool;
    private ServiceProvider serviceProvide;

    public ThreadPoolRPCServer(ServiceProvider serviceProvide) {
        threadPool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                1000, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));
        this.serviceProvide = serviceProvide;
    }

    public ThreadPoolRPCServer(ServiceProvider serviceProvide, int corePoolSize,
                               int maximumPoolSize,
                               long keepAliveTime,
                               TimeUnit unit,
                               BlockingQueue<Runnable> workQueue) {

        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        this.serviceProvide = serviceProvide;
    }


    @Override
    public void start(int port) {
        System.out.println("服务端启动了");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.execute(new WorkThread(socket, serviceProvide));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
    }
}
```

```java
public class WorkThread implements Runnable {
    private Socket socket;
    private ServiceProvider serviceProvide;

    public WorkThread(Socket socket, ServiceProvider serviceProvide) {
        this.socket = socket;
        this.serviceProvide = serviceProvide;
    }

    @Override
    public void run() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            // 读取客户端传过来的request
            RpcRequest request = (RpcRequest) ois.readObject();
            // 反射调用服务方法获得返回值
            RpcResponse response = getResponse(request);
            //写入到客户端
            oos.writeObject(response);
            oos.flush();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("从IO中读取数据错误");
        }
    }

    private RpcResponse getResponse(RpcRequest request) {
        // 得到服务名
        String interfaceName = request.getInterfaceName();
        // 得到服务端相应服务实现类
        Object service = serviceProvide.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object invoke = method.invoke(service, request.getParams());
            return RpcResponse.success(invoke);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.out.println("方法执行错误");
            return RpcResponse.fail();
        }
    }
}
```

测试

```java
public class RpcServerTest {
    public static void main(String[] args) {
        IUserService userService = new UserServiceImpl();
        IBlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

        IRpcServer rpcServer = new ThreadPoolRPCServer(serviceProvider);
        rpcServer.start(8899);
    }

}

```

```java
public class RpcClientTest {
    public static void main(String[] args) {
        // 使用代理类
        ClientProxy clientProxy = new ClientProxy("127.0.0.1", 8899);
        IUserService userService = clientProxy.createProxy(IUserService.class);

        IBlogService blogService = clientProxy.createProxy(IBlogService.class);


        for(int i=0; i<100; i++){
            User user = userService.getUser(RandomUtil.randomLong());
            System.out.println("接受的User: "+ user);

            blogService.addBlog(Blog.builder()
                    .id(RandomUtil.randomLong())
                    .title(RandomUtil.randomString(18))
                    .useId(RandomUtil.randomLong())
                    .build());
        }
    }
}

```

```java
插入的 Blog 为：Blog(id=1735656502409286872, useId=3061360331749512315, title=fjDZnWQpLaml6eWUzF)
插入的 Blog 为：Blog(id=-1551910759612728489, useId=-3270735373894307395, title=4NpQxbMZB3U17x8LKZ)
插入的 Blog 为：Blog(id=6695719282272084601, useId=7055497556433115325, title=PUe0K2aYGaoRj1Xket)
插入的 Blog 为：Blog(id=-4651285310832374473, useId=-6332385843336750982, title=HEdQtXzxIuHjB5Ins0)
插入的 Blog 为：Blog(id=1886277443951891754, useId=6612562425837510256, title=0bngpPlF0BPhqyNEB5)
插入的 Blog 为：Blog(id=5766178470860541582, useId=4793515534012689592, title=kWjlmqy9ZTGtEzvDI6)
插入的 Blog 为：Blog(id=7687076234003932188, useId=-1261437399964647501, title=R7gxtv2Do49XgcUq64)
插入的 Blog 为：Blog(id=-7515865886446537845, useId=5137253089783672994, title=Xzrjwjz7SedB7keAAP)
```

```java
接受的User: User(id=7882902267818290420, name=wLuaAg6ska, age=-201986861)
接受的User: User(id=6510379073261234147, name=B815OmkVWj, age=-1734455835)
接受的User: User(id=4533747017211991752, name=pODVM0ntKZ, age=-1471823159)
接受的User: User(id=5151190405224807154, name=SfpNi40yfI, age=1203328157)
接受的User: User(id=-1546645520292290317, name=zjxmZ3XsJe, age=1720785105)
接受的User: User(id=-8028126501044677890, name=OHsPC2b569, age=1341545052)
接受的User: User(id=-8407407682221650363, name=UlykO5mG8U, age=2097900651)
接受的User: User(id=-51627231076458295, name=pJXZ4l1AYi, age=1745526171)
接受的User: User(id=2515414120271619108, name=r3RcrWSZgo, age=-851455518)
接受的User: User(id=3572193338757092292, name=93HToPHlDE, age=-142014203)
接受的User: User(id=-6888951129549175355, name=sAUwGEeyP3, age=-739271821)
```



## 总结

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743596751770-e9072c66-0199-4f47-a55b-0dadbe7cacb6.png)



本节中，为了支持多服务，我们实现了 ServiceProvider 服务提供者。本质是利用Map将服务接口与具体服务实现类绑定起来。在服务端处理阶段，能过通过接口名称获取到具体服务类。



但是服务端中仍然是采用 `serverSocket.accept();`阻塞式响应。必须有客户端连接了才能获得响应。在没有客户端连接的时候，服务端一致处于阻塞状态。

传统 BIO 方式网络传输效率低





# V4. Netty + 自定义序列化

本节改造点

+ 利用 Netty 替换 ServerSocket 进行网络通信；从 BIO -> NIO
+ 自定义消息协议，并拓展序列化机制，减少字节流长度



## 引入 Netty

<font style="color:rgb(64, 64, 64);">Netty 是一个 </font>**<font style="color:rgb(64, 64, 64);">异步事件驱动</font>**<font style="color:rgb(64, 64, 64);"> 的高性能网络框架，基于 </font>**<font style="color:rgb(64, 64, 64);">NIO（Non-blocking I/O）</font>**<font style="color:rgb(64, 64, 64);">，适用于高并发、低延迟的网络通信（如 RPC、WebSocket、HTTP 等）</font>

<font style="color:rgb(64, 64, 64);">相比于 NIO 复杂的API，Netty 使用更为方便</font>



首先，我们先对 Client 进行抽象，Client 的共有方法是 发送请求 sendRequest，因此可以抽象出如下接口

```java
public interface IRpcClient {
    RpcResponse sendRequest(RpcRequest request);
}
```

```java
public class SimpleRpcClient implements IRpcClient {

    private String host;
    private int port;
    public SimpleRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        try {
            Socket socket = new Socket(host, port);

            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());

            // 发送请求
            objectOutputStream.writeObject(request);
            // 获取响应
            RpcResponse response = (RpcResponse) objectInputStream.readObject();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("SimpleRpcClient, sendRequest Exception: "+e.getMessage());
            return null;
        }
    }
}
```

同时，改造一下 ClientProxy 让它接受客户端对象

```java

public class ClientProxy implements InvocationHandler {
    private IRpcClient rpcClient;

    public ClientProxy(IRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // v2.
        // 代理对象执行每个方法时，都将执行这里的逻辑
        // 我们的目的是，利用这个代理类帮助我构建请求。这样能够有效减少重复的代码
        RpcRequest request = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramsTypes(method.getParameterTypes())
                .params(args)
                .build();
        // 然后将这个请求发送到服务端，并获取响应
        // v4. 利用 IRpcClient 对象发送请求
        RpcResponse response = rpcClient.sendRequest(request);
        return response==null ? null : response.getData();
    }

    // 获取代理对象
    public <T> T createProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T) o;
    }
}

```



OK，接下来正式引入 Netty 来替换我们的 Client 和 Server

引入 pom 依赖

```java
        <!-- https://mvnrepository.com/artifact/org.apache.curator/curator-recipes -->
        <dependency>
            <groupId>org.apache.curator</groupId>
            <artifactId>curator-recipes</artifactId>
            <version>5.8.0</version>
        </dependency>

```

接下来先简单了解一下 Netty 的工作模式，再对我们的 Server 和 Client 进行拓展



下面通过一张图来简单介绍一下 Netty 的使用

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743164383609-be9d3f27-e3b1-4c27-a9f9-ce276a1e7bfe.png)

显而易见，服务器和客户端是通过 channel 进行通信的。

因此双方通信时都会根据 host, port 连接到相同的 channel



可以看到客户端和服务端在 channel 初始化时，都得经过一些 pipelines，这些 pipelines 通常包括指定消息格式，指定**序列化方式**，指定**处理方式。**Netty 通过这个责任链可以定义消息的处理步骤。

可以自定义一个 `MyHandler extends SimpleChannelInboundHandler<RPCResponse>`然后重写里面的 `channelRead0`方法，来实现接收消息的处理逻辑。

我们在服务端的自定义 handler 中，处理 request 请求，并向 channel 发送一个 response

我们在客户端的自定义 handler 中，构建 request 请求，并从 channel 接受 response



**服务端**

```java
@Slf4j
public class NettyRpcServer implements IRpcServer {
    private final NettyServerInitializer nettyServerInitializer;
    private ChannelFuture channelFuture;

    public NettyRpcServer(NettyServerInitializer nettyServerInitializer) {
        // 通过注入的方式可以实现多种不同初始化方式的 Netty
        this.nettyServerInitializer = nettyServerInitializer;
    }

    @Override
    public void start(int port) {
        // netty 服务线程组boss负责建立连接， work负责具体的请求
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            // 启动netty服务器
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            // 初始化
            serverBootstrap
                    .group(bossGroup, workGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(nettyServerInitializer);
            // 同步阻塞
            channelFuture = serverBootstrap.bind(port).sync();
            // 死循环监听
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdown(bossGroup, workGroup);
        }
    }

    @Override
    public void stop() {
        if (channelFuture != null) {
            try {
                channelFuture.channel().close().sync();
                log.info("Netty服务端主通道已关闭");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("关闭Netty服务端主通道时中断：{}", e.getMessage(), e);
            }
        } else {
            log.warn("Netty服务端主通道尚未启动，无法关闭");
        }
    }

    private void shutdown(NioEventLoopGroup bossGroup, NioEventLoopGroup workGroup) {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (workGroup != null) {
            workGroup.shutdownGracefully().syncUninterruptibly();
        }
    }
}
```

```java
public class NettyServerInitializer extends ChannelInitializer<SocketChannel> {
    private final ServiceProvider serviceProvider;

    public NettyServerInitializer(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 消息格式 [长度][消息体], 解决粘包问题
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));
        // 计算当前待发送消息的长度，写入到前4个字节中
        pipeline.addLast(new LengthFieldPrepender(4));

        // 这里使用的还是java 序列化方式， netty的自带的解码编码支持传输这种结构
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));

        pipeline.addLast(new NettyRpcServerHandler(serviceProvider));
    }
}
```

```java

/**
 * 因为是服务器端，我们知道接受到请求格式是RPCRequest
 * Object类型也行，强制转型就行
 */
@Slf4j
public class NettyRpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {
    private final ServiceProvider serviceProvider;
    public NettyRpcServerHandler(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.info("NettyServer 接收请求: {}", request);
        RpcResponse response = getResponse(request);
        ctx.writeAndFlush(response);
//        ctx.close();
//        log.info("NettyServer 关闭连接");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("exceptionCaught: {}", cause.getMessage());
        ctx.close();
    }

    private RpcResponse getResponse(RpcRequest request) {
        // 得到服务名
        String interfaceName = request.getInterfaceName();

        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object ret = method.invoke(service, request.getParams());
            // 某些操作可能没有返回值
            Class<?> dataType = null;
            if (ret != null){
                dataType = ret.getClass();
            }
            return RpcResponse.builder()
                    .code(200)
                    .data(ret)
                    .dataType(dataType)
                    .message("OK")
                    .build();
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return RpcResponse.fail();
        }
    }
}
```



**客户端**

```java

/**
 * 实现RPCClient接口
 */
@Slf4j
public class NettyRpcClient implements IRpcClient {
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;

    private String host;
    private int port;

    public NettyRpcClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // netty客户端初始化，重复使用
    static {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }


    /**
     * 这里需要操作一下，因为netty的传输都是异步的，你发送request，会立刻返回， 而不是想要的相应的response
     */
    @Override
    public RpcResponse sendRequest(RpcRequest request) {

        try {
            ChannelFuture channelFuture = bootstrap.connect(host, port).sync();
            Channel channel = channelFuture.channel();

            // 发送数据
            channel.writeAndFlush(request);
            // closeFuture: channel关闭的Future
            // sync 表示阻塞等待 它 关闭
            channel.closeFuture().sync();
            // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            // AttributeKey是，线程隔离的，不会由线程安全问题。
            // 实际上不应通过阻塞，可通过回调函数
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("RPCResponse");
            RpcResponse rpcResponse = channel.attr(key).get();
            return rpcResponse;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        // 关闭 netty
        if(eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess()) {
                    log.info("关闭 Netty 成功");
                }else{
                    log.info("关闭 Netty 失败");
                }
            });
            try {
                eventLoopGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                log.error("关闭 Netty 异常: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

}
```

```java
/**
 * 通过 handler 获取客户端的结果
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 消息格式 [长度][消息体], 解决粘包问题
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE,0,4,0,4));
        // 计算当前待发送消息的长度，写入到前4个字节中
        pipeline.addLast(new LengthFieldPrepender(4));

        // 这里使用的还是java 序列化方式， netty的自带的解码编码支持传输这种结构
        pipeline.addLast(new ObjectEncoder());
        pipeline.addLast(new ObjectDecoder(new ClassResolver() {
            @Override
            public Class<?> resolve(String className) throws ClassNotFoundException {
                return Class.forName(className);
            }
        }));
        pipeline.addLast(new NettyClientHandler());
    }
}
```

```java

public class NettyClientHandler extends SimpleChannelInboundHandler<RpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse msg) throws Exception {
        // 接收到response, 给channel设计别名，让sendRequest里读取response
        AttributeKey<RpcResponse> key = AttributeKey.valueOf("RPCResponse");
        ctx.channel().attr(key).set(msg);
        ctx.channel().close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
```



**测试**

```java
public class RpcServerTest {
    public static void main(String[] args) {
        IUserService userService = new UserServiceImpl();
        IBlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

//        IRpcServer rpcServer = new ThreadPoolRPCServer(serviceProvider);
        IRpcServer rpcServer = new NettyRpcServer(serviceProvider);
        rpcServer.start(8899);
    }

}

```

```java
public class RpcClientTest {
    public static void main(String[] args) {
        // 使用代理类

//        IRpcClient rpcClient = new SimpleRpcClient("127.0.0.1", 8899);
        IRpcClient rpcClient = new NettyRpcClient("127.0.0.1", 8899);
        ClientProxy clientProxy = new ClientProxy(rpcClient);
        IUserService userService = clientProxy.createProxy(IUserService.class);

        IBlogService blogService = clientProxy.createProxy(IBlogService.class);


        for(int i=0; i<100; i++){
//            User user = userService.getUser(RandomUtil.randomLong());
//            System.out.println("接受的User: "+ user);

            blogService.addBlog(Blog.builder()
                    .id(RandomUtil.randomLong())
                    .title(RandomUtil.randomString(18))
                    .useId(RandomUtil.randomLong())
                    .build());
        }
    }
}
```



值得注意的是，在 Netty 的 initializer 中，我们通过 `LengthFieldBasedFrameDecoder` 进行解码，用 `LengthFieldPrepender` 进行编码。我们看下`LengthFieldBasedFrameDecoder`的API

```java
    public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip, true);
    }
```

<font style="background-color:#FBDE28;">这里，我们通过指定</font>`<font style="background-color:#FBDE28;">lengthFieldLength</font>`<font style="background-color:#FBDE28;"> 定义了 4 个字节的消息长度，来标记本次消息的有效字节数量，以防止 TCP 粘包。</font>

这里的消息格式为：

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743663473481-8a130193-8df1-46d5-9e37-d3cb6b8451d6.png)



## 定制消息协议 & 拓展序列化机制

我们查看一下 `LengthFieldBasedFrameDecoder` 和 `LengthFieldPrepender` 的父类。并通过继承这两个父类定义我们自己的编码/解码器。



RPC 通信中涉及两种类型的消息： RpcRequest, RpcResponse。

考虑到 JDK 自带的序列化机制的缺点：序列化速度慢，序列化后体积庞大。

因此这里，考虑引入其他序列化机制 —— Json, Kyro ...



综上，我们的协议需要包含：消息类型、序列化类型、消息长度、消息体等这四个字段

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743663888528-ebdd79ac-3e18-4143-9925-a068acf0b900.png)



为了支持多种序列化类型，我们定义一个序列化器的接口

```java

public interface ISerializer {
    // 把对象序列化成字节数组
    byte[] serialize(Object obj);

    // 从字节数组反序列化成消息, 使用java自带序列化方式不用messageType也能得到相应的对象（序列化字节数组里包含类信息）
    // 其它方式需指定消息格式，再根据message转化成相应的对象
    Object deserialize(byte[] bytes, int messageType);

    // 返回使用的序列器，是哪个
    // 0：java自带序列化方式, 1: json序列化方式
    int getType();


    // 定义静态常量 serializerMap
    // 这个主要用于获取序列化器的实例
    static final Map<Integer, ISerializer> serializerMap = new HashMap<>();

    // 根据序号取出序列化器，暂时有两种实现方式，需要其它方式，实现这个接口即可
    static ISerializer getSerializerByCode(int code) {
        ISerializer iSerializer = serializerMap.get(code);
        if (iSerializer == null) {
            throw new RuntimeException("No serializer registered for code " + code);
        }
        return iSerializer;
    }

    static void registerSerializer(int code, ISerializer serializer) {
        registerSerializer(code, serializer, false);
    }

    static void registerSerializer(int code, ISerializer serializer, boolean replace) {
        if (replace) {
            serializerMap.put(code, serializer);
        }else{
            serializerMap.putIfAbsent(code, serializer);
        }
    }

    static boolean containsSerializer(int code) {
        return serializerMap.containsKey(code);
    }

    public static enum SerializerType {
        JDK(0),
        JSON(1),
        KRYO(2)

        ;

        private final int code;
        SerializerType(int code) {
            this.code = code;
        }
        public int getCode() {
            return code;
        }
    }

}
```

并实现了一下几种序列化器

```java

public class ObjectSerializer implements ISerializer {

    // 利用java IO 对象 -> 字节数组
    @Override
    public byte[] serialize(Object obj) {
        byte[] bytes = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            bytes = bos.toByteArray();
            oos.close();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    // 字节数组 -> 对象
    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        Object obj = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try {
            ObjectInputStream ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            ois.close();
            bis.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return obj;
    }

    // 0 代表java原生序列化器
    @Override
    public int getType() {
        return SerializerType.JDK.getCode();
    }
}
```

```java
@Slf4j
public class JacksonSerializer implements ISerializer {
    private ObjectMapper objectMapper;

    public JacksonSerializer() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            log.error("Json 序列化发生错误: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty byte array");
        }
        // 传输的消息分为request与response
        if (MessageType.REQUEST.getCode() == messageType) {
            return handleRequest(bytes);
        } else if (MessageType.RESPONSE.getCode() == messageType) {
            return handleResponse(bytes);
        } else {
            System.out.println("暂时不支持此种消息");
            throw new RuntimeException("暂不支持此种类型的消息");
        }
    }

    private Object handleRequest(byte[] bytes) {
        // 序列化反序列化后，类型擦除了
        RpcRequest request = null;
        try {
            request = objectMapper.readValue(bytes, RpcRequest.class);
            // Convert JSON strings to corresponding objects
            for (int i = 0; i < request.getParamsTypes().length; i++) {
                Class<?> paramsType = request.getParamsTypes()[i];
                if (!paramsType.isAssignableFrom(request.getParams()[i].getClass())) {
                    byte[] tmpBytes = objectMapper.writeValueAsBytes(request.getParams()[i]);
                    request.getParams()[i] = objectMapper.readValue(tmpBytes, paramsType);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return request;
    }
    private Object handleResponse(byte[] bytes) {
        RpcResponse response = null;
        try {
            response = objectMapper.readValue(bytes, RpcResponse.class);
            Class<?> dataType = response.getDataType();
            if (!dataType.isAssignableFrom(response.getData().getClass())) {
                byte[] tmpBytes = objectMapper.writeValueAsBytes(response.getData());
                response.setData(objectMapper.readValue(tmpBytes, dataType));
//                response.setData(objectMapper.convertValue(response.getData(), dataType));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return response;
    }

    @Override
    public int getType() {
        return SerializerType.JSON.getCode();
    }
}
```

```java

@Slf4j
public class KryoSerializer implements ISerializer {
    private static final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class); // 显式注册类（提高性能）
        kryo.register(RpcResponse.class);
        kryo.register(Object[].class);
        kryo.register(Class[].class);
        kryo.register(Class.class);
        kryo.setReferences(true); // 支持循环引用
        return kryo;
    });

//    private Kryo kryo;
//    public KryoSerializer() {
//        kryo = new Kryo();
//        kryo.setReferences(false);
//        kryo.setRegistrationRequired(false);
//    }


    @Override
    public byte[] serialize(Object obj){
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
             Output output = new Output(byteArrayOutputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            // 动态注册 RpcRequest 的参数类型

            if (obj instanceof RpcRequest) {
                Class<?>[] paramTypes = ((RpcRequest) obj).getParamsTypes();
                for (Class<?> type : paramTypes) {
                    kryo.register(type);
                }
            }else if (obj instanceof RpcResponse) {
                Class<?> dataType = ((RpcResponse) obj).getDataType();
                kryo.register(dataType);
            }else{
                kryo.register(obj.getClass());
            }

            kryo.writeObject(output, obj);
            output.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Kryo serialization failed", e);
        }
    }

    @Override
    public Object deserialize(byte[] bytes, int messageType) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Cannot deserialize null or empty byte array");
        }
        if(MessageType.REQUEST.getCode()==messageType){
            return handleRequest(bytes);
        }else if(MessageType.RESPONSE.getCode()==messageType){
            return handleResponse(bytes);
        }else{
            log.error("暂不支持此种类型的消息: {}", messageType);
            throw new RuntimeException("暂不支持此种类型的消息");
        }
    }

    private Object handleResponse(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            return kryo.readObject(input, RpcResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Kryo deserialization failed", e);
        }
    }

    private Object handleRequest(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            return kryo.readObject(input, RpcRequest.class);
        } catch (Exception e) {
            throw new RuntimeException("Kryo deserialization failed", e);
        }
    }

    @Override
    public int getType() {
        return SerializerType.KRYO.getCode();
    }
}
```



接下来，我们实现编码器和解码器

```java

/**
 * 依次按照自定义的消息格式写入，传入的数据为request或者response
 * 需要持有一个serialize器，负责将传入的对象序列化成字节数组
 */
@AllArgsConstructor
@Slf4j
public class CommonEncode extends MessageToByteEncoder {
    private ISerializer serializer;

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        log.debug("MyEncode$encode mgs of Type: {}", msg.getClass());

        /**
         * 协议格式：
         * +----------------+---------------------+------------------+------------------+
         * |  消息类型        |   序列化方式          |  序列化长度        |  序列化字节       |
         * |  (2 Byte)      |   (4 Byte)          |  (4 Byte)        |  (变长)          |
         * +----------------+---------------------+------------------+------------------+
        **/

        // 写入消息类型
        if(msg instanceof RpcRequest){
            out.writeShort(MessageType.REQUEST.getCode());
        }
        else if(msg instanceof RpcResponse){
            out.writeShort(MessageType.RESPONSE.getCode());
        }
        // 写入序列化方式
        out.writeShort(serializer.getType());
        // 得到序列化数组
        byte[] serialize = serializer.serialize(msg);
        // 写入长度
        out.writeInt(serialize.length);
        // 写入序列化字节数组
        out.writeBytes(serialize);
    }
}
```

```java

/**
 * 按照自定义的消息格式解码数据
 */
@Slf4j
@AllArgsConstructor
public class CommonDecode extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("MyDecode$decode");

        // 1. 读取消息类型
        short messageType = in.readShort();
        // 现在还只支持request与response请求
        if (messageType != MessageType.REQUEST.getCode() &&
                messageType != MessageType.RESPONSE.getCode()) {
            log.error("暂不支持此种数据: {}", messageType);
            throw new RuntimeException("暂不支持此种数据");
        }
        // 2. 读取序列化的类型
        short serializerType = in.readShort();
        // 根据类型得到相应的序列化器
        ISerializer serializer = ISerializer.getSerializerByCode(serializerType);
        if (serializer == null) throw new RuntimeException("不存在对应的序列化器");
        // 3. 读取数据序列化后的字节长度
        int length = in.readInt();
        // 4. 读取序列化数组
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 用对应的序列化器解码字节数组
        Object deserialize = serializer.deserialize(bytes, messageType);
        out.add(deserialize);
    }
}
```



OK，接下来在我们的 Netty 里面引入 (服务端、客户端都引入)

```java
public class NettyClientInitializer extends ChannelInitializer<SocketChannel> {

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // 使用自定义的编解码器
        pipeline.addLast(new CommonDecode());
        // 编码需要传入序列化器，这里是json，还支持ObjectSerializer，也可以自己实现其他的
        pipeline.addLast(new CommonEncode(ISerializer.getSerializerByCode(ISerializer.SerializerType.JSON.getCode())));
        pipeline.addLast(new NettyClientHandler());
    }
}
```



## 总结

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743667507516-82e91235-6b3f-4357-8094-97d27c701ce0.png)

本节改造点

+ 利用 Netty 替换 ServerSocket 进行网络通信；从 BIO -> NIO
+ 自定义消息协议，并拓展了序列化机制



存在问题

我们的 netty 客户端需要知道服务所在的 host 和 port，然后才能进行连接。<font style="color:rgb(31, 35, 40);">每一个客户端都必须知道对应服务的ip与端口号， 并且如果服务挂了或者换地址了，就很麻烦。扩展性也不强</font>

<font style="color:rgb(31, 35, 40);"></font>

# <font style="color:rgb(31, 35, 40);">V5. 注册中心 + 负载均衡</font>

本节改进点

+ 引入 zookeeper 来管理服务端提供的服务 —— 服务提供者向 zookeeper 注册服务
+ 客户端提供服务接口名称，由服务端给出服务对应的 host & port —— 服务发现
+ 同一个服务的众多实例应该更均衡的被使用 —— 负载均衡

## 注册中心

客户端与服务端通信，每次都要管理之间的 host 和 port。

能不能服务端把提供的服务摆上台面，客户端直接给出需要的服务，服务提供者直接给你 host 和 port。

OK，这里引入 zookeeper 来实现上述功能。

服务端将自己提供的服务注册到注册中心。客户端通过给出接口，从注册中心获取服务的 host & port。



下面我们把 zookeeper 部署到 docker 上

**docker 部署**

```java
 docker run -d -e TZ="Asia/Shanghai" -p 2181:2181 -v ./data:/data --name jl-zk --restart always zookeeper
```

**引入 pom**

```java
<!-- https://mvnrepository.com/artifact/org.apache.curator/curator-recipes -->
<dependency>
    <groupId>org.apache.curator</groupId>
    <artifactId>curator-recipes</artifactId>
    <version>5.8.0</version>
</dependency>
```

有了这个注册中心，我们得先让这个它知道，”我手上有哪些服务“。

因此，我们得先对这些服务向注册中心注册，让它知道这个服务是需要被提供的

同时，客户端一般会提供服务的名称（如接口名称），然后注册需要根据这个名称给出服务。（从同一种服务多个实例中选出一个反馈给客户端 —— 负载均衡）

ok，上述过程涉及了服务，注册中心，客户之间两方面的功能：

+ 服务注册：服务端向注册中心注册可以被发现的服务
+ 服务发现：客户端根据服务名称可以从注册中心得到一个服务

因此，我们定义如下接口描述上述过程

```java
// 服务注册接口，两大基本功能，注册：保存服务与地址。 查询：根据服务名查找地址
public interface IServiceRegister {
    void register(String serviceName, InetSocketAddress serverAddress);
}
```

```java
// 服务发现
public interface IServiceDiscover {
    InetSocketAddress serviceDiscovery(String serviceName);
}
```

接下来我们先来实现

服务注册

```java

@Slf4j
public class ZkServiceRegister implements IServiceRegister {

    private final CuratorFramework client;

    public ZkServiceRegister(CuratorFramework client) {
        this.client = client;
        startClient();
    }

    private void startClient() {
        client.start();
        try {
            // 等待连接建立
            client.blockUntilConnected();
            log.info("Zookeeper连接成功，地址: {}", client.getZookeeperClient().getCurrentConnectionString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Zookeeper连接被中断", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        } catch (Exception e) {
            log.error("Zookeeper连接失败", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        }
    }

    private String getServicePath(String serviceName) {
        return String.format("/%s", serviceName);
    }

    private String getInstancePath(String serviceName, String addressName) {
        return String.format("/%s/%s",  serviceName, addressName);
    }


    @Override
    public void register(String serviceName, InetSocketAddress serverAddress) {
        if (serviceName == null || serverAddress == null) {
            throw new IllegalArgumentException("Service name and server address cannot be null");
        }
        String servicePath = getServicePath(serviceName);

        try {
            // 1. 创建持久化父节点（幂等操作） -- 一般是服务的分类，例如一个服务名
            if (client.checkExists().forPath(servicePath) == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath);
            }

            // 2. 注册临时节点（允许重复创建，实际会覆盖）-- 一般是具体的实例，服务名下，不同的实例
            String addressPath = getInstancePath(serviceName, getServiceAddress(serverAddress));
            try {
                client.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(addressPath);
                log.info("服务实例注册成功: {} -> {}", servicePath, serverAddress);
            } catch (Exception e) {
                // 节点已存在说明该实例正常在线，记录调试日志即可
                log.debug("服务实例已存在（正常心跳）: {}", addressPath);
            }

//            // 3. 创建 Retry 节点
//            if(retryable){
//                client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(String.format("/%s/%s", ZkConstants.RETRY, serviceName));
//            }

        } catch (Exception e) {
            log.error("服务注册失败: {}", servicePath, e);
            throw new RuntimeException("Failed to register service: " + servicePath, e);
        }
    }


    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() + ":" + serverAddress.getPort();
    }

}
```

服务发现

在服务发现中，我们先构建一个本地缓存，避免每次都需要去注册中心获取服务地址

```java

@Slf4j
public class ServiceCache {
    private final Map<String, List<String>> serviceCache = new HashMap<>();

    /**
     * 获取服务列表
     * @param serviceName 服务名称
     * @return 返回服务列表
     */
    public List<String> getServiceList(String serviceName) {
        return serviceCache.get(serviceName);
    }

    /**
     * 添加服务地址
     * @param serviceName
     * @param address
     */
    public void addServiceAddress(String serviceName, String address){
        serviceCache.putIfAbsent(serviceName, new ArrayList<String>());
        List<String> addressList = serviceCache.get(serviceName);
        addressList.add(address);
        log.debug("添加服务: {}, 地址: {}", serviceName, address);
    }


    /**
     * 添加服务地址列表
     * @param serviceName
     * @param addressList
     */
    public void addServiceList(String serviceName, List<String> addressList){
        serviceCache.putIfAbsent(serviceName, new ArrayList<String>());
        serviceCache.get(serviceName).addAll(addressList);
        log.debug("添加服务: {}, 地址列表: {}", serviceName,Arrays.toString(addressList.toArray()));
    }

    /**
     * 修改服务地址
     * @param serviceName 服务名称
     * @param oldAddress 旧服务地址
     * @param newAddress 新服务地址
     */
    public void replaceServiceAddress(String serviceName, String oldAddress, String newAddress) {
        if(serviceCache.containsKey(serviceName)) {
            List<String> serviceStrings = serviceCache.get(serviceName);
            serviceStrings.remove(oldAddress);
            serviceStrings.add(newAddress);
            log.debug("替换服务: {}, 旧地址: {}, 新地址: {}", serviceName, oldAddress, newAddress);
        }else{
            log.debug("服务名称: {} 服务不存在", serviceName);
        }
    }

    /**
     * 删除服务地址
     * @param serviceName
     * @param address
     */
    public void deleteServiceAddress(String serviceName,String address){
        List<String> addressList = serviceCache.get(serviceName);
        addressList.remove(address);
        log.debug("删除服务: {}, 地址: {} ", serviceName, address);
    }

}

```

```java
@Slf4j
public class ZkServiceDiscover implements IServiceDiscover {
    private final CuratorFramework client;


    // 既然做了一个本地缓存，缓存添加上去后，服务挂了，谁来更新缓存 ？
    private final ServiceCache serviceCache = new ServiceCache();

    // zk 提供了一种监控机制
    private CuratorCache curatorCache;


    public ZkServiceDiscover(CuratorFramework client) {
        this.client = client;
        this.client.start();
    }

    private String getServicePath(String serviceName) {
        return String.format("/%s", serviceName);
    }

    private String getInstancePath(String serviceName, String addressName) {
        return String.format("/%s/%s",  serviceName, addressName);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null");
        }
        String servicePath = getServicePath(serviceName);
        try {
            // 优先从缓存获取
//            List<String> instances = serviceCache.get(servicePath);
            List<String> instances = serviceCache.getServiceList(serviceName);
            // 没有获取到缓存，则从 zk 中读取
            if (instances == null || instances.isEmpty()) {
                instances = client.getChildren().forPath(servicePath);
                // 缓存 key 是 appName + serviceName
//                serviceCache.put(servicePath, instances);
                serviceCache.addServiceList(serviceName, instances);
            }

            if (instances.isEmpty()) {
                log.warn("未找到可用服务实例: {}", servicePath);
                return null;
            }
            // 未进行负载均衡，选择第一个

            return parseAddress(instances.get(0));
        } catch (Exception e) {
            log.error("服务发现失败: {}", servicePath, e);
            throw new RuntimeException("Failed to discover service: " + servicePath, e);
        }
    }


    private InetSocketAddress parseAddress(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address format: " + address);
        }
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }
}
```



ok，接下来改造一下服务提供者，将 服务注册 过程注入

```java
@Slf4j
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个服务接口，
     */
    private Map<String, Object> interfaceProvider;
    private final IServiceRegister serviceRegister;
    private final InetSocketAddress socketAddress;

    public ServiceProvider(IServiceRegister serviceRegister, InetSocketAddress socketAddress) {
        this.serviceRegister = serviceRegister;
        // 需要传入服务端自身的服务的网络地址
        this.interfaceProvider = new HashMap<>();
        this.socketAddress = socketAddress;
        log.debug("服务提供者启动: {}", socketAddress.toString());
    }

    public void provideServiceInterface(Object service) {
        Class<?>[] interfaces = service.getClass().getInterfaces();
        // 一个类可能实现多个服务接口
        for (Class<?> i : interfaces) {
            // 本机的映射表
            interfaceProvider.put(i.getName(), service);
            // 在注册中心注册服务
            serviceRegister.register(i.getName(), socketAddress);
        }
    }

    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }
}
```

在客户端通过与 zk 连接发现服务

```java
@Slf4j
public class NettyRpcClient implements IRpcClient {
    private static final Bootstrap bootstrap;
    private static final EventLoopGroup eventLoopGroup;
    private static final NettyClientInitializer nettyClientInitializer;

    // 通过注入
    private final IServiceDiscover serviceDiscover;

    // netty客户端初始化，重复使用
    static {
        nettyClientInitializer = new NettyClientInitializer();
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(nettyClientInitializer);
    }

    public NettyRpcClient(IServiceDiscover serviceDiscover) {
        this.serviceDiscover = serviceDiscover;
    }

    /**
     * 这里需要操作一下，因为netty的传输都是异步的，你发送request，会立刻返回， 而不是想要的相应的response
     */
    @Override
    public RpcResponse sendRequest(RpcRequest request) {
        InetSocketAddress address = serviceDiscover.serviceDiscovery(request.getInterfaceName());
        log.debug("RPC$远程服务地址: {}", address);
        if (address == null) {
            log.error("服务发现失败，返回的地址为 null");
            return RpcResponse.fail("服务发现失败，地址为 null");
        }
        try {
            ChannelFuture channelFuture = bootstrap.connect(address.getHostName(), address.getPort()).sync();
            Channel channel = channelFuture.channel();

            // 发送数据
            channel.writeAndFlush(request);
            // closeFuture: channel关闭的Future
            // sync 表示阻塞等待 它 关闭
            channel.closeFuture().sync();
            // 阻塞的获得结果，通过给channel设计别名，获取特定名字下的channel中的内容（这个在hanlder中设置）
            // AttributeKey是，线程隔离的，不会由线程安全问题。
            // 实际上不应通过阻塞，可通过回调函数
            AttributeKey<RpcResponse> key = AttributeKey.valueOf("RPCResponse");
            RpcResponse rpcResponse = channel.attr(key).get();
            return rpcResponse;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        // 关闭 netty
        if(eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully().addListener(future -> {
                if (future.isSuccess()) {
                    log.info("关闭 Netty 成功");
                }else{
                    log.info("关闭 Netty 失败");
                }
            });
            try {
                eventLoopGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                log.error("关闭 Netty 异常: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

}
```



测试

```java
public class RpcServerTest {

    public static void main(String[] args) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
                1000,
                3
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("192.168.160.128:2181")   // zk 服务地址 host:port
                .sessionTimeoutMs(30000)
                .retryPolicy(retryPolicy)
                .namespace("BobbyRPC")
                .build();

        IServiceRegister serviceRegister = new ZkServiceRegister(client);

        IUserService userService = new UserServiceImpl();
        IBlogService blogService = new BlogServiceImpl();

        ServiceProvider serviceProvider = new ServiceProvider(serviceRegister, new InetSocketAddress("127.0.0.1", 8899));
        serviceProvider.provideServiceInterface(userService);
        serviceProvider.provideServiceInterface(blogService);

//        IRpcServer rpcServer = new ThreadPoolRPCServer(serviceProvider);
        IRpcServer rpcServer = new NettyRpcServer(serviceProvider);
        rpcServer.start(8899);
    }

}
```

```java
public class RpcClientTest {
    public static void main(String[] args) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
                1000,
                3
        );

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("192.168.160.128:2181")   // zk 服务地址 host:port
                .sessionTimeoutMs(30000)
                .retryPolicy(retryPolicy)
                .namespace("BobbyRPC")
                .build();

        IServiceDiscover serviceDiscover = new ZkServiceDiscover(client);

//        IRpcClient rpcClient = new SimpleRpcClient("127.0.0.1", 8899);
        IRpcClient rpcClient = new NettyRpcClient(serviceDiscover);
        ClientProxy clientProxy = new ClientProxy(rpcClient);
        IUserService userService = clientProxy.createProxy(IUserService.class);

        IBlogService blogService = clientProxy.createProxy(IBlogService.class);


        for(int i=0; i<100; i++){
            User user = userService.getUser(RandomUtil.randomLong());
            System.out.println("接受的User: "+ user);

            blogService.addBlog(Blog.builder()
                    .id(RandomUtil.randomLong())
                    .title(RandomUtil.randomString(18))
                    .useId(RandomUtil.randomLong())
                    .build());
        }
    }
}

```

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743670044837-c059cbf4-463f-4c64-aefa-4cce933d8b84.png)



## 负载均衡

上面我们在服务发现的时候，是服务列表的第一个，下面我们实现两种简单的负载均衡策略 —— 随机、轮询



定义接口

```java
/**
 * 给服务器地址列表，根据不同的负载均衡策略选择一个
 */
public interface ILoadBalance {
    String balance(List<String> addressList);
}
```

```java
/**
 * 随机负载均衡
 */
public class RandomLoadBalance implements ILoadBalance {
    @Override
    public String balance(List<String> addressList) {
        Random random = new
                Random();
        int choose = random.nextInt(addressList.size());
        System.out.println("负载均衡选择了" + choose + "服务器");
        return addressList.get(choose);
    }
}
```

```java
/**
 * 轮询负载均衡
 */
public class RoundLoadBalance implements ILoadBalance {
    private int choose = -1;
    @Override
    public String balance(List<String> addressList) {
        choose++;
        choose = choose%addressList.size();
        return addressList.get(choose);
    }
}
```

然后在客户端，发现服务中，最后采用负载均衡策略

```java

@Slf4j
public class ZkServiceDiscover implements IServiceDiscover {
    private final CuratorFramework client;
    private final ILoadBalance loadBalance;


    // 既然做了一个本地缓存，缓存添加上去后，服务挂了，谁来更新缓存 ？
    private final ServiceCache serviceCache = new ServiceCache();

    // zk 提供了一种监控机制
    private CuratorCache curatorCache;


    public ZkServiceDiscover(CuratorFramework client, ILoadBalance loadBalance) {
        this.client = client;
        this.loadBalance = loadBalance;
        this.client.start();
        // 开启服务监听
        ZkWatcher zkWatcher = new ZkWatcher(client, serviceCache);
        zkWatcher.watch(ZkConstants.ZK_NAMESPACE);
    }

    private String getServicePath(String serviceName) {
        return String.format("/%s", serviceName);
    }

    private String getInstancePath(String serviceName, String addressName) {
        return String.format("/%s/%s",  serviceName, addressName);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null");
        }
        String servicePath = getServicePath(serviceName);
        try {
            // 优先从缓存获取
//            List<String> instances = serviceCache.get(servicePath);
            List<String> instances = serviceCache.getServiceList(serviceName);
            // 没有获取到缓存，则从 zk 中读取
            if (instances == null || instances.isEmpty()) {
                instances = client.getChildren().forPath(servicePath);
                // 缓存 key 是 appName + serviceName
//                serviceCache.put(servicePath, instances);
                serviceCache.addServiceList(serviceName, instances);
            }

            if (instances.isEmpty()) {
                log.warn("未找到可用服务实例: {}", servicePath);
                return null;
            }

            String selectedInstance = loadBalance.balance(instances);
            return parseAddress(selectedInstance);
        } catch (Exception e) {
            log.error("服务发现失败: {}", servicePath, e);
            throw new RuntimeException("Failed to discover service: " + servicePath, e);
        }
    }


    private InetSocketAddress parseAddress(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address format: " + address);
        }
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }

    @Override
    public boolean retryable(String serviceName) {
        boolean canRetry =false;
        try {
            List<String> serviceList = client.getChildren().forPath("/" + ZkConstants.RETRY);
            for(String s:serviceList){
                if(s.equals(serviceName)){
                    log.debug("服务: {} 在白名单上，可以进行重试", serviceName);
                    canRetry=true;
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return canRetry;
    }


}

```

## 总结

本小节解决

+ 注册中心：由注册中心来管理服务，并给出服务的具体地址
+ 负载均衡策略

存在问题

+ 当服务端的实例下线后，我们不能检测到，导致本地服务缓存没有更新。从而导致客户端获取到故障的服务实例。



# V6. 服务节点监控 + 心跳机制

本小节解决

+ 对 zk 管理的服务进行监控，当服务节点发生变化时，可以通知客户端（回调）
+ 心跳机制：用于监控网络服务。注册中心和服务示例维持着心跳健康检查，当实例宕机时，将该实例从注册中心中移除。并利用 zk watch 机制通知客户端的缓存进行更新。



## 服务节点监控

> <font style="color:rgb(64, 64, 64);">ZooKeeper 的 </font>**<font style="color:rgb(64, 64, 64);">Watch 机制</font>**<font style="color:rgb(64, 64, 64);"> 是一种</font>**<font style="color:rgb(64, 64, 64);">事件监听</font>**<font style="color:rgb(64, 64, 64);">模型，允许客户端在特定节点（ZNode）上注册监听（Watcher），当节点状态或数据发生变化时，ZooKeeper 会主动通知客户端，从而实现分布式系统的</font>**<font style="color:rgb(64, 64, 64);">事件驱动</font>**<font style="color:rgb(64, 64, 64);">协作。</font>

OK，所以我们可以通过 zk watch 来监听节点的变化，当某些事件发生时，我们可以进行一些处理。

```java
@Slf4j
public class ZkWatcher {
    private final CuratorFramework client;
    private final ServiceCache cache;
    private String currentWatchPath;

    public ZkWatcher(CuratorFramework client, ServiceCache cache) {
        this.client = client;
        this.cache = cache;
    }

    public void watch(String watchPath) {
        if (watchPath == null) {
            throw new IllegalArgumentException("Service name cannot be null");
        }
//        String servicePath = getServicePath(serviceName);
        this.currentWatchPath = watchPath;

        // 创建新的 CuratorCache
        CuratorCache curatorCache = CuratorCache.build(client, watchPath);
        curatorCache.start();


        // 添加监听器
        // 分别在创建时，改变时，删除时对本地缓存进行改动
        CuratorCacheListener listener = CuratorCacheListener.builder()
                .forCreates(this::handleNodeCreated)
                .forChanges(this::handleNodeUpdated)
                .forDeletes(this::handleNodeDeleted)
                .forInitialized(() -> log.info("节点监听器初始化完成，监听: {}", watchPath))
                .build();

        curatorCache.listenable().addListener(listener);

        log.info("已创建服务监听");
    }

    private String parseServiceName(ChildData childData){
        String s = new String(childData.getData());
        return s;
    }

    // 处理节点创建事件
    private void handleNodeCreated(ChildData childData) {
        if (!isDirectChild(childData.getPath(), currentWatchPath)) return;
        updateServiceCache(currentWatchPath);
        log.info("服务节点已创建: {}", childData.getPath());
    }

    // 处理节点更新事件
    private void handleNodeUpdated(ChildData oldData, ChildData newData) {
        if (!isDirectChild(oldData.getPath(), currentWatchPath)) return;
        updateServiceCache(currentWatchPath);
        log.debug("服务节点已更新: {}", oldData.getPath());
    }

    // 处理节点删除事件
    private void handleNodeDeleted(ChildData childData) {
        if (!isDirectChild(childData.getPath(), currentWatchPath)) return;

        updateServiceCache(currentWatchPath);
        log.debug("服务节点已下线: {}", childData.getPath());
    }

    // 更新本地缓存
    private void updateServiceCache(String servicePath) {
        try {
            List<String> instances = client.getChildren().forPath(servicePath);
            cache.addServiceList(servicePath, instances);
        } catch (Exception e) {
            log.error("服务节点缓存更新失败: {}", servicePath, e);
        }
    }

    // 判断是否为直接子节点（避免孙子节点干扰）
    public boolean isDirectChild(String fullPath, String parentPath) {
        return fullPath.startsWith(parentPath) &&
                fullPath.substring(parentPath.length()).lastIndexOf('/') <= 0;
    }
}

```

```java
@Slf4j
public class ZkServiceDiscover implements IServiceDiscover {
    private final CuratorFramework client;
    private final ILoadBalance loadBalance;


    // 既然做了一个本地缓存，缓存添加上去后，服务挂了，谁来更新缓存 ？
    private final ServiceCache serviceCache ;
    private final ZkWatcher zkWatcher;

    public ZkServiceDiscover(CuratorFramework client, ILoadBalance loadBalance) {
        this.client = client;
        this.loadBalance = loadBalance;

        this.client.start();

        serviceCache = new ServiceCache();
        // v6
        // 开启服务监听
        zkWatcher = new ZkWatcher(client, serviceCache);
//        // 监控根目录
//        zkWatcher.watch("/BobbyRPC");    // 监控的 根路径
        // 服务发现的话，一般只需监控自己所用的服务下的实例节点就好把？
        // 监控整个根路径反而会带来较大的性能开销
    }

    private String getServicePath(String serviceName) {
        return String.format("/%s", serviceName);
    }

    private String getInstancePath(String serviceName, String addressName) {
        return String.format("/%s/%s",  serviceName, addressName);
    }

    @Override
    public InetSocketAddress serviceDiscovery(String serviceName) {
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null");
        }
        String servicePath = getServicePath(serviceName);
        try {
            // 优先从缓存获取
            List<String> instances = serviceCache.getServiceList(servicePath);
            // 没有获取到缓存，则从 zk 中读取
            if (instances == null || instances.isEmpty()) {
                instances = client.getChildren().forPath(servicePath);
                // 缓存 key 是 appName + serviceName
//                serviceCache.put(servicePath, instances);
                serviceCache.addServiceList(servicePath, instances);

                // v6
                // 因此我们在服务发现的时候，动态的进行监控
                // 如果缓存中没有，表示是第一次获取，那么我们就对这些服务实例进行监控
                // 当这些服务实例发生变动时，就通知客户端
                zkWatcher.watch(servicePath);
            }

            if (instances.isEmpty()) {
                log.warn("未找到可用服务实例: {}", servicePath);
                return null;
            }
            // 未进行负载均衡，选择第一个
            String selectedInstance = loadBalance.balance(instances);

            return parseAddress(selectedInstance);
        } catch (Exception e) {
            log.error("服务发现失败: {}", servicePath, e);
            throw new RuntimeException("Failed to discover service: " + servicePath, e);
        }
    }


    private InetSocketAddress parseAddress(String address) {
        String[] parts = address.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid address format: " + address);
        }
        return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

```



## 心跳机制

心跳机制可以用来（以下我们先实现第一个方面）

- [x] 维持客户端与服务端的连接。当客户端调用了一个服务，大概率还会可能再调用，因此我们利用心跳机制把这个连接”保活“一段时间。当客户端关闭，超过时间后，服务端主动关闭连接。
- [ ] 注册中心对服务的”健康探测“（永久实例）
- [ ] 服务端向注册中心进行保活（临时实例）





对于 心跳包，我们用的是一个 request 。

为了辨别心跳包与正常的请求包，在 RpcRequest 里面添加一个区分字段。

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements Serializable {
    // 服务类名，客户端只知道接口名，在服务端中用接口名指向实现类
    private String interfaceName;
    // 方法名
    private String methodName;
    // 参数列表
    private Object[] params;
    // 参数类型
    private Class<?>[] paramsTypes;

    // v6. 包类型
    private RequestType requestType = RequestType.NORMAL;

    public enum RequestType {
        NORMAL(0),
        HEARTBEAT(1),
        ;
        private final int code;
        RequestType(int code){
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
```

然后在Netty责任链中添加一个空闲检测机制

> `<font style="color:rgb(64, 64, 64);">IdleStateHandler</font>`<font style="color:rgb(64, 64, 64);"> 是 </font>**<font style="color:rgb(64, 64, 64);">Netty</font>**<font style="color:rgb(64, 64, 64);"> 提供的一个关键处理器（ChannelHandler），用于检测连接的空闲状态（如读空闲、写空闲、读写空闲）。它的核心作用是</font>**<font style="color:rgb(64, 64, 64);">自动触发空闲事件</font>**<font style="color:rgb(64, 64, 64);">，帮助开发者处理长时间无数据交互的连接，避免资源浪费或实现业务层面的保活逻辑。</font>

```java
        // v6 心跳机制，使链接存活
        pipeline.addLast(new IdleStateHandler(0, 8, 0, TimeUnit.SECONDS));
        pipeline.addLast(new ClientHeartbeatHandler());
```

```java
@Slf4j
public class ClientHeartbeatHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof IdleStateEvent idleStateEvent) {

            IdleState idleState = idleStateEvent.state();

            if(idleState == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush(RpcRequest.heartBeat());
                log.info("超过8秒没有写数据，发送心跳包");
            }

        }else {
            super.userEventTriggered(ctx, evt);
        }
    }
}
```



```java
 // v6 添加心跳机制
        // 读空闲10s，写空闲20s
        pipeline.addLast(new IdleStateHandler(10, 20, 0, TimeUnit.SECONDS));
        pipeline.addLast(new ServerHeartbeatHandler());   // 对 IdelState 事件的处理
```

```java
@Slf4j
public class ServerHeartbeatHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        try {
            // 处理IdleState.READER_IDLE时间
            if(evt instanceof IdleStateEvent idleStateEvent) {
                IdleState idleState = idleStateEvent.state();
                // 如果是触发的是读空闲时间，说明已经超过n秒没有收到客户端心跳包
                if(idleState == IdleState.READER_IDLE) {
                    log.info("超过10秒没有收到客户端心跳， channel: " + ctx.channel());
                    // 关闭channel，避免造成更多资源占用
                    ctx.close();
                }else if(idleState ==IdleState.WRITER_IDLE){
                    log.info("超过20s没有写数据,channel: " + ctx.channel());
                    // 关闭channel，避免造成更多资源占用
                    ctx.close();
                }
            }
        }catch (Exception e){
            log.error("事件发生异常: "+e);
        }
    }
}
```

```java
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        log.info("NettyServer 接收请求: {}", request);
        if(request.getRequestType().equals(RpcRequest.RequestType.HEARTBEAT)){
            log.info("接收到客户端的心跳包");
            return;
        }
        if(request.getRequestType().equals(RpcRequest.RequestType.NORMAL)){

            RpcResponse response = getResponse(request);

            ctx.writeAndFlush(response);
        }
//        ctx.close();
//        log.info("NettyServer 关闭连接");
    }
```



测试

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743690027956-ba0e6ac7-7382-459f-9a07-32af2bc11f58.png)

## 总结

本小节实现了

+ 利用心跳机制来维持客户端与服务端的 channel 连接
    - 服务端，超过10s没有读事件（没有收到请求），则关闭 channel
    - 服务端，超过20s没有写事件（没有发送请求），则关闭 channel
    - 客户端，超过8s没有写事件，发送心跳包（占用连接）



必须等待心跳发送完，才能发送正常的业务消息？

好像破案了。服务端在发送完 response 后没有关闭 ctx, 然后就一致等待？



所以这里的心跳机制的作用是？

<font style="color:rgb(77, 77, 77);">这是因为很多情况服务端感知不到channel断开连接，比如手机突然强制关机、进入飞行模式等情况，这样的话</font><font style="color:rgb(78, 161, 219) !important;">TCP</font><font style="color:rgb(77, 77, 77);">连接没有经过四次挥手断开连接，因此服务端无法感知，还是需要心跳检测机制来确保客户端是否在线。</font>



存在问题

+ 大量请求并发下，可能把我们的服务端打崩，因此我们可以在服务端采取一些 限流措施
+ 服务端崩了之后，变得不可用，为了避免一直去请求这个不可用的服务，我们在客户端采取熔断措施



# V7. 限流机制 + 熔断机制

本节改进点

+ 服务端限流机制 —— 令牌桶限流
+ 客户端熔断机制



## 限流机制

为了方便拓展，我们定义一个限流接口

```java
public interface IRateLimit {
    boolean getToken();
}
```

`getToken` 用于表示当前请求能否被满足

然后我们基于该接口实现一个 令牌桶 的限流机制

```java

/**
 * 令牌桶限流器实现
 * 介绍：
 * 令牌桶算法是一种基于令牌的限流算法，它维护一个固定容量的令牌桶，按照固定速率往桶中添加令牌，
 * 每当有请求到来时，消耗一个令牌，如果桶中没有足够的令牌，则拒绝该请求。
 *
 * 主要是用来限制 单位时间内通过的请求数量
 *
 * 特点：
 * 1. 固定时间间隔会添加令牌。
 * 2. 桶满了，就不继续增加令牌
 * 3. 当令牌消费完后，就拒绝请求
 *
 * 原理：
 *
 *
 *
 * 参考：
 * https://www.cnblogs.com/DTinsight/p/18221858
 *
 *
 *
 */
public class TokenBucketRateLimitImpl implements IRateLimit {

    // 令牌产生速率 (ms)
    private static int RATE;
    // 桶容量
    private static  int CAPACITY;
    //当前桶容量
    private volatile int curCapcity;

    //时间戳
    private volatile long lastTimestamp;


    public TokenBucketRateLimitImpl(int rate,int capacity){
        RATE=rate;
        CAPACITY=capacity;
        curCapcity=capacity;
        lastTimestamp=System.currentTimeMillis();
    }


    @Override
    public boolean getToken() {
        // 如果当前桶中还有令牌，则可以访问
        if(curCapcity > 0){
            curCapcity--;
            return true;
        }

        // 桶中没有令牌，
        // 则添加令牌：按照时间差内能生成多少令牌
        // 当前时刻 - 上一时刻。在这段时间内能生成多少令牌
        long now = System.currentTimeMillis();
        long delta_timestamp = now - lastTimestamp;
        if(delta_timestamp > RATE){
            // 生成了至少一个令牌
            // 计算是不是有生成更多令牌
            if(delta_timestamp/RATE > 2){
                // 至少生成 2 个，才可以给桶中加上令牌
                // 因为这次请求要消耗一个
                curCapcity += (int)(delta_timestamp/RATE) - 1;
            }

            if(curCapcity > CAPACITY){
                curCapcity = CAPACITY; // 不能超过桶的容量
            }

            lastTimestamp = now;
            return true;
        }
        // 请求太快啦，令牌还没生成呢
        return false;
    }
}

```

同时为了拓展更多的限流机制，并且方便调用。我们创建了一个限流提供者。后续甚至可以针对服务进行限流

```java
/**
 * 针对每个服务，都可以设定限流器
 * 限流器一般设置在服务提供者
 */
public class RateLimitProvider {
    private final Map<String, IRateLimit> rateLimitMap;
    public RateLimitProvider() {
        rateLimitMap = new HashMap<>();
    }

    public IRateLimit getRateLimit(String interfaceName) {
        if( !rateLimitMap.containsKey(interfaceName)){
            rateLimitMap.put(interfaceName, new TokenBucketRateLimitImpl(100, 10));
        }
        return rateLimitMap.get(interfaceName);
    }
}
```

既然实在服务端进行限流的，那么我们应该在处理请求的时候进行限流

那么我们在服务提供的时候就应该确认一下，是否能够接纳该请求

因此，在服务提供者处注入该限流机制

```java
@Slf4j
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个服务接口，
     */
    private Map<String, Object> interfaceProvider;
    private final IServiceRegister serviceRegister;
    private final InetSocketAddress socketAddress;
    private final RateLimitProvider rateLimitProvider;


    public ServiceProvider(IServiceRegister serviceRegister, InetSocketAddress socketAddress, RateLimitProvider rateLimitProvider) {
        this.serviceRegister = serviceRegister;
        // 需要传入服务端自身的服务的网络地址
        this.interfaceProvider = new HashMap<>();
        this.socketAddress = socketAddress;
        this.rateLimitProvider = rateLimitProvider;
        log.debug("服务提供者启动: {}", socketAddress.toString());
    }

    public void provideServiceInterface(Object service) {
        Class<?>[] interfaces = service.getClass().getInterfaces();
        // 一个类可能实现多个服务接口
        for (Class<?> i : interfaces) {
            // 本机的映射表
            interfaceProvider.put(i.getName(), service);
            // 在注册中心注册服务
            serviceRegister.register(i.getName(), socketAddress);
        }
    }

    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }

    public RateLimitProvider getRateLimitProvider() {
        return rateLimitProvider;
    }
}
```

最后，我们在处理请求，调用服务返回响应之前，做一下限流

```java

    private RpcResponse getResponse(RpcRequest request) {
        // 得到服务名
        String interfaceName = request.getInterfaceName();

        // ve7. 在这里做限流措施
        IRateLimit rateLimit = serviceProvider.getRateLimitProvider().getRateLimit(interfaceName);
        if (!rateLimit.getToken()) {
            log.info("服务: {} 限流!!!", interfaceName);
            return RpcResponse.fail("服务限流!!!");
        }

        // 得到服务端相应服务实现类
        Object service = serviceProvider.getService(interfaceName);
        // 反射调用方法
        Method method = null;
        try {
            method = service.getClass().getMethod(request.getMethodName(), request.getParamsTypes());
            Object ret = method.invoke(service, request.getParams());
            // 某些操作可能没有返回值
            Class<?> dataType = null;
            if (ret != null) {
                dataType = ret.getClass();
            }
            return RpcResponse.builder()
                    .code(200)
                    .data(ret)
                    .dataType(dataType)
                    .message("OK")
                    .build();
        } catch (NoSuchMethodException | IllegalAccessException | NullPointerException | InvocationTargetException e) {
            e.printStackTrace();
            return RpcResponse.fail();
        }
    }
```



![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743746922217-9a89827d-ebcb-41b8-bb34-33f347a53125.png)



注意到，我们这里是按照服务接口名称做限流的

## 熔断机制

服务熔断，一般是指，客户端不进行远程访问了。在本地做一下快速失败

当我们的系统依赖于外部服务，外部服务失败多次或不可用时，就可以先不再去尝试了，可以考虑对该服务进行熔断

熔断器一般会设置 3 种状态

+ CLOSE: 关闭
+ HALF: 半开，可以自动进行检测服务是否恢复
+ OPEN: 全开

我们可以对一个接口进行监控，当失败次数超过一定次数之后，开启熔断机制。反之，当成功一定次数，可以将熔断器关闭

```java

/**
 * 熔断器
 * 当我们的系统依赖于外部服务，外部服务失败多次或不可用时，就可以先不再去尝试了，可以考虑对该服务进行熔断（即，快速失败，避免一直去调用）
 * 我们可以对一个接口进行监控，当失败次数超过一定次数之后，开启熔断机制。反之，当成功一定次数，可以将熔断器关闭
 *
 */
@Slf4j
public class CircuitBreaker {
    enum CircuitBreakerState {
        CLOSED,OPEN,HALF_OPEN
    }

    // 熔断器状态
    private CircuitBreakerState state = CircuitBreakerState.CLOSED;

    // 统计次数
    private AtomicInteger failureCount = new AtomicInteger(0);
    private AtomicInteger successCount = new AtomicInteger(0);
    private AtomicInteger requestCount = new AtomicInteger(0);

    // 失败次数阈值，超过该次数，熔断器就开启
    private final int failureThreshold;
    //半开启 -> 关闭状态的成功次数比例
    private final double halfOpenSuccessRate;
    //恢复时间
    private final long retryTimePeriod;

    //上一次失败时间
    private long lastFailureTime;

    public CircuitBreaker(int failureThreshold, double halfOpenSuccessRate, long retryTimePeriod) {
        this.failureThreshold = failureThreshold;
        this.halfOpenSuccessRate = halfOpenSuccessRate;
        this.retryTimePeriod = retryTimePeriod;
        this.lastFailureTime = 0;
    }


    /**
     * 查看当前熔断器是否允许请求通过
     * @return
     */
    public synchronized boolean allowRequest() {
        long currentTime = System.currentTimeMillis();
//        log.info("熔断器, 当前失败次数: {}", failureCount);
        switch (state) {
            case OPEN:
                if (currentTime - lastFailureTime > retryTimePeriod) {
                    state = CircuitBreakerState.HALF_OPEN;
                    resetCounts();
                    return true;
                }
                log.info("熔断生效");
                return false;
            case HALF_OPEN:
                requestCount.incrementAndGet();
                return true;
            case CLOSED:
            default:
                return true;
        }
    }

    /// ///////////////////////////////////////////////////////////////////////////////
    // 以下都是进行状态转换

    //记录成功
    public synchronized void recordSuccess() {
        if (state == CircuitBreakerState.HALF_OPEN) {
            successCount.incrementAndGet();
            if (successCount.get() >= halfOpenSuccessRate * requestCount.get()) {
                state = CircuitBreakerState.CLOSED;
                resetCounts();
            }
        } else {
            resetCounts();
        }
    }
    //记录失败

    /**
     * 出现一次失败时，就进入 half-open 状态
     * 当超过一定次数时，则进入 closed 状态
     */
    public synchronized void recordFailure() {
        failureCount.incrementAndGet();
        System.out.println("记录失败!!!!!!!失败次数"+failureCount);
        lastFailureTime = System.currentTimeMillis();
        if (state == CircuitBreakerState.HALF_OPEN) {
            state = CircuitBreakerState.OPEN;
            lastFailureTime = System.currentTimeMillis();
        } else if (failureCount.get() >= failureThreshold) {
            state = CircuitBreakerState.OPEN;
        }
    }
    //重置次数
    private void resetCounts() {
        failureCount.set(0);
        successCount.set(0);
        requestCount.set(0);
    }

}
```

```java

public class CircuitBreakerProvider {
    private Map<String,CircuitBreaker> circuitBreakerMap=new HashMap<>();

    public synchronized CircuitBreaker getCircuitBreaker(String serviceName){
        CircuitBreaker circuitBreaker;
        if(circuitBreakerMap.containsKey(serviceName)){
            circuitBreaker=circuitBreakerMap.get(serviceName);
        }else {
//            System.out.println("serviceName="+serviceName+"创建一个新的熔断器");
            circuitBreaker=new CircuitBreaker(1,0.5,10000);
            circuitBreakerMap.put(serviceName,circuitBreaker);
        }
        return circuitBreaker;
    }

    public void setCircuitBreakerForMethod(String serviceName, CircuitBreaker circuitBreaker){
        circuitBreakerMap.put(serviceName,circuitBreaker);
    }
}

```



我们在发送请求后，收到回复时，根据回复的响应状态来判断服务是否可用。并根据服务响应成功或失败的次数，来动态改变限流器的状态

```java

@Slf4j
public class ClientProxy implements InvocationHandler {
    private IRpcClient rpcClient;
    private final CircuitBreakerProvider circuitBreakerProvider;


    public ClientProxy(IRpcClient rpcClient, CircuitBreakerProvider circuitBreakerProvider) {
        this.rpcClient = rpcClient;
        this.circuitBreakerProvider = circuitBreakerProvider;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // v2.
        // 代理对象执行每个方法时，都将执行这里的逻辑
        // 我们的目的是，利用这个代理类帮助我构建请求。这样能够有效减少重复的代码
        RpcRequest request = RpcRequest.builder()
                .interfaceName(method.getDeclaringClass().getName())
                .methodName(method.getName())
                .paramsTypes(method.getParameterTypes())
                .params(args)
                .type(RpcRequest.RequestType.NORMAL)
                .build();

        // v7 熔断器
        //获取熔断器
        CircuitBreaker circuitBreaker= circuitBreakerProvider.getCircuitBreaker(method.getName());
        //判断熔断器是否允许请求经过
        if (!circuitBreaker.allowRequest()){
            //这里可以针对熔断做特殊处理，返回特殊值
            log.info("服务被熔断了");
            return RpcResponse.fail("服务被熔断了");
        }

        // 然后将这个请求发送到服务端，并获取响应
        // v6. 利用 IRpcClient 对象发送请求
        RpcResponse response = rpcClient.sendRequest(request);

        // v7 根据响应信息，更新熔断器状态

        if (response != null) {
            if (response.getCode() == 200) {
                circuitBreaker.recordSuccess();
            } else if (response.getCode() == 500) {
                circuitBreaker.recordFailure();
            }
            log.info("收到响应: {} 状态码: {}", request.getInterfaceName(), response.getCode());
            return response.getData();
        }

        return null;
    }

    // 获取代理对象
    public <T> T createProxy(Class<T> clazz) {
        Object o = Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{clazz}, this);
        return (T) o;
    }
}

```

## 总结

在本节中，我们利用

+ 限流器，来防止请求量过大导致服务崩坏
+ 熔断器，根据服务质量，如果该服务一直失败，限流器就从 hlaf-open 状态变为 open 状态，从而不再去发送这个无意义的请求。等过一段时间，再去尝试





# V8. 失败重试机制 + 白名单重试

客户端在发送消息失败后，可以进行重新发送。例如上面由于限流，导致客户端请求失败，那么失败的请求应该能自动再发送一次请求。这些请求必须是具有幂等性的请求

因此，本节改造如下

+ 利用 GuavaRetry 对发送失败或异常的请求进行重试。



```java
@Slf4j
public class GuavaRetry {

    public RpcResponse sendRequestWithRetry(RpcRequest request, IRpcClient rpcClient) {
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                //无论出现什么异常，都进行重试
                .retryIfException()
                //返回结果为 error时进行重试
                .retryIfResult(response -> !Objects.isNull(response) && Objects.equals(response.getCode(), 500))
                //重试等待策略：等待 2s 后再进行重试
                .withWaitStrategy(WaitStrategies.fixedWait(2, TimeUnit.SECONDS))
                //重试停止策略：重试达到 3 次
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        log.debug("重试机制, 第 {} 次重试", attempt.getAttemptNumber());
                    }
                })
                .build();
        try {
            return retryer.call(() -> rpcClient.sendRequest(request));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RpcResponse.fail();
    }
}

```



在客户端发送请求后，进行重试。



然而，我们要先确定哪些服务是可以重试的，并在服务注册的时候，将这些服务添加到一个 “可重试服务专区”（白名单）。这里我们可以在 zk 中新开一个分支作为重试服务分支即可。为了将可重试的服务注册上去，我们得对 IServiceRegister 改造一下。添加 boolean retryable 属性到 register 接口方法上。

```java
// 服务注册接口，两大基本功能，注册：保存服务与地址。 查询：根据服务名查找地址
public interface IServiceRegister {
    void register(String serviceName, InetSocketAddress serverAddress, boolean retryable);
}
```

```java

@Slf4j
public class ZkServiceRegister implements IServiceRegister {

    private final CuratorFramework client;

    public ZkServiceRegister(CuratorFramework client) {
        this.client = client;
        startClient();
    }

    private void startClient() {
        client.start();
        try {
            // 等待连接建立
            client.blockUntilConnected();
            log.info("Zookeeper连接成功，地址: {}", client.getZookeeperClient().getCurrentConnectionString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Zookeeper连接被中断", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        } catch (Exception e) {
            log.error("Zookeeper连接失败", e);
            throw new RuntimeException("Failed to connect to Zookeeper", e);
        }
    }

    private String getServicePath(String serviceName) {
        return String.format("/%s", serviceName);
    }

    private String getInstancePath(String serviceName, String addressName) {
        return String.format("/%s/%s",  serviceName, addressName);
    }


    @Override
    public void register(String serviceName, InetSocketAddress serverAddress, boolean retryable) {
        if (serviceName == null || serverAddress == null) {
            throw new IllegalArgumentException("Service name and server address cannot be null");
        }
        String servicePath = getServicePath(serviceName);

        try {
            // 1. 创建持久化父节点（幂等操作） -- 一般是服务的分类，例如一个服务名
            if (client.checkExists().forPath(servicePath) == null) {
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.PERSISTENT)
                        .forPath(servicePath);
            }

            // 2. 注册临时节点（允许重复创建，实际会覆盖）-- 一般是具体的实例，服务名下，不同的实例
            String addressPath = getInstancePath(serviceName, getServiceAddress(serverAddress));
            try {
                client.create()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath(addressPath);
                log.info("服务实例注册成功: {} -> {}", servicePath, serverAddress);
            } catch (Exception e) {
                // 节点已存在说明该实例正常在线，记录调试日志即可
                log.debug("服务实例已存在（正常心跳）: {}", addressPath);
            }

            // v8. 创建 Retry 节点
            if(retryable){
                if(client.checkExists().forPath(String.format("/%s/%s", "RETRY", serviceName))==null){
                    log.info("注册可重试服务: {} -> {}", servicePath, serverAddress);
                    client.create().creatingParentsIfNeeded()
                            .withMode(CreateMode.EPHEMERAL)
                            .forPath(String.format("/%s/%s", "RETRY", serviceName));
                }else{
                    log.info("重试服务已存在: {} -> {}", servicePath, serverAddress);
                }
            }

        } catch (Exception e) {
            log.error("服务注册失败: {}", servicePath, e);
            throw new RuntimeException("Failed to register service: " + servicePath, e);
        }
    }


    private String getServiceAddress(InetSocketAddress serverAddress) {
        return serverAddress.getHostName() + ":" + serverAddress.getPort();
    }
}
```

```java

@Slf4j
public class ServiceProvider {
    /**
     * 一个实现类可能实现多个服务接口，
     */
    private Map<String, Object> interfaceProvider;
    private final IServiceRegister serviceRegister;
    private final InetSocketAddress socketAddress;
    private final RateLimitProvider rateLimitProvider;


    public ServiceProvider(IServiceRegister serviceRegister, InetSocketAddress socketAddress, RateLimitProvider rateLimitProvider) {
        this.serviceRegister = serviceRegister;
        // 需要传入服务端自身的服务的网络地址
        this.interfaceProvider = new HashMap<>();
        this.socketAddress = socketAddress;
        this.rateLimitProvider = rateLimitProvider;
        log.debug("服务提供者启动: {}", socketAddress.toString());
    }

    public void provideServiceInterface(Object service, boolean retryable) {
        Class<?>[] interfaces = service.getClass().getInterfaces();
        // 一个类可能实现多个服务接口
        for (Class<?> i : interfaces) {
            // 本机的映射表
            interfaceProvider.put(i.getName(), service);
            // 在注册中心注册服务
            serviceRegister.register(i.getName(), socketAddress, retryable);
        }
    }

    public Object getService(String interfaceName) {
        return interfaceProvider.get(interfaceName);
    }

    public RateLimitProvider getRateLimitProvider() {
        return rateLimitProvider;
    }
}
```



即，注册过程可以指定该接口的服务是否是可以重试的。如果可以重试，不仅要添加到服务节点上，还要添加到可重试节点上。





# V9. 配置项 + starter + 注解驱动

参考

[https://developer.aliyun.com/article/893073](https://developer.aliyun.com/article/893073)



在本节中，我们将试着

+ 将一些配置提取出来， 可以统一的对 rpc 组件相关部分进行修改
+ 将模块构建成 starter，可以方便的引入其他项目中
+ 利用注解来实现服务发现与服务注册



## 配置项

首先考虑我们需要哪些配置项?

+ zk client `CuratorFramework` 的相关配置 ，例如 session time, namespace，zk的地址
+ netty 启动监听的端口
+ ...

因此，我们抽象出如下的配置（里面有些配置暂未使用到，暂时参考了 Dubbo 的配置）

```java

/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/6
 */
@Builder
@Data
@ConfigurationProperties(prefix = "brpc")
public class BRpcProperties {
    private String applicationName; // 暂时没用到
    private Boolean watch;
    private NettyProperties netty;
    private ZkProperties zk;

    @Data
    @Builder
    public static class NettyProperties{
        private int port;
        private String serializer;
    }

    @Data
    @Builder
    public static class ZkProperties {
        private String address;  // 直接映射 myrpc.zk.address
        private int sessionTimeoutMs;  // 自动绑定 session-timeout-ms
        private String namespace;   // zk 节点的命名空间。
        private RetryProperties retry;    // 嵌套对象
    }

    @Data
    @Builder
    public static class RetryProperties {
        private int maxRetries;      // 绑定 max-retries
        private int baseSleepTimeMs; // 绑定 base-sleep-time-ms
    }
}
```



## Starter

将我们的模块构建为 starter ，使我们的模块更加方便使用

我们创建一个配置类，用来创建我们 RPC 框架所需要的 Bean 对象

```java
@EnableConfigurationProperties(value = {BRpcProperties.class})
@Slf4j
public class BRpcAutoConfiguration {
    // 在这个配置项里面，创建相关的 bean 对象

    private final BRpcProperties brpcProperties;

    public BRpcAutoConfiguration(BRpcProperties brpcProperties) {
        this.brpcProperties = brpcProperties;
    }

    // zk client
    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public CuratorFramework zkClient() {
        log.info("Create bean of CuratorFramework zkClient");
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(
                brpcProperties.getZk().getRetry().getMaxRetries(),
                brpcProperties.getZk().getRetry().getMaxRetries()
        );

        try (CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(brpcProperties.getZk().getAddress())   // zk 服务地址 host:port
//                .connectString("192.168.160.128:2181")   // zk 服务地址 host:port
                .sessionTimeoutMs(brpcProperties.getZk().getSessionTimeoutMs())
                .retryPolicy(retryPolicy)
                .namespace(brpcProperties.getZk().getNamespace())
                .build()) {
//            client.start();
            return client;
        } catch (Exception e) {
            log.error("zk client create error", e);
            throw new RuntimeException("zk client create error", e);
        }
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IServiceRegister serviceRegister(CuratorFramework client) {
        log.info("Create bean of IServiceRegister serviceRegister");
        return new ZkServiceRegister(client);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public RateLimitProvider rateLimitProvider() {
        log.info("Create bean of RateLimitProvider rateLimitProvider");
        return new RateLimitProvider();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ServiceProvider serviceProvider(IServiceRegister serviceRegister, RateLimitProvider rateLimitProvider) {
        log.info("Create bean of ServiceProvider serviceProvider");

        // 本机 ip + 指定 netty 通信的端口
        // TODO 这里先用 localhost 作为 ip
        return new ServiceProvider(serviceRegister, new InetSocketAddress("127.0.0.1", brpcProperties.getNetty().getPort()), rateLimitProvider);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IRpcServer rpcServer(ServiceProvider serviceProvider) {
        log.info("Create bean of IRpcServer rpcServer");

        NettyRpcServer nettyRpcServer = new NettyRpcServer(serviceProvider);
        nettyRpcServer.start(brpcProperties.getNetty().getPort());
        return nettyRpcServer;
    }

    // Client

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ILoadBalance loadBalance() {
        log.info("Create bean of ILoadBalance loadBalance");

        return new RoundLoadBalance();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IServiceDiscover serviceDiscover(CuratorFramework client, ILoadBalance loadBalance) {
        log.info("Create bean of IServiceDiscover serviceDiscover");

        return new ZkServiceDiscover(client, loadBalance);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public CircuitBreakerProvider circuitBreakerProvider() {
        log.info("Create bean of CircuitBreakerProvider circuitBreakerProvider");

        return new CircuitBreakerProvider();
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public IRpcClient rpcClient(IServiceDiscover serviceDiscover) {
        log.info("Create bean of IRpcClient rpcClient");

        return new NettyRpcClient(serviceDiscover);
    }

    @Bean
    @Role(ROLE_INFRASTRUCTURE)
    public ClientProxy clientProxy(IRpcClient rpcClient, CircuitBreakerProvider circuitBreakerProvider, IServiceDiscover serviceDiscover) {
        log.info("Create bean of ClientProxy clientProxy");

        return new ClientProxy(rpcClient, circuitBreakerProvider, serviceDiscover);
    }


    // 注解驱动
    @Bean
    public RpcServiceProcessor rpcServiceProcessor(ServiceProvider serviceProvider) {
        log.info("Create bean of RpcServiceProcessor rpcServiceProcessor");
        return new RpcServiceProcessor(serviceProvider);
    }

    @Bean
    public RpcReferenceProcessor rpcReferenceProcessor(ClientProxy clientProxy) {
        log.info("Create bean of RpcReferenceProcessor rpcReferenceProcessor");
        return new RpcReferenceProcessor(clientProxy);
    }
}
```

然后，为了在项目中引入这个配置类，让starter生效我们有两种做法

+ 利用 Spring SPI 机制

![](https://cdn.nlark.com/yuque/0/2025/webp/50582501/1743944597361-010e542f-64a9-4753-b3eb-d1bb6fe19bb4.webp)

+ 利用 自定义注解，Import 相关配置类

```java
/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/6
 * 通过该注解，将我们的 RPC 框架引入到项目中
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(value = {BRpcAutoConfiguration.class})
public @interface EnableBRpc {
}
```

Spring 在启动时，就会加载这里面的bean 对象



## 注解驱动

参考 Dubbo 的用法。服务引用与实现，都采用同一个接口。即，这个接口可能定义在一个 common 模块中，服务引用和实现都是用该接口。

那么，我们在实现类可以使用一个注解 `@RpcService` (`@DubboService`) 来进行服务注册

在使用服务时，可以用 `@RpcReference` (`@DubboReference`) 进行服务引用

接下来我们定义这两个注解

```java
/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/3/30
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RpcService {
    /**
     * 服务接口类
     * @return 接口Class对象
     */
    Class<?> interfaceClass() default void.class;

    boolean retryable() default false;

    String version() default "0.01";
}
```

```java
/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/3/30
 *
 * 本质上是通过这个注解，扫描到需要注入的位置
 * 然后对该位置的接口进行代理
 * 代理类做的事情就是
 * - 构建请求
 * - 拿到数据
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface RpcReference {
    Class<?> interfaceClass() default void.class;

    String version() default "0.01";
}

```



为了能发现这两个注解的服务类和引用，我们需要在 bean 对象创建时，检查实现类是否带有`@RpcService`注解或字段里面是否包含`@RpcReference`注解的字段

因此，我们只要在 bean 创建后处理服务注册或服务发现

具体做法是，通过继承接口 `BeanPostProcessor` 来实现上述功能

```java
@Slf4j
public class RpcServiceProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;

    public RpcServiceProcessor(ServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        // 对所有 bean 试图获取 RpcService 注解
        RpcService rpcService = bean.getClass().getAnnotation(RpcService.class);
        if (rpcService != null) {
            register(bean, rpcService);
        }
        return bean;
    }

    private void register(Object bean, RpcService rpcService) {
        log.info("RpcServiceProcessor$register 正在注册服务: {}", bean.getClass().getName());
//        Class<?> interfaceClass = rpcService.interfaceClass();
//        // 默认使用第一个接口
//        if (interfaceClass == void.class) {
//            interfaceClass = bean.getClass().getInterfaces()[0];
//        }
//        String serviceName = interfaceClass.getName();
//         获取本应用的 host & port
        serviceProvider.provideServiceInterface(bean, rpcService.retryable());
    }

}
```

将该接口添加到 zk 节点上



```java
@Slf4j
public class RpcReferenceProcessor implements BeanPostProcessor {
    private final ClientProxy clientProxy;

    public RpcReferenceProcessor(ClientProxy clientProxy) {
        this.clientProxy = clientProxy;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Field[] fields = bean.getClass().getDeclaredFields();
        for (Field field : fields) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                log.debug("找到一个 RpcReference 的字段 {}", field.getName());
                // 实现类似 DubboReference
                // 接口是公共模块的
                // 接口的实现不在同一台服务器上
                // 我们通过代理类，为接口的每个调用构造请求
                // 通过远程调用来获取结果
                Class<?> rpcReferenceInterface = rpcReference.interfaceClass();
                if (rpcReferenceInterface == void.class) {
                    rpcReferenceInterface = field.getType();
                }
                // 根据接口获取代理类对象
                // 生成代理对象并注入
                log.debug("rpcReferenceInterface: {}", rpcReferenceInterface);

                Object proxy = clientProxy.createProxy(rpcReferenceInterface);
                field.setAccessible(true);
                try {
                    log.debug("代理类注入 bean: {}, declareField: {}, proxy: {}", bean.getClass().getTypeName(), field.getName(), proxy.getClass().getTypeName());
                    field.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("注入RPC服务失败", e);
                }
            }
        }
        return bean;
    }

}

```

将代理类注入到该字段上

## 测试

我们新建了两个模块，用来测试本节新增的功能点

整体项目结构如下

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743945012522-93d3dbb7-20c0-48ea-9659-3dd0f81a0aac.png)

+ V9 ：RPC 框架的核心
+ V9-Starter ：进行 bean 定义
+ Blog, User 两个测试服务模块，其中在 UserServiceImpl 中引用了 IBlogService 的功能

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743945098911-e56f9b43-8bbb-469a-a23a-a01f4e375cbe.png)

测试结果如下



服务注册：

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743945123665-cff762de-5957-4bbc-91fd-9c60d2c76869.png)

服务发现

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1743945149814-da2911cc-6b76-4c51-b49f-e5d428796545.png)

# 0410 编码、解码器的改进 —— 魔数、防止拆包

上面我们编码器的做法，能够有效的防止粘包，只要我们读取到消息长度的字段后，就可以完整的取出一个数据包。

然而，在发生拆包时，例如我们只读取到消息长度的某一部分，或者前面协议其他字段的某一部分时，这时候是不能继续往下解析的。否则就会发生 `DecoderException: java.lang.IndexOutOfBoundsException` 异常

我们可以通过如下测试用例进行测试

```java
/**
 * @author: Bobby
 * @email: vividbobby@163.com
 * @date: 2025/4/10
 */
public class DecoderTest {
    @Test
    public void testDecodeWithHalfPackets() {
        // 1. 准备解码器和测试用的 EmbeddedChannel
        CommonDecoder decoder = new CommonDecoder();
        EmbeddedChannel channel = new EmbeddedChannel(decoder);

        RpcResponse response = RpcResponse.builder()
                .data(1L)
                .dataType(Long.class)
                .message("hhhh")
                .code(200)
                .build();
        ISerializer serializer = ISerializer.getSerializerByCode(1);
        byte[] responseBytes = serializer.serialize(response);

        // 2. 构造一个完整的合法帧（假设总长度 4+2+2+4+serialize.length 字节）
        int totalLength = 4 + 2 + 2 + 4 + responseBytes.length;
        ByteBuf fullFrame = Unpooled.buffer();
        fullFrame.writeInt(BRpcConstants.MAGIC_NUMBER); // 4字节 魔数
        fullFrame.writeShort(MessageType.RESPONSE.getCode()); // 2字节 消息类型
        fullFrame.writeShort(1); // 2字节 序列化类型
        fullFrame.writeInt(responseBytes.length); // 4字节 数据长度
        fullFrame.writeBytes(responseBytes); //

        // 3. 模拟拆包：分 3 次写入（每次只写部分数据）
        ByteBuf slice1 = Unpooled.copiedBuffer(fullFrame.slice(0, 5));
        ByteBuf slice2 = Unpooled.copiedBuffer(fullFrame.slice(5, 10)); // 从 5 开始，读取长度为 10
        ByteBuf slice3 = Unpooled.copiedBuffer(fullFrame.slice(15, totalLength-15));

        // 4. 分次写入，检查解码器是否正确处理
        channel.writeInbound(slice1); // 第一次：数据不足，应该不触发 decode
        Object o1 = channel.readInbound();
        assertNull(o1); // 无输出

        channel.writeInbound(slice2); // 第二次：仍然不足（缺少剩余数据）
        Object o2 = channel.readInbound();
        assertNull(o2); // 无输出

        channel.writeInbound(slice3); // 第三次：数据完整，应解码成功
        Object decoded = channel.readInbound();
        assertNotNull(decoded); // 成功解码

        // 5. 释放资源
        fullFrame.release();
    }
}

```

OK

这是因为我们之前的 解码器 没有对拆包进行防御性编程。

参考 `[LengthFieldBasedFrameDecoder](https://github.com/netty/netty/blob/4.1/codec/src/main/java/io/netty/handler/codec/LengthFieldBasedFrameDecoder.java)` 的源码。一开始我们也需要对协议的字段进行校验或判断（例如，读取元数据的长度是否足够...）

在这之前我们已经将协议修改如下：添加魔数

![](https://cdn.nlark.com/yuque/0/2025/png/50582501/1744266029442-62645e0f-0aa0-438f-96b4-9876810e5773.png)

因此，在 decode 的过程中，例如，获取魔数，如果 `in.readableBytes < 4` 就说明数据包还没完整到达，此时返回 `null` 不进行处理。

我们的处理方式如下

```java

/**
 * 按照自定义的消息格式解码数据
 */
@Slf4j
@AllArgsConstructor
public class CommonDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        log.debug("MyDecode$decode");
        Object frame = decode(ctx, in);
        if (frame != null) {
            out.add(frame);
        }
    }

    private Object decode(ChannelHandlerContext ctx, ByteBuf in) {
        // 模仿 LengthFieldBasedFrameDecoder 防止拆包的思路
        // 先进行校验，如果数据长度不足，先返回 null

        // 0. 标记当前读指针位置（防拆包回退）
        in.markReaderIndex();

        // 1. 检查是否足够读取魔数（4字节）
        if (in.readableBytes() < 4) {
            return null; // 等待更多数据
        }

        // 2. 读取并验证魔数
        int magicNumber = readMagicNumber(in);
        if (magicNumber != BRpcProtocolConstants.MAGIC_NUMBER) {
            log.error("非法数据包: 魔数不匹配, 实际: 0x{}, 预期: 0x5250434D",
                    Integer.toHexString(magicNumber));
            throw new RuntimeException(String.format("Invalid Magic Number: 0x{}", Integer.toHexString(magicNumber)));
        }

        // 3. 检查剩余数据是否足够读取消息类型+序列化类型+长度（2+2+4=8字节）
        if (in.readableBytes() < 8) {
            in.resetReaderIndex(); // 回退起始位置
            return null;
        }
        // 4. 读取元数据 (消息类型，序列化类型，消息长度)
        short messageType = in.readShort();
        if (messageType != MessageType.REQUEST.getCode() &&
                messageType != MessageType.RESPONSE.getCode()) {
            log.error("暂不支持此种数据: {}", messageType);
            throw new RuntimeException("暂不支持此种数据");
        }
        short serializerType = in.readShort();
        int length = in.readInt();

        // 5. 检查是否足够读取实际数据
        if (in.readableBytes() < length) {
            in.resetReaderIndex(); // 回退起始位置
            return null;
        }

        ISerializer serializer = ISerializer.getSerializerByCode(serializerType);
        // 4. 读取序列化数组
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        // 用对应的序列化器解码字节数组
        Object frame = serializer.deserialize(bytes, messageType);
        return frame;
    }

    private int readMagicNumber(ByteBuf in) {
        return in.readInt();    // 我们魔术是定义 4 个字节
    }
}
```

这里面涉及几个关键的地方

+ `in.readableBytes()`表示当前缓冲区中可读的字节数
+ `in.markReaderIndex();` 标记当前读的位置，后面可以通过`in.resetReaderIndex();`回溯
+ `in.resetReaderIndex();`回到标记读的位置

在防止拆包中，我们通过每次获取缓冲区可读字节的数量来决定是否继续往下读取。利用 `in.markReaderIndex` 和 `in.resetReaderIndex` 来标记当前缓冲区读的位置和发生拆包时进行读指针回溯



相比于 `LengthFieldBasedFrameDecoder` 我们这里采用了 `mark/reset ReaderIndex` 来控制读指针。在 `LengthFieldBasedFrameDecoder` 源码中，它是预先算出了帧的大小。而我们这里由于预先读取了 魔数 来进行判断是不是我们这个 RPC 框架的消息（`in.readInt`会导致读指针往后移动），因此，当发生拆包的时候，就需要`in.resetReaderIndex`来重置读指针了




