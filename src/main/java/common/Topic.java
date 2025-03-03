package common;

import java.util.HashMap;
import java.util.Map;

public class Topic {
    private String name;
    private Map<String, Vote> votes = new HashMap<>();

    public Topic(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Vote getVote(String voteName) {
        return votes.get(voteName);
    }

    public Map<String, Vote> getAllVotes(){
        return votes;
    }

    public void addVote(Vote vote){
        votes.put(vote.getName(), vote);
    }

    public void deleteVote(String voteName){    // удаляем существующую опцию
            votes.remove(voteName);
    }
}