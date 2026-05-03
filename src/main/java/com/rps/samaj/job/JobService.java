package com.rps.samaj.job;

import com.rps.samaj.api.dto.JobDtos;
import com.rps.samaj.config.cache.RedisCacheConfig;
import com.rps.samaj.notification.AppNotification;
import com.rps.samaj.notification.FcmService;
import com.rps.samaj.notification.NotificationBatchWriter;
import com.rps.samaj.notification.NotificationFanoutAsync;
import com.rps.samaj.user.model.User;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class JobService {

    private final JobListingRepository jobRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationBatchWriter notificationBatchWriter;
    private final NotificationFanoutAsync fanoutAsync;
    private final FcmService fcmService;

    public JobService(
            JobListingRepository jobRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            NotificationBatchWriter notificationBatchWriter,
            NotificationFanoutAsync fanoutAsync,
            FcmService fcmService
    ) {
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.notificationBatchWriter = notificationBatchWriter;
        this.fanoutAsync = fanoutAsync;
        this.fcmService = fcmService;
    }

    // ---- User-facing ----

    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.Names.JOB_LISTINGS, allEntries = true)
    public JobDtos.JobDetail submitJob(UUID userId, JobDtos.JobCreateRequest req) {
        User user = requireUser(userId);
        JobListing job = new JobListing(UUID.randomUUID(), user, false, req.title().trim(), req.company().trim(), req.description().trim());
        applyFields(job, req);
        jobRepository.save(job);
        return toDetail(job, userId);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail updateOwn(UUID userId, UUID jobId, JobDtos.JobUpdateRequest req) {
        JobListing job = requireJob(jobId);
        if (job.getPostedBy() == null || !job.getPostedBy().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        if (job.getStatus() == JobStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Approved jobs cannot be edited directly; contact admin");
        }
        job.setStatus(JobStatus.PENDING);
        job.setRejectionReason(null);
        applyUpdateFields(job, req);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return toDetail(job, userId);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public void deleteOwn(UUID userId, UUID jobId) {
        JobListing job = requireJob(jobId);
        if (job.getPostedBy() == null || !job.getPostedBy().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        jobRepository.delete(job);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.Names.JOB_LISTINGS,
            key = "T(String).valueOf(#page) + ':' + T(String).valueOf(#size) + ':' + (#category != null ? #category : '') + ':' + (#jobType != null ? #jobType : '')")
    public JobDtos.JobPageResponse listApproved(String category, String jobType, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<JobListing> pg;
        boolean hasCategory = category != null && !category.isBlank();
        boolean hasType = jobType != null && !jobType.isBlank();
        if (hasCategory && hasType) {
            pg = jobRepository.findByStatusAndCategoryIgnoreCaseAndJobTypeIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
                    JobStatus.APPROVED, category.trim(), jobType.trim(), p);
        } else if (hasCategory) {
            pg = jobRepository.findByStatusAndCategoryIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
                    JobStatus.APPROVED, category.trim(), p);
        } else if (hasType) {
            pg = jobRepository.findByStatusAndJobTypeIgnoreCaseOrderByFeaturedDescCreatedAtDesc(
                    JobStatus.APPROVED, jobType.trim(), p);
        } else {
            pg = jobRepository.findByStatusOrderByFeaturedDescCreatedAtDesc(JobStatus.APPROVED, p);
        }
        return toPage(pg);
    }

    @Transactional
    public JobDtos.JobDetail getAndTrack(UUID jobId) {
        JobListing job = requireJob(jobId);
        if (job.getStatus() != JobStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        job.setViewCount(job.getViewCount() + 1);
        jobRepository.save(job);
        return toDetail(job, null);
    }

    @Transactional(readOnly = true)
    public JobDtos.JobDetail getMine(UUID userId, UUID jobId) {
        JobListing job = requireJob(jobId);
        if (job.getPostedBy() == null || !job.getPostedBy().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your listing");
        }
        return toDetail(job, userId);
    }

    @Transactional(readOnly = true)
    public JobDtos.JobPageResponse listMine(UUID userId, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        Page<JobListing> pg = jobRepository.findByPostedBy_IdOrderByCreatedAtDesc(userId, p);
        return toPage(pg);
    }

    // ---- Admin ----

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail adminCreate(JobDtos.JobCreateRequest req) {
        JobListing job = new JobListing(UUID.randomUUID(), null, true, req.title().trim(), req.company().trim(), req.description().trim());
        applyFields(job, req);
        jobRepository.save(job);
        fanoutAsync.fanOut(
                "New Job: " + job.getTitle(),
                job.getCompany() + (job.getLocation() != null ? " · " + job.getLocation() : ""),
                "JOB",
                "/jobs/" + job.getId(),
                null
        );
        fcmService.sendToTopic("general", "New Job Opening", job.getTitle() + " at " + job.getCompany(), "/jobs/" + job.getId(), "JOB");
        return toDetail(job, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail adminUpdate(UUID jobId, JobDtos.JobUpdateRequest req) {
        JobListing job = requireJob(jobId);
        applyUpdateFields(job, req);
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return toDetail(job, null);
    }

    @Transactional(readOnly = true)
    public JobDtos.JobAdminPageResponse adminList(String statusFilter, int page, int size) {
        Pageable p = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        Page<JobListing> pg;
        if (statusFilter == null || statusFilter.isBlank()) {
            pg = jobRepository.findAllByOrderByCreatedAtDesc(p);
        } else {
            JobStatus st = parseStatus(statusFilter);
            pg = jobRepository.findByStatusOrderByCreatedAtDesc(st, p);
        }
        List<JobDtos.JobAdminSummary> content = pg.getContent().stream().map(this::toAdminSummary).toList();
        return new JobDtos.JobAdminPageResponse(content, pg.getTotalElements(), pg.getTotalPages(), pg.getNumber(), pg.getSize());
    }

    @Transactional(readOnly = true)
    public JobDtos.JobDetail adminGet(UUID jobId) {
        return toDetail(requireJob(jobId), null);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail adminApprove(UUID jobId, JobDtos.AdminApproveRequest req) {
        JobListing job = requireJob(jobId);
        job.setStatus(JobStatus.APPROVED);
        job.setRejectionReason(null);
        if (req != null && req.featured() != null) job.setFeatured(req.featured());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        if (!job.isPostedByAdmin() && job.getPostedBy() != null) {
            notifyUser(job.getPostedBy(), "Job Listing Approved",
                    "Your job post \"" + job.getTitle() + "\" at " + job.getCompany() + " has been approved.",
                    "JOB_APPROVED", "/jobs/" + job.getId());
        }
        fanoutAsync.fanOut(
                "New Job: " + job.getTitle(),
                job.getCompany() + (job.getLocation() != null ? " · " + job.getLocation() : ""),
                "JOB",
                "/jobs/" + job.getId(),
                job.getPostedBy() != null ? job.getPostedBy().getId() : null
        );
        fcmService.sendToTopic("general", "New Job Opening", job.getTitle() + " at " + job.getCompany(), "/jobs/" + job.getId(), "JOB");
        return toDetail(job, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail adminReject(UUID jobId, JobDtos.AdminRejectRequest req) {
        JobListing job = requireJob(jobId);
        job.setStatus(JobStatus.REJECTED);
        job.setRejectionReason(req.reason().trim());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        if (!job.isPostedByAdmin() && job.getPostedBy() != null) {
            notifyUser(job.getPostedBy(), "Job Listing Not Approved",
                    "Your job post \"" + job.getTitle() + "\" was not approved. Reason: " + req.reason().trim(),
                    "JOB_REJECTED", "/jobs/my");
        }
        return toDetail(job, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public JobDtos.JobDetail adminToggleFeatured(UUID jobId) {
        JobListing job = requireJob(jobId);
        job.setFeatured(!job.isFeatured());
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);
        return toDetail(job, null);
    }

    @Transactional
    @CacheEvict(cacheNames = {RedisCacheConfig.Names.JOB_LISTINGS, RedisCacheConfig.Names.JOB_DETAIL}, allEntries = true)
    public void adminDelete(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        jobRepository.deleteById(jobId);
    }

    // ---- Helpers ----

    private void applyFields(JobListing job, JobDtos.JobCreateRequest req) {
        job.setTitle(req.title().trim());
        job.setCompany(req.company().trim());
        job.setDescription(req.description().trim());
        job.setLocation(nullTrim(req.location()));
        job.setJobType(nullTrim(req.jobType()));
        job.setCategory(nullTrim(req.category()));
        job.setRequirements(nullTrim(req.requirements()));
        job.setSalaryMin(req.salaryMin());
        job.setSalaryMax(req.salaryMax());
        job.setApplyUrl(nullTrim(req.applyUrl()));
        job.setContactEmail(nullTrim(req.contactEmail()));
        job.setContactPhone(nullTrim(req.contactPhone()));
        job.setDeadline(parseDeadline(req.deadline()));
    }

    private void applyUpdateFields(JobListing job, JobDtos.JobUpdateRequest req) {
        job.setTitle(req.title().trim());
        job.setCompany(req.company().trim());
        job.setDescription(req.description().trim());
        job.setLocation(nullTrim(req.location()));
        job.setJobType(nullTrim(req.jobType()));
        job.setCategory(nullTrim(req.category()));
        job.setRequirements(nullTrim(req.requirements()));
        job.setSalaryMin(req.salaryMin());
        job.setSalaryMax(req.salaryMax());
        job.setApplyUrl(nullTrim(req.applyUrl()));
        job.setContactEmail(nullTrim(req.contactEmail()));
        job.setContactPhone(nullTrim(req.contactPhone()));
        job.setDeadline(parseDeadline(req.deadline()));
    }

    private static String nullTrim(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private static Instant parseDeadline(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Instant.parse(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void notifyUser(User user, String title, String body, String type, String link) {
        try {
            AppNotification n = new AppNotification(UUID.randomUUID(), user, title, body, type);
            n.setLink(link);
            notificationBatchWriter.saveAllInNewTx(List.of(n));
        } catch (Exception ignored) {
        }
    }

    private JobDtos.JobPageResponse toPage(Page<JobListing> pg) {
        List<JobDtos.JobSummary> rows = pg.getContent().stream().map(this::toSummary).toList();
        return new JobDtos.JobPageResponse(rows, pg.getTotalPages(), pg.getTotalElements(), pg.getSize(), pg.getNumber(), pg.isFirst(), pg.isLast());
    }

    private JobDtos.JobSummary toSummary(JobListing j) {
        return new JobDtos.JobSummary(
                j.getId().toString(),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getJobType(),
                j.getCategory(),
                j.getSalaryMin(),
                j.getSalaryMax(),
                j.getStatus().name(),
                j.isFeatured(),
                j.isPostedByAdmin(),
                j.getDeadline() != null ? j.getDeadline().toString() : null,
                j.getViewCount(),
                j.getCreatedAt().toString()
        );
    }

    private JobDtos.JobDetail toDetail(JobListing j, UUID viewerUserId) {
        String submittedById = null;
        String submittedByName = null;
        if (j.getPostedBy() != null) {
            submittedById = j.getPostedBy().getId().toString();
            UserProfile p = userProfileRepository.findById(j.getPostedBy().getId()).orElse(null);
            submittedByName = resolveDisplayName(j.getPostedBy(), p);
        }
        boolean isOwner = viewerUserId != null && j.getPostedBy() != null && viewerUserId.equals(j.getPostedBy().getId());
        return new JobDtos.JobDetail(
                j.getId().toString(),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getJobType(),
                j.getCategory(),
                j.getDescription(),
                j.getRequirements(),
                j.getSalaryMin(),
                j.getSalaryMax(),
                j.getApplyUrl(),
                j.getContactEmail(),
                j.getContactPhone(),
                j.getStatus().name(),
                j.getRejectionReason(),
                j.isFeatured(),
                j.isPostedByAdmin(),
                submittedById,
                submittedByName,
                j.getDeadline() != null ? j.getDeadline().toString() : null,
                j.getViewCount(),
                isOwner,
                j.getCreatedAt().toString(),
                j.getUpdatedAt().toString()
        );
    }

    private JobDtos.JobAdminSummary toAdminSummary(JobListing j) {
        String submittedById = null;
        String submittedByName = null;
        String submittedByEmail = null;
        if (j.getPostedBy() != null) {
            submittedById = j.getPostedBy().getId().toString();
            submittedByEmail = j.getPostedBy().getEmail();
            UserProfile p = userProfileRepository.findById(j.getPostedBy().getId()).orElse(null);
            submittedByName = p != null && p.getFullName() != null ? p.getFullName() : "";
        }
        return new JobDtos.JobAdminSummary(
                j.getId().toString(),
                j.getTitle(),
                j.getCompany(),
                j.getLocation(),
                j.getJobType(),
                j.getCategory(),
                j.getStatus().name(),
                j.isPostedByAdmin(),
                j.isFeatured(),
                submittedById,
                submittedByName,
                submittedByEmail,
                j.getDeadline() != null ? j.getDeadline().toString() : null,
                j.getCreatedAt().toString(),
                j.getUpdatedAt().toString()
        );
    }

    private static String resolveDisplayName(User u, UserProfile p) {
        if (p != null && p.getFullName() != null && !p.getFullName().isBlank()) return p.getFullName();
        return u.getEmail() != null ? u.getEmail() : "Member";
    }

    private JobListing requireJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static JobStatus parseStatus(String s) {
        try {
            return JobStatus.valueOf(s.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + s);
        }
    }
}
