package com.sgms.sgms_backend.service.timeline;

import com.sgms.sgms_backend.enums.ComplaintAction;
import com.sgms.sgms_backend.enums.ComplaintStatus;
import com.sgms.sgms_backend.model.Complaint;
import com.sgms.sgms_backend.model.ComplaintUpdate;
import com.sgms.sgms_backend.model.User;
import com.sgms.sgms_backend.repository.ComplaintUpdateRepository;
import org.springframework.stereotype.Service;

@Service
public class ComplaintTimelineService {
    private final ComplaintUpdateRepository updateRepo;

    public ComplaintTimelineService(ComplaintUpdateRepository updateRepo) {
        this.updateRepo = updateRepo;
    }

    public void createTimeline(Complaint complaint,
                               ComplaintAction action,
                               ComplaintStatus from,
                               ComplaintStatus to,
                               String note,
                               User performedBy
    ) {

        ComplaintUpdate update=new ComplaintUpdate();
        update.setComplaint(complaint);
        update.setAction(action);
        update.setFromStatus(from);
        update.setToStatus(to);
        update.setNote(note);
        update.setPerformedBy(performedBy);

        updateRepo.save(update);

    }
}
