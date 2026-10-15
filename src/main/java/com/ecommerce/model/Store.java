package com.ecommerce.model;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/** All application data. Persisted to disk with plain Java serialization. */
public class Store implements Serializable {
    private static final long serialVersionUID = 1L;

    private final java.util.Map<String, Long> lastIds = new java.util.HashMap<>();
    public final List<User> users = new ArrayList<>();
    public final List<Product> products = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    public final List<WishlistItem> wishlist = new ArrayList<>();
    public final List<ViewRecord> views = new ArrayList<>();
    public final List<LogEntry> log = new ArrayList<>();

    /** Separate id sequence per kind ("user", "product", "order", "log"), so ids stay short and readable. */
    public long nextId(String kind) {
        return lastIds.merge(kind, 1L, Long::sum);
    }

    public static Store load(Path file) {
        if (file != null && Files.exists(file)) {
            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(file))) {
                return (Store) in.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Could not read " + file + " (" + e.getMessage() + "); starting with empty data.");
            }
        }
        return new Store();
    }

    public void save(Path file) {
        if (file == null) return;
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tmp))) {
                out.writeObject(this);
            }
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Warning: could not save data: " + e.getMessage());
        }
    }
}
