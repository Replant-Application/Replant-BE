package com.app.replant.domain.badge.service;

import com.app.replant.domain.badge.dto.BadgeResponse;
import com.app.replant.domain.badge.repository.UserBadgeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BadgeService {

    private final UserBadgeRepository userBadgeRepository;

    public List<BadgeResponse> getMyBadges(Long userId) {
        return userBadgeRepository.findValidBadgesByUserId(userId, LocalDateTime.now())
                .stream()
                .map(BadgeResponse::from)
                .collect(Collectors.toList());
    }

    public Page<BadgeResponse> getBadgeHistory(Long userId, Pageable pageable) {
        return userBadgeRepository.findByUserId(userId, pageable)
                .map(BadgeResponse::from);
    }
}
