package com.ecommerce.service;

import com.ecommerce.model.ActivityLog;
import com.ecommerce.repo.ActivityLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityService {
    private final ActivityLogRepository repo;

    public ActivityService(ActivityLogRepository repo) {
        this.repo = repo;
    }

    public void log(String actor, String message) {
        repo.save(new ActivityLog(actor, message));
    }

    public List<ActivityLog> latest() {
        return repo.findTop30ByOrderByIdDesc();
    }
}
