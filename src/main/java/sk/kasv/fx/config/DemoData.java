package sk.kasv.fx.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sk.kasv.fx.entity.*;
import sk.kasv.fx.repository.*;
import java.math.BigDecimal;

@Component
@Profile({"demo", "seed"})
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final ProviderProfileRepository providers;
    private final CategoryRepository categories;
    private final ServiceRepository services;
    private final PasswordEncoder passwords;
    public DemoData(UserRepository users, ProviderProfileRepository providers, CategoryRepository categories,
                    ServiceRepository services, PasswordEncoder passwords) {
        this.users = users; this.providers = providers; this.categories = categories;
        this.services = services; this.passwords = passwords;
    }
    @Override @Transactional
    public void run(String... args) {
        if (users.existsByEmail("zakaznik@demo.sk")) return;
        users.save(verified(new User("zakaznik@demo.sk", passwords.encode("DemoHeslo123!"), Role.CUSTOMER)));
        users.save(verified(new User("admin@demo.sk", passwords.encode("DemoHeslo123!"), Role.ADMIN)));
        Category home = category("Domácnosť"), garden = category("Záhrada"), it = category("IT pomoc"), moving = category("Sťahovanie");
        ProviderProfile peter = provider("majster@demo.sk", "Peter Novák", "Košice", "Pomôžem s montážou nábytku a drobnými opravami. Záleží mi na čistom výsledku aj poriadku po práci.");
        ProviderProfile lucia = provider("zahrada@demo.sk", "Lucia Zelená", "Prešov", "Postarám sa o trávnik, záhony aj sezónne práce vo vašej záhrade.");
        ProviderProfile martin = provider("technik@demo.sk", "Martin Kováč", "Košice", "Zrozumiteľná pomoc s počítačmi a domácou technikou, osobne aj na diaľku.");
        service(peter, home, "Montáž nábytku", "Nová skriňa, stôl alebo police? Nábytok poskladám a pripravím na používanie. Náradie si prinesiem so sebou.", "25.50", PriceUnit.HOUR);
        service(peter, home, "Drobné opravy v domácnosti", "Uvoľnená kľučka, polica alebo dvierka. Pomôžem s drobnosťami, na ktoré doma nezostáva čas.", "22.00", PriceUnit.HOUR);
        service(lucia, garden, "Kosenie a údržba záhrady", "Pravidelné kosenie, úprava okrajov trávnika a starostlivosť o záhony. Rozsah si dohodneme podľa vašej záhrady.", "18.00", PriceUnit.HOUR);
        service(martin, it, "Nastavenie počítača a Wi-Fi", "Nastavenie novej techniky, domácej siete alebo tlačiarne. Všetko vám aj jednoducho vysvetlím.", "30.00", PriceUnit.JOB);
        service(peter, moving, "Pomoc so sťahovaním", "Pomoc pri prenášaní krabíc a nábytku. Objednajte si čas, ktorý potrebujete, a do poznámky napíšte podrobnosti.", "20.00", PriceUnit.HOUR);
        service(lucia, garden, "Výsadba a úprava záhonov", "Príprava pôdy a výsadba rastlín. Materiál a konkrétny rozsah práce si dohodneme pred realizáciou.", "12.00", PriceUnit.SQUARE_METER);
    }
    private static User verified(User user) { user.setEmailVerified(true); return user; }
    private Category category(String name) {
        Category c = new Category(); c.setName(name); return categories.save(c);
    }
    private ProviderProfile provider(String email, String name, String location, String description) {
        User user = users.save(verified(new User(email, passwords.encode("DemoHeslo123!"), Role.PROVIDER)));
        ProviderProfile profile = new ProviderProfile(user, name, description, location, "Demo kontakt", null, true);
        profile.startTrial(java.time.Instant.now());
        return providers.save(profile);
    }
    private void service(ProviderProfile provider, Category category, String name, String description, String price, PriceUnit unit) {
        Service s = new Service(); s.setProvider(provider); s.setCategory(category); s.setName(name);
        s.setDescription(description); s.setPrice(new BigDecimal(price)); s.setPriceUnit(unit); s.setActive(true);
        services.save(s);
    }
}
