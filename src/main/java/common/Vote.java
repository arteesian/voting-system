package common;

import java.util.HashMap;
import java.util.Map;

public class Vote {
    private String name;
    private String description;
    private String creator;
    private Map<String, Integer> options;

    public Vote(String name, String description, Map<String, Integer> options, String creator) {
        this.name = name;
        this.description = description;
        this.options = new HashMap<>(options);
        this.creator = creator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getCreator() {
        return creator;
    }

    public void vote(String option){    // прибавляем 1 к счетчику голосов
        options.put(option, options.get(option) + 1);
    }

    public Map<String, Integer> getOptions(){
        if(options == null){
            options = new HashMap<>();
        }
        return options;
    }
}