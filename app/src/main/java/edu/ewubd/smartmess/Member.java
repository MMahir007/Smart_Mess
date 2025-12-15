package edu.ewubd.smartmess;

// Member.java
public class Member {
    private String id;
    private String name;

    public Member() {}   // Firestore er jonno empty constructor

    public Member(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;    // Spinner e name dekhabe
    }
}
