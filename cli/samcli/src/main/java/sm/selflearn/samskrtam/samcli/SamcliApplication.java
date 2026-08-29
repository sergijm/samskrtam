package sm.selflearn.samskrtam.samcli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import picocli.CommandLine;

class SpringFactory implements CommandLine.IFactory {
    private final ApplicationContext context;

    SpringFactory(ApplicationContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> clazz) {
        return (T) context.getBean(clazz);
    }
}

@SpringBootApplication
public class SamcliApplication {

    private static final Logger log = LoggerFactory.getLogger(SamcliApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SamcliApplication.class, args);
        System.exit(SpringApplication.exit(ctx));
    }

    @Bean
    public CommandLine.IFactory picocliFactory(ApplicationContext applicationContext) {
        return new SpringFactory(applicationContext);
    }

    @Bean
    public CommandLine commandLine(CommandLine.IFactory factory, RootCommand rootCommand) {
        return new CommandLine(rootCommand, factory);
    }

    @Bean
    public ExitCodeHolder exitCodeHolder() {
        return new ExitCodeHolder();
    }

    @Bean
    public CommandLineRunner commandLineRunner(CommandLine commandLine, ExitCodeHolder holder) {
        return args -> {
            int code = commandLine.execute(args);
            holder.setCode(code);
        };
    }

    @Bean
    public ExitCodeGenerator exitCodeGenerator(ExitCodeHolder holder) {
        return holder::getCode;
    }

    public static class ExitCodeHolder {
        private int code = 0;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }
    }
}
