package br.com.corely.dev.seed;

public class SeedResponse {
    private int students;
    private int classGroups;
    private int sessions;
    private int attendances;
    private int makeupRequests;

    public SeedResponse() {}

    public SeedResponse(int students, int classGroups, int sessions, int attendances, int makeupRequests) {
        this.students = students;
        this.classGroups = classGroups;
        this.sessions = sessions;
        this.attendances = attendances;
        this.makeupRequests = makeupRequests;
    }

    public int getStudents() { return students; }
    public void setStudents(int students) { this.students = students; }
    public int getClassGroups() { return classGroups; }
    public void setClassGroups(int classGroups) { this.classGroups = classGroups; }
    public int getSessions() { return sessions; }
    public void setSessions(int sessions) { this.sessions = sessions; }
    public int getAttendances() { return attendances; }
    public void setAttendances(int attendances) { this.attendances = attendances; }
    public int getMakeupRequests() { return makeupRequests; }
    public void setMakeupRequests(int makeupRequests) { this.makeupRequests = makeupRequests; }
}
