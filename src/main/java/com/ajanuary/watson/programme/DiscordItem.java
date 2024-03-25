package com.ajanuary.watson.programme;

import java.time.LocalDateTime;
import java.util.List;

public record DiscordItem(String id, String title, String body, String loc, String time, LocalDateTime dateTime) {

}
