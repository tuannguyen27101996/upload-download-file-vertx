import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

class Response {
    private String code;

    private String name;

    public Response(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
public class MainApp {

    public static Vertx vertx;

    public static void main(String[] args) {
       startAPI();

    }
    static void startAPI() {
        vertx = Vertx.vertx();

        DeploymentOptions options = new DeploymentOptions().setWorker(true);

        options.setWorkerPoolSize(16);
        options.setInstances(16);
        vertx.deployVerticle(MainVerticle.class.getName(), options);
    }
}
