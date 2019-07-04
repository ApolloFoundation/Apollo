/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static java.util.stream.Collectors.groupingBy;

import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ShufflingCache {
    private Map<Long, List<Shuffling>> allShufflings = new ConcurrentHashMap<>();

    public void putAll(List<Shuffling> shufflings) {
        allShufflings.putAll(shufflings.stream().collect(groupingBy(Shuffling::getId, Collectors.collectingAndThen(Collectors.toList(), l -> l.stream().sorted(Comparator.comparing(Shuffling::getHeight)).collect(Collectors.toList())))));
    }

    public void clear() {
        allShufflings.clear();
    }

    public void rollback(int height) {
        allShufflings.values().forEach(l-> l.removeIf(s->s.getHeight() > height));
        allShufflings.entrySet().forEach((entry)-> {
            if (entry.getValue().size() == 0) {
                allShufflings.remove(entry.getKey());
            }
        });
        allShufflings.values().forEach(l-> l.get(l.size() - 1).setLatest(true));
    }

    public Shuffling getById(long id) {
        List<Shuffling> shufflings = allShufflings.get(id);

        return shufflings == null ? null : shufflings.get(shufflings.size() - 1);
    }

    public Shuffling getCopyById(long id) {
        List<Shuffling> shufflings = allShufflings.get(id);
        if (shufflings != null) {
            Shuffling shuffling = shufflings.get(shufflings.size() - 1);
            return shuffling.deepCopy();
        } else {
            return null;
        }
    }

    public void put(Shuffling shuffling) {
        List<Shuffling> existingShufflings = allShufflings.get(shuffling.getId());
        shuffling.setLatest(true);
        if (existingShufflings == null) {
            List<Shuffling> shufflings = new ArrayList<>();
            shufflings.add(shuffling);
            allShufflings.put(shuffling.getId(), shufflings);
        } else {
            int lastPosition = existingShufflings.size() - 1; // assume that existing list of shufflings has min size 1
            Shuffling lastShuffling = existingShufflings.get(lastPosition);
            if (lastShuffling.getHeight() == shuffling.getHeight()) {
                existingShufflings.set(lastPosition, shuffling);
            } else {
                lastShuffling.setLatest(false);
                existingShufflings.add(shuffling);
            }
        }
    }

    public void delete(Shuffling shuffling) {
        List<Shuffling> existingShufflings = allShufflings.get(shuffling.getId());
        shuffling.setLatest(false);
        if (existingShufflings != null) {
            int lastPosition = existingShufflings.size() - 1; // assume that existing list of shufflings has min size 1
            Shuffling existingShuffling = existingShufflings.get(lastPosition);
            existingShuffling.setLatest(false);
            existingShufflings.add(shuffling);
        }
    }

    public void trim(int height) {
        allShufflings.forEach((id, l)-> {
            List<Shuffling> trimCandidates = l.stream().filter(s -> s.getHeight() < height).sorted(Comparator.comparing(Shuffling::getHeight)).collect(Collectors.toList());
            if (trimCandidates.size() > 1) {
                for (int i = 0; i < trimCandidates.size() - 1; i++) {
                    l.remove(trimCandidates.get(i));
                }
            }
            boolean delete = l.stream().noneMatch(s -> s.getHeight() >= height);
            if (delete) {
                List<Shuffling> deleteCandidates = l.stream().filter(s -> s.getHeight() < height && !s.isLatest()).sorted(Comparator.comparing(Shuffling::getHeight)).collect(Collectors.toList());
                for (Shuffling deleteCandidate : deleteCandidates) {
                    l.remove(deleteCandidate);
                }
            }
        });
    }

    public List<Shuffling> getActiveShufflings() {
        return  allShufflings.values()
                .stream()
                .map(l -> l.get(l.size() - 1))
                .filter(s -> s.getBlocksRemaining() != 0)
                .sorted(Comparator.comparing(Shuffling::getBlocksRemaining)
                        .thenComparing(Comparator.comparing(Shuffling::getHeight).reversed()))
                .map(Shuffling::deepCopy)
                .collect(Collectors.toList());
    }
}
