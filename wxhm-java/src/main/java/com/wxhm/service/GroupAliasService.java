package com.wxhm.service;

import com.wxhm.entity.GroupAlias;
import com.wxhm.repository.GroupAliasRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GroupAliasService {
    private static final String SHORT_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final int DEFAULT_SHORT_LENGTH = 6;

    private final GroupAliasRepository repository;
    private final QrService qrService;

    public GroupAliasService(GroupAliasRepository repository, QrService qrService) {
        this.repository = repository;
        this.qrService = qrService;
    }

    public String resolveGroupName(String input) {
        if (input == null || input.isBlank()) return input;
        if (qrService.groupExists(input)) return input;
        String normalized = normalizeShortName(input);
        return repository.findByShortName(normalized).map(GroupAlias::getGroupName).orElse(input);
    }

    public String getShortName(String groupName) {
        return repository.findByGroupName(groupName).map(GroupAlias::getShortName).orElse("");
    }

    public String ensureShortName(String groupName) {
        Optional<GroupAlias> exist = repository.findByGroupName(groupName);
        if (exist.isPresent() && exist.get().getShortName() != null && !exist.get().getShortName().isBlank()) {
            return exist.get().getShortName();
        }
        String generated = generateUniqueShortName();
        GroupAlias alias = exist.orElseGet(GroupAlias::new);
        alias.setGroupName(groupName);
        alias.setShortName(generated);
        repository.save(alias);
        return generated;
    }

    public Map<String, String> getShortNameMap(List<String> groupNames) {
        Map<String, String> map = new HashMap<>();
        for (GroupAlias a : repository.findAllByGroupNameIn(groupNames)) {
            map.put(a.getGroupName(), a.getShortName());
        }
        return map;
    }

    public void setShortName(String groupName, String shortName) {
        String normalized = normalizeShortName(shortName);
        if (normalized.isBlank()) {
            ensureShortName(groupName);
            return;
        }
        validateShortName(normalized);
        Optional<GroupAlias> occupied = repository.findByShortName(normalized);
        if (occupied.isPresent() && !occupied.get().getGroupName().equals(groupName)) {
            throw new IllegalArgumentException("短链名已被其他群使用");
        }
        GroupAlias alias = repository.findByGroupName(groupName).orElseGet(GroupAlias::new);
        alias.setGroupName(groupName);
        alias.setShortName(normalized);
        repository.save(alias);
    }

    public void onGroupRenamed(String oldName, String newName) {
        repository.findByGroupName(oldName).ifPresent(alias -> {
            alias.setGroupName(newName);
            repository.save(alias);
        });
    }

    public void onGroupDeleted(String groupName) {
        repository.deleteByGroupName(groupName);
    }

    private static void validateShortName(String shortName) {
        if (shortName.length() < 2 || shortName.length() > 32 || !shortName.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("短链名仅支持 2-32 位小写字母、数字、-、_");
        }
    }

    private static String normalizeShortName(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private String generateUniqueShortName() {
        for (int i = 0; i < 50; i++) {
            String candidate = randomShortName(DEFAULT_SHORT_LENGTH);
            if (repository.findByShortName(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("短链名生成失败，请重试");
    }

    private static String randomShortName(int len) {
        StringBuilder sb = new StringBuilder(len);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < len; i++) {
            sb.append(SHORT_ALPHABET.charAt(r.nextInt(SHORT_ALPHABET.length())));
        }
        return sb.toString();
    }
}
