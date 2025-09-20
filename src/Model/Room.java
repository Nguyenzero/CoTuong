package Model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Room {
    private final SimpleStringProperty name;
    private final SimpleStringProperty host;
    private final SimpleStringProperty status;

    public Room(String name, String host, String status) {
        this.name = new SimpleStringProperty(name);
        this.host = new SimpleStringProperty(host);
        this.status = new SimpleStringProperty(status);
    }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }

    public String getHost() { return host.get(); }
    public StringProperty hostProperty() { return host; }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
}

