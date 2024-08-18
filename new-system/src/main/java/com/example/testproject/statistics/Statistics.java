package com.example.testproject.statistics;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
@NoArgsConstructor
@Component
public class Statistics {
    private final AtomicInteger newPatients = new AtomicInteger(0);
    private final AtomicInteger newClients = new AtomicInteger(0);
    private final AtomicInteger invalidClientData = new AtomicInteger(0);
    private final AtomicInteger newUsers = new AtomicInteger(0);
    private final AtomicInteger invalidComments = new AtomicInteger(0);
    private final AtomicInteger newNotes = new AtomicInteger(0);
    private final AtomicInteger updatedNotes = new AtomicInteger(0);

    public void addNewPatients(int newPatients) {
        this.newPatients.getAndAdd(newPatients);
    }

    public void addNewClients(int newClients) {
        this.newClients.getAndAdd(newClients);
    }

    public void addInvalidClientData(int invalidClientData) {
        this.invalidClientData.getAndAdd(invalidClientData);
    }

    public void addNewUsers(int newUsers) {
        this.newUsers.getAndAdd(newUsers);
    }

    public void addInvalidComments(int invalidComments) {
        this.invalidComments.getAndAdd(invalidComments);
    }

    public void addNewNotes(int newNotes) {
        this.newNotes.getAndAdd(newNotes);
    }

    public void addUpdatedNotes(int updatedNotes) {
        this.updatedNotes.getAndAdd(updatedNotes);
    }

    public void reset() {
        newPatients.set(0);
        newClients.set(0);
        invalidClientData.set(0);
        newUsers.set(0);
        invalidComments.set(0);
        newNotes.set(0);
        updatedNotes.set(0);
    }

    public void printStatistics() {
        System.out.println("---------------- STATISTICS ----------------");
        System.out.println("New patients: " + newPatients);
        System.out.println("New clients: " + newClients);
        System.out.println("Invalid clients: " + invalidClientData);
        System.out.println("New users: " + newUsers);
        System.out.println("Invalid comments: " + invalidComments);
        System.out.println("New notes: " + newNotes);
        System.out.println("Updated notes: " + updatedNotes);
        System.out.println("--------------------------------------------");
    }
}
