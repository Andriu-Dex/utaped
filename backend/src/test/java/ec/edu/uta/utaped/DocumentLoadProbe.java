package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bounded, isolated concurrency probe. Not a production capacity benchmark. */
final class DocumentLoadProbe {
    record Result(int status,byte[] body) {}
    static class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).connectTimeout(Duration.ofSeconds(10)).build();
        final int port;String csrf;
        Browser(int port,JdbcTemplate jdbc,String email) throws Exception {
            this.port=port;assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));
            refresh();String answer=LoginChallenges.answer(client,port,jdbc);
            assertEquals(204,request("POST","/api/auth/login","application/x-www-form-urlencoded",("username="+email+"&password=Test-only-password-2026&captcha="+answer).getBytes(StandardCharsets.UTF_8)).status());refresh();
        }
        void refresh() throws Exception { csrf=com.jayway.jsonpath.JsonPath.read(new String(request("GET","/api/auth/csrf",null,null).body(),StandardCharsets.UTF_8),"$.token"); }
        Result request(String method,String path,String type,byte[] bytes) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(60));
            if(type!=null) builder.header("Content-Type",type);if(!method.equals("GET")) builder.header("X-CSRF-TOKEN",csrf);
            var response=client.send(builder.method(method,bytes==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),HttpResponse.BodyHandlers.ofByteArray());
            return new Result(response.statusCode(),response.body());
        }
    }
    private final List<Map<String,Object>> phases=new ArrayList<>();
    private final List<Integer> attempts=new CopyOnWriteArrayList<>();
    Result measured(Callable<Result> operation) throws Exception { var result=operation.call();attempts.add(result.status());return result; }
    Result retryBusy(Callable<Result> operation) throws Exception {
        for(int attempt=0;attempt<60;attempt++) {
            var result=measured(operation);
            if(result.status()!=503) return result;
            Thread.sleep(500);
        }
        throw new AssertionError("Busy operation did not recover within bounded retries");
    }
    List<Result> phase(String name,List<Callable<Result>> tasks) throws Exception {
        assertTrue(tasks.size()>=2&&tasks.size()<=8);
        attempts.clear();
        var heap=java.lang.management.ManagementFactory.getMemoryMXBean();
        var os=(com.sun.management.OperatingSystemMXBean)java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        var peak=new AtomicLong(heap.getHeapMemoryUsage().getUsed());long cpu=os.getProcessCpuTime(),start=System.nanoTime();
        var durations=new CopyOnWriteArrayList<Double>();var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(tasks.size());var sampler=Executors.newSingleThreadScheduledExecutor()) {
            sampler.scheduleAtFixedRate(()->peak.accumulateAndGet(heap.getHeapMemoryUsage().getUsed(),Math::max),0,25,TimeUnit.MILLISECONDS);
            var pending=new ArrayList<Future<Result>>();
            for(var task:tasks) pending.add(pool.submit(()->{assertTrue(gate.await(10,TimeUnit.SECONDS));long since=System.nanoTime();try{return task.call();}finally{durations.add((System.nanoTime()-since)/1e6);}}));
            gate.countDown();var results=new ArrayList<Result>();
            try { for(var future:pending) results.add(future.get(90,TimeUnit.SECONDS)); }
            finally { for(var future:pending) future.cancel(true);sampler.shutdownNow();pool.shutdownNow(); }
            double elapsed=(System.nanoTime()-start)/1e6;var times=durations.stream().sorted().toList();var statuses=new TreeMap<Integer,Integer>();
            for(var result:results) statuses.merge(result.status(),1,Integer::sum);
            var attemptCounts=new TreeMap<Integer,Integer>();for(int status:attempts) attemptCounts.merge(status,1,Integer::sum);
            phases.add(Map.of("name",name,"operations",tasks.size(),"statuses",statuses,"attemptStatuses",attemptCounts,"elapsedMs",elapsed,
                "p95Ms",times.get((int)Math.ceil(times.size()*.95)-1),"processCpuMs",(os.getProcessCpuTime()-cpu)/1e6,
                "sampledHeapPeakBytes",peak.get(),"responseBytes",results.stream().mapToLong(r->r.body().length).sum()));
            return results;
        }
    }
    void save(String name) throws Exception {
        Path directory=Path.of("target/load-qa");Files.createDirectories(directory);
        Files.writeString(directory.resolve(name+".json"),tools.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(Map.of(
            "database","utaped_test","maximumWorkers",8,"javaVersion",System.getProperty("java.version"),"processors",Runtime.getRuntime().availableProcessors(),
            "scope","bounded local acceptance; no institutional SLA","phases",phases)));
    }
}
