package br.com.stickerswap.moderation.infrastructure.persistence;

import br.com.stickerswap.moderation.domain.model.ReportStatus;
import br.com.stickerswap.moderation.domain.model.UserReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserReportRepository extends JpaRepository<UserReport, UUID> {

    Page<UserReport> findByStatus(ReportStatus status, Pageable pageable);
}
