package com.ems.rooms.service;
import com.ems.rooms.model.Room;

import java.util.ArrayList;
import java.util.List;

public class RoomService {
    private List<Room> rooms = new ArrayList<>();
    private int nextId = 1;

    public RoomService() {
        // Add dummy mock data for easy initial beginner testing
        registerRoom("Main Conference Hall", 50, 101);
        registerRoom("Workshop Room A", 15, 101);
        registerRoom("Seminar Lab B", 25, 202);
    }

    public void registerRoom(String name, int capacity, int eventId) {
        Room room = new Room(nextId++, name, capacity, eventId);
        rooms.add(room);
    }

    public List<Room> getAllRooms() {
        return rooms;
    }

    public List<Room> searchRoomsByName(String name) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            if (r.getName().toLowerCase().contains(name.toLowerCase())) {
                result.add(r);
            }
        }
        return result;
    }

    public int getAssignedCapacityForEvent(int eventId) {
        int total = 0;
        for (Room r : rooms) {
            if (r.getEventId() == eventId) {
                total += r.getCapacity();
            }
        }
        return total;
    }

    // Static counts simulation for beginner simplicity (ignores complex registrations DB lookup)
    public int getRegisteredAttendeeCountForEvent(int eventId) {
        if (eventId == 101) return 45; // Below total capacity (50 + 15 = 65)
        if (eventId == 202) return 30; // Overbooked (Capacity 25)
        return 10;
    }

    public boolean handleAttendeeCheckIn(int id) {
        for (Room r : rooms) {
            if (r.getId() == id) {
                return r.enter();
            }
        }
        return false;
    }

    public boolean handleAttendeeExit(int id) {
        for (Room r : rooms) {
            if (r.getId() == id) {
                return r.exit();
            }
        }
        return false;
    }
}
