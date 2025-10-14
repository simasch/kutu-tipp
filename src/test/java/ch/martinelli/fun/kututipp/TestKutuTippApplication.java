package ch.martinelli.fun.kututipp;

import org.springframework.boot.SpringApplication;

public class TestKutuTippApplication {

    public static void main(String[] args) {
        SpringApplication.from(KutuTippApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
