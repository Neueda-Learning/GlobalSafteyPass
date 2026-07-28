package travelassistant.dto;

import java.util.ArrayList;
import java.util.List;

public class ReadinessResult {

    private int score;
    private String level;
    private List<WarningItem> warnings = new ArrayList<>();
    private List<ActionItem> actions = new ArrayList<>();

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public List<WarningItem> getWarnings() { return warnings; }
    public void setWarnings(List<WarningItem> warnings) { this.warnings = warnings; }
    public List<ActionItem> getActions() { return actions; }
    public void setActions(List<ActionItem> actions) { this.actions = actions; }

    public static class WarningItem {
        private String type;
        private String message;

        public WarningItem() {}
        public WarningItem(String type, String message) {
            this.type = type;
            this.message = message;
        }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ActionItem {
        private String action;
        private String description;

        public ActionItem() {}
        public ActionItem(String action, String description) {
            this.action = action;
            this.description = description;
        }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
