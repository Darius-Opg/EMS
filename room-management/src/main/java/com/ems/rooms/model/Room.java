package com.ems.rooms.model;

public class Room {
    private int id;
    private String name;
    private int capacity;
    private int currentOccupancy;
    private Integer eventId;
    private String status;

    public Room(int id, String name, int capacity, int eventId) {
        this(id, name, capacity, 0, eventId == 0 ? null : eventId,
                eventId == 0 ? "AVAILABLE" : "ASSIGNED");
    }

    public Room(int id, String name, int capacity, int currentOccupancy,
                Integer eventId, String status) {
        this.id = id;
        this.name = name;
        setCapacity(capacity);
        setCurrentOccupancy(currentOccupancy);
        this.eventId = eventId;
        this.status = status;
    }

    public boolean enter() {
        if (isFull()) return false;
        currentOccupancy++;
        return true;
    }

    public boolean exit() {
        if (currentOccupancy <= 0) return false;
        currentOccupancy--;
        return true;
    }

    public boolean isFull() { return currentOccupancy >= capacity; }
    public int getRemainingCapacity() { return Math.max(0, capacity - currentOccupancy); }
    public double getOccupancyPercentage() {
        return capacity == 0 ? 0.0 : ((double) currentOccupancy / capacity) * 100;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive.");
        this.capacity = capacity;
    }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public void setCurrentOccupancy(int currentOccupancy) {
        if (currentOccupancy < 0) throw new IllegalArgumentException("Occupancy cannot be negative.");
        this.currentOccupancy = currentOccupancy;
    }
    /** Legacy panel API: 0 displays an unassigned room whose database event_id is NULL. */
    public int getEventId() { return eventId == null ? 0 : eventId; }
    public void setEventId(int eventId) { this.eventId = eventId == 0 ? null : eventId; }
    public Integer getAssignedEventId() { return eventId; }
    public void setAssignedEventId(Integer eventId) { this.eventId = eventId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
