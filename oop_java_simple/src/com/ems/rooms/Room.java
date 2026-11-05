package com.ems.rooms;

public class Room {
    private int id;
    private String name;
    private int capacity;
    private int currentOccupancy;
    private int eventId;

    public Room(int id, String name, int capacity, int eventId) {
        this.id = id;
        this.name = name;
        setCapacity(capacity);
        this.currentOccupancy = 0;
        this.eventId = eventId;
    }

    public boolean enter() {
        if (isFull()) {
            return false;
        }
        this.currentOccupancy++;
        return true;
    }

    public boolean exit() {
        if (this.currentOccupancy <= 0) {
            return false;
        }
        this.currentOccupancy--;
        return true;
    }

    public boolean isFull() {
        return this.currentOccupancy >= this.capacity;
    }

    public int getRemainingCapacity() {
        return this.capacity - this.currentOccupancy;
    }

    public double getOccupancyPercentage() {
        if (this.capacity == 0) return 0.0;
        return ((double) this.currentOccupancy / this.capacity) * 100;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { 
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive.");
        this.capacity = capacity; 
    }
    public int getCurrentOccupancy() { return currentOccupancy; }
    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }
}
