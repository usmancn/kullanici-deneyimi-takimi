package deneme;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static int idCounter = 1;

    private final int id;
    private final String message;
    private final String time;

    public Message(String message) {
        this.id = idCounter++;
        this.message = message;
        this.time = LocalTime.now().format(TIME_FMT);
    }

    public int getId()         { return id; }
    public String getMessage() { return message; }
    public String getTime()    { return time; }
}