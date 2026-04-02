package com.nexus.platform.service;

import com.nexus.platform.dto.Result;
import com.nexus.platform.dto.UserProfileDetailDto;
import com.nexus.platform.dto.WalletSummaryDto;
import com.nexus.platform.entity.User;
import com.nexus.platform.entity.UserProfile;
import com.nexus.platform.entity.WalletAccount;
import com.nexus.platform.repository.UserProfileRepository;
import com.nexus.platform.repository.UserRepository;
import com.nexus.platform.repository.WalletAccountRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final WalletAccountRepository walletAccountRepository;

    public Result<UserProfileDetailDto> getProfile(User currentUser) {
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        UserProfile profile = ensureProfile(user);
        return Result.success(toProfileDto(user, profile));
    }

    @Transactional
    public Result<UserProfileDetailDto> updateProfile(
            User currentUser,
            String displayName,
            String avatarUrl,
            String languageTag,
            String email,
            String phone
    ) {
        User user = userRepository.findById(currentUser.getId()).orElse(null);
        if (user == null) {
            return Result.error("User not found");
        }
        UserProfile profile = ensureProfile(user);

        if (displayName != null) {
            profile.setDisplayName(trimToNull(displayName, 128));
        }
        if (avatarUrl != null) {
            profile.setAvatarUrl(trimToNull(avatarUrl, 512));
        }
        if (languageTag != null && !languageTag.isBlank()) {
            profile.setLanguageTag(languageTag.trim());
        }
        if (email != null) {
            user.setEmail(trimToNull(email, 255));
        }
        if (phone != null) {
            user.setPhone(trimToNull(phone, 255));
        }

        userRepository.save(user);
        userProfileRepository.save(profile);
        return Result.success(toProfileDto(user, profile));
    }

    public Result<WalletSummaryDto> getWalletSummary(User currentUser) {
        WalletAccount wallet = ensureWallet(currentUser);
        BigDecimal available = wallet.getBalance().subtract(wallet.getFrozenBalance());
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }
        return Result.success(new WalletSummaryDto(
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                available,
                wallet.getTodayIncome(),
                wallet.getTotalIncome()
        ));
    }

    @Transactional
    protected UserProfile ensureProfile(User user) {
        return userProfileRepository.findByUserId(user.getId()).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserId(user.getId());
            profile.setDisplayName(user.getUsername());
            profile.setLanguageTag("zh-CN");
            return userProfileRepository.save(profile);
        });
    }

    @Transactional
    protected WalletAccount ensureWallet(User user) {
        return walletAccountRepository.findByUserId(user.getId()).orElseGet(() -> {
            WalletAccount wallet = new WalletAccount();
            wallet.setUserId(user.getId());
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setFrozenBalance(BigDecimal.ZERO);
            wallet.setTodayIncome(BigDecimal.ZERO);
            wallet.setTotalIncome(BigDecimal.ZERO);
            return walletAccountRepository.save(wallet);
        });
    }

    private UserProfileDetailDto toProfileDto(User user, UserProfile profile) {
        return new UserProfileDetailDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                defaultIfBlank(profile.getDisplayName(), user.getUsername()),
                profile.getAvatarUrl(),
                defaultIfBlank(profile.getLanguageTag(), "zh-CN")
        );
    }

    private String trimToNull(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLen) {
            return trimmed.substring(0, maxLen);
        }
        return trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
