package org.fyp.minidrivestoragebe.repository;

import org.fyp.minidrivestoragebe.entity.DownloadRequest;
import org.fyp.minidrivestoragebe.enums.DownloadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DownloadRequestRepository extends JpaRepository<DownloadRequest, String> {
    
    Optional<DownloadRequest> findByRequestId(String requestId);
    
    Optional<DownloadRequest> findByRequestIdAndUserId(String requestId, String userId);
    
    long countByStatus(DownloadStatus status);
}
