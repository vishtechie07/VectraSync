package com.vectrasync;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Push
@Theme(value = "vectrasync", variant = Lumo.DARK)
public class VectraSyncApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(VectraSyncApplication.class, args);
    }
}
