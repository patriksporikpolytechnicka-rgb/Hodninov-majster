package sk.kasv.fx.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppInfoController {
    private final boolean demo;
    public AppInfoController(@Value("${app.demo:false}") boolean demo) { this.demo = demo; }
    @GetMapping("/api/info")
    public AppInfo info() { return new AppInfo("Hodinový majster", demo); }
    public record AppInfo(String name, boolean demo) {}
}
