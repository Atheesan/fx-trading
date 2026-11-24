package com.fxai.user;

import com.fxai.instrument.SupportedCurrencyPairs;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class UserDataService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public UserDataService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    public record Watchlist(@NotNull @Size(max=8) List<@NotBlank @Pattern(regexp="[A-Z]{6}") String> symbols,
                            @Pattern(regexp="[A-Z]{6}") String selectedSymbol) {
        public Watchlist {
            if (symbols == null || symbols.size() > 8 || symbols.stream().anyMatch(s -> SupportedCurrencyPairs.findBySymbol(s).isEmpty())
                    || new HashSet<>(symbols).size() != symbols.size()
                    || (selectedSymbol != null && !symbols.contains(selectedSymbol))) {
                throw new IllegalArgumentException("Invalid watchlist");
            }
            symbols = List.copyOf(symbols);
        }
    }
    public record Settings(@NotNull @Pattern(regexp="light|dark|system") String theme) {}

    public Watchlist watchlist(UUID user) {
        return jdbc.query("SELECT state FROM user_watchlist WHERE user_id=?",
                (rs,n) -> json.readValue(rs.getString(1), Watchlist.class), user).stream().findFirst()
                .orElse(new Watchlist(List.of("EURUSD","GBPUSD","USDJPY","AUDUSD","NZDUSD"), "EURUSD"));
    }

    @Transactional
    public Watchlist saveWatchlist(UUID user, Watchlist state) {
        jdbc.update("INSERT INTO user_watchlist(user_id,state) VALUES (?,?) ON CONFLICT (user_id) DO UPDATE SET state=EXCLUDED.state",
                user, json.writeValueAsString(state));
        return state;
    }

    public Settings settings(UUID user) {
        return jdbc.query("SELECT theme FROM user_settings WHERE user_id=?", (rs,n) -> new Settings(rs.getString(1)), user)
                .stream().findFirst().orElse(new Settings("system"));
    }

    @Transactional
    public Settings saveSettings(UUID user, Settings settings) {
        jdbc.update("INSERT INTO user_settings(user_id,theme) VALUES (?,?) ON CONFLICT (user_id) DO UPDATE SET theme=EXCLUDED.theme", user, settings.theme());
        return settings;
    }
}
