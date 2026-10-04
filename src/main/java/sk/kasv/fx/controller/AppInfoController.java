package sk.kasv.fx.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppInfoController {
    private final boolean demo;
    private final boolean demoAccounts;
    public AppInfoController(@Value("${app.demo:false}") boolean demo,
                                            @Value("${app.demo-accounts:false}") boolean demoAccounts) {
        this.demo = demo;
        this.demoAccounts = demoAccounts;
    }
    @GetMapping("/api/info")
    public AppInfo info() { return new AppInfo("Hodinový majster", demo, demoAccounts); }
    public record AppInfo(String name, boolean demo, boolean demoAccounts) {}
}
